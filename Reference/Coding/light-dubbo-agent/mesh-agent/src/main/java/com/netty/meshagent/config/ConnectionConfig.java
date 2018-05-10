package com.netty.meshagent.config;

/**
 * @author xiele
 * @date 2018/04/14
 */
public abstract class ConnectionConfig {

    public static final int TCP_BACKLOG = 20480;

    public static final int READ_CLIENT_TIMEOUT = 5000;

    public static final int CONNECTION_TIMEOUT = 5000;

    public static final int CLIENT_MAX_CONNECTION = 5;

    /**
     * 最大响应报文10M
     */
    public static final int MAX_HTTP_CONTENT_LENGTH = (1 << 20) * 10; // 1024 * 1024 * 10; 10M

    public static final int MAX_FRAME_LENGTH = (1 << 20); // 1M;

    public static final int MIN_MAX_HTTP_CONTENT_LENGTH = (1 << 10); // 1K
    public static final int MIN_MAX_FRAME_LENGTH = (1 << 10); // 1K;

    public static final String AGENT_SERVER_MASTER_NAME = "AgentServerMaster";
    public static final String AGENT_SERVER_WORKER_NAME = "AgentServerWorker";

    public static final String HTTP_SERVER_MASTER_NAME = "HttpServerMaster";
    public static final String HTTP_SERVER_WORKER_NAME = "HttpServerWorker";


    public static final int PROCESSOR_NUM = Runtime.getRuntime().availableProcessors() * 2 + 1;

}
