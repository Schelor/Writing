package com.alibaba.tianchi.dubbo.meshagent.dubbo;

import com.alibaba.tianchi.dubbo.meshagent.config.ConnectionConfig;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DubboNettyClient {

    protected static final Logger logger = LogManager.getLogger(DubboNettyClient.class);

    private NettyChannelPoolClient nettyChannelPoolClient;


    public DubboNettyClient() {
        String portProps = System.getProperty("dubbo.protocol.port");
        nettyChannelPoolClient = new NettyChannelPoolClient("127.0.0.1",
            Integer.valueOf(portProps),
            ConnectionConfig.CLIENT_MAX_CONNECTION, new DubboClientChannelPoolHandler());
    }

    public Object executeDubboCall(Object msg) {
        Object result = null;
        Channel channel = null;
        try {
            RpcRequest request = (RpcRequest) msg;
            RpcFuture future = new RpcFuture();
            RpcRequestHolder.put(request.getId(), future);
            channel = nettyChannelPoolClient.acquireChannel();
            channel.writeAndFlush(request);
            result = future.get();
        } catch (Exception e) {
            logger.error("executeDubboCall error!", e);
        } finally {
            if (channel != null) {
                nettyChannelPoolClient.releaseChannel(channel);
            }
        }
        return result;
    }
}