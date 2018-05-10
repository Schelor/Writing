package com.netty.meshagent.agent;

import com.alibaba.fastjson.JSON;

import java.net.URI;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class JsonRequest extends AbstractAgentRequest {

    public static JsonRequest create(URI uri) {
        return new JsonRequest(uri);
    }

    public JsonRequest(URI uri) {
        super(uri);
    }


    @Override
    public String message() {
        logger.info("use json message");
        if (parameter == null || parameter.isEmpty()) {
            return "{}";
        }
        return JSON.toJSONString(rpcRequest());
    }


    @Override
    public URI uri() {
        return uri;
    }
}
