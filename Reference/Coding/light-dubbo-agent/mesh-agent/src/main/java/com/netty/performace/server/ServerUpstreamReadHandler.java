package com.netty.performace.server;

import com.netty.performace.common.ChannelPeerManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@ChannelHandler.Sharable
public class ServerUpstreamReadHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LogManager.getLogger(ServerUpstreamReadHandler.class);

    public static final String HANDLER_NAME = ServerUpstreamReadHandler.class + ".channelHandler";

    public static final ServerUpstreamReadHandler INSTANCE = new ServerUpstreamReadHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //通过outboundChannel去找inboundChannel,一定能找到
        Channel inboundChannel = ChannelPeerManager.findInboundChannel(ctx.channel());
        try {
            ByteBuf byteBuf = (ByteBuf) msg;
            //两个换行符 +  一个响应标志
            int len = byteBuf.readableBytes() - 3;
            ByteBuf result = Unpooled.directBuffer(len);
            result.writeBytes(byteBuf, 2, len);
            inboundChannel.writeAndFlush(result);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("AgentServerUpstreamHandler error", cause);
    }
}
