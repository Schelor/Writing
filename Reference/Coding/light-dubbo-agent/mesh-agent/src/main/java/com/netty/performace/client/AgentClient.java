package com.netty.performace.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netty.performace.common.EtcdRegistry;
import com.netty.performace.common.EventLoopConfigFactory;
import com.netty.performace.common.ServerNodeSelector;
import com.alibaba.fastjson.JSON;
import com.netty.meshagent.registry.LightNode;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by juntao.hjt on 2018-04-20 03:01.
 * <p>
 */
public class AgentClient {

    private static Logger logger = LogManager.getLogger(AgentClient.class);

    private static final int IO_THREADS = 8;

    private static final EventLoopGroup bossGroup = EventLoopConfigFactory.createEventLoopGroup(1);

    private static final EventLoopGroup workerGroup = EventLoopConfigFactory.createEventLoopGroup(
            IO_THREADS, "AgentClientWorker"); //4核4G

    private static Channel serverSocketChannel;

    private static final Map<LightNode, Bootstrap> upstreamBootstrapMap = new HashMap<>(4);

    public static void main(String[] args) throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(EventLoopConfigFactory.getServerChannelClass())
                .option(ChannelOption.SO_BACKLOG, 2048) //tcp接受连接的队列大小
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(agentHttpChannelInitializer());

        List<LightNode> nodeList = EtcdRegistry.findLightNodes();
        if (nodeList.size() <= 0) {
            throw new IllegalArgumentException("not found the server nodes, please check agent server or etcd");
        }
        logger.info("findLightNodes success:{}", JSON.toJSONString(nodeList));
        ServerNodeSelector.loadWeightNode(nodeList);
        // bind port
        ChannelFuture channelFuture = serverBootstrap.bind(20000).sync();
        if (channelFuture.isSuccess()) {
            serverSocketChannel = channelFuture.channel();
            initUpstreamBootstrapMap(nodeList);
            logger.info("Agent Client started success, threads num: {}", IO_THREADS);
            Runtime.getRuntime().addShutdownHook(new Thread(AgentClient::stopAgentClient));
        }
    }

    private static void initUpstreamBootstrapMap(List<LightNode> nodeList) {
        Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(EventLoopConfigFactory.getSocketChannelClass())
                .handler(clientChannelInitializer());
        for (LightNode lightNode : nodeList) {
            Bootstrap cloneBootstrap = bootstrap.clone();
            cloneBootstrap.remoteAddress(lightNode.getHost(), lightNode.getPort());
            upstreamBootstrapMap.put(lightNode, cloneBootstrap);
        }
    }

    public static Bootstrap getUpstreamBootstrap(LightNode node) {
        return upstreamBootstrapMap.get(node);
    }


    private static ChannelHandler clientChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel channel) {
                channel.pipeline().addLast(ClientUpstreamWriteHandler.HANDLER_NAME, ClientUpstreamWriteHandler.INSTANCE);
                channel.pipeline().addLast(ClientUpstreamReadHandler.HANDLER_NAME, ClientUpstreamReadHandler.INSTANCE);
            }
        };
    }

    private static void stopAgentClient() {
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serverSocketChannel.close();
        } catch (Exception e) {
            //ignore
            logger.error("stopAgentClient error", e);
        }
    }

    private static ChannelHandler agentHttpChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("serverCodec", new HttpServerCodec());
                pipeline.addLast("httpKeepAlive", new HttpServerKeepAliveHandler());
                pipeline.addLast("aggregator", new HttpObjectAggregator(16384));
                pipeline.addLast("handler", AgentClientHandler.INSTANCE);
            }
        };
    }
}
