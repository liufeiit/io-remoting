package io.remoting.netty.codec;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.remoting.protocol.RemotingCommand;
import io.remoting.utils.RemotingUtils;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月18日 上午11:57:58
 */
public class ProtocolEncoder extends MessageToByteEncoder<RemotingCommand> {
    
    private static final Logger log = LoggerFactory.getLogger(ProtocolEncoder.class.getSimpleName());

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotingCommand command, ByteBuf out) throws Exception {
        try {
            ByteBuffer header = command.encodeHeader();
            out.writeBytes(header);
            out.writeBytes(command.getBody());
        } catch (Throwable e) {
            log.error("encode RemotingCommand to ByteBuf Error, RemoteAddr : " + RemotingUtils.parseChannelRemoteAddr(ctx.channel()), e);
            RemotingUtils.closeChannel(ctx.channel());
        }
    }
}
