package com.netty.meshagent.client;

import java.net.URI;

import com.netty.meshagent.agent.AgentClient;
import com.netty.meshagent.agent.AgentRequest;
import com.netty.meshagent.agent.ChannelInitialConnector;
import com.netty.meshagent.util.CodecBuilder;
import com.netty.meshagent.agent.AgentResponse;
import com.netty.meshagent.agent.JsonResponse;
import com.netty.meshagent.client.handler.ClientJsonHandler;
import com.netty.meshagent.config.ConnectionConfig;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class JsonAgentClient extends ConnectionFactory implements AgentClient {

    public JsonAgentClient() {
        super();
    }

    @Override
    public ChannelInitialConnector clientConnector() {
        return () -> new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel channel) throws Exception {
                configureChannel(channel.config());
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new StringDecoder())
                    .addLast(new StringEncoder())
                    .addLast(new DelimiterBasedFrameDecoder(ConnectionConfig.MAX_FRAME_LENGTH,
                        CodecBuilder.defaultDelimiter()));

            }
        };
    }

    @Override
    public ListenableFuture<AgentResponse> executeAsync(AgentRequest request) {
        String message = request.message();
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("json message is empty");
        }
        SettableFuture<AgentResponse> responseFuture = SettableFuture.create();
        ChannelFutureListener connectionListener = future -> {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                channel.pipeline().addLast(new ClientJsonHandler(responseFuture));
                channel.writeAndFlush(message);
            } else {
                // 没连上，需要选择重连
                responseFuture.set(JsonResponse.empty());
            }
        };
        URI uri = request.uri();
        ChannelFuture connect = this.getBootstrap().connect(uri.getHost(),
            getPort(uri));
        connect.addListener(connectionListener);
        Channel channel = connect.channel();
        logger.info("connectChannelId: {}", channel.id());
        return responseFuture;
    }
}

