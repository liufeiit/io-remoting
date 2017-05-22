package io.remoting;

import java.util.concurrent.ExecutorService;

import io.remoting.exception.RemotingConnectException;
import io.remoting.exception.RemotingSendRequestException;
import io.remoting.exception.RemotingTimeoutException;
import io.remoting.netty.NettyCommandProcessor;
import io.remoting.protocol.RemotingCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月18日 下午9:45:43
 */
public interface RemotingClient extends RemotingService {

    RemotingCommand invokeSync(final String addr, final RemotingCommand request, final long timeoutMillis) throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException;

    void invokeAsync(final String addr, final RemotingCommand request, final long timeoutMillis, final RemotingCallback callback) throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException;

    void invokeOneway(final String addr, final RemotingCommand request) throws InterruptedException, RemotingConnectException, RemotingSendRequestException;

    void registerProcessor(int commandCode, NettyCommandProcessor processor, ExecutorService executor);
}
