package com.alibaba.dubbo.mesh.agent.common;

import com.alibaba.tianchi.dubbo.meshagent.registry.LightNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by juntao.hjt on 2018-04-20 09:56.
 */
public class ServerNodeSelector {

    private static final Logger logger = LogManager.getLogger(ServerNodeSelector.class);

    //内部 volatile Object[] array 保证可见性
    private static final List<LightNode> selectorList = new CopyOnWriteArrayList<>();
    private static final AtomicInteger selectIdx = new AtomicInteger();

    public static LightNode selectWeightNode() {
        return selectorList.get(selectIdx.getAndIncrement() % selectorList.size());
    }

    public static void loadWeightNode(List<LightNode> nodeList) {
        if (nodeList.isEmpty()) {
            return;
        }
        nodeList.sort((o1, o2) -> o2.getWeight() - o1.getWeight());
        for (LightNode lightNode : nodeList) {
            for (int i = 0; i < lightNode.getWeight(); i++) {
                selectorList.add(lightNode);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        List<LightNode> lightNodeList =
                Arrays.asList(new LightNode("localhost", 30000, 1),
                        new LightNode("localhost", 30001, 2),
                        new LightNode("localhost", 30002, 3));
        loadWeightNode(lightNodeList);
        int threads = 256;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < 600000; j++) {
                    selectWeightNode();
                }
                latch.countDown();
            });
        }
        latch.await();
        executorService.shutdown();
    }


}
