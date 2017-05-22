package io.invoker.processor;

import io.netty.channel.ChannelHandlerContext;
import io.remoting.netty.NettyCommandProcessor;
import io.remoting.protocol.RemotingCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:09:36
 */
public class InvokerCommandProcessor implements NettyCommandProcessor {

    @Override
    public RemotingCommand processCommand(ChannelHandlerContext ctx, RemotingCommand request) throws Throwable {
//        InvokerCommand command = 
        
        return null;
    }

    @Override
    public boolean reject() {
        return false;
    }
}
