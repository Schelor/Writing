package com.netty.meshagent.lb;

import java.util.List;

import com.netty.meshagent.registry.LightNode;

/**
 * Created by juntao.hjt on 2018-04-15 11:13.
 */
public class ConsistentHashLoadBalance implements LoadBalance {

    private  final ConsistentHashSelector<LightNode> selector = new ConsistentHashSelector<>(160);


    public void init(List<LightNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return ;
        }
        selector.add(nodes);
    }


    @Override
    public LightNode select(Router router) {
        return selector.get(router.parameter());
    }

    @Override
    public LightNode select(List<LightNode> nodes) {
        throw new IllegalArgumentException("not supported");
    }
}
