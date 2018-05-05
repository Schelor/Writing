package com.simon.springmvc.http.netty.context;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.simon.springmvc.http.netty.conf.ServerBlockConf;
import com.simon.springmvc.http.netty.conf.ServerBlockConf.Location;
import com.simon.springmvc.http.netty.util.logger.ServerLoggerFactory;
import org.apache.logging.log4j.Logger;

/**
 * @author xiele
 * @date 2018/04/04
 */
public class StoredUpstreamServer {

    private static Logger logger = ServerLoggerFactory.getLogger();

    private static final String STAR_SYMBOL = "*";

    private static final Map<String, UpstreamWrapper> store = Maps.newLinkedHashMap();

    public static void storeServer(ServerBlockConf serverConf) {
        String serverName = serverConf.getServerName();
        emptyServerConf(serverConf);
        if (!serverName.contains(STAR_SYMBOL)) { // 不带*通配符
            store.put(serverName,
                UpstreamWrapper.create(serverName, Pattern.compile(serverName), serverConf.getLocations()));
            return;
        }
        // 只支持前缀和后缀通配符
        List<String> splittedServer = Splitter.on(STAR_SYMBOL).splitToList(serverName);
        invalidServerName(splittedServer);

        if ("".equals(splittedServer.get(0))) {
            store.put(splittedServer.get(1), UpstreamWrapper
                .create(serverName, Pattern.compile(serverName.replace(STAR_SYMBOL, ".*")), serverConf.getLocations()));
        }
        if ("".equals(splittedServer.get(1))) {
            store.put(splittedServer.get(0), UpstreamWrapper
                .create(serverName, Pattern.compile(serverName.replace(STAR_SYMBOL, "*")), serverConf.getLocations()));
        }

    }

    public static String getUpstreamHost(String requestHost, String requestUri) {
        if (requestHost == null) {
            return null;
        }
        // 先取匹配
        UpstreamWrapper uw = store.get(requestHost);
        if (uw == null) {
            // 再去前缀匹配
            uw = findHeadWildcard(requestHost);
            // 最后取后缀匹配
            if (uw == null) {
                uw = findTailWildcard(requestHost);
            }
        }
        if (uw == null) {
            return null;
        }
        boolean matches = matchDefinition(requestHost, uw.getPattern());
        logger.info("request host matches the upstream server. "
            + "requestHost: {}, serverPattern: {}, serverUri: {}",
            requestHost, uw.getPattern(), JSON.toJSONString(uw.getLocations()));
        if (!matches) {
            return null;
        }
        String actualServer = null;
        List<Location> locations = uw.getLocations();

        // TODO 此处还需处理 负载均衡 逻辑
        for (Location l : locations) {
            // 此处先做URI查找, 避免同时匹配
            if (requestUri.indexOf(l.getUriPattern()) > 0) {
                return l.getProxyPass();
            } else {
                if (requestUri.startsWith(l.getUriPattern())) {
                    actualServer = l.getProxyPass();
                    break;
                }
            }
        }
        logger.info("found the actual server which is {}", actualServer);
        return actualServer;
    }

    public static boolean matchDefinition(String requestHost, Pattern pattern) {
        return (pattern != null && pattern.matcher(requestHost).matches());
    }


    public static boolean supportUpstream(String upstreamHost) {
        if (upstreamHost == null) {
            return false;
        }

        return true;
    }


    private static void emptyServerConf(ServerBlockConf serverConf) {
        if (serverConf == null || serverConf.getServerName() == null
            || serverConf.getLocations() == null || serverConf.getLocations().isEmpty()) {
            throw new IllegalArgumentException("illegal server conf which should not be empty");
        }

    }

    private static void invalidServerName(List<String> splittedServer) {
        if (splittedServer == null || splittedServer.size() > 2) {
            throw new IllegalArgumentException("illegal server name which only supports prefix or suffix wildcard");
        }
    }

    private static UpstreamWrapper findHeadWildcard(String host) {
        int index = 0;
        for (;;) {
            int i = host.indexOf(".", index+1);
            int last = host.lastIndexOf(".");
            if (i >= last) {
                break;
            }
            index = i;
            String str = host.substring(i);
            logger.info("resolve head host. current: {}", str);
            UpstreamWrapper uw = store.get(str);
            if (uw != null) {
                return uw;
            }
        }
        return null;
    }

    // 后缀只支持 aaa.bbb.*
    private static UpstreamWrapper findTailWildcard(String host) {
        String tailWildCard = host.substring(0, host.lastIndexOf(".") + 1);
        return store.get(tailWildCard);
    }

    static class UpstreamWrapper {
        String serverName;
        Pattern pattern;
        List<ServerBlockConf.Location> locations;

        public static UpstreamWrapper create(String serverName, Pattern p, List<ServerBlockConf.Location> l) {
            UpstreamWrapper uw = new UpstreamWrapper();
            uw.setServerName(serverName).setPattern(p).setLocations(l);
            logger.info("stored upstream server is: {}", JSON.toJSONString(uw));
            return uw;
        }

        public String getServerName() {
            return serverName;
        }

        public UpstreamWrapper setServerName(String serverName) {
            this.serverName = serverName;
            return UpstreamWrapper.this;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public UpstreamWrapper setPattern(Pattern pattern) {
            this.pattern = pattern;
            return UpstreamWrapper.this;
        }

        public List<Location> getLocations() {
            return locations;
        }

        public UpstreamWrapper setLocations(List<Location> locations) {
            this.locations = locations;
            return UpstreamWrapper.this;
        }
    }


}
