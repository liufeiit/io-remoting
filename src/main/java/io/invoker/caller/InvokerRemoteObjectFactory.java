package io.invoker.caller;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

import io.invoker.Invoker;
import io.invoker.RemoteObject;
import io.invoker.RemoteObjectFactory;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 下午1:33:46
 */
@SuppressWarnings("unchecked")
public class InvokerRemoteObjectFactory implements RemoteObjectFactory {
    private Invoker invoker;
    private ConcurrentHashMap<String, Object> remoteObjects = new ConcurrentHashMap<String, Object>();

    @Override
    public <T> T getRemoteObject(String serializeStub, Class<T> serviceInterface) {
        Object remoteObject = remoteObjects.get(serializeStub);
        if (remoteObject != null) {
            return (T) remoteObject;
        }
        InvokerCaller caller = new InvokerCaller(invoker, serializeStub);
        remoteObject = Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[] {serviceInterface, RemoteObject.class}, caller);
        remoteObjects.put(serializeStub, remoteObject);
        return (T) remoteObject;
    }

    @Override
    public <T> T getRemoteObject(Class<T> serviceInterface) {
        return getRemoteObject(new RemoteStub(serviceInterface).serializeStub(), serviceInterface);
    }
    
    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }
}
