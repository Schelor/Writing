package com.simon.springmvc.http.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xiele
 * @date 2018/03/29
 */
@Value @Slf4j
public class HttpServerConnector implements ServerConnector {

    public static final int DEFAULT_PORT = 7777;
    /**
     * 编解码handler
     */
    public static final String CODEC_HANDLER_NAME = "codec_handler";

    /**
     * 压缩数据handler
     */
    public static final String COMPRESSOR_HANDLER_NAME = "compressor_handler";

    /**
     * 聚合handler
     */
    public static final String AGGREGATOR_HANDLER_NAME = "aggregator_handler";

    /**
     * http请求处理handler
     */
    public static final String HTTP_REQUEST_HANDLER_NAME = "http_request_handler";

    public int port() {
        return DEFAULT_PORT;
    }

    public ChannelInitializer<?> getChannelInitializer() {
        return new ChannelInitializer<NioSocketChannel>() {

            protected void initChannel(final NioSocketChannel ch) throws Exception {

                ch.pipeline()
                        .addLast(CODEC_HANDLER_NAME, new HttpServerCodec())
                        .addLast(AGGREGATOR_HANDLER_NAME, new HttpObjectAggregator(512 * 1024))
                        .addLast(HTTP_REQUEST_HANDLER_NAME, new NettyClientUpstreamHandler())
                        .addAfter(CODEC_HANDLER_NAME, COMPRESSOR_HANDLER_NAME, new HttpContentCompressor())
                ;


            }
        };
    }

    /**
     * 是否开启压缩，默认为true
     * @return
     */
    public boolean compress() {
        return true;
    }

}
