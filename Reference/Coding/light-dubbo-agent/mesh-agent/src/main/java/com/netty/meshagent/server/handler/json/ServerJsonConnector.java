package com.netty.meshagent.server.handler.json;

import com.netty.meshagent.agent.ChannelInitialConnector;
import com.netty.meshagent.config.ConnectionConfig;
import com.netty.meshagent.util.CodecBuilder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class ServerJsonConnector implements ChannelInitialConnector {

    private final static int MAX_FRAME_LENGTH = ConnectionConfig.MAX_FRAME_LENGTH;

    @Override
    public ChannelInitializer<?> getChannelInitializer() {
        return new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel ch) throws Exception {
                ch.pipeline()
                    .addLast(new StringDecoder())
                    .addLast(new StringEncoder())
                    .addLast(new DelimiterBasedFrameDecoder(MAX_FRAME_LENGTH,
                        CodecBuilder.defaultDelimiter()))
                    .addLast(new ServerJsonHandler())
                ;

            }
        };
    }


}
