# 搞懂一致性Hash算法
## 前言
哈希算法是一种应用类似f(x) = y的函数，将一个原始值映射为一个新值，常用于键值对的数据结构。一致性哈希算法是一种特殊的哈希算法，多用于分布式环境中的缓存服务器选址，或远程调用服务器选址等场景，一致性的含义为：对请求数据与服务器节点都运用相同的Hash算法，此为一致性之意。

## 一致性Hash
对节点和数据，都做一次hash运算，然后比较节点和数据的hash值，数据值和节点最相近的节点作为处理节点。为了分布得更均匀，通过使用虚拟节点的方式，每个节点计算出n个hash值，均匀地放在hash环上这样数据就能比较均匀地分布到每个节点。

### 原理
#### 抽象的环形空间
按照常用的hash算法来将对应的key哈希到一个具有2^32次方个节点的空间中，即0 ~ (2^32)-1的数字空间中。现在我们可以将这些数字头尾相连，想象成一个闭合的环形。

![](./hash_ring.jpg)

#### 映射服务器节点
将各个服务器使用Hash进行一个哈希，具体可以选择服务器的ip或唯一主机名作为关键字进行哈希，这样每台机器就能确定其在哈希环上的位置。假设我们将四台服务器使用ip地址哈希后在环空间的位置如下：

![](./map_server_to_ring.jpg)

#### 映射数据
现在我们将objectA、objectB、objectC、objectD四个对象通过特定的Hash函数计算出对应的key值，然后散列到Hash环上,然后从数据所在位置沿环顺时针“行走”，第一台遇到的服务器就是其应该定位到的服务器。

![](./map_data_to_ring.jpg)

#### 服务器的删除与添加
如果此时NodeC宕机了，此时Object A、B、D不会受到影响，只有Object C会重新分配到Node D上面去，而其他数据对象不会发生变化

如果在环境中新增一台服务器Node X，通过hash算法将Node X映射到环中，通过按顺时针迁移的规则，那么Object C被迁移到了Node X中，其它对象还保持这原有的存储位置。通过对节点的添加和删除的分析，一致性哈希算法在保持了单调性的同时，还是数据的迁移达到了最小，这样的算法对分布式集群来说是非常合适的，避免了大量数据迁移，减小了服务器的的压力。

![](./remap_data.jpg)

#### 虚拟节点
到目前为止一致性hash也可以算做完成了，但是有一个问题还需要解决，那就是平衡性。从下图我们可以看出，当服务器节点比较少的时候，会出现一个问题，就是此时必然造成大量数据集中到一个节点上面，极少数数据集中到另外的节点上面。
为了解决这种数据倾斜问题，一致性哈希算法引入了虚拟节点机制，即对每一个服务节点计算多个哈希，每个计算结果位置都放置一个此服务节点，称为虚拟节点。具体做法可以先确定每个物理节点关联的虚拟节点数量，然后在ip或者主机名后面增加编号。例如上面的情况，可以为每台服务器计算三个虚拟节点，于是可以分别计算 “Node A#1”、“Node A#2”、“Node A#3”、“Node B#1”、“Node B#2”、“Node B#3”的哈希值，于是形成六个虚拟节点：

![](./vitual_node.jpg)

一个物理节点应该拆分为多少虚拟节点，下面可以先看一张图：

![](./virtual_node_count.png)

横轴表示需要为每台福利服务器扩展的虚拟节点倍数，纵轴表示的是实际物理服务器数。可以看出，物理服务器很少，需要更大的虚拟节点；反之物理服务器比较多，虚拟节点就可以少一些。比如有10台物理服务器，那么差不多需要为每台服务器增加100~200个虚拟节点才可以达到真正的负载均衡。

## Java实现
### 数据结构选择
一致性Hash算法的关键点为在统一的有序环形空间里查找hash值比目标Key大的第一个hash值。本质上是做一个排序与查找，对应的实现可以用`ArrayList+排序`,或`LinkedList+遍历+排序`。数据量较大时，这两种算法的时间复杂度都较高。另一种较好的方式是排序二叉树，如AVL与红黑树。在JDK里提供了红黑树TreeMap的实现，可以开箱即用，`TreeMap`中和了排序与查找的性质，在时间与空间复杂度效率上都较好，在API功能上提供了`tailMap`的方法，返回大于Key的所有K-V子Map.很实用在一致性Hash算法中的查找。

### Hash算法选择
在分布式集群部署场景下，服务器的IP或`host name`都有一定的相似性，采用Java里默认String重写的hashCode，分散性差，大多的server的hashcode相差不大，因此无法做到很好的均衡效果。示例：

@Test
public void testUserStringHashCode() {
    System.out.println("192.168.0.0的哈希值：" + "192.168.0.0".hashCode());
    System.out.println("192.168.0.1的哈希值：" + "192.168.0.1".hashCode());
    System.out.println("192.168.0.2的哈希值：" + "192.168.0.2".hashCode());
    System.out.println("192.168.0.3的哈希值：" + "192.168.0.3".hashCode());
    System.out.println("192.168.0.4的哈希值：" + "192.168.0.4".hashCode());
}

```Java
192.168.0.0的哈希值：55965011
192.168.0.1的哈希值：55965012
192.168.0.2的哈希值：55965013
192.168.0.3的哈希值：55965014
192.168.0.4的哈希值：55965015
```
环形空间的范围为[0, 2^32-1],最大值为4294967295,当前server的分布只占据了一小范围,可见分布在环形空间太密集，不分散。很容易造成大量请求都落到部分的节点上。
综上，String重写的hashCode()方法在一致性Hash算法中没有任何实用价值，得找个算法重新计算HashCode。这种重新计算Hash值的算法有很多，比如CRC32_HASH、FNV1_32_HASH、KETAMA_HASH等，其中KETAMA_HASH是默认的MemCache推荐的一致性Hash算法，用别的Hash算法也可以，比如FNV1_32_HASH算法的计算效率就会高一些。

