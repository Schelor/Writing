package com.netty.meshagent.registry;

import com.coreos.jetcd.watch.WatchEvent;

/**
 * @author xiele
 * @date 2018/04/14
 */
public interface NotifyListener {

    void notify(WatchEvent watchEvent);
}
