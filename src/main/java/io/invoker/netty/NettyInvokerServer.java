package io.invoker.netty;

import java.util.concurrent.Executors;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.invoker.Address;
import io.invoker.InvokerServer;
import io.invoker.exception.InvokerException;
import io.invoker.lookup.LookupModule;
import io.invoker.port.ServerPort;
import io.remoting.netty.NettyCommandProcessor;
import io.remoting.netty.NettyRemotingServer;
import io.remoting.netty.NettyServerConfigurator;
import io.remoting.utils.RemotingThreadFactory;
import io.remoting.utils.RemotingUtils;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:16:27
 */
public class NettyInvokerServer implements InvokerServer {
    private static final Logger log = LoggerFactory.getLogger(NettyInvokerServer.class.getSimpleName());
    private NettyServerConfigurator serverConfigurator;
    private NettyRemotingServer remotingServer;
    private LookupModule lookupModule;
    private ServerPort serverPort;
    private Address serverAddress;

    @Override
    public void start() {
        serverConfigurator.setListenPort(serverPort.selectPort());
        remotingServer = new NettyRemotingServer(serverConfigurator);
        remotingServer.registerDefaultProcessor(this.newCommandProcessor(),
                Executors.newFixedThreadPool(serverConfigurator.getServerWorkerProcessorThreads(), RemotingThreadFactory.newThreadFactory("NettyInvokerServerWorkerProcessor-%d", false)));
        remotingServer.start();
        serverAddress = new Address(RemotingUtils.getLocalAddress(), remotingServer.getServerAddress().getPort());
        log.info("NettyInvokerServer 'NettyRemotingServer' start success bind {}", serverAddress);
    }

    @Override
    public void bind(String... serviceGroups) {
        if (ArrayUtils.isEmpty(serviceGroups)) {
            log.info("bind serviceGroups is empty.");
            return;
        }
        for (String serviceGroup : serviceGroups) {
            int commandCode = serviceGroup.hashCode();
            remotingServer.registerProcessor(commandCode, this.newCommandProcessor(), Executors.newFixedThreadPool(serverConfigurator.getServerWorkerProcessorThreads(),
                    RemotingThreadFactory.newThreadFactory("NettyInvokerServerWorkerProcessor[" + serviceGroup + "]-%d", false)));
            log.info("bind serviceGroup {} success.", serviceGroup);
        }
    }

    @Override
    public void deploy(String serviceGroup, String serviceId, Object bean) throws InvokerException {
        log.info("deploy service serviceGroup : {}, serviceId : {}", new Object[] {serviceGroup, serviceId});
        lookupModule.registry(serviceGroup, serviceId, serverAddress);
    }

    protected NettyCommandProcessor newCommandProcessor() {
        return null;
    }

    @Override
    public void shutdown() {
        if (remotingServer != null) {
            remotingServer.shutdown();
            remotingServer = null;
        }
    }

    public void setLookupModule(LookupModule lookupModule) {
        this.lookupModule = lookupModule;
    }
    
    public void setServerConfigurator(NettyServerConfigurator serverConfigurator) {
        this.serverConfigurator = serverConfigurator;
    }
    
    public void setServerPort(ServerPort serverPort) {
        this.serverPort = serverPort;
    }
}