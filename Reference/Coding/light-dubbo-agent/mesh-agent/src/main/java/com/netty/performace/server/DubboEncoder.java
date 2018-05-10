package com.netty.performace.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.netty.meshagent.dubbo.protocol.ByteUtils;

import io.netty.buffer.ByteBuf;

/**
 * Created by juntao.hjt on 2018-04-21 22:21.
 */
public class DubboEncoder {

    // header length.
    public static final int HEADER_LENGTH = 16;
    // magic header.
    private static final short MAGIC = (short) 0xdabb;
    // message flag.
    private static final byte FLAG_REQUEST = (byte) 0x80;
    private static final byte FLAG_TWOWAY = (byte) 0x40;

    public static void encode(Map<String, String> paramData, ByteBuf buffer) throws Exception {
        // header.
        byte[] header = new byte[HEADER_LENGTH];
        // set magic number.
        ByteUtils.short2bytes(MAGIC, header);

        // set request and serialization flag.
        header[2] = (byte) (FLAG_REQUEST | 6);

        header[2] |= FLAG_TWOWAY;

        // set request id.
        ByteUtils.long2bytes(1, header, 4);

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encodeAllRequestDataDirectly(bos, paramData);

        int len = bos.size();
        buffer.writeBytes(bos.toByteArray());
        ByteUtils.int2bytes(len, header, 12);

        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);

    }

    private static void encodeRequestData(OutputStream out, Map<String, String> paramMap) throws Exception {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        writeObject("", writer); //dubbo version
        writeObject(paramMap.get("interface"), writer); //service name
        writeObject("0.0.0", writer); //service version
        writeObject(paramMap.get("method"), writer); //method name
        writeObject(paramMap.get("parameterTypesString"), writer); //parameter types
        writeObject(paramMap.get("parameter"), writer);//arguments

        Map<String, String> attachments = new HashMap<>(0);
        writeObject(attachments, writer);
    }

    private static void writeObject(Object obj, PrintWriter writer) throws IOException {
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);
        serializer.write(obj);
        out.writeTo(writer);
        out.close(); // for reuse SerializeWriter buf
        writer.println();
        writer.flush();
    }

    private static void encodeAllRequestData(OutputStream os, Map<String, String> paramMap) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os));
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);

        /// dubbo version
        serializer.write("");
        serializer.println();

        //service name
        serializer.write(paramMap.get("interface"));
        serializer.println();

        //service version
        serializer.write("0.0.0");
        serializer.println();

        //method name
        serializer.write(paramMap.get("method"));
        serializer.println();

        //parameter types
        serializer.write(paramMap.get("parameterTypesString"));
        serializer.println();

        //arguments
        serializer.write(paramMap.get("parameter"));
        serializer.println();

        // attachments
        Map<String, String> attachments = new HashMap<>(0);
        serializer.write(attachments);
        serializer.println();

        out.writeTo(writer);
        writer.flush();

    }

    private static void encodeAllRequestDataDirectly(OutputStream os, Map<String, String> paramMap) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os));
        SerializeWriter out = new SerializeWriter();

        StringBuilder builder = new StringBuilder();
        builder
            .append("\"\"\n" + "\"" + paramMap.get("interface") + "\"\n" + "\"" + "0.0.0" + "\"\n" + "\"" + paramMap.get("method") + "\"\n" + "\"" + paramMap.get("parameterTypesString") + "\"\n" + "\"" + paramMap.get("parameter") + "\"\n{}\n")
            ;
        out.write(builder.toString());
        out.writeTo(writer);
        writer.flush();

    }

    private static final ThreadLocal<DubboEncoderResource> DUBBO_ENCODER_RESOURCE =
        ThreadLocal.withInitial(DubboEncoderResource::new);

    private static class DubboEncoderResource {
        private ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private PrintWriter pw = new PrintWriter(new OutputStreamWriter(bos));
        private StringBuilder builder = new StringBuilder();
        public ByteArrayOutputStream bos() {
            bos.reset();
            return bos;
        }

        public PrintWriter pw() {
            return pw;
        }

        public StringBuilder builder() {
            int length = builder.length();
            if (length > 0) {
                builder.delete(0, length);
            }
            return builder;
        }
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> paramMap = new HashMap<>(4);
        paramMap.put("interface", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        paramMap.put("method", "hash");
        paramMap.put("parameterTypesString", "Ljava/lang/String;");
        paramMap.put("parameter", "aaa1234");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encodeRequestData(bos, paramMap);

        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        encodeAllRequestData(bos2, paramMap);
        ByteArrayOutputStream bos3 = new ByteArrayOutputStream();
        encodeAllRequestDataDirectly(bos3, paramMap);

        String a1 = bos.toString();
        String a2 = bos2.toString();
        String a3 = bos3.toString();

        System.out.println(a1.toCharArray());
        System.out.println(a2.toCharArray());
        System.out.println(a3.toCharArray());

        System.out.println(a1.equals(a2));
        System.out.println(a1.equals(a3));

    }


}
