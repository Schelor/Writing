package com.alibaba.tianchi.dubbo.meshagent.server.handler.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.tianchi.dubbo.meshagent.dubbo.DubboNettyClient;
import com.alibaba.tianchi.dubbo.meshagent.dubbo.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 负责接收agent发送的json请求，并响应从provider获取的结果
 *
 * @author xiele
 * @date 2018/04/14
 */
public class ServerJsonHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LogManager.getLogger(ServerJsonHandler.class);

    private static DubboNettyClient dubboNettyClient = new DubboNettyClient();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        logger.info("receive the agent request: {}", msg);

        RpcRequest request = JSON.parseObject(msg, RpcRequest.class);

        logger.info("requestId=" + request.getId());

        Object result = dubboNettyClient.executeDubboCall(request);

        ctx.writeAndFlush(result);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("agent json handler error", cause);
        ctx.writeAndFlush("{\"msg\": \"error\"}");
    }
}
