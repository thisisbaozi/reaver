package io.messaginglabs.reaver.config;

import io.messaginglabs.reaver.utils.AddressUtils;

public class Node {

    private String ip;
    private int port;
    private volatile long id = 0;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long id() {
        if (id == 0) {
            id = AddressUtils.composite(ip, port);
        }
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Node node = (Node)o;

        return port == node.port && (ip != null ? ip.equals(node.ip) : node.ip == null);
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}
