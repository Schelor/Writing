package com.netty.performace.common;

import com.alibaba.fastjson.JSON;
import com.netty.meshagent.registry.LightNode;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.options.GetOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by juntao.hjt on 2018-04-20 03:40.
 */
public class EtcdRegistry {


    private static final Logger logger = LogManager.getLogger(EtcdRegistry.class);

    private static final String REGISTER_PREFIX = "AgentNode_";

    private static final ConcurrentHashMap<String, LightNode> registerNodeMap = new ConcurrentHashMap<>(4);

    private static KV kv;
    private static Lease lease;
    private static long leaseId;

    static {
        try {
            String etcdUrl = System.getProperty("etcd.url");
            if (etcdUrl == null) {
                etcdUrl = "http://127.0.0.1:2379";
            }
            Client client = Client.builder().endpoints(etcdUrl).build();
            kv = client.getKVClient();
            lease = client.getLeaseClient();
            leaseId = lease.grant(30).get().getID();
        } catch (Exception e) {
            logger.error("Init EtcdRegistry error", e);
        }
    }

    public static void register(LightNode node) {
        String nodeKey = buildKeyByNode(node);
        String nodeValue = JSON.toJSONString(node);
        try {
            CompletableFuture<PutResponse> future = kv.put(ByteSequence.fromString(nodeKey), ByteSequence.fromString(nodeValue));
            future.get();
            logger.info("register light node success, {}:{}", nodeKey, nodeValue);
        } catch (Exception e) {
            logger.error("register light node error", e);
        }
    }


    public static void unregister(LightNode node) {
        String key = buildKeyByNode(node);
        try {
            CompletableFuture<DeleteResponse> responseFuture =
                    kv.delete(ByteSequence.fromString(key));
            DeleteResponse response = responseFuture.get();
            logger.info("delete key: {}, response: {}", node, response);
        } catch (Exception e) {
            logger.error("delete key from etcd error, key is " + key, e);
        }
    }

    private static String buildKeyByNode(LightNode node) {
        return REGISTER_PREFIX + node.nodeKey();
    }

    public static List<LightNode> findLightNodes() {
        if (registerNodeMap.isEmpty()) {
            loadServerNodeConfig();
        }
        return new ArrayList<>(registerNodeMap.values());
    }

    private static void loadServerNodeConfig() {
        try {
            ByteSequence keySeq = ByteSequence.fromString(REGISTER_PREFIX);
            CompletableFuture<GetResponse> responseFuture = kv.get(keySeq, GetOption.newBuilder().withPrefix(keySeq).build());
            GetResponse getResponse = responseFuture.get();
            List<KeyValue> keyValueList = getResponse.getKvs();
            for (KeyValue keyValue : keyValueList) {
                String key = keyValue.getKey().toStringUtf8();
                String value = keyValue.getValue().toStringUtf8();
                LightNode lightNodeValue = JSON.parseObject(value, LightNode.class);
                registerNodeMap.put(key, lightNodeValue);
            }
            if (!registerNodeMap.isEmpty()) {
                logger.info("loadServerNodeConfig success:{}", JSON.toJSONString(registerNodeMap));
            }
        } catch (Exception e) {
            logger.error("loadServerNodeConfig error", e);
        }
    }

}
