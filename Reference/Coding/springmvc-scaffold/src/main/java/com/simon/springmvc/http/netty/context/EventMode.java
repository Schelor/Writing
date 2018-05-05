package com.simon.springmvc.http.netty.context;

/**
 * @author xiele
 * @date 2018/04/07
 */
public enum EventMode {

    EPOLL("epoll"),

    NIO("nio"),

    ;

    private String use;

    EventMode(String use) {
        this.use = use;
    }

    public String use() {
        return use;
    }

    public static boolean isEpoll(String eventMode) {
        return EPOLL.use().equals(eventMode);
    }

    public static boolean isNio(String eventMode) {
        return NIO.use().equals(eventMode);
    }
}
