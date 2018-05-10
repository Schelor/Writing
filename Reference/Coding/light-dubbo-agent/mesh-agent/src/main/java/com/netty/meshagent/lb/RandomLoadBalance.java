package com.netty.meshagent.lb;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.netty.meshagent.registry.LightNode;

/**
 * 随机，按权重设置随机概率, 比较平均
 * Created by juntao.hjt on 2018-04-15 11:14.
 */
public class RandomLoadBalance implements LoadBalance {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();


    public LightNode select(Router router) {
        throw new IllegalArgumentException("not supported");
    }

    @Override
    public LightNode select(List<LightNode> nodes) {
        int length = nodes.size();
        int totalWeight = 0; // The sum of weights
        boolean sameWeight = true; // Every invoker has the same weight?
        for (int i = 0; i < length; i++) {
            int weight = getWeight(nodes, i);
            totalWeight += weight; // Sum
            if (sameWeight && i > 0 && weight != getWeight(nodes, i - 1)) {
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            int offset = random.nextInt(totalWeight);
            for (int i = 0; i < length; i++) {
                offset -= getWeight(nodes, i);
                if (offset < 0) {
                    return nodes.get(i);
                }
            }
        }
        return nodes.get(random.nextInt(length));
    }


    private int getWeight(List<LightNode> nodes, int index) {
        return nodes.get(index).getWeight();
    }
}
