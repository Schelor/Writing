package com.netty.meshagent.dubbo.protocol;

import com.netty.meshagent.dubbo.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.Arrays;
import java.util.List;

public class DubboRpcDecoder extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 16;

    public DubboRpcDecoder(){
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        byte[] data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(data);

        //json序列化 前后写入了两个换行符
        byte[] dataBytes = Arrays.copyOfRange(data, HEADER_LENGTH + 2, data.length - 1);

        byte[] requestIdBytes = Arrays.copyOfRange(data, 4, 12);
        long requestId = ByteUtils.bytes2long(requestIdBytes, 0);

        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setRequestId(requestId);
        rpcResponse.setResult(new String(dataBytes));

        list.add(rpcResponse);
    }

}
