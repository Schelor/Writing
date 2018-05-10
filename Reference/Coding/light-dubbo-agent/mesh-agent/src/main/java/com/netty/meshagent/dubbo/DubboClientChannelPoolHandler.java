package com.netty.meshagent.dubbo;

import com.netty.meshagent.dubbo.protocol.DubboRpcDecoder;
import com.netty.meshagent.dubbo.protocol.DubboRpcEncoder;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Created by juntao.hjt on 2018-04-17 14:36.
 */
public class DubboClientChannelPoolHandler implements ChannelPoolHandler {

    @Override
    public void channelReleased(Channel ch) throws Exception {
        //ch.writeAndFlush(Unpooled.EMPTY_BUFFER); //flush掉所有写回的数据
    }

    @Override
    public void channelAcquired(Channel ch) throws Exception {
    }

    @Override
    public void channelCreated(Channel ch) throws Exception {
        // 新创建的NioSocketChannel需要添加Handler
        NioSocketChannel channel = (NioSocketChannel) ch;
        channel.pipeline()
                .addLast(new DubboRpcEncoder())
                .addLast(new DubboRpcDecoder())
                .addLast(new RpcClientHandler());
    }
}
