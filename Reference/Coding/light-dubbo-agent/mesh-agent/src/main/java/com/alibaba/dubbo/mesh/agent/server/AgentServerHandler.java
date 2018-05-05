package com.alibaba.dubbo.mesh.agent.server;

import com.alibaba.dubbo.mesh.agent.common.ChannelPeerManager;
import com.alibaba.dubbo.mesh.agent.common.MsgPack;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Created by juntao.hjt on 2018-04-20 11:03.
 */
@ChannelHandler.Sharable
public class AgentServerHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LogManager.getLogger(AgentServerHandler.class);

    public static final String HANDLER_NAME = AgentServerHandler.class.getName();

    public static final AgentServerHandler INSTANCE = new AgentServerHandler();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Bootstrap bootstrap = AgentServer.getUpstreamBootstrap();
        ChannelFuture channelFuture = bootstrap.connect();
        ChannelPeerManager.addChannelUpstreamFuture(ctx.channel(), channelFuture);
        channelFuture.addListener((f) -> ChannelPeerManager.addChannelPeer(ctx.channel(), channelFuture.channel()));
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        byte[] bytes = new byte[in.readableBytes()];
        in.readBytes(bytes);
        Map<String, String> paramMap = MsgPack.deserialize(bytes);
        Channel outboundChannel = ChannelPeerManager.findOutboundChannel(ctx.channel());
        if (outboundChannel != null) { //连接已经创建好
            outboundChannel.writeAndFlush(paramMap, outboundChannel.voidPromise());
        } else {//连接未创建好
            ChannelFuture channelFuture = ChannelPeerManager.findOutboundChannelFuture(ctx.channel());
            channelFuture.addListener((f) -> channelFuture.channel().writeAndFlush(paramMap));
        }
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelPeerManager.inboundServerChannelClose(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("AgentServerHandler error", cause);
    }
}
