package com.alibaba.tianchi.dubbo.meshagent.registry;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class LightNode implements Serializable {

    private String protocol;
    private String host;
    private int port;
    private int weight;

    public LightNode() {
    }

    public LightNode(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public LightNode(String host, int port, int weight) {
        this(host, port);
        this.weight = weight;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String nodeKey() {
        return nodeKey(host, port, weight);
    }

    private static String nodeKey(String host, int port, int weight) {
        return host + ":" + port + "_" + weight;
    }

    @Override
    public String toString() {
        return "LightNode{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", weight=" + weight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LightNode lightNode = (LightNode) o;
        return port == lightNode.port &&
                weight == lightNode.weight &&
                Objects.equals(host, lightNode.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, weight);
    }
}
