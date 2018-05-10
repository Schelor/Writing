package com.netty.meshagent.lb;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.hash.Hashing;

/**
 * @author xiele
 * @date 2018/04/17
 */

class ConsistentHashSelector<T> {

    private final int numberOfVirtualNodeReplicas;
    private final SortedMap<Long, T> circle = new TreeMap<>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    public ConsistentHashSelector(int numberOfVirtualNodeReplicas) {
        this.numberOfVirtualNodeReplicas = numberOfVirtualNodeReplicas;
    }

    public synchronized void add(T node) {
        w.lock();
        try {
            addNode(node);
        } finally {
            w.unlock();
        }
    }

    public synchronized void add(List<T> nodes) {
        w.lock();
        try {
            for (T node : nodes) {
                addNode(node);
            }
        } finally {
            w.unlock();
        }
    }

    public synchronized void remove(List<T> nodes) {
        w.lock();
        try {
            for (T node : nodes) {
                removeNode(node);
            }
        } finally {
            w.unlock();
        }
    }

    public synchronized void remove(T node) {
        w.lock();
        try {
            removeNode(node);
        } finally {
            w.unlock();
        }
    }

    public T get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = getKey(key.toString());
        r.lock();
        try {
            if (!circle.containsKey(hash)) {
                SortedMap<Long, T> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }
            return circle.get(hash);
        } finally {
            r.unlock();
        }
    }

    private void addNode(T node) {
        for (int i = 0; i < numberOfVirtualNodeReplicas / 4; i++) {
            byte[] digest = md5(node + "-" + i);
            for (int h = 0; h < 4; h++) {
                circle.put(getKey(digest, h), node);
            }
        }
    }

    private void removeNode(T node) {
        for (int i = 0; i < numberOfVirtualNodeReplicas / 4; i++) {
            byte[] digest = md5(node.toString() + "-" + i);
            for (int h = 0; h < 4; h++) {
                circle.remove(getKey(digest, h));
            }
        }
    }

    public static byte[] md5(String text) {
        return Hashing.md5().hashBytes(text.getBytes()).asBytes();
    }

    public static long[] getKeys(String text) {
        long[] pairs = new long[4];
        byte[] digest = md5(text);
        for (int h = 0; h < 4; h++) {
            pairs[h] = getKey(digest, h);
        }
        return pairs;
    }

    public static long getKey(final String k) {
        byte[] digest = md5(k);
        return getKey(digest, 0) & 0xffffffffL;
    }

    public static Long getKey(byte[] digest, int h) {
        return ((long)(digest[3 + h * 4] & 0xFF) << 24) | ((long)(digest[2 + h * 4] & 0xFF) << 16) | (
            (long)(digest[1 + h * 4] & 0xFF) << 8) | (digest[h * 4] & 0xFF);
    }
}
