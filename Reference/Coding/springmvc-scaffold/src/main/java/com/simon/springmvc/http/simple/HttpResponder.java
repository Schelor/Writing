package com.simon.springmvc.http.simple;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xiele
 * @date 2018/03/29
 */
@Slf4j
public abstract class HttpResponder {

    public FullHttpResponse processRequest(final FullHttpRequest request) {

        // 是否支持协议版本
        if (request.getProtocolVersion() != HttpVersion.HTTP_1_1) {
            return createResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED,  "HTTP 1.1 Required".getBytes());
        }

        HttpHeaders headers = request.headers();
        log.info("Request Headers: {}", headers);

        // 业务处理
        FullHttpResponse response = generateResponse(request, headers);
        if (HttpHeaders.isKeepAlive(request)) {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        // 设置cookie
        headers.set(
                HttpHeaders.Names.COOKIE, ClientCookieEncoder.encode(
                        new DefaultCookie("my-cookie", "foo"),
                        new DefaultCookie("another-cookie", "bar"))
        );


        return response;
    }



    public static FullHttpResponse createResponse(final HttpVersion version, final HttpResponseStatus status, byte[] payload) {

        if (payload == null || payload.length == 0) {
            return new DefaultFullHttpResponse(version, status);

        }

        FullHttpResponse response = new DefaultFullHttpResponse(version, status, Unpooled.copiedBuffer(payload));
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, payload.length);
        return response;
    }

    protected abstract FullHttpResponse generateResponse(HttpRequest request, HttpHeaders headers);

}
