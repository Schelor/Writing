package com.netty.meshagent.registry;

import java.util.List;

/**
 * @author xiele
 * @date 2018/04/14
 */
public interface Registrable {

    void register(LightNode node);

    void unregister(LightNode node);

    void subscribe(String appName);

    List<LightNode> lookup();

    List<LightNode> lookup(LightNode node);
}
