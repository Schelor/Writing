package com.alibaba.tianchi.dubbo.meshagent.dubbo.protocol;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.tianchi.dubbo.meshagent.dubbo.RpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.*;

public class DubboRpcEncoder extends MessageToByteEncoder {
    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        RpcRequest req = (RpcRequest) msg;
        // header.
        byte[] header = new byte[HEADER_LENGTH];
        // set magic number.
        ByteUtils.short2bytes(MAGIC, header);

        // set request and serialization flag.
        header[2] = (byte) (FLAG_REQUEST | 6);

        header[2] |= FLAG_TWOWAY;

        // set request id.
        ByteUtils.long2bytes(req.getId(), header, 4);

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        encodeRequestData(bos, req);

        int len = bos.size();
        buffer.writeBytes(bos.toByteArray());
        ByteUtils.int2bytes(len, header, 12);

        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
    }

    private void encodeRequestData(OutputStream out, Object data) throws Exception {

        RpcRequest rpcRequest = (RpcRequest)data;


        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        writeObject(rpcRequest.getAttachment("dubbo", "2.0.1"), writer);
        writeObject(rpcRequest.getAttachment("path"), writer);
        writeObject(rpcRequest.getAttachment("version"), writer);
        writeObject(rpcRequest.getMethodName(), writer);
        writeObject(rpcRequest.getParameterTypes(), writer);

        writeObject(rpcRequest.getParameter(), writer);
        writeObject(rpcRequest.getAttachments(), writer);
    }

    private static void writeObject(Object obj, PrintWriter writer) throws IOException {

        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);

        serializer.config(SerializerFeature.WriteEnumUsingToString, true);
        serializer.write(obj);

        out.writeTo(writer);
        out.close(); // for reuse SerializeWriter buf

        writer.println();
        writer.flush();
    }

    private static void writeBytes(byte[] bytes, PrintWriter writer) {
        writer.print(new String(bytes));
        writer.flush();
    }

}
