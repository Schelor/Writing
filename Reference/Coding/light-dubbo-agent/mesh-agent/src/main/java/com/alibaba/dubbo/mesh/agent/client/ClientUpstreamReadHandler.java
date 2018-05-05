package com.alibaba.dubbo.mesh.agent.client;

import com.alibaba.dubbo.mesh.agent.common.ChannelPeerManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ChannelHandler.Sharable
public class ClientUpstreamReadHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LogManager.getLogger(ClientUpstreamReadHandler.class);

    public static final String HANDLER_NAME = ClientUpstreamReadHandler.class + ".channelHandler";

    public static ClientUpstreamReadHandler INSTANCE = new ClientUpstreamReadHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        //通过outboundChannel去找inboundChannel
        Channel channel = ChannelPeerManager.findInboundChannel(ctx.channel());
        channel.writeAndFlush(HttpUtils.okResponse(bytes), channel.voidPromise());
        ReferenceCountUtil.release(msg);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("AgentClientUpstreamHandler error", cause);
    }
}