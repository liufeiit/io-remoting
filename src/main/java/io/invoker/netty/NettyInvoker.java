package io.invoker.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.invoker.Address;
import io.invoker.Invoker;
import io.invoker.InvokerCallback;
import io.invoker.InvokerCommand;
import io.invoker.cluster.LoadBalance;
import io.invoker.exception.InvokerException;
import io.invoker.exception.InvokerTimeoutException;
import io.invoker.lookup.LookupModule;
import io.remoting.RemotingCallback;
import io.remoting.ReplyFuture;
import io.remoting.exception.RemotingConnectException;
import io.remoting.exception.RemotingSendRequestException;
import io.remoting.exception.RemotingTimeoutException;
import io.remoting.netty.NettyClientConfigurator;
import io.remoting.netty.NettyRemotingClient;
import io.remoting.protocol.ProtocolFactory;
import io.remoting.protocol.ProtocolFactorySelector;
import io.remoting.protocol.RemotingCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:16:51
 */
public class NettyInvoker implements Invoker {
    private static final Logger log = LoggerFactory.getLogger(NettyInvoker.class.getSimpleName());
    private NettyClientConfigurator clientConfigurator;
    private NettyRemotingClient remotingClient;
    private LookupModule lookupModule;
    private LoadBalance loadBalance;
    private ProtocolFactorySelector protocolFactorySelector;

    @Override
    public void start() {
        remotingClient = new NettyRemotingClient(protocolFactorySelector, clientConfigurator);
        remotingClient.start();
        log.info("NettyInvoker 'NettyRemotingClient' start success.");
    }
    
    @Override
    public InvokerCommand invokeSync(final InvokerCommand command, final long timeoutMillis) throws InvokerException, InvokerTimeoutException {
        try {
            long startMillis = System.currentTimeMillis();
            int commandCode = command.getServiceGroup().hashCode();
            ProtocolFactory protocolFactory = protocolFactorySelector.select(command.getProtocolCode());
            RemotingCommand request = new RemotingCommand();
            request.setCode(commandCode);
            request.setVersion(command.getVersion());
            request.setProtocolCode(command.getProtocolCode());
            protocolFactory.encode(command, request);
            Address addr = lookupModule.lookup(command.getServiceGroup(), command.getServiceId(), loadBalance);
            RemotingCommand response = remotingClient.invokeSync(addr.toString(), request, timeoutMillis);
            if (commandCode == response.getCode()) {
                InvokerCommand invokerCommand = protocolFactory.decode(InvokerCommand.class, response);
                long endMillis = System.currentTimeMillis();
                log.info("invoker serviceId<{}>, used {}(ms) success.", new Object[] {command.commandSignature(), (endMillis - startMillis)});
                return invokerCommand;
            }
            log.error("invoker serviceId<{}>, code<{}> Error.", new Object[] {command.commandSignature(), response.getCode()});
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + ">, code<" + response.getCode() + "> Error.");
        } catch (RemotingConnectException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingSendRequestException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingTimeoutException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
            throw new InvokerTimeoutException("invoker serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void invokeAsync(final InvokerCommand command, final long timeoutMillis, final InvokerCallback callback) throws InvokerException, InvokerTimeoutException {
        try {
            final long startMillis = System.currentTimeMillis();
            final int commandCode = command.getServiceGroup().hashCode();
            final ProtocolFactory protocolFactory = protocolFactorySelector.select(command.getProtocolCode());
            RemotingCommand request = new RemotingCommand();
            request.setCode(commandCode);
            request.setVersion(command.getVersion());
            request.setProtocolCode(command.getProtocolCode());
            protocolFactory.encode(command, request);
            Address addr = lookupModule.lookup(command.getServiceGroup(), command.getServiceId(), loadBalance);
            remotingClient.invokeAsync(addr.toString(), request, timeoutMillis, new RemotingCallback() {
                @Override
                public void onComplete(ReplyFuture replyFuture) {
                    if (replyFuture.getResponse() == null) {
                        callback.onError(new InvokerException("Can't found RemotingCommand response from ReplyFuture."));
                        return;
                    }
                    RemotingCommand response = replyFuture.getResponse();
                    if (commandCode == response.getCode()) {
                        InvokerCommand invokerCommand = protocolFactory.decode(InvokerCommand.class, response);
                        long endMillis = System.currentTimeMillis();
                        log.info("invoker serviceId<{}>, used {}(ms) success.", new Object[] {command.commandSignature(), (endMillis - startMillis)});
                        callback.onComplete(invokerCommand);
                        return;
                    }
                    callback.onError(new InvokerException("invoker serviceId<" + command.commandSignature() + ">, code<" + response.getCode() + "> Error."));
                }
            });
        } catch (RemotingConnectException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingSendRequestException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingTimeoutException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
            throw new InvokerTimeoutException("invoker serviceId<" + command.commandSignature() + "> timeout " + e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void invokeOneway(InvokerCommand command) throws InvokerException, InvokerTimeoutException {
        try {
            long startMillis = System.currentTimeMillis();
            int commandCode = command.getServiceGroup().hashCode();
            ProtocolFactory protocolFactory = protocolFactorySelector.select(command.getProtocolCode());
            RemotingCommand request = new RemotingCommand();
            request.setCode(commandCode);
            request.setVersion(command.getVersion());
            request.setProtocolCode(command.getProtocolCode());
            protocolFactory.encode(command, request);
            Address addr = lookupModule.lookup(command.getServiceGroup(), command.getServiceId(), loadBalance);
            remotingClient.invokeOneway(addr.toString(), request);
            long endMillis = System.currentTimeMillis();
            log.info("invoker serviceId<{}>, used {}(ms) success.", new Object[] {command.commandSignature(), (endMillis - startMillis)});
        } catch (RemotingConnectException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (RemotingSendRequestException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.error("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
            throw new InvokerException("invoker serviceId<" + command.commandSignature() + "> " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void shutdown() {
        if (remotingClient != null) {
            remotingClient.shutdown();
            remotingClient = null;
        }
    }
    
    public void setClientConfigurator(NettyClientConfigurator clientConfigurator) {
        this.clientConfigurator = clientConfigurator;
    }
    
    public void setLookupModule(LookupModule lookupModule) {
        this.lookupModule = lookupModule;
    }
    
    public void setLoadBalance(LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }
    
    public void setProtocolFactorySelector(ProtocolFactorySelector protocolFactorySelector) {
        this.protocolFactorySelector = protocolFactorySelector;
    }
}
