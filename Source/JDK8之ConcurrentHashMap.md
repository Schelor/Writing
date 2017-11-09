# JDK8之ConcurrentHashMap

## 设计概述
`ConcurrentHashMap`支持全面的并发读和高性能的并发写。与`Hashtable`类似，每个方法都遵从相同的功能规范。纵然如此，ConcurrentHashMap的所有操作是线程安全的，读操作不需要锁，而且对任何其他写操作都不需要锁全表。
`get`操作不会阻塞线程，因此`put`和`remove`操作可与之同时进行,读和写都遵循JVM中的先行发生原则,读取的通常是写完后的结果.对于聚集操作，如`putAll, clear`,当前的读到的可能是新增或移除后的结果。类似的，用于遍历或分割Map的迭代器，其结果反映的是Hash表在某个时刻的状态值，而不会抛`ConcurrentModificationException`异常。然而，迭代器在某个时刻只能被用于一个线程。
需要知晓的是，一些聚集状态的的方法如`size, isEmpty, containsValue`典型的只能在没有其他线程并发写的情况下才有用，不然只能是瞬时值。只能用于适当的监控或评估场景，不能用于程序逻辑控制。

内部的表在发生大量的Hash冲突时会自动扩容(Key的HashCode不同但是落到了Table的同一个槽中)。预期的平均情况下，每个槽维持两个Entry节点，而相应的扩容因子为0.75。当然围绕这个平均值，在大量的新增和删除的情况下，实际容量会存在很大的变化。但是整体上，在时间和空间上可以达到一个平衡。
然而，扩容或缩容(resize)往往是相对很耗时的操作，所以在构造Hash表时，应该估算一个合理的初始容量。作为可选的方案，还可以提供一个加载因子作为构造参数，用以进一步自定义表的容量，从而可以计算用于分配的元素的空间数。需要注意的是，如果很多Key的Hash值恰好相同，对于任何Hash表其性能都会变慢，为了改善这种影响，如果Key实现了Comparable接口，那么则可以根据这些Key到对比结果，然后拆分实际的Entry节点。

ConcurrentHashMap限制其Key和Value不允许为`null`，同时支持一组串行或并行的批量操作。这些操作方法在设计上是安全的，很容易的用于多线程更新。例如，当计算一个共享注册表中的快照时，ConcurrentHashMap提供了3种类型的操作，每种类型有4种不同的形式，都可用于接收一些Function参数，并返回相应的结果。

ConcurrentHashMap的首要设计目标是降低写竞争同时维持并发的易读性，次要目标是保证空间消耗要比HashMap要更优，还要支持多线程写空表的起始写入率。
ConcurrentHashMap通常是作为桶式的Hash表，每个key-value都存在一个Node节点里，大多数的Node实例都是带有key,value,hash值，next引用的基本类型的Node.然而Node有多种子类：TreeNode用于平衡树中，而不再使用链表。TreeBin用于存储树的根节点和子节点集合，ForwardingNode用于引用resize时桶中的头结点。ReservationNode在`computeIfAbsent`及其他相关方法上用作占位节点。这些子类都不持有key，value,和hash值，在查找是很容易区分，因为其hash字段是负数，key和value是null。
Hash表在首次插入时才初始化，其大小需要为2的N次方，每个桶都可能包含一个节点列表(大多情况下列表只有0个或一个元素)。访问Table时需要volatile或原子性的读写，或CAS操作。

我们用节点的hash字段的最高位来作控制位，由于寻址限制，这个方案在任何时候都是可行的。带负数的hash节点会作为特殊处理，或被忽略。
第一次插入节点到桶中直接采用CAS写，对于大多数Key的分布，这是目前最常见的方案。其他写操作(insert,delete,replace)则需要锁。我们不想为每个桶都关联一个锁对象，从而浪费空间，因此我们直接用桶中的第一个节点本身作为锁。而在此版本中，我们用内置的synchronized监视器锁来做锁支持。

仅仅用第一个节点作锁还不够。当第一个节点被锁住后，其他的更新操作首先要校验它仍然是第一个节点，如果不是的话，还需循环的重试。因为新节点会持续的追加到链表中，一旦某节点成为桶中的第一个节点，它则会保持直到被删除，或当期桶在扩容时变得不可用。

