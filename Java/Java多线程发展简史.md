# 前言
在一段时间里，对于Java多线程，自我觉得还是蛮了解的，自从看到了四火写的一篇文章(http://www.raychase.net/698)，深受启发，思考问题以及作文该如此啊。
于是想在该文的基础上，做些自己的解读，顺便学习一下其作文思路。

# 开始
假如别人问道你一个问题，提到Java的多线程编程，你会想到什么？我能想到的是：
- synchronized、volatile、锁机制
- 竞争和同步
- 线程池和队列
- Disruptor、单写原则（之前研究过Disruptor）

四火同志提到有：
1. 模型（Java内存模型）JCM(Java并发模型)
2. 使用：JDK中的并发包
3. 实践：怎样写出线程安全的代码
4. 排错：使用工具来分析并发问题

同时作者提到一种思路，从历史的角度来看Java多线程编程是怎样演变的，在这个过程中，采取了哪些正确的决定，犯了哪些错误？未来有哪些发展趋势.
As Linus said: Anyway, We all talk is cheap, show me the code, 所以作者尽量用代码说话。

# Java多线程编程历史
## 从Java诞生开始
Java的基因来自于1990年12月Sun公司的一个内部项目，目标设备正是家用电器，但是C++的可移植性和API的易用性都让程序员反感。旨在解决这样的问题，于是又了Java的前身Oak语言，但是知道1995年3月，它正式更名为Java，才算Java语言真正的诞生。

## JDK 1.0
抢占式和协作式是两种常见的进程/线程调度方式，操作系统非常适合使用抢占式方式来调度它的进程，它给不同的进程分配时间片，对于长期无响应的进程，它有能力剥夺它的资源，甚至将其强行停止（如果采用协作式的方式，需要进程自觉、主动地释放资源，也许就不知道需要等到什么时候了）。Java语言一开始就采用协作式的方式，并且在后面发展的过程中，逐步废弃掉了粗暴的stop/resume/suspend这样的方法，它们是违背协作式的不良设计，转而采用wait/notify/sleep这样的两边线程配合行动的方式。

一种线程间的通信方式是使用中断：
```java
/**
 * @author xiele
 * @date 2017/10/18
 */
public class InterruptCheck extends Thread {


    @Override
    public void run() {
        System.out.println("Start");
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
        System.out.println("SubThread Exit");
    }

    public static void main(String[] args) {

        InterruptCheck ic = new InterruptCheck();
        ic.start();

        try {
             sleep(3000);
        } catch (InterruptedException e) {

        }
        // 主线程发起让子线程中断，子线程在while(true)中感知到中断后，退出
        ic.interrupt();

        System.out.println("Main Exit");

    }
}

```
这是中断的一种使用方式，看起来就像是一个标志位，线程A设置这个标志位，线程B时不时地检查这个标志位。另外还有一种使用中断通信的方式，如下：
```java
public class InterruptWait extends Thread {

    // 监视器
    private Object lock = new Object();

    @Override
    public void run() {
        System.out.println("SubThread Start");
        // 进入锁临界区，
        synchronized (lock) {
            try {
                // 让出当前锁，进入等待中(条件队列)
                lock.wait();
            } catch (InterruptedException e) {
                System.out.println("SubTread是否中断: " + Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
                System.out.println("SubTread是否中断: " + Thread.currentThread().isInterrupted());
                e.printStackTrace();
            }
            System.out.println("SubTread Exit");
        }
    }

    public static void main(String[] args) {
        Thread t = new InterruptWait();
        t.start();
        try {
            sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t.interrupt();
        System.out.println("Main Exit");

    }
}

```
在这种方式下，如果使用wait方法处于等待中的线程，被另一个线程使用中断唤醒，于是抛出InterruptedException，`同时，中断标志清除，这时候我们通常会在捕获该异常的地方重新设置中断，以便后续的逻辑通过检查中断状态来了解该线程是如何结束的`。
在比较稳定的JDK 1.0.2版本中，已经可以找到Thread和ThreadUsage这样的类，这也是线程模型中最核心的两个类。整个版本只包含了这样几个包：java.io、 java.util、java.net、java.awt和java.applet，所以说Java从一开始这个非常原始的版本就确立了一个持久的线程模型。
