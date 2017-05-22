package io.remoting.protocol;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午4:46:42
 */
public class JacksonProtocolFactory implements ProtocolFactory {

    private Serializer serializer = new JacksonSerializer();
    
    @Override
    public int getProtocolCode() {
        return 0;
    }

    @Override
    public void encode(Object body, RemotingCommand command) {
        command.setBody(serializer.serializeAsBytes(body));
    }

    @Override
    public <T> T decode(Class<T> bodyType, RemotingCommand command) {
        return serializer.deserialize(bodyType, command.getBody());
    }
}
