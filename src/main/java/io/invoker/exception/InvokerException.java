package io.invoker.exception;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:58:23
 */
public class InvokerException extends RuntimeException {

    private static final long serialVersionUID = 4101671557585323670L;

    public InvokerException() {
        super();
    }

    public InvokerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvokerException(String message) {
        super(message);
    }

    public InvokerException(Throwable cause) {
        super(cause);
    }
}
