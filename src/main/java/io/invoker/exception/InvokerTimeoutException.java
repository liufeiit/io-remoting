package io.invoker.exception;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午3:04:05
 */
public class InvokerTimeoutException extends RuntimeException {

    private static final long serialVersionUID = -8099650661198763093L;

    public InvokerTimeoutException(String serviceId) {
        super("invoker serviceId<" + serviceId + "> timeout Error.");
    }

    public InvokerTimeoutException(String serviceId, Throwable cause) {
        super("invoker serviceId<" + serviceId + "> timeout Error.", cause);
    }
}
