package com.alibaba.tianchi.dubbo.meshagent.agent;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author xiele
 * @date 2018/04/17
 */
public interface AgentClient {

    ListenableFuture<AgentResponse> executeAsync(AgentRequest request);
}
