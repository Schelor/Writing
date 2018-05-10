package com.netty.meshagent.server.handler;

import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.netty.meshagent.dubbo.DubboNettyClient;
import com.netty.meshagent.dubbo.RpcRequest;
import com.netty.meshagent.config.ConnectionConfig;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 负责接收agent发送的请求，并响应从provider获取的结果
 *
 * @author xiele
 * @date 2018/04/14
 */
public class ServerMsgpackHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LogManager.getLogger(ServerMsgpackHandler.class);

    private static DubboNettyClient dubboNettyClient = new DubboNettyClient();

    private static ListeningExecutorService listeningExecutor = MoreExecutors.listeningDecorator(Executors
        .newFixedThreadPool(ConnectionConfig.PROCESSOR_NUM,
            new ThreadFactoryBuilder().setNameFormat("ServerAgentMsgpackWorkerListener").build()));

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        logger.info("receive the msgpack request: {}", msg);

        ListenableFuture<Object> future = listeningExecutor.submit(() -> dubboNettyClient.executeDubboCall(msg));
        Futures.addCallback(future, new FutureCallback<Object>() {

            @Override
            public void onSuccess(@Nullable Object result) {
                ctx.writeAndFlush(result);
            }

            @Override
            public void onFailure(Throwable t) {
                ctx.writeAndFlush("0");
            }
        });
        //Object result = dubboNettyClient.executeDubboCall(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("agent msgpack handler error", cause);
        ctx.writeAndFlush("0");
    }
}
