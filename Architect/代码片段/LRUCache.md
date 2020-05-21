## LRUCache
> LRU (Least Recently Used) 的意思就是近期最少使用算法，它的核心思想就是会优先淘汰那些近期最少使用的缓存对象。一般采用：LinkedList+HashMap实现，在Java中用LinkedHashMap实现。

**LinkedList+HashMap实现方案**
1) get/put时把value放到双向链表的tail
2) map的size大于设定的容量后,移除链表的head.
3）内存大概的结构为:
head -> node -> node -> tail.

```Java
/**
 * @author xiele.xl
 * @date 2020-04-17 19:18
 */
public class LRUCache {

    private int capacity;

    private HashMap<Integer, Integer> map;
    private LinkedList<Integer> list;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        map = new HashMap<>();
        list = new LinkedList<>();
    }

    public int get(int key) {
        if (map.containsKey(key)) {
            list.remove((Integer)key);
            list.addLast(key);
            return map.get(key);
        }
        return -1;
    }

    public void put(int key, int value) {
        if (map.containsKey(key)) {
            list.remove((Integer)key);
            list.addLast(key);
            map.put(key, value);
            return;
        }
        if (list.size() == capacity) {
            map.remove(list.removeFirst());
            map.put(key, value);
            list.addLast(key);
        } else {
            map.put(key, value);
            list.addLast(key);
        }
    }

}
```

**直接继承LinkedHashMap**
```Java
public class LRUCache<K,V> extends LinkedHashMap<K,V>{

    private final int cacheSize;

    public LRUCache(int cacheSize) {
        super((int)Math.ceil((cacheSize / 0.75) + 1), 0.75F, true);

        this.cacheSize = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Entry<K, V> eldest) {
        return size() > cacheSize;
    }
}
```
