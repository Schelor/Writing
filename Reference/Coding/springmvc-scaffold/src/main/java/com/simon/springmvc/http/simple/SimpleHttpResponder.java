package com.simon.springmvc.http.simple;

import io.netty.handler.codec.http.*;

/**
 * @author xiele
 * @date 2018/03/29
 */
public class SimpleHttpResponder extends HttpResponder {

    protected FullHttpResponse generateResponse(HttpRequest request, HttpHeaders headers) {
        return createResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, "This is sugar http server".getBytes());
    }

    // Netty4ClientHttpRequest
}
