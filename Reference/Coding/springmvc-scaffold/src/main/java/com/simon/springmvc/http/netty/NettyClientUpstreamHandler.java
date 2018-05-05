package com.simon.springmvc.http.netty;

import com.simon.springmvc.http.netty.context.StoredUpstreamServer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpHeaders.Names;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.concurrent.ListenableFuture;

import java.io.IOException;

/**
 * @author xiele
 * @date 2018/04/01
 */
@Slf4j
public class NettyClientUpstreamHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private NettyClientHttpRequestFactory factory;

    public NettyClientUpstreamHandler() {
        factory = new NettyClientHttpRequestFactory();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        log.info("接收到的请求信息：{}", httpRequest.toString());
        final String host = getHostFromHeader(httpRequest);
        String upstreamHost = StoredUpstreamServer.getUpstreamHost(host, httpRequest.getUri());
        if (StoredUpstreamServer.supportUpstream(upstreamHost)) {
            doExecuteProxy(ctx, upstreamHost, httpRequest);
        } else {
           ctx.writeAndFlush(createBadRequestResponse());
        }
    }

    private void doExecuteProxy(ChannelHandlerContext ctx, final String upstreamHost, FullHttpRequest httpRequest) throws IOException {

        try {
            // 使用Netty转发到上游服务器
            // 解析path + queryString
            NettyClientHttpProxyRequest request = factory.createUpstreamRequest(upstreamHost);
            ListenableFuture<FullHttpResponse> listenableFuture = request.executeInternal(httpRequest);

            FullHttpResponse proxyResponse = listenableFuture.get();
            log.info("代理转发收到的响应为: {}", proxyResponse);

            ctx.writeAndFlush(proxyResponse);

        } catch (Exception e) {
            log.error("转发请求出现异常", e);
            ctx.writeAndFlush(create502BadGatewayResponse());
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        ctx.writeAndFlush(create502BadGatewayResponse());

    }

    private static String getHostFromHeader(FullHttpRequest httpRequest) {
        String host = httpRequest.headers().get(Names.HOST);
        if (host.indexOf(":") > 0) {
            return host.substring(0, host.lastIndexOf(":"));
        }
        return host;
    }

    public static FullHttpResponse createBadRequestResponse() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.BAD_REQUEST,
            Unpooled.copiedBuffer("no upstream host found".getBytes()));
        return response;
    }

    public static FullHttpResponse create502BadGatewayResponse() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_GATEWAY,
                Unpooled.copiedBuffer(HttpResponseStatus.BAD_GATEWAY.toString().getBytes()));
        return response;
    }
}
