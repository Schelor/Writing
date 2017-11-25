1. 伪共享问题
不同的变量值存在同一个缓存行中，并且存在多线程去更新

2. 有限通配符设计
>
 <? extends E> E的某种子类型的集合，E是自身的子类型
 <? super E> E的某种超类的集合，E是自身的超类
 PECS produce-extends, consumer-super
 子类型写，超类型读

3. CPU缓存行
http://ifeve.com/mechanical-sympathy/

Mac: sysctl machdep.cpu
Linux: cat /sys/devices/system/cpu/cpu0/cache/index0-3/size

CPU缓存行：64size
max: machdep.cpu.cache.linesize: 64
CPU存取缓存都是按照一行，为最小单位操作的

计算对象大小问题：

4. https://github.com/decaywood/decaywood.github.io/blob/master/_posts/2016/1/2016-01-22-disruptor-guide.markdown
逃逸分析 用户态 内核态
线程与CPU

5. Sequence 怎么控制线程安全


顺便看看：
http://ifeve.com/%E5%B8%B8%E7%94%A8%E5%BC%80%E6%BA%90%E6%A1%86%E6%9E%B6%E4%B8%AD%E8%AE%BE%E8%AE%A1%E6%A8%A1%E5%BC%8F%E4%BD%BF%E7%94%A8%E5%88%86%E6%9E%90/


6. Java代码到Java堆： https://www.ibm.com/developerworks/cn/java/j-codetoheap/
作为操作系统进程，Java 运行时面临着与其他进程完全相同的内存限制：架构提供的寻址能力以及操作系统提供的用户空间


7. http://ziyue1987.github.io/pages/2013/09/22/disruptor-use-manual.html#end


why disruptor padding the entries
