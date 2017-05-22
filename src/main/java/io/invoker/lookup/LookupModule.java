package io.invoker.lookup;

import io.invoker.Address;
import io.invoker.Module;
import io.invoker.cluster.LoadBalance;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:22:17
 */
public interface LookupModule extends Module {
    void registry(String serviceGroup, String serviceId, Address addr);
    Address lookup(String serviceGroup, String serviceId, LoadBalance loadBalance);
}
