package io.invoker;

import io.invoker.exception.InvokerException;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午12:56:14
 */
public interface InvokerServer {
    void deploy(String serviceGroup, String serviceId, Object bean) throws InvokerException;
    void bind(String... serviceGroups);
    void start();
    void shutdown();
}
