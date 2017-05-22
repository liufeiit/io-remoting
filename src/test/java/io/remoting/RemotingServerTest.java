package io.remoting;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.channel.ChannelHandlerContext;
import io.remoting.RemotingCommandTest.SimpleBean;
import io.remoting.exception.RemotingConnectException;
import io.remoting.exception.RemotingSendRequestException;
import io.remoting.exception.RemotingTimeoutException;
import io.remoting.netty.NettyClientConfigurator;
import io.remoting.netty.NettyCommandProcessor;
import io.remoting.netty.NettyRemotingClient;
import io.remoting.netty.NettyRemotingServer;
import io.remoting.netty.NettyServerConfigurator;
import io.remoting.protocol.CommandCode;
import io.remoting.protocol.CommandVersion;
import io.remoting.protocol.JacksonSerializer;
import io.remoting.protocol.RemotingCommand;

public class RemotingServerTest {
    private static RemotingServer remotingServer;
    private static RemotingClient remotingClient;

    public static RemotingServer createRemotingServer() throws InterruptedException {
        NettyServerConfigurator config = new NettyServerConfigurator();
        RemotingServer remotingServer = new NettyRemotingServer(config);
        remotingServer.registerDefaultProcessor(new NettyCommandProcessor() {
            public RemotingCommand processCommand(ChannelHandlerContext ctx, RemotingCommand request) {
                JacksonSerializer serializer = new JacksonSerializer();
                SimpleBean simpleBean = serializer.deserialize(SimpleBean.class, request.getBody());
                System.err.println("收到客户端请求 : " + serializer.deserialize(String.class, simpleBean.getAvt()));
                simpleBean.setAvt(serializer.serializeAsBytes("来之服务器的问候" + simpleBean.getName() + ", " + simpleBean.getAge() + " 很好啊"));
                request.setBodyObject(simpleBean);
                return request;
            }
            public boolean reject() {
                return false;
            }
        }, Executors.newCachedThreadPool());
        remotingServer.start();
        System.err.println(remotingServer.getServerAddress().getAddress());
        System.err.println(remotingServer.getServerAddress().getHostName());
        System.err.println(remotingServer.getServerAddress().getHostString());
        System.err.println(remotingServer.getServerAddress().getPort());
        return remotingServer;
    }

    public static RemotingClient createRemotingClient() {
        NettyClientConfigurator config = new NettyClientConfigurator();
        RemotingClient client = new NettyRemotingClient(config);
        client.start();
        return client;
    }

    @BeforeClass
    public static void setup() throws InterruptedException {
        remotingServer = createRemotingServer();
        remotingClient = createRemotingClient();
    }

    @AfterClass
    public static void destroy() {
        remotingClient.shutdown();
        remotingServer.shutdown();
    }

    @Test
    public void testInvokeSync() throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        try {
            JacksonSerializer serializer = new JacksonSerializer();
            RemotingCommand command = new RemotingCommand();
            command.setCode(CommandCode.SUCCESS);
            command.setVersion(CommandVersion.V1);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.setName("刘飞");
            simpleBean.setAge(30);
            simpleBean.setAvt(serializer.serializeAsBytes("你好吗-sync"));
            command.setBodyObject(simpleBean);
            RemotingCommand response = remotingClient.invokeSync("localhost:8888", command, 1000 * 3);
            System.err.println("response : " + response);
            SimpleBean reply = serializer.deserialize(SimpleBean.class, response.getBody());
            System.err.println("reply command response : " + serializer.deserialize(String.class, reply.getAvt()));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
    @Test
    public void testInvokeAsync() throws InterruptedException, RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException {
        try {
            final JacksonSerializer serializer = new JacksonSerializer();
            RemotingCommand command = new RemotingCommand();
            command.setCode(CommandCode.SUCCESS);
            command.setVersion(CommandVersion.V1);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.setName("刘飞");
            simpleBean.setAge(30);
            simpleBean.setAvt(serializer.serializeAsBytes("你好吗-async"));
            command.setBodyObject(simpleBean);
            final CountDownLatch wait = new CountDownLatch(1);
            remotingClient.invokeAsync("localhost:8888", command, 1000 * 3, new RemotingCallback() {
                @Override
                public void onComplete(ReplyFuture replyFuture) {
                    RemotingCommand response = replyFuture.getResponse();
                    System.err.println("response : " + response);
                    SimpleBean reply = serializer.deserialize(SimpleBean.class, response.getBody());
                    System.err.println("reply command response : " + serializer.deserialize(String.class, reply.getAvt()));
                    wait.countDown();
                }
            });
            wait.await(3, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Test
    public void testInvokeOneway() throws InterruptedException, RemotingConnectException, RemotingTimeoutException, RemotingSendRequestException {
        try {
            JacksonSerializer serializer = new JacksonSerializer();
            RemotingCommand command = new RemotingCommand();
            command.setCode(CommandCode.SUCCESS);
            command.setVersion(CommandVersion.V1);
            SimpleBean simpleBean = new SimpleBean();
            simpleBean.setName("刘飞");
            simpleBean.setAge(30);
            simpleBean.setAvt(serializer.serializeAsBytes("你好吗-oneway"));
            command.setBodyObject(simpleBean);
            remotingClient.invokeOneway("localhost:8888", command);
            Thread.sleep(2000L);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
