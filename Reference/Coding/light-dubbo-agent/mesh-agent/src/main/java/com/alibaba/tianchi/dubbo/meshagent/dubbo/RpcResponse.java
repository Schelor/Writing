package com.alibaba.tianchi.dubbo.meshagent.dubbo;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class RpcResponse {

    private long requestId;
    private Object result;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId=" + requestId +
                ", result=" + result +
                '}';
    }
}
