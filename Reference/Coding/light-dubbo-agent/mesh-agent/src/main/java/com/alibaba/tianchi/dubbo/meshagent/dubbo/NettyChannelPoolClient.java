package com.alibaba.tianchi.dubbo.meshagent.dubbo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;

import com.alibaba.tianchi.dubbo.meshagent.config.ConnectionConfig;

/**
 * Created by juntao.hjt on 2018-04-17 20:05.
 */
public class NettyChannelPoolClient {

    private ChannelPool channelPool;

    private int maxConnections;
    private String host;
    private int port;
    private ChannelPoolHandler channelPoolHandler;


    public NettyChannelPoolClient(String host, int port, int maxConnections, ChannelPoolHandler channelPoolHandler) {
        this.host = host;
        this.port = port;
        this.maxConnections = maxConnections;
        this.channelPoolHandler = channelPoolHandler;
        initBootstrapAndChannelPool();
    }

    public Channel acquireChannel() throws Exception {
        return channelPool.acquire().get();
    }

    public void releaseChannel(Channel channel) {
        channelPool.release(channel);
    }

    private void initBootstrapAndChannelPool() {
        Bootstrap bootstrap = new Bootstrap()
                .group(new NioEventLoopGroup(ConnectionConfig.PROCESSOR_NUM * 2,
                    new DefaultThreadFactory("DubboClientWorker", true)))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(host, port));
        channelPool = new FixedChannelPool(bootstrap, channelPoolHandler, maxConnections);
    }
}
