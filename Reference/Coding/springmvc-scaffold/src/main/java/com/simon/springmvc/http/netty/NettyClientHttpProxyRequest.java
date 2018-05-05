package com.simon.springmvc.http.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.net.URI;
import java.util.Map;

/**
 * 使用Netty转发upstream请求
 * @see org.springframework.http.client.Netty4ClientHttpRequest
 * @author xiele
 * @date 2018/04/01
 */
@Slf4j
public class NettyClientHttpProxyRequest {

    private final Bootstrap bootstrap;
    private final URI uri;

    public NettyClientHttpProxyRequest(Bootstrap bootstrap, URI uri) {
        this.bootstrap = bootstrap;
        this.uri = uri;
    }

    public ListenableFuture<FullHttpResponse> executeInternal(final FullHttpRequest request) {
        final SettableListenableFuture<FullHttpResponse> responseFuture =
                new SettableListenableFuture<>();
        ChannelFutureListener connectionListener = future -> {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                channel.pipeline().addLast(new UpstreamRequestExecuteHandler(responseFuture));
                channel.writeAndFlush(createFullHttpRequest(request));
            } else {
                log.error("Upstream转发请求出现异常", future.cause());
                responseFuture.set(NettyClientUpstreamHandler.create502BadGatewayResponse());
            }
        };
        // 此处应该设置为：上游服务器的URI 和端口
        ChannelFuture channelFuture = this.bootstrap.connect(this.uri.getHost(), getPort(this.uri));
        channelFuture.addListener(connectionListener);
        return responseFuture;
    }

    private static int getPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                port = 80;
            }
            else if ("https".equalsIgnoreCase(uri.getScheme())) {
                port = 443;
            }
        }
        return port;
    }

    private FullHttpRequest createFullHttpRequest(final FullHttpRequest request) {

        FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                request.getMethod(), request.getUri(), request.content());
        for (Map.Entry<String, String> entry : request.headers().entries()) {
            nettyRequest.headers().add(entry.getKey(), entry.getValue());
        }
        nettyRequest.headers().set(io.netty.handler.codec.http.HttpHeaders.Names.HOST, uri.getHost());
        nettyRequest.headers().set(io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION,
                io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE);
        log.info("转发到：{}, 带去的Header：{}", this.uri, nettyRequest.headers().entries());
        return nettyRequest;
    }

    /**
     * A SimpleChannelInboundHandler to update the given SettableListenableFuture.
     */
    private static class UpstreamRequestExecuteHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final SettableListenableFuture<FullHttpResponse> responseFuture;

        public UpstreamRequestExecuteHandler(SettableListenableFuture<FullHttpResponse> responseFuture) {
            this.responseFuture = responseFuture;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, FullHttpResponse response) throws Exception {

            // FullHttpResponse proxyResponse = createProxyResponse(response);
            // @see https://stackoverflow.com/questions/41556208/io-netty-util-illegalreferencecountexception-refcnt-0-in-netty

            this.responseFuture.set(response.retain());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
            log.error("转发请求Handler处理响应数据出现异常", cause);
            this.responseFuture.set(NettyClientUpstreamHandler.create502BadGatewayResponse());
        }

        private FullHttpResponse createProxyResponse(FullHttpResponse proxyResponse) {
            if (proxyResponse == null) {
                log.error("proxy response is null");
                return NettyClientUpstreamHandler.create502BadGatewayResponse();
            }
            FullHttpResponse clientResponse = new DefaultFullHttpResponse(proxyResponse.getProtocolVersion(), proxyResponse.getStatus(), proxyResponse.content());


            for (Map.Entry<String, String> entry : proxyResponse.headers().entries()) {
                clientResponse.headers().add(entry.getKey(), entry.getValue());
            }

            return clientResponse;

        }
    }
}
