package com.netty.meshagent.client.handler;

import com.netty.meshagent.agent.AgentResponse;
import com.netty.meshagent.agent.JsonResponse;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class ClientJsonHandler extends SimpleChannelInboundHandler<String> {

    private static Logger logger = LogManager.getLogger(ClientJsonHandler.class);

    private SettableFuture<AgentResponse> future;

    public ClientJsonHandler(SettableFuture<AgentResponse> future) {
        this.future = future;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // agent server 处理结束后，返回json响应
        if (msg == null) {
            this.future.set(JsonResponse.empty());
        } else {
            this.future.set(JsonResponse.json(msg));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("client json handler error", cause);
        this.future.set(JsonResponse.empty());
    }
}
