package com.alibaba.tianchi.dubbo.meshagent.util;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class ServerException extends RuntimeException {

    public ServerException() {
    }

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable e) {
        super(message, e);
    }
}
