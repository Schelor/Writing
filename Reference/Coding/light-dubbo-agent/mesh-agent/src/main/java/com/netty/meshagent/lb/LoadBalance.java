package com.netty.meshagent.lb;

import com.netty.meshagent.registry.LightNode;

import java.util.List;

/**
 * Created by juntao.hjt on 2018-04-15 11:12.
 */
public interface LoadBalance {

    LightNode select(Router router);

    LightNode select(List<LightNode> nodes);

    /**
     * 目前只根据参数路由
     */
    interface Router {
        String parameter();
    }
}
