package com.netty.performace.server;

import com.netty.performace.common.EtcdRegistry;
import com.netty.performace.common.EventLoopConfigFactory;
import com.netty.meshagent.registry.LightNode;
import com.netty.meshagent.util.IpAddressFinder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by juntao.hjt on 2018-04-20 03:13.
 */
public class AgentServer {

    private static final Logger logger = LogManager.getLogger(AgentServer.class);

    private static Map<Integer, Integer> serverCoreMap = new HashMap<>(4);

    static {
        serverCoreMap.put(30000, 1); //1核2G
        serverCoreMap.put(30001, 2); //2核4G
        serverCoreMap.put(30002, 3); //3核6G
    }

    private static int serverIOThreads() {
        int weight = getCoresWeight();
        return weight + 1;
    }


    private static EventLoopGroup bossGroup = EventLoopConfigFactory.createEventLoopGroup(1);
    private static EventLoopGroup workerGroup = EventLoopConfigFactory.createEventLoopGroup(serverIOThreads(), "AgentServerWorker");

    private static Channel serverSocketChannel;

    private static Bootstrap upstreamBootstrap;

    public static void main(String[] args) throws Exception {
        //Agent server端
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        int serverPort = getServerPort();
        int coresWeight = getCoresWeight();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(EventLoopConfigFactory.getServerChannelClass())
                .option(ChannelOption.SO_BACKLOG, 2048) //tcp接受连接的队列大小
                .option(ChannelOption.SO_RCVBUF, 256 * 1024)
                .option(ChannelOption.SO_SNDBUF, 256 * 1024)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.SO_LINGER, 0)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(serverChannelInitializer());
        // bind port
        ChannelFuture serverChannelFuture = serverBootstrap.bind(serverPort).sync();
        if (serverChannelFuture.isSuccess()) {
            serverSocketChannel = serverChannelFuture.channel();
            initUpstreamBootstrap();
            InetAddress localAddress = IpAddressFinder.getLocalAddress();
            LightNode lightNode = new LightNode(localAddress.getHostAddress(), serverPort, coresWeight);
            EtcdRegistry.register(lightNode);
            logger.info("Agent Server started success, threads num: {}", serverIOThreads());
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopAgentServer(lightNode)));
        }

    }

    private static ChannelHandler upstreamChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel channel) {
                channel.pipeline().addLast(ServerUpstreamWriteHandler.HANDLER_NAME, ServerUpstreamWriteHandler.INSTANCE);
                channel.pipeline().addLast(new LengthFieldBasedFrameDecoder(2048, 12, 4, 0, 16));
                channel.pipeline().addLast(ServerUpstreamReadHandler.HANDLER_NAME, ServerUpstreamReadHandler.INSTANCE);
            }
        };
    }

    public static Bootstrap getUpstreamBootstrap() {
        return upstreamBootstrap;
    }

    private static void initUpstreamBootstrap() {
        upstreamBootstrap = new Bootstrap().group(workerGroup)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .channel(EventLoopConfigFactory.getSocketChannelClass())
                .handler(upstreamChannelInitializer())
                .remoteAddress("127.0.0.1", getDubboPort());
    }

    private static int getDubboPort() {
        String dubboPotocolPort = System.getProperty("dubbo.protocol.port");
        logger.info("get dubbo protocol port :{}", dubboPotocolPort);
        if (dubboPotocolPort == null) {
            dubboPotocolPort = "20880";
        }
        return Integer.valueOf(dubboPotocolPort);
    }


    private static void stopAgentServer(LightNode node) {
        try {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serverSocketChannel.close();
            EtcdRegistry.unregister(node);
            logger.info("stopAgentServer in shutdown hook");
        } catch (Exception e) {
            //ignore
            logger.error("stopAgentClient error", e);
        }
    }

    private static ChannelHandler serverChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new LengthFieldBasedFrameDecoder(2048, 0, 2, 0, 2));
                pipeline.addLast(AgentServerHandler.HANDLER_NAME, AgentServerHandler.INSTANCE);
            }
        };
    }

    private static int getCoresWeight() {
        return serverCoreMap.get(getServerPort());
    }

    private static int getServerPort() {
        //server.port
        // 30000 1核2G
        // 30001 2核4G
        // 30002 3核6G
        String serverPort = System.getProperty("server.port");
        if (serverPort == null) {
            serverPort = "30002";
        }
        logger.info("get server port :{}", serverPort);
        return Integer.valueOf(serverPort);
    }
}
