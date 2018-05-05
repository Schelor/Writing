package com.alibaba.tianchi.dubbo.meshagent.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class Bootstrap {

    private static Logger logger = LogManager.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        String runType = System.getProperty("type");
        if (runType == null || runType.isEmpty()) {
            throw new IllegalArgumentException("please provide -Dtype=consumer or -Dtype=provider");
        }
        //-Dtype=provider -Dserver.port=30000 -Detcd.url=http://:2379 -Ddubbo.protocol.port=20880
        if ("provider".equals(runType)) {

            RunAgentServer.run();
            logger.info("RunAgentServer Done");
            return;
        }
        //-Dtype=consumer -Dserver.port=20000 -Detcd.url=http://localhost:2379
        if ("consumer".equals(runType)) {

            RunAgentClient.run();
            logger.info("RunAgentClient Done");
        }
    }
}
