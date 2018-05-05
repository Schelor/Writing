package com.simon.springmvc.http.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

/**
 * 实现初始化，清理资源的接口
 * @author xiele
 * @date 2018/04/01
 */
@Slf4j
public class NettyClientHttpRequestFactory implements InitializingBean, DisposableBean  {

    public static final int DEFAULT_MAX_RESPONSE_SIZE = 1024 * 1024 * 10;

    private final EventLoopGroup ioEventLoopGroup;

    private final boolean defaultEventLoopGroup;

    private int maxResponseSize = DEFAULT_MAX_RESPONSE_SIZE;

    private SslContext sslContext;

    private int connectTimeout = -1;

    private int readTimeout = -1;

    private volatile Bootstrap bootstrap;

    public NettyClientHttpRequestFactory() {
        int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
        this.ioEventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
        this.defaultEventLoopGroup = true;
    }

    private Bootstrap getBootstrap() {
        if (this.bootstrap == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(this.ioEventLoopGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            configureChannel(channel.config());
                            ChannelPipeline pipeline = channel.pipeline();
                            configureSSL(channel);
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
                            if (readTimeout > 0) {
                                pipeline.addLast(new ReadTimeoutHandler(readTimeout,
                                        TimeUnit.MILLISECONDS));
                            }
                        }
                    });
            this.bootstrap = bootstrap;
        }
        return this.bootstrap;
    }

    protected void configureChannel(SocketChannelConfig config) {
        if (this.connectTimeout >= 0) {
            config.setConnectTimeoutMillis(this.connectTimeout);
        }
    }

    protected void configureSSL(SocketChannel channel) {
        //try {
        //    sslContext = SslContextBuilder.forClient().build();
        //} catch (SSLException e) {
        //    log.error("初始化SSLContext出现异常", e);
        //}
        //
        if (sslContext != null) {
            channel.pipeline().addLast(sslContext.newHandler(channel.alloc()));
        }
    }


    public NettyClientHttpProxyRequest createUpstreamRequest(String host) throws IOException {

        return new NettyClientHttpProxyRequest(getBootstrap(), getUpstreamUri(host));
    }

    public URI getUpstreamUri(String host) {
        try {
            return new URI(host);
        } catch (URISyntaxException e) {
            log.error("解析URI出现异常", e);
            throw new IllegalArgumentException("Invalid URL after inserting base URL: " + host, e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        getBootstrap();
        log.info("初始化Netty请求客户端.");
    }

    @Override
    public void destroy() throws Exception {
        if (defaultEventLoopGroup) {
            ioEventLoopGroup.shutdownGracefully();
        }
    }
}
