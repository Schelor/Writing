package com.alibaba.dubbo.mesh.agent.client;

import java.util.Map;

import com.alibaba.dubbo.mesh.agent.common.ChannelPeerManager;
import com.alibaba.dubbo.mesh.agent.common.ServerNodeSelector;
import com.alibaba.tianchi.dubbo.meshagent.registry.LightNode;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by juntao.hjt on 2018-04-20 03:03.
 */
@ChannelHandler.Sharable
public class AgentClientHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LogManager.getLogger(AgentClientHandler.class);

    public static final AgentClientHandler INSTANCE = new AgentClientHandler();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LightNode targetNode = ServerNodeSelector.selectWeightNode();
        Bootstrap bootstrap = AgentClient.getUpstreamBootstrap(targetNode);
        ChannelFuture channelFuture = bootstrap.connect();
        ChannelPeerManager.addChannelUpstreamFuture(ctx.channel(), channelFuture);
        channelFuture.addListener((f) -> ChannelPeerManager.addChannelPeer(ctx.channel(), channelFuture.channel()));
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest request = (FullHttpRequest) msg;
        Map<String, String> paramMap = HttpUtils.parseHttpParameter(request);
        Channel outboundChannel = ChannelPeerManager.findOutboundChannel(ctx.channel());
        if (outboundChannel != null) {//连接已经创建好
            outboundChannel.writeAndFlush(paramMap, outboundChannel.voidPromise());
        } else {//连接未创建好
            ChannelFuture channelFuture = ChannelPeerManager.findOutboundChannelFuture(ctx.channel());
            channelFuture.addListener((f) -> channelFuture.channel().writeAndFlush(paramMap));
        }
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ChannelPeerManager.inboundClientChannelClose(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("AgentClientHandler error", cause);
    }
}
