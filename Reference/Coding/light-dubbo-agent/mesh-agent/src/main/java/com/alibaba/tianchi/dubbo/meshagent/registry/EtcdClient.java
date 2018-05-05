package com.alibaba.tianchi.dubbo.meshagent.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.alibaba.fastjson.JSON;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.DeleteResponse;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.ExecutorServices;

/**
 * Created by juntao.hjt on 2018-04-14 17:11.
 */
public class EtcdClient {

    private static final Logger logger = LogManager.getLogger(EtcdClient.class);

    /**
     * 与注册中心心跳检查时间 单位秒
     */
    private static final int KEEP_ALIVE_CHECK_TIME = 30;

    private static volatile Client client = null;

    private static Lease lease = null;

    private static long leaseId;

    public static void init() {
        String etcdHost = System.getProperty("etcd.url");
        if (etcdHost == null) {
            throw new IllegalArgumentException("please provide the etcd url");
        }
        try {
            synchronized (EtcdClient.class) {
                if (client == null) {
                    client = Client.builder().endpoints(etcdHost).build();
                    if (client == null) {
                        throw new IllegalArgumentException("failed to connect etcd");
                    }
                    logger.info("connect the etced success. client: {}", client.toString());
                }
                lease = client.getLeaseClient();
                leaseId = lease.grant(KEEP_ALIVE_CHECK_TIME).get().getID();
            }
        } catch (Exception e) {
            logger.error("init etcd client and lease error", e);
            throw new IllegalArgumentException("etcd client error");
        }
    }

    public static void putKeyValue(String key, String value) {
        try {
            KV kv = client.getKVClient();
            CompletableFuture<PutResponse> putResponseFuture =
                    kv.put(ByteSequence.fromString(key), ByteSequence.fromString(value));
            PutResponse put = putResponseFuture.get();
            logger.info("put key: {}, response: {}", key, put);
        } catch (Exception e) {
            logger.error("put key to etcd error, key is " + key, e);
        }
    }

    public static boolean deleteKeyValue(String key) {
        try {
            KV kv = client.getKVClient();
            CompletableFuture<DeleteResponse> responseFuture = kv.delete(ByteSequence.fromString(key));
            DeleteResponse response = responseFuture.get();
            logger.info("delete key: {}, response: {}", key, response);
            return response.getDeleted() == 0L;
        } catch (Exception e) {
            logger.error("delete key from etcd error, key is " + key, e);
        }
        return false;
    }

    public static List<LightNode> getValueByKey(String key) {
        try {
            KV kvClient = client.getKVClient();
            CompletableFuture<GetResponse> getFuture = kvClient.get(ByteSequence.fromString(key));
            GetResponse response = getFuture.get();
            logger.info("get value key: {}, response: {}", key, response);
            List<KeyValue> kvs = response.getKvs();
            if (kvs == null || kvs.isEmpty()) {
                return Collections.emptyList();
            }
            return Lists.transform(kvs, new Function<KeyValue, LightNode>() {
                @Nullable
                @Override
                public LightNode apply(@Nullable KeyValue input) {
                    // TODO
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("get value from etcd error, key is " + key, e);
        }
        return null;
    }

    public static void watch(String key, NotifyListener notifyListener) {
        ByteSequence keySeq = ByteSequence.fromString(key);
        Watch.Watcher watcher = client.getWatchClient().watch(
                keySeq, WatchOption.newBuilder()
                .withPrefix(keySeq)
                .withNoPut(false)
                .withNoDelete(false)
                .build());

        Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("ClientToEtcdWatcher").build()).submit(() -> {
            WatchResponse response = null;
            try {
                response = watcher.listen();
            } catch (InterruptedException e) {
                logger.error("watch etcd error", e);
            }
            for (WatchEvent event : response.getEvents()) {
                notifyListener.notify(event);
            }
        });

    }

    public static Map<String, LightNode> getNodeMapByPrefixKey(String keyPrefix) {
        Map<String, LightNode> kvMap = new HashMap<>(4);
        try {
            KV kv = client.getKVClient();
            ByteSequence keySeq = ByteSequence.fromString(keyPrefix);
            CompletableFuture<GetResponse> responseFuture = kv.get(keySeq, GetOption.newBuilder().withPrefix(keySeq).build());
            GetResponse getResponse = responseFuture.get();
            List<KeyValue> keyValueList = getResponse.getKvs();
            if (keyValueList == null || keyValueList.isEmpty()) {
                return Collections.emptyMap();
            }
            for (KeyValue keyValue : keyValueList) {
                String key = keyValue.getKey().toStringUtf8();
                String value = keyValue.getValue().toStringUtf8();
                LightNode lightNodeValue = JSON.parseObject(value, LightNode.class);
                kvMap.put(key, lightNodeValue);
            }
        } catch (Exception e) {
            logger.error("prefix get error, prefix is " + keyPrefix, e);
        }
        return kvMap;
    }

    public static void keepAlive() {
        Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("HostToEtcdAliveWorker").build()).submit(() -> {
            try {
                Lease.KeepAliveListener listener = lease.keepAlive(leaseId);
                LeaseKeepAliveResponse listen = listener.listen();
                logger.info("host to etcd keep alive check. leaseId: {}, response: {}", leaseId, listen);
            } catch (Exception e) {
                logger.error("keep alive check error", e);
            }
        });
    }
}
