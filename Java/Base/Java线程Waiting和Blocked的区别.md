
# Java线程Waiting和Blocked的区别

## 概要
1. Blocked意味着等待在某资源上(等待在同步块的监视器锁上,等待进入同步块), 由JVM来来唤醒
2. Waiting表示当前线程让出了资源(锁)，而让自己处于等待状态， 需要由另一个线程来唤醒
3. 从本质上说，Blocked在锁上，线程需要进入锁对应的同步队列，而Waiting的线程让出锁后，进入锁对应的条件队列，直到另一个线程notify后，
转而进入锁对应的同步对象，然后参与竞争锁，重新试图进入同步代码块。

## 参考
看看JDK中的注释：
```
public enum State {
        /**
         * Thread state for a thread which has not yet started.
         */
        NEW,

        /**
         * Thread state for a runnable thread.  A thread in the runnable
         * state is executing in the Java virtual machine but it may
         * be waiting for other resources from the operating system
         * such as processor.
         */
        RUNNABLE,

        /**
         * Thread state for a thread blocked waiting for a monitor lock.
         * A thread in the blocked state is waiting for a monitor lock
         * to enter a synchronized block/method or
         * reenter a synchronized block/method after calling
         * {@link Object#wait() Object.wait}.
         */
        BLOCKED,

        /**
         * Thread state for a waiting thread.
         * A thread is in the waiting state due to calling one of the
         * following methods:
         * <ul>
         *   <li>{@link Object#wait() Object.wait} with no timeout</li>
         *   <li>{@link #join() Thread.join} with no timeout</li>
         *   <li>{@link LockSupport#park() LockSupport.park}</li>
         * </ul>
         *
         * <p>A thread in the waiting state is waiting for another thread to
         * perform a particular action.
         *
         * For example, a thread that has called <tt>Object.wait()</tt>
         * on an object is waiting for another thread to call
         * <tt>Object.notify()</tt> or <tt>Object.notifyAll()</tt> on
         * that object. A thread that has called <tt>Thread.join()</tt>
         * is waiting for a specified thread to terminate.
         */
        WAITING,
}

```
参考一下知乎上一位同志说的：

假设t1，t2先后两个线程，都执行如下代码：
```java
synchronized(Obj) {
    Obj.wait();
}
```
t1先进，最后在Obj.wait()下卡住，这时java管t1的状态waitting状态t2后进，直接在第一行就卡住了，这时java叫t2为blocked状态。请注意，blocked是过去分词，意味着他是被卡住的(无辜啊，全是泪)。因为这段代码只让一条线程运行。同时，jvm是知道怎么结束blocked的，只要别的线程退出这段代码，他就会自动让你进去。也就是说别的线程无需唤醒你，由jvm自动来干。而waiting是说我调用wait()等函数，主动卡住自己而waiting，请jvm在满足某种条件后(白富美发消息让我们晚上见)，比如另条线程调用了notify()后，把我唤醒。这个唤醒的责任在于别的线程,明确的调用一些唤醒函数。做这样的区分，是jvm出于管理的需要，做了这种区分，比如两个原因的线程放两个队列里管理，如果别的线程运行出了synchronized这段代码，我只需要去blocked队列，放个出来。而某人调用了notify()，我只需要去waitting队列里取个出来。
P.S. 从linux内核来看，这些线程都是等待状态，没区别，区别只在于java的管理需要。通常我们在系统级别说线程的blocked，是说线程操作io，被暂停了，这种线程由linux内核来唤醒（io设备报告数据来了，内核把block的线程放进可运行的进程队列，依次得到处理器时间），而wait是说，等待一个内核mutex对象，另个线程signal这个mutex后，这个线程才可以运行。区别在于由谁唤醒，是操作系统，还是另一个线程，这里倒和java很相似。
