package com.netty.meshagent.client.handler;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.netty.meshagent.dubbo.RpcRequest;

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
public class ClientMsgpackEncoder extends MessageToByteEncoder {

    private static Logger logger = LogManager.getLogger(ClientMsgpackEncoder.class);
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        logger.info("client msgpack encode begin");
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        RpcRequest request = (RpcRequest)msg;
        if (request.getId() == null) {
            request.setId(RpcRequest.newId());
        }
        packer.packLong(request.getId());

        packString(packer, request.getInterfaceName());
        packString(packer, request.getMethodName());
        packString(packer, request.getParameterTypes());
        packString(packer, request.getParameter());

        Map<String, String> attachments = request.getAttachments();
        if (attachments != null) {
            packer.packMapHeader(attachments.size());
            for (Entry<String, String> entry : attachments.entrySet()) {
                packString(packer, entry.getKey());
                packString(packer, entry.getValue());
            }
        }
        packer.close();
        out.writeBytes(packer.toByteArray());

        packer.clear();
        logger.info("client msgpack encode done");
    }

    private void packString(MessageBufferPacker packer, String value) throws IOException {
        if (Objects.isNull(value)) {
            packer.packNil();
        } else {
            packer.packString(value);
        }
    }

}
