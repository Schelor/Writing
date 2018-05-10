package com.netty.meshagent.util;

import java.util.concurrent.ThreadLocalRandom;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * @author xiele
 * @date 2018/04/14
 */
public final class ResponseBuilder {

    public static FullHttpResponse response(final HttpVersion version, final HttpResponseStatus status, String  payload) {

        if (payload == null || payload.length() == 0) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(version, status);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
            return response;
        }
        byte[] bytes = payload.getBytes();
        FullHttpResponse response = new DefaultFullHttpResponse(version, status, Unpooled.copiedBuffer(
            bytes));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        return response;
    }

    public static  FullHttpResponse success(String body) {

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(body.getBytes()));

        return response;
    }

    public static FullHttpResponse badGateWay() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.BAD_GATEWAY,
            Unpooled.copiedBuffer(HttpResponseStatus.BAD_GATEWAY.toString().getBytes()));
        return response;
    }

    public static FullHttpResponse badRequest(String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.BAD_REQUEST,
            Unpooled.copiedBuffer(message.getBytes()));
        return response;
    }

    public static FullHttpResponse unsupportedMethod() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.METHOD_NOT_ALLOWED,
            Unpooled.copiedBuffer(HttpResponseStatus.METHOD_NOT_ALLOWED.toString().getBytes()));
        return response;
    }

    public static FullHttpResponse noSurvivor() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.SERVICE_UNAVAILABLE,
            Unpooled.copiedBuffer("no survivor of provider".getBytes()));
        return response;
    }

    public static FullHttpResponse mockSuccess() {
        long l = ThreadLocalRandom.current().nextLong();
        byte[] content = String.valueOf(l).getBytes();
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(content));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

        return response;

    }
}