### 一致性Hash算法实现版本：不带虚拟节点与带虚拟节点
```Java
public class ConsistentHash {

    private final TreeMap<Integer, String> ring = new TreeMap<>();

    /**
     * 初始化hash环
     * 设定服务器server的格式为ip:port
     *
     * @param servers
     */
    public void initRing(List<String> servers) {

        servers.stream().forEach(item -> {
                final int hash = getHash(item);
                System.out.println(String.format("server node=%s, hash=%s", item, hash));
                ring.put(hash, item);
            }
        );

    }

    /**
     * 初始化带虚拟节点的Server
     * @param servers
     */
    public void initRingWithVirtualNode(List<String> servers) {
        servers.stream().forEach(item -> {
                for (int i = 0; i < 10; i++) {
                    String newItem = item + "#" + i;
                    final int hash = getHash(newItem);
                    System.out.println(String.format("server node=%s, vnode=%s,hash=%s", item, newItem,  hash));
                    ring.put(hash, newItem);
                }

            }
        );
    }

    /**
     * 根据请求的key查找对应的server node
     *
     * @param key
     * @return
     */
    public String findServer(String key) {

        final int hashForKey = getHash(key);
        System.out.println("request key hash=" + hashForKey);
        SortedMap<Integer, String> subMap = ring.tailMap(hashForKey);
        if (subMap == null || subMap.isEmpty()) {
            return ring.get(ring.firstKey());
        }
        Integer targetKey = subMap.firstKey();
        // 如果返回的tailMap的第一个key为null, 则取ring的第一个key
        if (targetKey == null) {
            targetKey = ring.firstKey();
        }
        return ring.get(targetKey);

    }


    /**
     * 根据请求的key查找对应的server node
     *
     * @param key
     * @return
     */
    public String findServerWithVirtualNode(String key) {

        final int hashForKey = getHash(key);
        System.out.println("request key hash=" + hashForKey);
        SortedMap<Integer, String> subMap = ring.tailMap(hashForKey);
        Integer targetKey;
        if (subMap == null || subMap.isEmpty()) {
            targetKey = ring.firstKey();
        } else {
            targetKey = subMap.firstKey();
            // 如果返回的tailMap的第一个key为null, 则取ring的第一个key
            if (targetKey == null) {
                targetKey = ring.firstKey();
            }
        }

        String vnode = ring.get(targetKey);
        return vnode.substring(0, vnode.indexOf("#"));

    }


    /**
     * 使用FNV1_32_HASH算法计算服务器的Hash值,这里不使用重写hashCode的方法
     *
     * @param str
     * @return
     */
    private static int getHash(String str) {

        final int p = 16777619;
        int hash = (int)2166136261L;
        for (int i = 0; i < str.length(); i++) { hash = (hash ^ str.charAt(i)) * p; }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        // 如果算出来的值为负数则取其绝对值
        if (hash < 0) { hash = Math.abs(hash); }
        return hash;
    }

    @Test
    public void testFindServer() {

    ConsistentHash ch = new ConsistentHash();
    ch.initRingWithVirtualNode(new ArrayList<>(Arrays.asList(
        "30.23.224.81:12200",
        "30.23.224.82:12200",
        "30.23.224.83:12200",
        "30.23.224.84:12200",
        "30.23.224.85:12200"
    )));

    String key = "hello,world";
    String server = ch.findServerWithVirtualNode(key);

    System.out.println(String.format("find server, key=%s,server=%s", key, server));
}
}

运行结果:
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#0,hash=267666629
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#1,hash=808533591
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#2,hash=1687365852
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#3,hash=798514940
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#4,hash=978985582
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#5,hash=2132577108
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#6,hash=646115721
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#7,hash=115609433
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#8,hash=1693947362
server node=30.23.224.81:12200, vnode=30.23.224.81:12200#9,hash=820130953
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#0,hash=2086351301
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#1,hash=1416515605
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#2,hash=459114849
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#3,hash=193293835
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#4,hash=2049843152
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#5,hash=570195581
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#6,hash=1683324189
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#7,hash=186930102
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#8,hash=869762788
server node=30.23.224.82:12200, vnode=30.23.224.82:12200#9,hash=1996236454
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#0,hash=1373522883
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#1,hash=1620442417
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#2,hash=361815801
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#3,hash=374550472
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#4,hash=1633432850
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#5,hash=451347877
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#6,hash=739278830
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#7,hash=842256790
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#8,hash=1320066887
server node=30.23.224.83:12200, vnode=30.23.224.83:12200#9,hash=1021230837
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#0,hash=20200109
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#1,hash=489102096
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#2,hash=503441929
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#3,hash=585373937
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#4,hash=195537698
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#5,hash=335124402
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#6,hash=481750680
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#7,hash=147248156
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#8,hash=1459362027
server node=30.23.224.84:12200, vnode=30.23.224.84:12200#9,hash=611893684
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#0,hash=31167799
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#1,hash=1632282222
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#2,hash=1108221530
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#3,hash=903545256
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#4,hash=1696334375
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#5,hash=136165989
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#6,hash=1703968952
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#7,hash=1429317432
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#8,hash=732640802
server node=30.23.224.85:12200, vnode=30.23.224.85:12200#9,hash=1688156986
request key hash=1659918577
find server, key=hello,world,server=30.23.224.82:12200

```
## Dubbo中的一致性Hash算法分析
