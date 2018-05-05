package com.simon.springmvc.http.netty.bootstrap;

import com.simon.springmvc.http.netty.NettyHttpServer;

/**
 * @author xiele
 * @date 2018/04/07
 */
public class BootstrapPortal {

    public static void main(String[] args) {

        NettyHttpServer server = new NettyHttpServer();

        server.start();
    }

}
