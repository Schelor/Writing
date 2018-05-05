package com.alibaba.tianchi.dubbo.meshagent.agent;

import io.netty.channel.ChannelInitializer;

/**
 * @author xiele
 * @date 2018/04/14
 */
public interface ChannelInitialConnector {

    ChannelInitializer<?> getChannelInitializer();
}
