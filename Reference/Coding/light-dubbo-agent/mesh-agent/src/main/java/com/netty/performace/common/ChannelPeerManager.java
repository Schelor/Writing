package com.netty.performace.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by juntao.hjt on 2018-04-24 12:37.
 */
public class ChannelPeerManager {

    private static final Logger logger = LogManager.getLogger(ChannelPeerManager.class);


    private static final AttributeKey<Channel> UPSTREAM_CHANNEL_PEER_KEY =
            AttributeKey.valueOf("upstream.channel.key");
    private static final AttributeKey<ChannelFuture> CHANNEL_FUTURE_ATTRIBUTE_KEY =
            AttributeKey.valueOf("upstream.channel.future.key");

    //outbound channel 1<------>1  inbound channel
    public static void addChannelPeer(Channel inbound, Channel outbound) {
        inbound.attr(UPSTREAM_CHANNEL_PEER_KEY).set(outbound);
        outbound.attr(UPSTREAM_CHANNEL_PEER_KEY).set(inbound);
    }

    public static void addChannelUpstreamFuture(Channel inbound, ChannelFuture outboundFuture) {
        inbound.attr(CHANNEL_FUTURE_ATTRIBUTE_KEY).set(outboundFuture);
    }

    public static Channel findOutboundChannel(Channel inbound) {
        return inbound.attr(UPSTREAM_CHANNEL_PEER_KEY).get();
    }

    public static ChannelFuture findOutboundChannelFuture(Channel inbound) {
        return inbound.attr(CHANNEL_FUTURE_ATTRIBUTE_KEY).get();
    }

    public static Channel findInboundChannel(Channel outbound) {
        return outbound.attr(UPSTREAM_CHANNEL_PEER_KEY).get();
    }

    public static void inboundClientChannelClose(Channel inbound) {
        try {
            Channel outbound = inbound.attr(UPSTREAM_CHANNEL_PEER_KEY).get();
            outbound.close();
        } catch (Exception e) {
            logger.error("inboundChannelClose error", e);
        }
    }

    public static void inboundServerChannelClose(Channel inbound) {
        try {
            Channel outbound = inbound.attr(UPSTREAM_CHANNEL_PEER_KEY).get();
            outbound.close();
        } catch (Exception e) {
            logger.error("inboundChannelClose error", e);
        }
    }
}
