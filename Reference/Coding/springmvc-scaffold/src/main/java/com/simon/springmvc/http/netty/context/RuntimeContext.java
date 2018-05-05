package com.simon.springmvc.http.netty.context;

import java.util.Set;

import com.google.common.collect.Sets;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * @author xiele
 * @date 2018/04/04
 */
public final class RuntimeContext {

    private static Set<Integer> workingPort = Sets.newHashSet();

    private static int DEFAULT_EVENT_LOOP_THREADS;

    private static int workerConnections;

    private static EventMode eventMode;

    static {
        // 实际上需要根据监控动态调整
        DEFAULT_EVENT_LOOP_THREADS = Math.max(8, SystemPropertyUtil.getInt(
            "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 4));
    }

    private RuntimeContext() {}

    public static void addWorkingPort(int port) {
        workingPort.add(port);
    }

    public static Set<Integer> getWorkingPort() {
        return workingPort;
    }

    public static int getIoThreads() {
        return DEFAULT_EVENT_LOOP_THREADS;
    }

    public static int getWorkerConnections() {
        return workerConnections;
    }

    public static void setWorkerConnections(int connections) {
        workerConnections = connections;
    }

    public static EventMode getEventMode() {

        return eventMode;
    }

    public static void setEventMode(String eventMode) {
        if (EventMode.isEpoll(eventMode)) {
            RuntimeContext.eventMode =  EventMode.EPOLL;
            return ;
        }
        if (EventMode.isNio(eventMode)) {
            RuntimeContext.eventMode = EventMode.NIO;
            return ;
        }
        // 运行时校验，默认为epoll

        RuntimeContext.eventMode = EventMode.EPOLL;


    }
}
