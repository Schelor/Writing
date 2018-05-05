package com.alibaba.dubbo.mesh.agent.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


@ChannelHandler.Sharable
public class ServerUpstreamWriteHandler extends MessageToByteEncoder {

    private static Logger logger = LogManager.getLogger(ServerUpstreamWriteHandler.class);

    public static final String HANDLER_NAME = ServerUpstreamWriteHandler.class + ".channelHandler";

    public static final ServerUpstreamWriteHandler INSTANCE = new ServerUpstreamWriteHandler();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        DubboEncoder.encode((Map) msg, out);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("AgentServerUpstreamHandler error", cause);
    }
}
