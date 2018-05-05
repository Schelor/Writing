package com.alibaba.tianchi.dubbo.meshagent.dubbo;

import java.util.concurrent.ConcurrentHashMap;

public class RpcRequestHolder {

    // key: requestId     value: RpcFuture
    private static ConcurrentHashMap<Long,RpcFuture> processingRpc = new ConcurrentHashMap<>();

    public static void put(long requestId,RpcFuture rpcFuture){
        processingRpc.put(requestId,rpcFuture);
    }

    public static RpcFuture get(long requestId){
        return processingRpc.get(requestId);
    }

    public static void remove(long requestId){
        processingRpc.remove(requestId);
    }
}
