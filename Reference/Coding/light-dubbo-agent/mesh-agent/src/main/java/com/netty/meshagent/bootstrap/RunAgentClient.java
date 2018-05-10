package com.netty.meshagent.bootstrap;

import com.netty.meshagent.server.HttpServer;

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
