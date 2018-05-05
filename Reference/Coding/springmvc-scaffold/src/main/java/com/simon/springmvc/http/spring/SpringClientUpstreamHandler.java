package com.simon.springmvc.http.spring;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLException;
import java.util.List;
import java.util.Map;

/**
 * @author xiele
 * @date 2018/03/31
 */
@Slf4j
public class SpringClientUpstreamHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private RestTemplate restTemplate;

    public SpringClientUpstreamHandler() {
        RestTemplate template = new RestTemplate();
        Netty4ClientHttpRequestFactory factory = new Netty4ClientHttpRequestFactory();
        SslContext build = null;
        try {
            build = SslContextBuilder.forClient().build();
        } catch (SSLException e) {
            e.printStackTrace();
        }
        factory.setSslContext(build);
        template.setRequestFactory(factory);


        this.restTemplate = template;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {

        log.info("接收到的请求信息：{}", msg.toString());

        // 转发
        // 读取到客户端的请求后，此时转发到上游服务端
        // 设置请求Header method url body
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        for (Map.Entry<String, String> entry : msg.headers().entries()) {
            headers.add(entry.getKey(), entry.getValue());
        }

        HttpEntity<byte[]> requestEntity = new HttpEntity(msg.content().array(), headers);
        log.info("转发到上游的请求实体为：{}", requestEntity);

        // 转发的uri 临时设置为：https://www.baidu.com
        final String uri = "https://www.baidu.com" + msg.getUri();
        ResponseEntity<byte[]> response =
                this.restTemplate.exchange(uri,
                org.springframework.http.HttpMethod.resolve(msg.getMethod().name()),
                        requestEntity, byte[].class);

        //ResponseEntity<byte[]> response = this.restTemplate.getForEntity(uri, byte[].class);
        log.info("从上游返回的相应结果为：{}", response);

        // 在转换为Netty的响应
        FullHttpResponse nettyResponse =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.valueOf(response.getStatusCode().value()),
                        Unpooled.copiedBuffer(response.getBody()));
        HttpHeaders springHeaders = response.getHeaders();
        for (Map.Entry<String, List<String>> entry : springHeaders.entrySet()) {
            nettyResponse.headers().add(entry.getKey(), entry.getValue());
        }

        ctx.writeAndFlush(nettyResponse);


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.writeAndFlush(create502Response());
    }


    public static FullHttpResponse create502Response() {

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, Unpooled.copiedBuffer("502 Bad Gateway".getBytes()));
        return response;
    }
}
