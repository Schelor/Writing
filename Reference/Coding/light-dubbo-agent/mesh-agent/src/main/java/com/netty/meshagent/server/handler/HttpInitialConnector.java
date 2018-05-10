package com.netty.meshagent.server.handler;

import com.netty.meshagent.agent.ChannelInitialConnector;
import com.netty.meshagent.config.ConnectionConfig;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class HttpInitialConnector implements ChannelInitialConnector {

    /**
     * 编解码handler
     */
    private static final String CODEC_HANDLER_NAME = "codec_handler";

    /**
     * 压缩数据handler
     */
    private static final String COMPRESSOR_HANDLER_NAME = "compressor_handler";

    /**
     * 聚合handler
     */
    private static final String AGGREGATOR_HANDLER_NAME = "aggregator_handler";

    /**
     * http请求处理handler
     */
    private static final String HTTP_REQUEST_HANDLER_NAME = "http_request_handler";

    @Override
    public ChannelInitializer<?> getChannelInitializer() {
        return new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(final NioSocketChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(CODEC_HANDLER_NAME, new HttpServerCodec())
                        .addLast(new HttpServerKeepAliveHandler())
                        .addLast(AGGREGATOR_HANDLER_NAME, new HttpObjectAggregator(ConnectionConfig.MIN_MAX_HTTP_CONTENT_LENGTH))
                        .addLast(HTTP_REQUEST_HANDLER_NAME, new HttpDispatchHandler());

                //.addAfter(CODEC_HANDLER_NAME, COMPRESSOR_HANDLER_NAME, new HttpContentCompressor())
                ;
            }
        };
    }
}
