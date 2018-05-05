package com.alibaba.tianchi.dubbo.meshagent.server;

import com.alibaba.tianchi.dubbo.meshagent.agent.ChannelInitialConnector;
import com.alibaba.tianchi.dubbo.meshagent.config.ConnectionConfig;
import com.alibaba.tianchi.dubbo.meshagent.registry.EtcdRegistryFactory;
import com.alibaba.tianchi.dubbo.meshagent.util.IpAddressFinder;
import com.alibaba.tianchi.dubbo.meshagent.util.ServerException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;

/**
 * @author xiele
 * @date 2018/04/14
 */
public abstract class AbstractServer implements ServerLifecycle {

    protected static final Logger logger = LogManager.getLogger(AbstractServer.class);

    private static final int IO_THREADS_NUM = ConnectionConfig.PROCESSOR_NUM * 2;

    protected ServerBootstrap serverBootstrap;

    protected Channel channel;

    // mesh-agent 有两个server，需要共享线程池
    protected EventLoopGroup masterGroup;
    protected EventLoopGroup workerGroup;

    protected String host;
    protected int port;

    @Override

    public void start() {
        checkArgument();
        try {
            initEventLoop();
            doStart();
            afterStart();
            addShutdownHook();
        } catch (Throwable e) {
            logger.info("start server error", e);
            throw new ServerException("start server failed, the local address is " + getInetAddress(), e);
        }
    }

    @Override
    public int port() {
        String serverPort = System.getProperty("server.port");
        logger.info("check agent server port: {}", serverPort);
        if (serverPort == null || serverPort.isEmpty()) {
            throw new IllegalArgumentException("please provide a server port");
        }
        return port = Integer.valueOf(serverPort);
    }

    protected void doStart() {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(masterGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, ConnectionConfig.TCP_BACKLOG); //tcp接受连接的队列大小
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(connector().getChannelInitializer());
        // bind port
        ChannelFuture channelFuture = serverBootstrap.bind(port);
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();

    }

    protected abstract ChannelInitialConnector connector();

    protected abstract void afterStart();

    protected abstract void addShutdownHook();

    protected abstract String masterName();

    protected abstract String workerName();

    @Override
    public void stop() {
        // 先关闭channel
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }

        // 再关闭线程池
        try {
            if (serverBootstrap != null) {
                masterGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    protected void checkArgument() {
        getInetAddress();
        port();
        // 检查注册中心是否连接成功
        EtcdRegistryFactory.checkInit();
        logger.info("check argument ok. current host: {}, post: {}", host, port);
    }

    protected void initEventLoop() {
        if (masterGroup == null) {
            masterGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(masterName(), false));
            logger.info("checkInit master event loop done");
        }
        if (workerGroup == null) {
            workerGroup = new NioEventLoopGroup(IO_THREADS_NUM, new DefaultThreadFactory(workerName(), false));
            logger.info("checkInit worker event loop done");
        }

    }


    protected String getInetAddress() {
        InetAddress address = IpAddressFinder.getLocalAddress();
        logger.info("the server ip address is {}", address.getHostAddress());
        if (address.getHostAddress() == null) {
            throw new IllegalArgumentException("fail to get host address");
        }
        host = address.getHostAddress();
        return host;
    }

}
