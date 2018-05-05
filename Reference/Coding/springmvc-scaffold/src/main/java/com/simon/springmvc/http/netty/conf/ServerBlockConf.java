package com.simon.springmvc.http.netty.conf;

import java.util.List;

/**
 * @author xiele
 * @date 2018/04/04
 */
public class ServerBlockConf {

    private int listen;

    private String serverName;

    private boolean defaultServer;

    private List<Location> locations;

    public int getListen() {
        return listen;
    }

    public void setListen(int listen) {
        this.listen = listen;
    }

    public String getServerName() {
        return serverName;
    }


    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public boolean isDefaultServer() {
        return defaultServer;
    }

    public void setDefaultServer(boolean defaultServer) {
        this.defaultServer = defaultServer;
    }


    public static class Location {

        String uriPattern;
        String proxyPass;

        public String getUriPattern() {
            return uriPattern;
        }

        public void setUriPattern(String uriPattern) {
            this.uriPattern = uriPattern;
        }

        public String getProxyPass() {
            return proxyPass;
        }

        public void setProxyPass(String proxyPass) {
            this.proxyPass = proxyPass;
        }
    }
}
