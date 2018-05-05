package com.simon.springmvc.http.netty;

import io.netty.channel.ChannelInitializer;

/**
 * @author xiele
 * @date 2018/03/29
 */
public interface ServerConnector {

    int port();

    ChannelInitializer<?> getChannelInitializer();
}
