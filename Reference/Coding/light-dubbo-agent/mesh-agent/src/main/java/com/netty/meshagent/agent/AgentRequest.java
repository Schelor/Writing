package com.netty.meshagent.agent;

import java.net.URI;

/**
 * @author xiele
 * @date 2018/04/15
 */
public interface AgentRequest {

    URI uri();

    <T> T message();
}
