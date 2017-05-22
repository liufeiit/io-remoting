package io.invoker.caller;

import java.lang.reflect.InvocationHandler;

import org.apache.commons.lang3.ArrayUtils;

import io.invoker.InvokeType;
import io.invoker.Invoker;
import io.invoker.InvokerCallback;
import io.invoker.InvokerCommand;
import io.invoker.annotation.Method;
import io.invoker.annotation.Service;
import io.invoker.utils.CorrelationIds;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午9:12:40
 */
public class InvokerCaller implements InvocationHandler {
    private final Class<?> serviceInterface;
    private final Invoker invoker;

    public InvokerCaller(Invoker invoker, Class<?> serviceInterface) {
        super();
        this.invoker = invoker;
        this.serviceInterface = serviceInterface;
    }

    @Override
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
        Service service = this.serviceInterface.getAnnotation(Service.class);
        Method serviceMethod = method.getAnnotation(Method.class);
        String group = service.group();
        long timeoutMillis = serviceMethod.timeoutMillis();
        InvokeType type = serviceMethod.type();
        InvokerCommand command = new InvokerCommand();
        command.setId(CorrelationIds.buildGuid());
        command.setVersion(serviceMethod.version());
        command.setProtocolCode(serviceMethod.protocol());
        command.setServiceGroup(group);
        command.setServiceId(this.serviceInterface.getName());
        command.setMethod(method.getName());
        command.setSignature(method.getParameterTypes());
        command.setArgs(args);
        switch (type) {
            case SYNC:
                InvokerCommand response = this.invoker.invokeSync(command, timeoutMillis);
                if (response.getT() != null) {
                    throw response.getT();
                }
                return response.getRetObject();
            case ASYNC:
                final Callback callback = getCallbackObject(args);
                this.invoker.invokeAsync(command, timeoutMillis, new InvokerCallback() {
                    public void onError(Throwable e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }

                    public void onComplete(InvokerCommand command) {
                        if (callback != null) {
                            callback.onComplete(command.getRetObject());
                        }
                    }
                });
                break;
            case ONEWAY:
                this.invoker.invokeOneway(command);
                break;
        }
        return null;
    }

    private Callback getCallbackObject(Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Callback) {
                return (Callback) arg;
            }
        }
        return null;
    }
}
