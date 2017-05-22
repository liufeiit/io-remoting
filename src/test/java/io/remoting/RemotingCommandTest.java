package io.remoting;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

import io.invoker.InvokerCommand;
import io.remoting.protocol.CommandCode;
import io.remoting.protocol.CommandVersion;
import io.remoting.protocol.JacksonProtocolFactory;
import io.remoting.protocol.JacksonSerializer;
import io.remoting.protocol.ProtocolFactory;
import io.remoting.protocol.RemotingCommand;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月19日 下午3:23:29
 */
public class RemotingCommandTest {
    
    @Test
    public void testByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putInt(1);
        byteBuffer.putInt(2);
        byteBuffer.putInt(3);
        byteBuffer.putInt(4);
        byteBuffer.flip();
        System.err.println("length : " + byteBuffer.array().length);
        System.err.println(byteBuffer.getInt());
        System.err.println(byteBuffer.getInt());
        System.err.println(byteBuffer.getInt());
        System.err.println(byteBuffer.getInt());
    }
    
    @Test
    public void testInvokerCommand() throws Throwable {
        ProtocolFactory protocolFactory = new JacksonProtocolFactory();
        JacksonSerializer serializer = new JacksonSerializer();
        InvokerCommand invokerCommand = new InvokerCommand();
        invokerCommand.setId(UUID.randomUUID().toString());
        invokerCommand.setServiceGroup("test.srv.group");
        invokerCommand.setServiceId("test.Service");
        invokerCommand.setMethod("hello");
        invokerCommand.setSignature(new Class<?>[]{String.class});
        invokerCommand.setArgs(new Object[] {"刘飞"});
        invokerCommand.setRetObject("你好刘飞");
        invokerCommand.setT(new RuntimeException("test"));
        RemotingCommand command = new RemotingCommand();
        command.setOneway();
        command.setReply();
        command.setCode(CommandCode.REQUEST_CODE_NOT_SUPPORTED);
        command.setVersion(CommandVersion.V1);
        protocolFactory.encode(invokerCommand, command);
        byte[] serializeBytes = serializer.serializeAsBytes(command);
        System.err.println("serializeBytes : " + serializeBytes.length);
        RemotingCommand commandFromBytes = serializer.deserialize(RemotingCommand.class, serializeBytes);
        InvokerCommand invokerCommandFromBytes = serializer.deserialize(InvokerCommand.class, commandFromBytes.getBody());
        System.err.println("invokerCommandFromBytes : " + serializer.serializeAsString(invokerCommandFromBytes));
        throw invokerCommandFromBytes.getT();
    }

    @Test
    public void testRemotingCommand() throws Throwable {
        ProtocolFactory protocolFactory = new JacksonProtocolFactory();
        JacksonSerializer serializer = new JacksonSerializer();
        RemotingCommand command = new RemotingCommand();
        System.err.println("1 isOneway " + command.isOneway());
        System.err.println("1 isReply " + command.isReply());
        command.setOneway();
        command.setReply();
        command.setCode(CommandCode.REQUEST_CODE_NOT_SUPPORTED);
        command.setVersion(CommandVersion.V1);
//        command.setBody("hello world!".getBytes("UTF-8"));
        
        SimpleBean simpleBean = new SimpleBean();
        simpleBean.setName("刘飞");
        simpleBean.setAge(30);
        simpleBean.setAvt(serializer.serializeAsBytes("你好吗"));
        protocolFactory.encode(simpleBean, command);
        
        System.err.println("2 isOneway " + command.isOneway());
        System.err.println("2 isReply " + command.isReply());
        System.err.println("2 getCode " + command.getCode());
        System.err.println("2 getVersion " + command.getVersion());
        System.err.println("2 getCommand " + command.getCommand());
//        System.err.println("2 getBody " + new String(command.getBody(), "UTF-8"));
        
        byte[] serializeBytes = serializer.serializeAsBytes(command);
        System.err.println("serializeBytes : " + serializeBytes.length);
        
        
        RemotingCommand commandFromBytes = serializer.deserialize(RemotingCommand.class, serializeBytes);
        
        System.err.println("3 isOneway " + commandFromBytes.isOneway());
        System.err.println("3 isReply " + commandFromBytes.isReply());

        System.err.println("3 getCode " + commandFromBytes.getCode());
        System.err.println("3 getVersion " + commandFromBytes.getVersion());
        System.err.println("3 getCommand " + commandFromBytes.getCommand());
        
        SimpleBean bean = serializer.deserialize(SimpleBean.class, commandFromBytes.getBody());
        
        System.err.println("3 getBody bean name " + bean.getName());
        System.err.println("3 getBody bean age " + bean.getAge());
        System.err.println("3 getBody bean avt " + serializer.deserialize(String.class, bean.getAvt()));
        
    }
    
    public static class SimpleBean {
        private String name;
        private int age;
        private byte[] avt;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public int getAge() {
            return age;
        }
        public void setAge(int age) {
            this.age = age;
        }
        public byte[] getAvt() {
            return avt;
        }
        public void setAvt(byte[] avt) {
            this.avt = avt;
        }
    }
}
