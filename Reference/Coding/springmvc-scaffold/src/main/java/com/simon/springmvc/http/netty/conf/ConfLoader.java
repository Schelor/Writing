package com.simon.springmvc.http.netty.conf;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author xiele
 * @date 2018/04/04
 */
@Slf4j
public final class ConfLoader {

    private static MainBlockConf mainConf;

    static {
        mainConf = load();
    }

    public static MainBlockConf load() {
        ClassLoader clc = Thread.currentThread().getContextClassLoader();
        InputStream in = clc.getResourceAsStream("conf.json");
        try {
            MainBlockConf conf = JSON.parseObject(in, new TypeReference<MainBlockConf>() {
            }.getType());
            log.info("load conf: {}", JSON.toJSONString(conf,true));
            return conf;
        } catch (IOException e) {
            throw new IllegalArgumentException("load conf error");
        }

    }

    public static MainBlockConf getMainConf() {
        if (mainConf == null) {
            throw new IllegalArgumentException("there maybe a missing conf.json");
        }
        return mainConf;
    }

    public static EventBlockConf getEventConf() {
        EventBlockConf events = getMainConf().getEvents();
        if (events == null) {
            throw new IllegalArgumentException("conf error, missing event conf");
        }
        return events;
    }

    public static HttpBlockConf getHttpConf() {

        HttpBlockConf http = getMainConf().getHttp();
        if (http == null) {
            throw new IllegalArgumentException("conf error, missing http conf");
        }
        return http;
    }

    public static List<ServerBlockConf> getServerConf() {
        List<ServerBlockConf> servers = getHttpConf().getServers();
        if (servers == null) {
            throw new IllegalArgumentException("conf error, missing server conf");
        }
        return servers;
    }

    // for test
    public static void main(String[] args) {
        load();
    }

}

