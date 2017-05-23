package io.invoker;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.remoting.protocol.CommandVersion;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午12:56:45
 */
public class InvokerCommand implements Serializable {
    private static final long serialVersionUID = 7227421307748173734L;
    private String id;
    private Application application;
    private int version = CommandVersion.V1;
    private int protocolCode = 0;
    private String serviceGroup;
    private String serviceId;
    private String method;
    private Class<?>[] signature;
    private Object[] args;
    private Object retObject;
    private Throwable t;

    @JsonIgnore
    public String commandSignature() {
        return serviceId + "." + method;
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Class<?>[] getSignature() {
        return signature;
    }

    public void setSignature(Class<?>[] signature) {
        this.signature = signature;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object getRetObject() {
        return retObject;
    }

    public void setRetObject(Object retObject) {
        this.retObject = retObject;
    }

    public Throwable getT() {
        return t;
    }

    public void setT(Throwable t) {
        this.t = t;
    }
}
