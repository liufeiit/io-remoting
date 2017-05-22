package io.invoker.processor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.invoker.InvokerCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午6:26:48
 */
public class ServiceObject {
    private Object service;
    private Method method;

    public Object invoke(InvokerCommand command) throws Throwable {
        try {
            return method.invoke(service, command.getArgs());
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException) e).getCause();
            }
            throw e;
        }
    }

    public Object getService() {
        return service;
    }

    public void setService(Object service) {
        this.service = service;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
