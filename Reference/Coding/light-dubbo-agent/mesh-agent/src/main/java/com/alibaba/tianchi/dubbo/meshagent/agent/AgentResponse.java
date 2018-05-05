package com.alibaba.tianchi.dubbo.meshagent.agent;

import java.util.Optional;

/**
 * @author xiele
 * @date 2018/04/15
 */
public interface AgentResponse {

    /**
     * 报文
     * @param <T>
     * @return
     */
    <T> Optional<T> message();

}
