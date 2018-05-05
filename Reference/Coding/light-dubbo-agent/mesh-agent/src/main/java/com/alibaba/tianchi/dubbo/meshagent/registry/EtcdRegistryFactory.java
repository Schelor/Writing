package com.alibaba.tianchi.dubbo.meshagent.registry;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class EtcdRegistryFactory {

    private static final EtcdRegistry etcd = new EtcdRegistry();

    public static void checkInit() {
        EtcdClient.init();
    }

    public static EtcdRegistry registry() {
        return etcd;
    }


}
