package com.netty.meshagent.client.handler;

import java.util.List;

import com.netty.meshagent.agent.MsgpackResponse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class ClientMsgpackDecoder extends ByteToMessageDecoder {

    private static Logger logger = LogManager.getLogger(ClientMsgpackDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        logger.info("client msgpacker decode begin.");

        byte[] data = new byte[in.readableBytes()];
        in.readBytes(data);

        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        logger.info("server decoder readable lenght: {}", data.length);
        ImmutableValue val = unpacker.unpackValue();
        String text = null;
        if (val != null && val.isStringValue()) {
            text = val.asStringValue().asString();
        }
        out.add(MsgpackResponse.text(text));
        logger.info("client msgpacker decode done.");
    }

}
