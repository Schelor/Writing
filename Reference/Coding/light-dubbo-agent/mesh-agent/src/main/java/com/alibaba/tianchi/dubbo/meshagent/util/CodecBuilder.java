package com.alibaba.tianchi.dubbo.meshagent.util;

import com.alibaba.tianchi.dubbo.meshagent.config.MessageConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class CodecBuilder {

    public static ByteBuf[] defaultDelimiter() {
        return new ByteBuf[]
            { Unpooled.wrappedBuffer(new byte[] {MessageConstants.JSON_MESSAGE_CONNECTOR }),
            };
    }
}
