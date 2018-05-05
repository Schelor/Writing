package com.simon.springmvc.http.netty;

import java.util.Set;

import com.simon.springmvc.http.netty.context.ContextInitializer;
import com.simon.springmvc.http.netty.context.EventMode;
import com.simon.springmvc.http.netty.context.RuntimeContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * TODO
 * 1. 通配符域名匹配
 * 2. 负载均衡处理
 *
 * @author xiele
 * @date 2018/03/29
 */
@Slf4j
public class NettyHttpServer implements HttpServer {

    private HttpServerConnector connector = new HttpServerConnector();

    private final EventLoopGroup masterGroup;
    private final EventLoopGroup slaveGroup;

    public NettyHttpServer() {
        masterGroup = new NioEventLoopGroup(RuntimeContext.getWorkingPort().size());
        slaveGroup = new NioEventLoopGroup(RuntimeContext.getIoThreads());
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
            shutdown();
        }));
    }

    private void initConf() {
        ContextInitializer.init();
    }

    private void startServer() {

        log.info("starting the {} http server", getName());
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(masterGroup, slaveGroup);
        if (RuntimeContext.getEventMode() == EventMode.EPOLL) {
            bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            bootstrap.channel(NioServerSocketChannel.class);
        }
        bootstrap.childHandler(connector.getChannelInitializer());
        bootstrap.option(ChannelOption.SO_BACKLOG, RuntimeContext.getWorkerConnections()); // tcp接受连接的队列大小
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            Set<Integer> workingPort = RuntimeContext.getWorkingPort();
            for (Integer port : workingPort) {
                ChannelFuture future = bootstrap.bind(port).sync();
                future.channel().closeFuture().sync();
            }

        } catch (InterruptedException e) {
            log.error("start http server error", e);
        }
    }
    public void start() {

        addShutdownHook();
        initConf();
        startServer();
    }

    public void shutdown() {

        log.info("ready to shutting down the {} http server.", getName());
        if (masterGroup != null) {
            masterGroup.shutdownGracefully();
        }
        if (slaveGroup != null) {
            slaveGroup.shutdownGracefully();
        }

    }


    protected String getName() {
        return this.getClass().getSimpleName();
    }

    private static NettyHttpServer server;

    public static void main(String[] args) {
        if (server == null) {
            server = new NettyHttpServer();
        }
        server.start();

    }
}
