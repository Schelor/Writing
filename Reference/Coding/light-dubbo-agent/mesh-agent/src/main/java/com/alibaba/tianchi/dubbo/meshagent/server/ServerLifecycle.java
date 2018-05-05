package com.alibaba.tianchi.dubbo.meshagent.server;

/**
 * @author xiele
 * @date 2018/04/14
 */
public interface ServerLifecycle {

    void start();

    void stop();

    int port();
}
