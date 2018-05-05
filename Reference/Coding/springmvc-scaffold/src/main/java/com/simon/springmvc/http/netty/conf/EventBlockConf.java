package com.simon.springmvc.http.netty.conf;

/**
 * @author xiele
 * @date 2018/04/04
 */
public class EventBlockConf {

    private String use;

    private int workerConnections;

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public int getWorkerConnections() {
        return workerConnections;
    }

    public void setWorkerConnections(int workerConnections) {
        this.workerConnections = workerConnections;
    }
}