每个桶持有一个锁的缺点是，由于每个桶上的锁，其他的更新操作可能会被阻塞，然而统计表明，在随机hashcode下，这不是一种普遍的问题。理想的情况下，桶中的节点频度会符合泊松分布(http://en.wikipedia.org/wiki/Poisson_distribution)。
在随机Hash的场景下，对于两个线程写入不同的元素，其产生锁竞争的的概率粗略是`1 / (8 * 元素数量)`。

在实践中，实际的hash值分布会背离均匀的随机性。 在N>2的30次方时，一些Key是必然会出现冲突的。类似的，在一些恶意用法中(多个Key共享同一hashcode),冲突也是必然的。因此当桶中的元素数量超过一定的阈值时，我们采用二级策略来避免hash碰撞。采用基于平衡树的TreeBin(红黑树的特殊形式)来存储这些节点,从而限定其搜索时间复杂度为O(log N).在TreeBin中的每次搜索步骤，其速度会比在常规列表中慢两倍，但由于给定的容量不可能超过2的64次方(64位寻址空间)。因此这限制了搜索次数，持有锁的时间可到达一个合理的常量(最坏的情况每次要查找100个节点),只要Key是可比较的(如常见的String,Long等等).TreeBin的节点(TreeNode)仍然维持着next引用，作为遍历指针，因此可用于相同的迭代器中。

当Table的占用率达到一定的阈值时(名义上0.75,见下文)，此时需要调整Table的大小。当初始线程分配并迁移数组时，任何其他的线程观察到有过多的桶时，会辅助扩容。相对于阻塞等待，这些线程会帮助插入数据。TreeBin可以防止在扩容时出现过的的分配空间。扩容时由转移的桶一个个的从原表迁移到另一个表中，然而线程可以申请一小块的索引区间，用以减少竞争。在`sizeCtrl`字段上生成的标记保证了不会同时发生扩容，因为我们采用2的N次方扩容，桶的索引要么在原有的位置，要么被迁移到另一个偏移索引中了。我们还预估了不必要的创建节点，在一些场景中，重用那些原来的节点，因为他们的next字段不会发生变化。因此在扩容过程中，平均只需要创建六分之一的新节点。被替换后的节点会被GC清理。在转移时，老的Table仅仅包含一个特殊的转向节点(hash字段值为MOVED),该节点的key执行下一个新的Table。读取或写入数据时，当识别到转向节点后，则会转向新Table，由新Table来处理。

当扩容时, 每个桶转移时需要一个桶锁，这会阻塞其他线程。但线程可以加入到扩容行动中来，而不去竞争锁。在扩容期间平均等待时间则变得更短。
迁移操作必须保证访问老Table和新Table都是可用的。遍历Table时，当看见转向节点后，只需要访问新Table而不用重新访问原节点。在迁移原节点时，为了确保没有跳过中间的节点，使用TableStack来控制。

遍历模式应用了分区遍历，用以支持分区的聚合操作。
Table的延迟初始化最小化内存占用，也避免了putAll带来的扩容，带Map的构造参数或反序列化这些场景会试图覆盖初始的容量设置，但是这并这会造成什么影响。

元素的数量是由一个特殊的LongAdder来维护的，为了避免在竞争场景中创建多个CounterCell，我们合并了一个特殊的LongAdder而不是直接使用它。这个计数器机制避免了写竞争，但是当存在太频繁的读counter，则会存在缓存波动。为了避免太频繁的读，在竞争下的resize中会试图提前添加两个节点在桶中。

TreeBin使用了一个特殊的比较器来执行查找或其他相关方法(这也是不能用TreeMap的主要原因), TreeBin包含可比较的元素，但也有其他类型的。T类型的元素如果不能比较的话，其有序性将基于hash值来做比较。查找节点时，如果元素不能比较或比较结果为相同，则需查找左右两边的子节点，然后比较节点的hash值。在插入时，为了保证整体有序的平衡树，我们使用类的identityHashCodes的比较结果拆分节点。

TreeBin同样需要锁机制。对于列表而言，即使同时发生更新和遍历也是可行的。但树的遍历则不行，主要因为树的旋转会改变根节点以及其关联节点。TreeBin包含了一个简单的读写锁机制并附加在主要的桶锁策略上。插入或删除这类的结构调整是在桶锁下进行的，不会与其他写入线程冲突，但是必须要在读取结束后才能调整树。
由于可能只会有一个等待线程，因此我们用一个waiter字段来表示等待的线程。然而读线程则不会被阻塞。如果根节点锁被持有，读线程则用最慢的路径来遍历，直到锁被释放。

## 源码解读
#### 重要常量定义
```java
最大表容量(1073741824 百亿)
private static final int MAXIMUM_CAPACITY = 1 << 30;

默认表容量16
private static final int DEFAULT_CAPACITY = 16;

默认并发级别，新版本中已经未使用，主要是保持低版本序列化时的兼容性
private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

默认的容量载入因子,用更简洁的来代替：n - (n >>> 2)
private static final float LOAD_FACTOR = 0.75f;

桶中的节点在冲突时，开始是链表的形式。当超过此阈值时，需要转换为红黑树
static final int TREEIFY_THRESHOLD = 8;

桶中的红黑树树节点树小于此阈值时，需收缩成链表
static final int UNTREEIFY_THRESHOLD = 6;

转换表为红黑树的最小容量。如果表的整体容量小于此值，则先不用转换。
static final int MIN_TREEIFY_CAPACITY = 64;

当表容量很大时，扩容需要拆分成多少段，默认16段
private static final int MIN_TRANSFER_STRIDE = 16;

调整容量的标记位
private static int RESIZE_STAMP_BITS = 16;

并发的场景下，当Table再调整大小时，可由一些并发线程参与
private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

siteCtrl上控制位，用以位移标记
private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

特殊的hash标记值
static final int MOVED     = -1; // 转移节点的hash值
static final int TREEBIN   = -2; // 红黑树的根节点hash值
static final int RESERVED  = -3; // 遍历Table时的节点保留hash值

正常节点的可用来hash的位
static final int HASH_BITS = 0x7fffffff;

当前机器的Cpus核数
static final int NCPU = Runtime.getRuntime().availableProcessors();
```
### 节点数据结构定义
#### 通用节点定义,特殊场景由子类来完成
```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K,V> next;

    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }
}
```
#### 红黑树
TreeBin主要表示抽象的红黑树结构。在桶中的第一个位置，本身不存储Key,Value。用以指向具体的根节点以及树节点。
同时,TreeBin还关联一个读写锁来控制读写顺序，保证读线程读取结束后才能写入新节点,因为新节点写入可能会调整树的结构(重新平衡树)
```java
static final class TreeBin<K,V> extends Node<K,V> {
    TreeNode<K,V> root; 根节点
    volatile TreeNode<K,V> first; 记录第一个节点
    volatile Thread waiter; 写线程
    volatile int lockState; 用CAS锁
    // values for lockState
    static final int WRITER = 1; 已经持有写锁
    static final int WAITER = 2; 等待写锁
    static final int READER = 4; 持有读锁
}
```
#### 红黑树节点
```java
static final class TreeNode<K,V> extends Node<K,V> {
    TreeNode<K,V> parent;  树链表
    TreeNode<K,V> left;
    TreeNode<K,V> right;
    TreeNode<K,V> prev;    删除节点时需要断开上一个关联的节点
    boolean red;

    TreeNode(int hash, K key, V val, Node<K,V> next,
             TreeNode<K,V> parent) {
        super(hash, key, val, next);
        this.parent = parent;
    }
}

```

#### 转移节点
调整Table大小时,用一个节点来封装新Table，进而直接处理节点即可
```java
static final class ForwardingNode<K,V> extends Node<K,V> {
    final Node<K,V>[] nextTable;
    ForwardingNode(Node<K,V>[] tab) {
        super(MOVED, null, null, null);
        this.nextTable = tab;
    }
}

```

### 构造与初始化
ConcurrentHashMap引入延迟初始化，只有再第一次Insert数据时才创建内部的Table。
初始化时采用无锁化的CAS机制。
```java
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    默认情况下，Table为null
    while ((tab = table) == null || tab.length == 0) {
        当前的容量大小被设置为-1时，表示已有线程正在初始化，其余线程暂时让出CPU,让调度器重新调度。
        if ((sc = sizeCtl) < 0)
            Thread.yield();
        CAS写入-1成功，表示获取进入创建Table的权限    
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                二次判空，防止Table在putAl方法中被提创建过
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    此处sc为最大容量*0.75，表示实际容量
                    sc = n - (n >>> 2);
                }
            } finally {
                最后写入实例字段中
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}

```
### Hash值计算
采用高位异或后再与最高位为0的HASH_BIT取与。JDK8简化了Hash值的计算，在计算速度，实用性等作了权衡。
```java
static final int spread(int h) {
    return (h ^ (h >>> 16)) & HASH_BITS;
}

```

### 基于内存模型和CAS机制访问底层Table的几组方法
```java
    根据索引下标读取Table中的桶，实际的数据基于数组的内存位置加元素的偏移位置
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {

        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }
    CAS写入某个索引对应的数据
    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }
    在锁的保护下，直接写入
    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }

```

### put方法
主要逻辑
1. 计算hash值
2. 初始化Table
3. 插入节点到链表
4. 链表转化为红黑树结构
5. 如果桶中已为TreeBin，插入节点到红黑树中
6. 计算当前元素数量

代码梳理如下：
```java
public V put(K key, V value) {

        return putVal(key, value, false);
}

onlyIfAbsent表示如果不存在则插入,常规情况为false, 有则替换，无则新增
final V putVal(K key, V value, boolean onlyIfAbsent) {
        key value 不允许为空
        if (key == null || value == null) throw new NullPointerException();
        获取hashcode
        int hash = spread(key.hashCode());

        记录当前桶中节点的数量
        int binCount = 0;

        CAS机制，需要不断的循环
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;

            初始化Table
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            写入桶中第一个节点
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            当前Table正在Resize
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);

            开始实际的写入节点
            else {
                V oldVal = null;
                1. 新的JVM内置锁性能已有很大的改善，所以直接使用内置锁
                2. 为了创建不必要的监视器锁，直接复用桶中第一个节点
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        此处表示新增普通节点，每个桶中的节点小于8个时，直接用链表来关联
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        此处表示,桶中所有节点已经以红黑树的形式存储,新节点直接插入到树中
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            红黑树原理，内部涉及到插入节点后的左旋，右旋，见TreeMap源码，其可读性更好，此处代码可读性不佳
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    当桶中的链表长度已经达到8时，就开始考虑把链表转化为红黑树
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        基于LongAddr机制来计数,LongAddr原理后文待续
        addCount(1L, binCount);
        return null;
    }

```

#### 并发操作红黑树
在putTreeVal中，涉及到多线程写新节点，进而需要重新平衡树，此处涉及到并发的控制，在JDK8中用了一个CAS锁来实现。
```java
....
lockRoot();
  try {
      root = balanceInsertion(root, x);
  } finally {
      unlockRoot();
  }
....

private final void lockRoot() {
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
                contendedLock(); // offload to separate method
}

如果有多个并发线程同时写，其余的线程需要竞争锁, 记录一个等待线程,然后让该线程进入等待状态
private final void contendedLock() {
    boolean waiting = false;
    for (int s;;) {
        其他线程已经写完
        if (((s = lockState) & ~WAITER) == 0) {
            if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                if (waiting)
                    waiter = null;
                return;
            }
        }
        其他线程还没有写完，需要等待
        else if ((s & WAITER) == 0) {
            if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                waiting = true;
                waiter = Thread.currentThread();
            }
        }
        多次观察后，没办法，只能park
        else if (waiting)
            LockSupport.park(this);
    }
}

由于之前已经获取到锁，释放锁则不用CAS，改用volatile写，保证线程可见性即可
private final void unlockRoot() {
    lockState = 0;
}

```
### get方法
get方法相对简易写。根据Key找到对应的桶，然后根据其hash值来判定是否为特殊节点。
普通节点其hash值大于等于0,于是直接查链表。如果是特殊节点，则调用节点的find方法，执行树查找。
```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}

```

### containsKey方法

```java
public boolean containsKey(Object key) {
    return get(key) != null;
}

需要避免一些用法，避免多次查找。如
if (containsKey(key)) {
  V v = get(key);
}
```
