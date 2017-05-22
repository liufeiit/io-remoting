package io.invoker;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

/**
 * @author 刘飞 E-mail:liufei_it@126.com
 *
 * @version 1.0.0
 * @since 2017年5月22日 下午2:29:08
 */
public class Address implements Serializable {
    private static final long serialVersionUID = -3826228304734021497L;
    private String host;
    private int port;

    public Address() {
        super();
    }
    
    public Address(String addr) {
        String[] addrs = StringUtils.split(addr, ":");
        this.setHost(addrs[0]);
        this.setPort(Integer.parseInt(addrs[1]));
    }

    public Address(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
