package com.netty.meshagent.server.handler;

import com.netty.meshagent.agent.ChannelInitialConnector;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class ServerMsgpackConnector implements ChannelInitialConnector {

    @Override
    public ChannelInitializer<?> getChannelInitializer() {
        return new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new ServerMsgpackEncoder())
                        .addLast(new ServerMsgpackDecoder())
                        .addLast(new ServerMsgpackHandler());
            }
        };
    }
}
