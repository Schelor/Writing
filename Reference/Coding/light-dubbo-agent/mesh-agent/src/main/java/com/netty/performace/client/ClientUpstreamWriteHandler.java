package com.netty.performace.client;

import com.netty.performace.common.MsgPack;
import com.netty.meshagent.dubbo.protocol.ByteUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@ChannelHandler.Sharable
public class ClientUpstreamWriteHandler extends MessageToByteEncoder {

    private static Logger logger = LogManager.getLogger(ClientUpstreamWriteHandler.class);

    public static final String HANDLER_NAME = ClientUpstreamWriteHandler.class + ".channelHandler";

    public static ClientUpstreamWriteHandler INSTANCE = new ClientUpstreamWriteHandler();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        Map<String, String> paramMap = (Map) msg;
        byte[] bytes = MsgPack.serialize(paramMap);
        out.writeBytes(ByteUtils.short2bytes((short)bytes.length));
        out.writeBytes(bytes);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("AgentClientUpstreamEncodeHandler error", cause);
    }
}