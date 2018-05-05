package com.alibaba.dubbo.mesh.agent.client;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by juntao.hjt on 2018-04-20 10:50.
 */
public class HttpUtils {

    private static final Logger logger = LogManager.getLogger(HttpUtils.class);

    public static Map<String, String> parseHttpParameter(FullHttpRequest request) {
        Map<String, String> parameterMap = new HashMap<>(4);
        try {
            if (HttpMethod.GET == request.method()) {
                QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
                Map<String, List<String>> keyValueListMap = decoder.parameters();
                for (String key : keyValueListMap.keySet()) {
                    parameterMap.put(key, keyValueListMap.get(key).get(0));
                }
            } else if (HttpMethod.POST == request.method()) {
                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
                decoder.offer(request);
                List<InterfaceHttpData> list = decoder.getBodyHttpDatas();
                for (InterfaceHttpData data : list) {
                    Attribute attr = (Attribute) data;
                    parameterMap.put(attr.getName(), attr.getValue());
                }
                decoder.destroy();
            }
        } catch (Exception e) {
            logger.error("parseHttpParameter error", e);
        }
        return parameterMap;
    }

    public static FullHttpResponse okResponse(byte[] bytes) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bytes));
        setResponseHeader(response);
        return response;
    }

    private static void setResponseHeader(FullHttpResponse response) {
        //response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
    }
}
