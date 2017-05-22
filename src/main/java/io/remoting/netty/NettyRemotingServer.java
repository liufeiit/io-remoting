package io.remoting.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.remoting.Pair;
import io.remoting.RemotingCallback;
import io.remoting.RemotingServer;
import io.remoting.exception.RemotingSendRequestException;
import io.remoting.exception.RemotingTimeoutException;
import io.remoting.netty.codec.ProtocolDecoder;
import io.remoting.netty.codec.ProtocolEncoder;
import io.remoting.protocol.RemotingCommand;
import io.remoting.utils.RemotingThreadFactory;
import io.remoting.utils.RemotingUtils;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月19日 下午6:54:31
 */
public class NettyRemotingServer extends NettyAbstractRemotingService implements RemotingServer {
    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup eventLoopGroupSelector;
    private final EventLoopGroup eventLoopGroupBoss;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private Channel serverChannel;
    private InetSocketAddress serverAddress;
    private final NettyServerConfigurator nettyServerConfigurator;
    private final ExecutorService defaultThreadPoolExecutor;

    public NettyRemotingServer(final NettyServerConfigurator nettyServerConfigurator) {
        super();
        this.serverBootstrap = new ServerBootstrap();
        this.nettyServerConfigurator = nettyServerConfigurator;
        this.defaultThreadPoolExecutor = Executors.newFixedThreadPool(8, RemotingThreadFactory.newThreadFactory("NettyRemotingServerDefaultThreadPoolExecutor-%d", false));
        this.eventLoopGroupBoss = new NioEventLoopGroup(2, RemotingThreadFactory.newThreadFactory("NettyRemotingServerBoss-%d", false));
        int serverSelectorThreads = nettyServerConfigurator.getServerSelectorThreads();
        if (RemotingUtils.isLinuxPlatform() && nettyServerConfigurator.isUseEpollNativeSelector()) {
            this.eventLoopGroupSelector = new EpollEventLoopGroup(serverSelectorThreads, RemotingThreadFactory.newThreadFactory("NettyRemotingServerEPOLLSelector-" + serverSelectorThreads + "-%d", false));
        } else {
            this.eventLoopGroupSelector = new NioEventLoopGroup(serverSelectorThreads, RemotingThreadFactory.newThreadFactory("NettyRemotingServerNIOSelector-" + serverSelectorThreads + "-%d", false));
        }
    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyServerConfigurator.getServerWorkerThreads(), RemotingThreadFactory.newThreadFactory("NettyRemotingServerCodecThread-%d", false));
        ServerBootstrap childHandler = this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_SNDBUF, this.nettyServerConfigurator.getServerSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, this.nettyServerConfigurator.getServerSocketRcvBufSize())
                .localAddress(new InetSocketAddress(this.nettyServerConfigurator.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(defaultEventExecutorGroup, new ProtocolEncoder(), new ProtocolDecoder(),
                                new IdleStateHandler(0, 0, nettyServerConfigurator.getServerChannelMaxIdleTimeSeconds()), 
                                new NettyConnectManageHandler(), new NettyServerHandler());
                    }
                });
        if (nettyServerConfigurator.isServerPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }
        try {
            ChannelFuture sync = this.serverBootstrap.bind().sync();
            this.serverChannel = sync.channel();
            this.serverAddress = (InetSocketAddress) this.serverChannel.localAddress();
        } catch (InterruptedException ex) {
            throw new RuntimeException("ServerBootstrap.bind().sync() InterruptedException", ex);
        }
    }

    @Override
    public void registerProcessor(int commandCode, NettyCommandProcessor processor, ExecutorService executor) {
        ExecutorService executorPair = (executor == null) ? this.defaultThreadPoolExecutor : executor;
        Pair<NettyCommandProcessor, ExecutorService> pair = new Pair<NettyCommandProcessor, ExecutorService>(processor, executorPair);
        this.processors.put(commandCode, pair);
    }

    @Override
    public void registerDefaultProcessor(NettyCommandProcessor processor, ExecutorService executor) {
        this.defaultCommandProcessor = new Pair<NettyCommandProcessor, ExecutorService>(processor, executor);
    }

    @Override
    public Channel getServerChannel() {
        return this.serverChannel;
    }

    @Override
    public InetSocketAddress getServerAddress() {
        return this.serverAddress;
    }

    @Override
    public Pair<NettyCommandProcessor, ExecutorService> getProcessorPair(int commandCode) {
        return this.processors.get(commandCode);
    }

    @Override
    public RemotingCommand invokeSync(Channel channel, RemotingCommand request, long timeoutMillis) throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException {
        return this.__invokeSync(channel, request, timeoutMillis);
    }

    @Override
    public void invokeAsync(Channel channel, RemotingCommand request, long timeoutMillis, RemotingCallback callback) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException {
        this.__invokeAsync(channel, request, timeoutMillis, callback);
    }

    @Override
    public void invokeOneway(Channel channel, RemotingCommand request) throws InterruptedException, RemotingSendRequestException {
        this.__invokeOneway(channel, request);
    }

    @Override
    public void shutdown() {
        try {
            this.eventLoopGroupBoss.shutdownGracefully();
            this.eventLoopGroupSelector.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            log.error("NettyRemotingServer shutdown Error.", e);
        }
        if (this.defaultThreadPoolExecutor != null) {
            try {
                this.defaultThreadPoolExecutor.shutdown();
            } catch (Throwable e) {
                log.error("NettyRemotingServer shutdown Error.", e);
            }
        }
    }

    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingCommand> {
        protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand command) throws Exception {
            commandProcessor(ctx, command);
        }
    }

    class NettyConnectManageHandler extends ChannelDuplexHandler {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    RemotingUtils.closeChannel(ctx.channel());
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = RemotingUtils.parseChannelRemoteAddr(ctx.channel());
            log.warn("NettyRemotingServer pipeline: exceptionCaught {}", remoteAddress);
            log.warn("NettyRemotingServer pipeline: exceptionCaught Error.", cause);
            RemotingUtils.closeChannel(ctx.channel());
        }
    }
}
