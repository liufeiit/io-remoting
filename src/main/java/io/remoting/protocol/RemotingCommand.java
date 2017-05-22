package io.remoting.protocol;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月18日 下午3:42:41
 */
public class RemotingCommand {
    public static final int HEADER_CODE_LENGTH = 4;
    public static final int HEADER_VERSION_LENGTH = 4;
    public static final int HEADER_COMMAND_LENGTH = 4;
    public static final int HEADER_SERIALIZE_LENGTH = 4;
    public static final int HEADER_OPAQUE_LENGTH = 8;

    public static final int HEADER_BODY_LENGTH = 4;

    public static final int COMMAND_HEADER_LENGTH = HEADER_CODE_LENGTH + HEADER_VERSION_LENGTH + HEADER_COMMAND_LENGTH + HEADER_SERIALIZE_LENGTH + HEADER_OPAQUE_LENGTH + HEADER_BODY_LENGTH;

    private static final int RPC = 0;
    private static final int ONEWAY = 1;

    private static final AtomicLong REQ_ID = new AtomicLong(1);

    private int code = 0;
    private int version = CommandVersion.V1;
    private int command = 0;
    private int serializeCode = 0;
    private long opaque = REQ_ID.getAndIncrement();

    private transient byte[] body;

    public static RemotingCommand replyCommand(RemotingCommand request, int code, Object message) {
        RemotingCommand command = new RemotingCommand();
        command.setCode(code);
        command.setVersion(request.getVersion());
        command.setReply();
        if (request.isOneway()) {
            command.setOneway();
        }
        command.setSerializeCode(request.getSerializeCode());
        command.setOpaque(request.getOpaque());
        command.setBodyObject(message);
        return command;
    }

    public static RemotingCommand decode(ByteBuffer byteBuffer) {
        int code = byteBuffer.getInt();
        int version = byteBuffer.getInt();
        int command = byteBuffer.getInt();
        int serializeCode = byteBuffer.getInt();
        long opaque = byteBuffer.getLong();
        int bodyLength = byteBuffer.getInt();
        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setCode(code);
        remotingCommand.setVersion(version);
        remotingCommand.setCommand(command);
        remotingCommand.setSerializeCode(serializeCode);
        remotingCommand.setOpaque(opaque);
        byte[] body = new byte[bodyLength];
        byteBuffer.get(body);
        remotingCommand.setBody(body);
        return remotingCommand;
    }

    public static RemotingCommand decode(byte[] bytes) {
        return decode(ByteBuffer.wrap(bytes));
    }

    @JsonIgnore
    public ByteBuffer encodeHeader() {
        ByteBuffer headerBytes = ByteBuffer.allocate(COMMAND_HEADER_LENGTH);
        headerBytes.putInt(code);
        headerBytes.putInt(version);
        headerBytes.putInt(command);
        headerBytes.putInt(serializeCode);
        headerBytes.putLong(opaque);
        headerBytes.putInt(body.length);
        headerBytes.flip();
        return headerBytes;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getCommand() {
        return command;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public int getSerializeCode() {
        return serializeCode;
    }

    public void setSerializeCode(int serializeCode) {
        this.serializeCode = serializeCode;
    }

    public long getOpaque() {
        return opaque;
    }

    public void setOpaque(long opaque) {
        this.opaque = opaque;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @JsonIgnore
    public void setBodyObject(Object bean) {
        Serializer serializer = SerializeType.valueOf(serializeCode).getSerializer();
        this.setBody(serializer.serializeAsBytes(bean));
    }

    @JsonIgnore
    public boolean isOneway() {
        int bits = 1 << ONEWAY;
        return (this.command & bits) == bits;
    }

    @JsonIgnore
    public void setOneway() {
        int bits = 1 << ONEWAY;
        this.command |= bits;
    }

    @JsonIgnore
    public boolean isReply() {
        int bits = 1 << RPC;
        return (this.command & bits) == bits;
    }

    @JsonIgnore
    public void setReply() {
        int bits = 1 << RPC;
        this.command |= bits;
    }

    @Override
    public String toString() {
        return "RemotingCommand [code=" + code + ", version=" + version + ", command=" + command + ", serializeCode=" + serializeCode + ", opaque=" + opaque + "]";
    }
}
