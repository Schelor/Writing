package com.netty.meshagent.client;

import com.netty.meshagent.agent.ChannelInitialConnector;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by juntao.hjt on 2018-04-17 14:36.
 */
public class AgentClientChannelPoolHandler implements ChannelPoolHandler {

    private static Logger logger = LogManager.getLogger(AgentClientChannelPoolHandler.class);

    private ChannelInitialConnector channelInitialConnector;

    public AgentClientChannelPoolHandler(ChannelInitialConnector channelInitialConnector) {
        this.channelInitialConnector = channelInitialConnector;
    }

    @Override
    public void channelReleased(Channel ch) throws Exception {
        logger.info("AgentClientChannelReleased,channelId: {}", ch.id());
    }

    @Override
    public void channelAcquired(Channel ch) throws Exception {
        logger.info("AgentClientChannelAcquired, channelId: {}", ch.id());
    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        // 新创建的NioSocketChannel需要添加Handler
        NioSocketChannel channel = (NioSocketChannel) ch;
        channel.config().setKeepAlive(true);
        channel.config().setTcpNoDelay(true);
        channel.pipeline().addLast(channelInitialConnector.getChannelInitializer());
        logger.info("AgentClientChannelCreated,channelId: {}", ch.id());
    }
}
