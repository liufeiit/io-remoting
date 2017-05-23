package io.invoker.track;

import io.invoker.Application;
import io.invoker.InvokerCommand;
import io.invoker.Module;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 上午11:39:59
 */
public interface TrackModule extends Module {
    void track(final InvokerCommand command, final Application application);
}
