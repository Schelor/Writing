package com.alibaba.tianchi.dubbo.meshagent.bootstrap;

import com.alibaba.tianchi.dubbo.meshagent.server.AgentServer;

/**
 * @author xiele
 * @date 2018/04/16
 */
public class RunAgentServer {

    public static void main(String[] args) throws Exception {
        run();
    }

    public static void run() {
        AgentServer agent = new AgentServer();
        agent.start();
    }

}
