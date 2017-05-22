package io.invoker.port;

import io.invoker.Module;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:15:11
 */
public interface ServerPort extends Module {
    int selectPort();
}
