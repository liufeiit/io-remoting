package io.invoker;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 下午1:31:20
 */
public interface RemoteObjectFactory {
    <T> T getRemoteObject(String serializeStub, Class<T> serviceInterface);
    <T> T getRemoteObject(Class<T> serviceInterface);
}