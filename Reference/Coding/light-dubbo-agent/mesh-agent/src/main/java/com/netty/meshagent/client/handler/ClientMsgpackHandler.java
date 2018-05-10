package com.netty.meshagent.client.handler;

import com.netty.meshagent.agent.AgentResponse;
import com.netty.meshagent.agent.MsgpackResponse;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class ClientMsgpackHandler extends SimpleChannelInboundHandler<MsgpackResponse> {

    private static Logger logger = LogManager.getLogger(ClientMsgpackHandler.class);

    private volatile SettableFuture<AgentResponse> future;

    public ClientMsgpackHandler() {
    }

    public void setFuture(SettableFuture<AgentResponse> future) {
        this.future = future;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MsgpackResponse msg) throws Exception {
        // agent server 处理结束后，返回msgpack响应
        if (msg == null) {
            this.future.set(MsgpackResponse.empty());
        } else {
            // 由decoder解码为MsgpackResponse
            System.out.println("real executeAsync success set value:" + Thread.currentThread());
            this.future.set(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("client msgpack handler error", cause);
        this.future.set(MsgpackResponse.empty());
    }

}
