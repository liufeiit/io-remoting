package io.invoker.caller;

import java.lang.reflect.InvocationHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.invoker.InvokeType;
import io.invoker.Invoker;
import io.invoker.InvokerCallback;
import io.invoker.InvokerCommand;
import io.invoker.RemoteObject;
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
    private final RemoteStub remoteObject;

    public InvokerCaller(Invoker invoker, Class<?> serviceInterface) {
        super();
        this.invoker = invoker;
        this.serviceInterface = serviceInterface;
        Service service = this.serviceInterface.getAnnotation(Service.class);
        this.remoteObject = new RemoteStub();
        remoteObject.setServiceId(this.serviceInterface.getName());
        remoteObject.setServiceGroup(service.group());
        remoteObject.setVersion(service.version());
        remoteObject.setProtocolCode(service.protocol());
    }

    @Override
    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass().equals(RemoteObject.class)) {
            return method.invoke(remoteObject, args);
        }
        if (method.getDeclaringClass().equals(Object.class)) {
            return method.invoke(this, args);
        }
        Method serviceMethod = method.getAnnotation(Method.class);
        long timeoutMillis = serviceMethod.timeoutMillis();
        InvokeType type = serviceMethod.type();
        InvokerCommand command = new InvokerCommand();
        command.setId(CorrelationIds.buildGuid());
        command.setVersion(remoteObject.getVersion());
        command.setProtocolCode(remoteObject.getProtocolCode());
        command.setServiceGroup(remoteObject.getServiceGroup());
        command.setServiceId(this.serviceInterface.getName());
        command.setMethod(method.getName());
        command.setSignature(method.getParameterTypes());
        command.setArgs(args);
        switch (type) {
            case SYNC:
                InvokerCommand syncResponse = this.invoker.invokeSync(command, timeoutMillis);
                if (syncResponse.getT() != null) {
                    throw syncResponse.getT();
                }
                return syncResponse.getRetObject();
            case ASYNC:
                final InvokerCommandFuture invokerCommandFuture = new InvokerCommandFuture();
                this.invoker.invokeAsync(command, timeoutMillis, new InvokerCallback() {
                    public void onError(Throwable e) {
                        invokerCommandFuture.setT(e);
                    }

                    public void onComplete(InvokerCommand command) {
                        invokerCommandFuture.setCommand(command);
                    }
                });
                InvokerCommand asyncResponse = invokerCommandFuture.waitFor(timeoutMillis);
                if (asyncResponse == null) {
                    if (invokerCommandFuture.getT() != null) {
                        throw invokerCommandFuture.getT();
                    }
                    return null;
                }
                return asyncResponse.getRetObject();
            case ONEWAY:
                this.invoker.invokeOneway(command);
                break;
        }
        return null;
    }

    private class InvokerCommandFuture {
        private InvokerCommand command;
        private Throwable t;
        private CountDownLatch sync = new CountDownLatch(1);

        public InvokerCommand waitFor(long timeoutMillis) throws InterruptedException {
            sync.await(timeoutMillis, TimeUnit.MILLISECONDS);
            return command;
        }

        public void setCommand(InvokerCommand command) {
            this.command = command;
            sync.countDown();
        }

        public void setT(Throwable t) {
            this.t = t;
            sync.countDown();
        }

        public Throwable getT() {
            return t;
        }
    }
}
