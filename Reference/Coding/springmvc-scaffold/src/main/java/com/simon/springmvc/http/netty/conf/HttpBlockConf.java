package com.simon.springmvc.http.netty.conf;

import java.util.List;

/**
 * @author xiele
 * @date 2018/04/04
 */
public class HttpBlockConf {

    private String index;

    private String include;

    private List<ErrorPage> errorPages;

    private String accessLog;

    private String errorLog;

    private List<ServerBlockConf> servers;

    private List<Upstream> upstream;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getInclude() {
        return include;
    }

    public void setInclude(String include) {
        this.include = include;
    }

    public List<ErrorPage> getErrorPages() {
        return errorPages;
    }

    public void setErrorPages(List<ErrorPage> errorPages) {
        this.errorPages = errorPages;
    }

    public String getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(String accessLog) {
        this.accessLog = accessLog;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public List<ServerBlockConf> getServers() {
        return servers;
    }

    public void setServers(List<ServerBlockConf> servers) {
        this.servers = servers;
    }

    public List<Upstream> getUpstream() {
        return upstream;
    }

    public void setUpstream(List<Upstream> upstream) {
        this.upstream = upstream;
    }

    static class ErrorPage {
        int code;
        String uri;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

    }

    static class Upstream {
        String name;
        List<String> servers;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getServers() {
            return servers;
        }

        public void setServers(List<String> servers) {
            this.servers = servers;
        }
    }
}
