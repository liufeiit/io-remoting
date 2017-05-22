package io.invoker.caller;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午9:19:37
 */
public interface Callback {
    void onComplete(Object retObject);
    void onError(Throwable e);
}
