package com.netty.performace;

import com.netty.performace.client.AgentClient;
import com.netty.performace.server.AgentServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by juntao.hjt on 2018-04-20 11:24.
 */
public class AppStarter {

    private static Logger logger = LogManager.getLogger(AppStarter.class);

    //启动顺序 AgentTCPServer ProviderApp AgentTCPClient
    public static void main(String[] args) throws Exception {
        String runType = System.getProperty("type");
        if (runType == null || runType.isEmpty()) {
            throw new IllegalArgumentException("please provide -Dtype=consumer or -Dtype=provider");
        }
        //-Dtype=consumer -Dserver.port=20000 -Detcd.url=http://localhost:2379
        if ("consumer".equals(runType)) {
            AgentClient.main(args);
            logger.info("AgentHttpServer vNew5 started");
        }
        //-Dtype=provider -Dserver.port=30000 -Detcd.url=http://:2379 -Ddubbo.protocol.port=20880
        if ("provider".equals(runType)) {
            AgentServer.main(args);
            logger.info("AgentTCPServer vNew5 started");
        }
    }
}
