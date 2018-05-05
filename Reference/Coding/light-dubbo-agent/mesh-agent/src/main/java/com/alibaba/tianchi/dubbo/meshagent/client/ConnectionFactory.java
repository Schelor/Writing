package com.alibaba.tianchi.dubbo.meshagent.client;

import com.alibaba.tianchi.dubbo.meshagent.agent.ChannelInitialConnector;
import com.alibaba.tianchi.dubbo.meshagent.config.ConnectionConfig;
import com.alibaba.tianchi.dubbo.meshagent.lb.LoadBalanceFactory;
import com.alibaba.tianchi.dubbo.meshagent.registry.EtcdRegistry;
import com.alibaba.tianchi.dubbo.meshagent.registry.EtcdRegistryFactory;
import com.alibaba.tianchi.dubbo.meshagent.registry.LightNode;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.alibaba.tianchi.dubbo.meshagent.config.ConnectionConfig.PROCESSOR_NUM;

/**
 * @author xiele
 * @date 2018/04/15
 */
public abstract class ConnectionFactory {

    protected static final Logger logger = LogManager.getLogger(ConnectionFactory.class);

    protected static EventLoopGroup ioEventLoopGroup;
    protected Bootstrap bootstrap;
    protected int connectTimeout = ConnectionConfig.CONNECTION_TIMEOUT;
    protected int maxConnections = ConnectionConfig.CLIENT_MAX_CONNECTION;

    protected ChannelPoolMap<URI, ChannelPool> channelPoolMap;

    public ConnectionFactory() {
        initIoEventLoop();
        initChannelPool();
    }

    public void configureChannel(SocketChannelConfig config) {
        if (this.connectTimeout >= 0) {
            config.setConnectTimeoutMillis(this.connectTimeout);
        }
    }

    public int getPort(URI uri) {
        return uri.getPort();
    }

    public String getHost(URI uri) {
        return uri.getHost();
    }

    public ChannelPool getChannelPool(URI uri) {
        return channelPoolMap.get(uri);
    }

    public Channel getChannel(ChannelPool channelPool) throws Exception {
        Channel channel = channelPool.acquire().get();
        logger.info("getChannel, channelId: {}, isOpen: {}", channel.id(), channel.isOpen());
        return channel;
    }

    public void releaseChannel(ChannelPool channelPool, Channel channel) {
        channelPool.release(channel);
    }

    public abstract ChannelInitialConnector clientConnector();

    protected Bootstrap getBootstrap() {
        if (this.bootstrap == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ioEventLoopGroup)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .channel(NioSocketChannel.class);
            logger.info("initialize the connect bootstrap done. id: {}", bootstrap.toString());
            this.bootstrap = bootstrap;
        }
        return bootstrap;
    }

    private void initIoEventLoop() {
        int ioWorkerCount = PROCESSOR_NUM * 2;
        if (ioEventLoopGroup == null) {
            ioEventLoopGroup = new NioEventLoopGroup(ioWorkerCount,
                new DefaultThreadFactory("MsgpackClientWorker", true));
            logger.info("checkInit the client io event loop done， ioWorkerCount: {}", ioWorkerCount);
        }

    }

    private void initChannelPool() {
        if (this.channelPoolMap == null) {
            channelPoolMap = new AbstractChannelPoolMap<URI, ChannelPool>() {
                @Override
                protected ChannelPool newPool(URI uri) {
                    Bootstrap bootstrap = getBootstrap();
                    return new FixedChannelPool(
                            bootstrap.remoteAddress(new InetSocketAddress(getHost(uri), getPort(uri))),
                            new AgentClientChannelPoolHandler(clientConnector()), maxConnections);
                }
            };
        }
        EtcdRegistry registry = EtcdRegistryFactory.registry();
        List<LightNode> lookup = registry.lookup();
        if (lookup != null) {
            for (LightNode node : lookup) {
                Optional<URI> opt = LoadBalanceFactory.toUri(node);
                if (opt.isPresent()) {
                    URI uri = opt.get();
                    ChannelPool pool = this.channelPoolMap.get(uri);
                    logger.info("init the channel pool. uri: {} -> poolId: {}", uri, pool.toString());
                    // 先提前创建一个连接, 预热
                    warmupChannel(pool);
                }
            }
        }
    }

    private void warmupChannel(ChannelPool pool) {
        Channel channel = null;
        try {
            channel = getChannel(pool);
            logger.info("warmup the channel. channelId: {}", channel.id());
        } catch (Exception e) {
            // do nothing;
        } finally {
            if (channel != null) {
                releaseChannel(pool, channel);
            }

        }
    }


}
