package com.netty.meshagent.agent;

import io.netty.channel.ChannelInitializer;

/**
 * @author xiele
 * @date 2018/04/14
 */
public interface ChannelInitialConnector {

    ChannelInitializer<?> getChannelInitializer();
}
