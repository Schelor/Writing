package com.simon.springmvc.http.simple;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xiele
 * @date 2018/03/29
 */
@Slf4j @Data
public class HttpHandler extends ChannelInboundHandlerAdapter {

    private final HttpResponder httpResponder;

    public HttpHandler(HttpResponder httpResponder) {
        this.httpResponder = httpResponder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        // 开始读取客户端发送的数据
        if (msg instanceof FullHttpRequest) {

            ctx.writeAndFlush(httpResponder.processRequest((FullHttpRequest) msg));

        } else {

            super.channelRead(ctx, msg);
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        // 读取客户端数据完成
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       log.error("读取http请求出现异常", cause);


    }
}
