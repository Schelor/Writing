package com.netty.meshagent.bootstrap;

import com.netty.meshagent.server.AgentServer;

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
