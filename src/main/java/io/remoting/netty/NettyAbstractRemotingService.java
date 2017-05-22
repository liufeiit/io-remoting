package io.remoting.netty;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.remoting.Pair;
import io.remoting.RemotingCallback;
import io.remoting.ReplyFuture;
import io.remoting.exception.RemotingSendRequestException;
import io.remoting.exception.RemotingTimeoutException;
import io.remoting.protocol.CommandCode;
import io.remoting.protocol.RemotingCommand;
import io.remoting.utils.RemotingThreadFactory;
import io.remoting.utils.RemotingUtils;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月19日 下午8:45:47
 */
public abstract class NettyAbstractRemotingService {
    protected final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());
    protected final ConcurrentHashMap<Long, ReplyFuture> replies;
    protected final HashMap<Integer, Pair<NettyCommandProcessor, ExecutorService>> processors;
    protected Pair<NettyCommandProcessor, ExecutorService> defaultCommandProcessor;
    
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public NettyAbstractRemotingService() {
        super();
        replies = new ConcurrentHashMap<Long, ReplyFuture>(256);
        processors = new HashMap<Integer, Pair<NettyCommandProcessor, ExecutorService>>(64);
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(2, RemotingThreadFactory.newThreadFactory("NettyRemotingReply-%d", false), new CallerRunsPolicy() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                log.info("rejectedExecution Thread resource work out.");
                super.rejectedExecution(r, e);
            }
        });
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                final List<ReplyFuture> replyFutures = new LinkedList<ReplyFuture>();
                Iterator<Entry<Long, ReplyFuture>> it = replies.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<Long, ReplyFuture> next = it.next();
                    ReplyFuture replyFuture = next.getValue();
                    if (replyFuture.isTimeout(replyFuture.getTimeoutMillis() + 1000L)) {
                        it.remove();
                        replyFutures.add(replyFuture);
                        log.warn("remove timeout command " + replyFuture);
                    }
                }

                for (ReplyFuture replyFuture : replyFutures) {
                    if (!replyFuture.isAsyncCallback()) {
                        continue;
                    }
                    try {
                        replyFuture.invokeCallback();
                    } catch (Throwable e) {
                        log.warn("invoke ReplyFuture callback Error.", e);
                    }
                }
            }
        }, 5l, 1l, TimeUnit.SECONDS);
    }

    protected void commandProcessor(ChannelHandlerContext ctx, RemotingCommand command) throws Exception {
        if (command == null) {
            return;
        }
        final RemotingCommand cmd = command;
        if (cmd.isReply()) {
            replyCommand(ctx, cmd);
            return;
        }
        receivedCommand(ctx, cmd);
    }

    private void receivedCommand(final ChannelHandlerContext ctx, final RemotingCommand cmd) {
        final Pair<NettyCommandProcessor, ExecutorService> commandProcessor = this.processors.get(cmd.getCode());
        final Pair<NettyCommandProcessor, ExecutorService> pair = (null == commandProcessor) ? this.defaultCommandProcessor : commandProcessor;
        final long opaque = cmd.getOpaque();
        if (pair == null) {
            String message = " command code " + cmd.getCode() + " not supported";
            final RemotingCommand response = RemotingCommand.replyCommand(cmd, CommandCode.REQUEST_CODE_NOT_SUPPORTED, message);
            ctx.writeAndFlush(response);
            log.error(RemotingUtils.parseChannelRemoteAddr(ctx.channel()) + message);
            return;
        }
        if (pair.getKey().reject()) {
            final RemotingCommand response = RemotingCommand.replyCommand(cmd, CommandCode.SYSTEM_BUSY, "system busy, start flow control for a while");
            ctx.writeAndFlush(response);
            return;
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    final RemotingCommand response = pair.getKey().processCommand(ctx, cmd);
                    if (!cmd.isOneway()) {
                        if (response != null) {
                            response.setOpaque(opaque);
                            response.setReply();
                            try {
                                ctx.writeAndFlush(response);
                            } catch (Throwable e) {
                                log.error("process command over, but reply Error.", e);
                            }
                        }
                    }
                } catch (Throwable e) {
                    log.error("process command Error.", e);
                    if (!cmd.isOneway()) {
                        final RemotingCommand response = RemotingCommand.replyCommand(cmd, CommandCode.SYSTEM_ERROR, RemotingUtils.exceptionToString(e));
                        ctx.writeAndFlush(response);
                    }
                }
            }
        };
        try {
            pair.getValue().submit(task);
        } catch (RejectedExecutionException e) {
            log.warn(RemotingUtils.parseChannelRemoteAddr(ctx.channel()) + ", too many command and system thread pool busy, RejectedExecutionException " + pair.getValue().toString()
                    + " command code: " + cmd.getCode());
            if (!cmd.isOneway()) {
                final RemotingCommand response = RemotingCommand.replyCommand(cmd, CommandCode.SYSTEM_BUSY, "system busy, start flow control for a while");
                ctx.writeAndFlush(response);
            }
        }
    }

    protected void replyCommand(ChannelHandlerContext ctx, RemotingCommand cmd) {
        final long opaque = cmd.getOpaque();
        final ReplyFuture replyFuture = replies.get(opaque);
        if (replyFuture == null) {
            log.warn("receive command, but not matched any command, " + RemotingUtils.parseChannelRemoteAddr(ctx.channel()));
            return;
        }
        replyFuture.setResponse(cmd);
        replies.remove(opaque);
        if (replyFuture.isAsyncCallback()) {
            replyFuture.invokeCallback();
        } else {
            replyFuture.setReply(cmd);
        }
    }

    protected RemotingCommand __invokeSync(final Channel channel, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        final long opaque = request.getOpaque();
        try {
            final ReplyFuture replyFuture = new ReplyFuture(opaque, timeoutMillis);
            this.replies.put(opaque, replyFuture);
            final SocketAddress addr = channel.remoteAddress();
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        replyFuture.setSent(true);
                        return;
                    }
                    replyFuture.setSent(false);
                    replies.remove(opaque);
                    replyFuture.setCause(channelFuture.cause());
                    replyFuture.setReply(null);
                    log.warn("send command to channel <" + addr + "> Error.");
                }
            });
            RemotingCommand responseCommand = replyFuture.waitFor(timeoutMillis);
            if (null == responseCommand) {
                if (replyFuture.isSent()) {
                    throw new RemotingTimeoutException(RemotingUtils.parseSocketAddressAddr(addr), timeoutMillis, replyFuture.getCause());
                }
                throw new RemotingSendRequestException(RemotingUtils.parseSocketAddressAddr(addr), replyFuture.getCause());
            }

            return responseCommand;
        } finally {
            this.replies.remove(opaque);
        }
    }

    protected void __invokeAsync(final Channel channel, final RemotingCommand request, final long timeoutMillis, final RemotingCallback callback) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException {
        try {
            final long opaque = request.getOpaque();
            final ReplyFuture replyFuture = new ReplyFuture(opaque, timeoutMillis);
            replyFuture.setCallback(callback);
            this.replies.put(opaque, replyFuture);
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        replyFuture.setSent(true);
                        return;
                    }
                    replyFuture.setSent(false);
                    replyFuture.setReply(null);
                    replies.remove(opaque);
                    log.warn("send command to channel <{}> Error.", RemotingUtils.parseChannelRemoteAddr(channel));
                }
            });
        } catch (Throwable e) {
            log.warn("send command to channel <" + RemotingUtils.parseChannelRemoteAddr(channel) + "> Error.", e);
            throw new RemotingSendRequestException(RemotingUtils.parseChannelRemoteAddr(channel), e);
        }
    }

    protected void __invokeOneway(final Channel channel, final RemotingCommand request) throws InterruptedException, RemotingSendRequestException {
        try {
            request.setOneway();
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (!channelFuture.isSuccess()) {
                        log.warn("send command to channel <" + channel.remoteAddress() + "> Error.");
                    }
                }
            });
        } catch (Throwable e) {
            log.warn("write send command to channel <" + channel.remoteAddress() + "> Error.");
            throw new RemotingSendRequestException(RemotingUtils.parseChannelRemoteAddr(channel), e);
        }
    }
}
