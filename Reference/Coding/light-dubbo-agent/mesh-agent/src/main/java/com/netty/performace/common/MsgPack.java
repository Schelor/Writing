package com.netty.performace.common;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.buffer.ArrayBufferInput;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by juntao.hjt on 2018-04-20 09:05.
 */
public class MsgPack {

    private static final Logger logger = LogManager.getLogger(MsgPack.class);

    private static final ThreadLocal<MessageBufferPacker> PACKER_THREAD_LOCAL =
            ThreadLocal.withInitial(MessagePack::newDefaultBufferPacker);//需要缓存 非线程安全

    private static final ThreadLocal<MessageUnpacker> UNPACKER_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> MessagePack.newDefaultUnpacker(new byte[0])); //缓存 200万 -2s

    public static byte[] serialize(Map<String, String> paramMap) {
        MessageBufferPacker bufferPacker = PACKER_THREAD_LOCAL.get();
        try {
            bufferPacker.clear();
            bufferPacker.packMapHeader(paramMap.size());
            for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                bufferPacker.packString(entry.getKey());
                bufferPacker.packString(entry.getValue());
            }
            bufferPacker.close();
        } catch (Exception e) {
            logger.error("serialize param map error", e);
        }
        return bufferPacker.toByteArray();
    }

    public static Map<String, String> deserialize(byte[] dataBytes) {
        try {
            MessageUnpacker messageUnpacker = UNPACKER_THREAD_LOCAL.get();
            ArrayBufferInput bufferInput = new ArrayBufferInput(dataBytes);
            messageUnpacker.reset(bufferInput);
            int mapSizeHeader = messageUnpacker.unpackMapHeader();
            Map<String, String> paramMap = new HashMap<>(mapSizeHeader);
            for (int i = 0; i < mapSizeHeader; i++) {
                String key = messageUnpacker.unpackString();
                String val = messageUnpacker.unpackString();
                paramMap.put(key, val);
            }
            messageUnpacker.close();
            return paramMap;
        } catch (Exception e) {
            logger.error("deserialize param map error", e);
        }
        return Collections.emptyMap();
    }

    public static void main(String[] args) {
        String str = RandomStringUtils.random(1024, true, true);
        Map<String, String> paramMap = new HashMap<>(4);
        paramMap.put("interface", "com.alibaba.dubbo.performance.demo.provider.IHelloService");
        paramMap.put("method", "hash");
        paramMap.put("parameterTypesString", "Ljava/lang/String;");
        paramMap.put("parameter", "zA0paVS1P5xOiNTPq3WY0HhIKTKMKK31DTDihkD4x1CrffXAno8AGYPUnB6cDWHScCxa2YurQMnttwd8wv9YESqw3fZZ2SMGIQmPsELptBUQV6cuHT19F5Tctttp4SfVw6rX8AUDwTtgdq7fJ3r1BnIIRT0gMTFHfplw3oGXdOuL8Z4aKMc2VJrEtFLhjMJorDa8wK0SbzA0paVS1P5xOiNTPq3WY0HhIKTKMKK31DTDihkD4x1CrffXAno8AGYPUnB6cDWHScCxa2YurQMnttwd8wv9YESqw3fZZ2SMGIQmPsELptBUQV6cuHT19F5Tctttp4SfVw6rX8AUDwTtgdq7fJ3r1BnIIRT0gMTFHfplw3oGXdOuL8Z4aKMc2VJrEtFLhjMJorDa8wK0Sb41hYHWFLppTKSO8Y19E7e1LB0vI75cOvrAVYXimWsytC8WIVUb8OhQtaCMRELj8gXqsvcnKZtE26SvdTGbzXRDXBr1Y1QerVsm9tbUKh0ZxVqNazpZ9P3e4CFhKFpvN1Mb34VHNIhAmbH5gIVgmBjsAbjzH1KvKwqURhV7cHXCFxFskDzZlm3rijvNW5LMZFzYFFVLoFR9mnaozN3aqACiNrzwR72xgvO5l8Z9DAqj0Rpc8nAbdDAESFKAr6d1sfPb7ptemyiMvm6QBjqRA53TuNjFz5laT7LdaTALwNVHxGVWubSmyL6wNqwFa7PIPSiM1rC2877bCmDjER7w46siSP2MGBMOCKSr5vSZvEWU82PDzdnDLZaGqyHZ94CauyNiT5QUJ1nQIEeaQ3JtlY0lDV7a28sxyDzQfnUJIj4fYHb1vXIEOrtyHQUcCcon6PlVnITvehSrvRzsWoPWN6vqwQQMd4C8MXYjSPKn3GkvoetOHaAWU9ZT41hYHWFLppTKSO8Y19E7e1LB0vI75cOvrAVYXimWsytC8WIVUb8OhQtaCMRELj8gXqsvcnKZtE26SvdTGbzXRDXBr1Y1QerVsm9tbUKh0xgvO5l8wFJI932j");
    }
}
