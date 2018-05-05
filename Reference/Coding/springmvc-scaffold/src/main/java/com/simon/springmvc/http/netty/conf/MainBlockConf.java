package com.simon.springmvc.http.netty.conf;

import java.io.Serializable;

/**
 * @author xiele
 * @date 2018/04/04
 */
public class MainBlockConf implements Serializable {

    private String user;

    private int readClientTimeout = -1;

    private int connectionTimeout = -1;

    private EventBlockConf events;

    private HttpBlockConf http;


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getReadClientTimeout() {
        return readClientTimeout;
    }

    public void setReadClientTimeout(int readClientTimeout) {
        this.readClientTimeout = readClientTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public EventBlockConf getEvents() {
        return events;
    }

    public void setEvents(EventBlockConf events) {
        this.events = events;
    }

    public HttpBlockConf getHttp() {
        return http;
    }

    public void setHttp(HttpBlockConf http) {
        this.http = http;
    }
}
