package io.invoker.protocol;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.remoting.protocol.ProtocolFactory;
import io.remoting.protocol.ProtocolFactorySelector;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午5:53:37
 */
public class InvokerProtocolFactorySelector implements ProtocolFactorySelector {

    private static final Logger log = LoggerFactory.getLogger(InvokerProtocolFactorySelector.class.getSimpleName());

    private ConcurrentHashMap<Integer, ProtocolFactory> protocolFactories = new ConcurrentHashMap<Integer, ProtocolFactory>();

    @Override
    public ProtocolFactory select(int serializeCode) {
        return protocolFactories.get(serializeCode);
    }

    public ProtocolFactory registry(ProtocolFactory protocolFactory) {
        return protocolFactories.putIfAbsent(protocolFactory.getProtocolCode(), protocolFactory);
    }

    public void setProtocolFactories(List<ProtocolFactory> protocolFactories) {
        if (CollectionUtils.isEmpty(protocolFactories)) {
            return;
        }
        for (ProtocolFactory protocolFactory : protocolFactories) {
            ProtocolFactory pf = registry(protocolFactory);
            log.info("registry protocolFactory code : {}, protocolFactory : {}", protocolFactory.getProtocolCode(), pf);
        }
    }
}
