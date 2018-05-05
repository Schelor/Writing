package com.alibaba.tianchi.dubbo.meshagent.agent;

import java.net.URI;

import com.alibaba.fastjson.JSON;
import com.alibaba.tianchi.dubbo.meshagent.dubbo.RpcRequest;

/**
 * Msgpack请求与响应的编解码在pipeline中
 *
 * @author xiele
 * @date 2018/04/17
 */
public class MsgpackRequest extends AbstractAgentRequest {

    public static MsgpackRequest create(URI uri) {
        return new MsgpackRequest(uri);
    }

    public MsgpackRequest() {
        super(null);
    }

    public MsgpackRequest(URI uri) {
        super(uri);
    }


    @Override
    public URI uri() {
        return uri;
    }

    @Override
    public RpcRequest message() {
        // check not null
        RpcRequest request = rpcRequest();

        logger.info("use msgpack message: {}", JSON.toJSONString(request));
        return request;
    }
}
