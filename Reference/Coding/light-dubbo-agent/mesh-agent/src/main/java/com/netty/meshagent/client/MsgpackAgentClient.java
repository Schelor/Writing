package com.netty.meshagent.client;

import com.netty.meshagent.client.handler.ClientMsgpackDecoder;
import com.netty.meshagent.agent.AgentClient;
import com.netty.meshagent.agent.AgentRequest;
import com.netty.meshagent.agent.AgentResponse;
import com.netty.meshagent.agent.ChannelInitialConnector;
import com.netty.meshagent.agent.MsgpackResponse;
import com.netty.meshagent.client.handler.ClientMsgpackEncoder;
import com.netty.meshagent.client.handler.ClientMsgpackHandler;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class MsgpackAgentClient extends ConnectionFactory implements AgentClient {

    private static final String CLIENT_MSG_PACK_HANDLER = ClientMsgpackHandler.class.getName();

    public ListenableFuture<AgentResponse> executeAsync(AgentRequest request) {
        SettableFuture<AgentResponse> responseFuture = SettableFuture.create();
        Channel channel = null;
        URI uri = request.uri();
        ChannelPool channelPool = getChannelPool(uri);
        if (channelPool == null) {
            responseFuture.set(MsgpackResponse.empty());
        }
        try {
            channel = getChannel(channelPool);
            setNewFuture(channel, responseFuture);
            channel.writeAndFlush(request.message());
        } catch (Exception e) {
            logger.error("MsgpackAgentClient executeAsync error", e);
            responseFuture.set(MsgpackResponse.empty());
        } finally {
            if (channelPool != null && channel != null) {
                releaseChannel(channelPool, channel);
            }
        }
        return responseFuture;
    }

    private void setNewFuture(Channel channel, SettableFuture newFuture) {
        ClientMsgpackHandler msgpackHandler = (ClientMsgpackHandler) channel.pipeline().get(CLIENT_MSG_PACK_HANDLER);
        msgpackHandler.setFuture(newFuture);
    }


    @Override
    public ChannelInitialConnector clientConnector() {
        return () -> new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel channel) throws Exception {
                configureChannel(channel.config());
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new ClientMsgpackEncoder());
                pipeline.addLast(new ClientMsgpackDecoder());
                pipeline.addLast(CLIENT_MSG_PACK_HANDLER, new ClientMsgpackHandler());
            }
        };
    }


}
