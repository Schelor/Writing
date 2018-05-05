package com.simon.springmvc.http.netty.context;

import com.alibaba.fastjson.JSON;

import com.simon.springmvc.http.netty.conf.ServerBlockConf;
import com.simon.springmvc.http.netty.util.logger.ServerLoggerFactory;
import com.simon.springmvc.http.netty.conf.ConfLoader;
import com.simon.springmvc.http.netty.conf.EventBlockConf;
import com.simon.springmvc.http.netty.conf.HttpBlockConf;
import org.apache.logging.log4j.Logger;

import static com.simon.springmvc.http.netty.context.StoredUpstreamServer.getUpstreamHost;

/**
 * @author xiele
 * @date 2018/04/04
 */
public class ContextInitializer {

    static {
        // 最先确认是否已经初始化日志文件
        ServerLoggerFactory.createAccessLoggerIfNotExist(ConfLoader.getHttpConf().getAccessLog());
    }

    private static Logger ACCESS_LOGGER = ServerLoggerFactory.getLogger();

    public static void init() {

        HttpBlockConf httpConf = ConfLoader.getHttpConf();
        ServerLoggerFactory.createAccessLoggerIfNotExist(httpConf.getAccessLog());
        ACCESS_LOGGER.info("init access logger done, the log path is: {}", httpConf.getAccessLog());

        // 初始化带通配符的虚拟server name
        for (ServerBlockConf server : ConfLoader.getServerConf()) {
            StoredUpstreamServer.storeServer(server);
            ACCESS_LOGGER.info("init server name done, the pattern is: {}", server.getServerName());

            // 统计需要监听的端口
            int listen = server.getListen();
            if (listen < 0) {
                throw new IllegalArgumentException(
                    "illegal server port which should be greater than 0 and better be 80 or 443");
            }
            RuntimeContext.addWorkingPort(listen);

            ACCESS_LOGGER.info("init server port done, the port is: {}", listen);
        }

        // 初始化事件配置
        EventBlockConf eventConf = ConfLoader.getEventConf();
        if (eventConf.getWorkerConnections() < 0) {
            throw new IllegalArgumentException("illegal working connections which should be greater than 0");
        }
        RuntimeContext.setWorkerConnections(eventConf.getWorkerConnections());
        ACCESS_LOGGER.info("init event worker connection done which is: {}", eventConf.getWorkerConnections());

        String use = eventConf.getUse();
        RuntimeContext.setEventMode(use);

    }


    public static void main(String[] args) {
        // 测试全匹配
        String host = "login.daily.taobao.net:7777";

        // 测试前缀匹配
        String host2 = "hello.login.daily.taobao.net";

        // 测试后缀匹配
        String host3 = "login.daily.taobao.com";

        ContextInitializer.init();

        String upstreamHost = getUpstreamHost(host, "/login/oauth2");
        String upstreamHost2 = getUpstreamHost(host2, "/login");
        String upstreamHost3 = getUpstreamHost(host3, "/hello/");
        System.out.println(upstreamHost);
        System.out.println(upstreamHost2);
        System.out.println(upstreamHost3);


    }
}
