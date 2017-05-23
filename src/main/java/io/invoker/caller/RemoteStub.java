package io.invoker.caller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import io.invoker.RemoteObject;
import io.remoting.protocol.CommandVersion;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月23日 上午11:12:52
 */
public class RemoteStub implements RemoteObject {
    private static final Pattern REMOTE_STUB_PATTERN = Pattern.compile("^(.*),(.*),(.*),(.*)$");
    private String serviceId;
    private String serviceGroup;
    private int version = CommandVersion.V1;
    private int protocolCode = 0;

    @Override
    public String serializeStub() {
        StringBuffer sb = new StringBuffer();
        sb.append(serviceId);
        sb.append(",").append(serviceGroup);
        sb.append(",").append(version);
        sb.append(",").append(protocolCode);
        return sb.toString();
    }

    public RemoteStub deserializeStub(String serializeStub) {
        Matcher matcher = REMOTE_STUB_PATTERN.matcher(serializeStub);
        boolean matchFound = matcher.find();
        if (matchFound) {
            this.serviceId = matcher.group(1);
            this.serviceGroup = StringUtils.defaultString(matcher.group(2), null);
            this.version = NumberUtils.toInt(matcher.group(3), CommandVersion.V1);
            this.protocolCode = NumberUtils.toInt(matcher.group(4), 0);
            return this;
        }
        throw new RuntimeException("Illegal serialize data for RemoteObject.");
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getProtocolCode() {
        return protocolCode;
    }

    public void setProtocolCode(int protocolCode) {
        this.protocolCode = protocolCode;
    }

    @Override
    public String toString() {
        return serializeStub();
    }
}
