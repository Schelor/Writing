## 常用命令
获取Jvm进程Pid:
ps -ef | grep java 或 /opt/install/java/bin/jpv -v

查看最耗费cpu的线程

top -H -p jvm进程pid(ps -ef | grep java)

printf "5x\n" pid  -> 线程16进制(A)

sudo -u admin /opt/install/java/bin/jstack pid | grep A

-- 堆dump
sudo /opt/install/java/bin/jmap -dump:format=b,file=/home/admin/tools/heap.bin 26362
-- 查看堆信息
sudo /opt/install/java/bin/jmap -heap 26362

-- 查看单次GC
sudo /opt/install/java/bin/jstat -gc 26362

sudo /opt/install/taobao/java/bin/jstat -gcutil 26362 1s 3
sudo
-- 堆占用前10
sudo -u admin  /opt/install/java/bin/jmap -histo 26362

## GC日志

2019-05-17T10:53:19.906+0800: 1065.563: [GC (Allocation Failure) 2019-05-17T10:53:19.906+0800: 1065.563: [ParNew(新生代并行收集器): 710072K->40939K(720896K), 0.0641636 secs] 1004802K->344114K(4128768K), 0.0644972 secs] [Times: user=0.43 sys=0.01, real=0.07 secs]
1. 2019-05-17T10:53:19.906+0800: 1065.563: GC开始时间
2. GC – 用来区分(distinguish)是 Minor GC 还是 Full GC 的标志(Flag). 这里的 GC 表明本次发生的是 Minor GC.
3. Allocation Failure – 引起垃圾回收的原因. 本次GC是因为年轻代中没有任何合适的区域能够存放需要分配的数据结构而触发的.
4. ParNew(新生代并行收集器) DefNew – 使用的垃圾收集器的名字. DefNew 这个名字代表的是: 单线程(single-threaded), 采用标记复制(mark-copy)算法的, 使整个JVM暂停运行(stop-the-world)的年轻代(Young generation) 垃圾收集器(garbage collector).
5. 710072K->40939K(720896K), 0.0641636 secs] 在本次垃圾收集之前和之后的年轻代内存使用情况(Usage)，年轻代的总的大小(Total size)， 本次gc时间
6. [Times: user=0.43 sys=0.01, real=0.07 secs]
user – 此次垃圾回收, 垃圾收集线程消耗的所有CPU时间(Total CPU time).
sys – 操作系统调用(OS call) 以及等待系统事件的时间(waiting for system event)
real – 应用程序暂停的时间(Clock time). 由于串行垃圾收集器(Serial Garbage Collector)只会使用单个线程, 所以 real time 等于 user 以及 system time 的总和.

## 频繁fgc下fump内存
主要参考这天ATA文章https://www.atatech.org/articles/53533
机器上没有gdb，需要sudo yum install gdb
操作流程
找到java进程，gdb attach上去， 例如 gdb -p 22443

找到这个HeapDumpBeforeFullGC的地址（这个flag如果为true，会在FullGC之前做HeapDump，默认是false）

(gdb) p &HeapDumpBeforeFullGC
$2 = (<data variable, no debug info> *) 0x7f7d50fc660f <HeapDumpBeforeFullGC>
然后把他设置为true，这样下次FGC之前就会生成一份dump文件

(gdb) set *0x7f7d50fc660f = 1
(gdb) quit
最后，等一会，等下次FullGC触发，你就有HeapDump了！
(如果没有指定heapdump的名字，默认是 java_pidxxx.hprof)

(PS. jstat -gcutil pid 可以查看gc的概况)

(操作完成后记得gdb上去再设置回去，不然可能一直fullgc，导致把磁盘打满)

## 新生代频繁GC
2019-05-17T10:53:19.906+0800: 1065.563: [GC (Allocation Failure) 2019-05-17T10:53:19.906+0800: 1065.563: [ParNew: 710072K->40939K(720896K), 0.0641636 secs] 1004802K->344114K(4128768K), 0.0644972 secs] [Times: user=0.43 sys=0.01, real=0.07 secs]

查看jvm参数后，发下-Xmn768m 意味着只有768M的新生代大小，导致不够。

## 查看jvm 进程启动初始化参数
jinfo -flags 77134

## 下载机器上的文件
scp -r username@servername:remote_dir/ /tmp/local_dir

scp xiele.xl@11.162.254.82:/home/xiele.xl/java.hprof /Users/simon/logs
