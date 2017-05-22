package io.remoting.netty;

import io.netty.channel.ChannelHandlerContext;
import io.remoting.protocol.RemotingCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月19日 下午9:14:07
 */
public interface NettyCommandProcessor {
    RemotingCommand processCommand(ChannelHandlerContext ctx, RemotingCommand request) throws Throwable;
    boolean reject();
}
