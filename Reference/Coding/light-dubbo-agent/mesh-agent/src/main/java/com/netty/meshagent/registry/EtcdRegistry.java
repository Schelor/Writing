package com.netty.meshagent.registry;

import com.alibaba.fastjson.JSON;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.watch.WatchEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class EtcdRegistry implements Registrable {

    private static final Logger logger = LogManager.getLogger(EtcdRegistry.class);

    private static final String REGISTER_PREFIX = "AgentNode_";

    private final ConcurrentHashMap<String, LightNode> registNodeMap = new ConcurrentHashMap<>(4);

    @Override
    public void register(LightNode node) {
        String nodeKey = buildKeyByNode(node);
        String nodeValue = JSON.toJSONString(node);
        EtcdClient.putKeyValue(nodeKey, nodeValue);
    }

    private String buildKeyByNode(LightNode node) {
        return REGISTER_PREFIX + node.nodeKey();
    }

    @Override
    public void unregister(LightNode node) {
        String nodeKey = buildKeyByNode(node);
        EtcdClient.deleteKeyValue(nodeKey);
    }

    @Override
    public void subscribe(String appName) {
        logger.info("http server subscribed {} all nodes", appName);
        EtcdClient.watch(REGISTER_PREFIX, (watchEvent) -> {
            WatchEvent.EventType eventType = watchEvent.getEventType();
            KeyValue keyValue = watchEvent.getKeyValue();
            String key = keyValue.getKey().toStringUtf8();
            if (eventType == WatchEvent.EventType.PUT) {
                String jsonValue = keyValue.getKey().toStringUtf8();
                registNodeMap.put(key, JSON.parseObject(jsonValue, LightNode.class));
            }
            if (eventType == WatchEvent.EventType.DELETE) {
                registNodeMap.remove(key);
            }
        });
    }

    @Override
    public List<LightNode> lookup() {
        if (registNodeMap.isEmpty()) {
            Map<String, LightNode> lightNodeMap =
                    EtcdClient.getNodeMapByPrefixKey(REGISTER_PREFIX);
            registNodeMap.putAll(lightNodeMap);
        }
        return new ArrayList<>(registNodeMap.values());
    }

    @Override
    public List<LightNode> lookup(LightNode node) {
        return lookup();
    }
}
