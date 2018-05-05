package com.alibaba.dubbo.mesh.agent.common;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SystemPropertyUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * Created by juntao.hjt on 2018-04-24 13:53.
 */
public class EventLoopConfigFactory {

    private static final Logger logger = LogManager.getLogger(EventLoopConfigFactory.class);

    private static final int IO_RATIO = 50;

    public static EventLoopGroup createEventLoopGroup(int nThreads, String poolName) {
        if (isLinux()) {
            EpollEventLoopGroup epollEventLoopGroup = new EpollEventLoopGroup(nThreads, new DefaultThreadFactory(poolName));
            epollEventLoopGroup.setIoRatio(IO_RATIO);
            return epollEventLoopGroup;
        }
        KQueueEventLoopGroup kQueueEventLoopGroup = new KQueueEventLoopGroup(nThreads, new DefaultThreadFactory(poolName));
        kQueueEventLoopGroup.setIoRatio(IO_RATIO);
        return kQueueEventLoopGroup;
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads) {
        if (isLinux()) {
            EpollEventLoopGroup epollEventLoopGroup = new EpollEventLoopGroup(nThreads);
            epollEventLoopGroup.setIoRatio(IO_RATIO);
            return epollEventLoopGroup;
        }
        KQueueEventLoopGroup kQueueEventLoopGroup = new KQueueEventLoopGroup(nThreads);
        kQueueEventLoopGroup.setIoRatio(IO_RATIO);
        return kQueueEventLoopGroup;
    }

    public static Class<? extends ServerSocketChannel> getServerChannelClass() {
        if (isLinux()) {
            return EpollServerSocketChannel.class;
        }
        return KQueueServerSocketChannel.class;
    }

    public static Class<? extends SocketChannel> getSocketChannelClass() {
        if (isLinux()) {
            return EpollSocketChannel.class;
        }
        return KQueueSocketChannel.class;
    }

    private static boolean isLinux() {
        String name = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        logger.info("os name:{}", name);
        return name.startsWith("linux");
    }
}
