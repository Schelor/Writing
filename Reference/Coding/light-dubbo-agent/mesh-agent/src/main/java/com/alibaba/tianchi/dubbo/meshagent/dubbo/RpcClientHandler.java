package com.alibaba.tianchi.dubbo.meshagent.dubbo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse response) {
        long requestId = response.getRequestId();
        RpcFuture future = RpcRequestHolder.get(requestId);
        if (null != future) {
            RpcRequestHolder.remove(requestId);
            future.done(response);
        }
    }
}
