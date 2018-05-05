package com.alibaba.tianchi.dubbo.meshagent.server;

import com.alibaba.tianchi.dubbo.meshagent.agent.ChannelInitialConnector;
import com.alibaba.tianchi.dubbo.meshagent.config.ConnectionConfig;
import com.alibaba.tianchi.dubbo.meshagent.registry.EtcdRegistry;
import com.alibaba.tianchi.dubbo.meshagent.registry.EtcdRegistryFactory;
import com.alibaba.tianchi.dubbo.meshagent.registry.LightNode;
import com.alibaba.tianchi.dubbo.meshagent.server.handler.ServerMsgpackConnector;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class AgentServer extends AbstractServer {

    @Override
    protected ChannelInitialConnector connector() {
        return new ServerMsgpackConnector();
    }


    @Override
    public int port() {
        String dubboPort = System.getProperty("dubbo.protocol.port");
        if (dubboPort == null || dubboPort.isEmpty()) {
            throw new IllegalArgumentException("请添加-Ddubbo.protocol.port启动参数");
        }
        return super.port();
    }

    @Override
    protected void afterStart() {
        logger.info("start the agent server successfully.");
        // 注册到注册中心
        LightNode node = new LightNode(host, port);
        node.setWeight(ConnectionConfig.PROCESSOR_NUM);
        EtcdRegistry registry = EtcdRegistryFactory.registry();

        registry.register(node);
        logger.info("register to the node. host: {}, port: {}", host, port);
    }

    @Override
    protected void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("shutdown in hook");
            stop();
            // 取消订阅
            LightNode node = new LightNode(host, port);
            node.setWeight(ConnectionConfig.PROCESSOR_NUM);
            EtcdRegistryFactory.registry().unregister(node);
        }));
    }

    @Override
    protected String masterName() {
        return ConnectionConfig.AGENT_SERVER_MASTER_NAME;
    }

    @Override
    protected String workerName() {
        return ConnectionConfig.AGENT_SERVER_WORKER_NAME;
    }

}
