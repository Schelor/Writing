package com.netty.meshagent.server.handler;

import java.io.IOException;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class ServerMsgpackEncoder extends MessageToByteEncoder {

    private static Logger logger = LogManager.getLogger(ServerMsgpackEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        logger.info("server msgpack encode begin");
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        if (msg == null) {
           packer.packNil();
        } else {
            packString(packer, msg);
        }
        packer.close();
        out.writeBytes(packer.toByteArray());
        packer.clear();
        logger.info("server msgpack encode done");
    }

    private void packString(MessageBufferPacker packer, Object value) throws IOException {
        if (Objects.isNull(value)) {
            packer.packNil();
        } else {
            packer.packString((String) value);
        }
    }

    private void packLong(MessageBufferPacker packer, Object value) throws IOException {
        if (value == null) {
            packer.packNil();
            return;
        }
        if (value instanceof Long) {
            packer.packLong((Long) value);
            return;
        }

    }
}
