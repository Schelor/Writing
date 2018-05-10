package com.netty.performace.common;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

import com.netty.meshagent.registry.LightNode;

/**
 * 随机，按权重设置随机概率, 比较平均
 * Created by juntao.hjt on 2018-04-15 11:14.
 */
public class RandomNodeSelector {

    private static final List<LightNode> selectorList = new CopyOnWriteArrayList<>();

    private static final ConcurrentMap<String, LongAdder> nodeLoadNumMap = new ConcurrentHashMap<>();

    public static void loadWeightNode(List<LightNode> nodeList) {
        if (nodeList.isEmpty()) {
            return;
        }
        selectorList.addAll(nodeList);
    }

    public static LightNode selectWeightNode() {
        LightNode node = select(selectorList);
        nodeLoadNumMap.computeIfAbsent(node.nodeKey(), k -> new LongAdder()).increment();
        return node;
    }

    public static LightNode select(List<LightNode> nodes) {
        int length = selectorList.size();
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
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < length; i++) {
                offset -= getWeight(nodes, i);
                if (offset < 0) {
                    return nodes.get(i);
                }
            }
        }
        return nodes.get(ThreadLocalRandom.current().nextInt(length));
    }


    private static int getWeight(List<LightNode> nodes, int index) {
        return nodes.get(index).getWeight();
    }

    public static void main(String[] args) throws InterruptedException {
        List<LightNode> nodes =
            Arrays.asList(new LightNode("localhost", 30000, 1),
                new LightNode("localhost", 30001, 2),
                new LightNode("localhost", 30002, 3));

        loadWeightNode(nodes);
        int threads = 256;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < 600000; j++) {
                    LightNode select = selectWeightNode();
                    LongAdder la = nodeLoadNumMap.computeIfAbsent(select.nodeKey(), s -> new LongAdder());
                    la.increment();
                }
                latch.countDown();
            });
        }
        latch.await();
        System.out.println(nodeLoadNumMap);
        executorService.shutdown();

    }


}
