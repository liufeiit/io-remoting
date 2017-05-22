package io.remoting.protocol;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:41:51
 */
public interface ProtocolFactory {
    int getProtocolCode();
    void encode(Object body, RemotingCommand command);
    <T> T decode(Class<T> bodyType, RemotingCommand command);
}
