package com.alibaba.tianchi.dubbo.meshagent.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.tianchi.dubbo.meshagent.agent.ChannelInitialConnector;
import com.alibaba.tianchi.dubbo.meshagent.config.ConnectionConfig;
import com.alibaba.tianchi.dubbo.meshagent.registry.EtcdRegistry;
import com.alibaba.tianchi.dubbo.meshagent.registry.EtcdRegistryFactory;
import com.alibaba.tianchi.dubbo.meshagent.registry.LightNode;
import com.alibaba.tianchi.dubbo.meshagent.server.handler.HttpInitialConnector;

import java.util.List;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class HttpServer extends AbstractServer {

    @Override
    protected ChannelInitialConnector connector() {
        return new HttpInitialConnector();
    }

    @Override
    protected void afterStart() {
        logger.info("start the http server successfully.");

        // 订阅注册中心节点变化
        EtcdRegistry registry = EtcdRegistryFactory.registry();

        registry.subscribe("LightMeshAgent");

        // 启动结束后拉取所有的可用节点
        List<LightNode> lookup = registry.lookup();
        logger.info("consumer agent cached the nodes: {}", JSON.toJSONString(lookup));
    }

    @Override
    protected void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    @Override
    public int port() {
        String serverPort = System.getProperty("server.port");
        logger.info("check http server port: {}", serverPort);
        if (serverPort == null || serverPort.isEmpty()) {
            throw new IllegalArgumentException("please provide a server port");
        }
        return port = Integer.valueOf(serverPort);
    }


    @Override
    protected String masterName() {
        return ConnectionConfig.HTTP_SERVER_MASTER_NAME;
    }

    @Override
    protected String workerName() {
        return ConnectionConfig.HTTP_SERVER_WORKER_NAME;
    }
}
