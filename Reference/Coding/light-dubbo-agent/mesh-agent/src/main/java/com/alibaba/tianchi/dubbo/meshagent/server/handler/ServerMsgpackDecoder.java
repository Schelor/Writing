package com.alibaba.tianchi.dubbo.meshagent.server.handler;

import com.alibaba.tianchi.dubbo.meshagent.dubbo.RpcRequest;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;
import org.msgpack.value.ValueType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class ServerMsgpackDecoder extends ByteToMessageDecoder {

    private static Logger logger = LogManager.getLogger(ServerMsgpackDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        logger.info("server msgpacker decode begin.");

        byte[] data = new byte[in.readableBytes()];
        in.readBytes(data);

        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        logger.info("server decoder readable lenght: {}", data.length);

        RpcRequest request = new RpcRequest();
        request.setId(unpacker.unpackLong());
        request.setInterfaceName(unpackString(unpacker));
        request.setMethodName(unpackString(unpacker));
        request.setParameterTypes(unpackString(unpacker));
        request.setParameter(unpackString(unpacker));
        request.setAttachments(unpackMap(unpacker));

        out.add(request);
        logger.info("server msgpacker decode done. requestId: {}", request.getId());
    }

    private String unpackString(MessageUnpacker unpacker) throws IOException {
        ImmutableValue val = unpacker.unpackValue();
        if (val == null) {
            return null;
        }
        if (val.getValueType() == ValueType.NIL) {
            return null;
        }
        if (val.isStringValue()) {
            return val.asStringValue().asString();
        }
        return null;
    }

    private Map<String, String> unpackMap(MessageUnpacker unpacker) throws IOException {
        int mapSize = unpacker.unpackMapHeader();
        if (mapSize > 0) {
            Map<String, String> attr = Maps.newHashMapWithExpectedSize(mapSize);
            for (int i = 0; i < mapSize; ++i) {
                String key = unpackString(unpacker);
                String value = unpackString(unpacker);
                if (Objects.nonNull(key)) {
                    attr.put(key, value);
                }
            }
            return attr;
        }
        return null;
    }
}
