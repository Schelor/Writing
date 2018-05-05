package com.alibaba.tianchi.dubbo.meshagent.bootstrap;

import com.alibaba.tianchi.dubbo.meshagent.server.HttpServer;

/**
 * @author xiele
 * @date 2018/04/16
 */
public class RunAgentClient {

    public static void main(String[] args) throws Exception {

       run();

    }

    public static void run() {
        HttpServer http = new HttpServer();
        http.start();
    }
}
