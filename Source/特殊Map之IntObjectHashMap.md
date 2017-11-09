# IntObjectHashMap

## 前言
参考SpringSide-Util中提到的特殊Map，在特殊场景中性能较HashMap更优。IntObjectHashMap移植自Netty的工具类中，旨在优化性能。
IntObjectHashMap采用Int类型的开放地址而非使用链表来设计Key, 减少内存占用. 在数组中使用线性探测来解决Hash冲突。删除节点时需要压缩数组，
因此时间消耗解决O(N), 因此推荐使用偏小的负载因子(默认0.5)

## 源码解读

### 接口设计
```java
public interface IntObjectMap<V> extends Map<Integer, V> {

    /**
     * 基础Int类型的Entry
     */
    interface PrimitiveEntry<V> {

        int key();

        V value();

        void setValue(V value);
    }
  }

V get(int key);

V put(int key, V value);

V remove(int key);

Iterable<PrimitiveEntry<V>> entries();

boolean containsKey(int key);

```

IntObjectHashMap源码解释

```java

public class IntObjectHashMap<V> implements IntObjectMap<V> {

  默认初始容量为8
	public static final int DEFAULT_CAPACITY = 8;

  默认加载因子
	public static final float DEFAULT_LOAD_FACTOR = 0.5f;

  null值的占位符,避免在数组中存储null.
	private static final Object NULL_VALUE = new Object();

  最大的容量
	private int maxSize;

  加载因子
	private final float loadFactor;

  Key数组
	private int[] keys;

  Value数组
	private V[] values;

  实际元素数量，Map的大小
	private int size;

  掩码,大小为数组的length-1, 用以与hash值作与运算, 比直接取模性能更加
	private int mask;

  Key视图
	private final Set<Integer> keySet = new KeySet();

  Entry视图
	private final Set<Entry<Integer, V>> entrySet = new EntrySet();

  当前Map迭代器
	private final Iterable<PrimitiveEntry<V>> entries = new Iterable<PrimitiveEntry<V>>() {
		@Override
		public Iterator<PrimitiveEntry<V>> iterator() {
			return new PrimitiveIterator();
		}
	};

  默认构造方法
	public IntObjectHashMap() {
		this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public IntObjectHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public IntObjectHashMap(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0.0f || loadFactor > 1.0f) {
			// Cannot exceed 1 because we can never store more than capacity elements;
			// using a bigger loadFactor would trigger rehashing before the desired load is reached.
			throw new IllegalArgumentException("loadFactor must be > 0 and <= 1");
		}

		this.loadFactor = loadFactor;

		调整容量为2的N次方
		int capacity = MathUtil.nextPowerOfTwo(initialCapacity);
		mask = capacity - 1;

		分配Key数组,Value数组
		keys = new int[capacity];
		@SuppressWarnings({ "unchecked" })
		V[] temp = (V[]) new Object[capacity];
		values = temp;

		实际的容量为数组最大的容量 * 负载因子
		maxSize = calcMaxSize(capacity);
	}

  把取出的数据对外作空转换
	private static <T> T toExternal(T value) {
		return value == NULL_VALUE ? null : value;
	}

  如果写入null,对内转换为NULL_VALUE存储
	@SuppressWarnings("unchecked")
	private static <T> T toInternal(T value) {
		return value == null ? (T) NULL_VALUE : value;
	}

	@Override
	public V get(int key) {
    根据当前key找到对应的索引,如果有冲突，则继续找下一个, 如果冲突的key对应的索引中的实际Key相同,则查找成功
		int index = indexOf(key);
		return index == -1 ? null : toExternal(values[index]);
	}

	@Override
	public V put(int key, V value) {
		int startIndex = hashIndex(key);
		int index = startIndex;

		for (;;) {
      未冲突，写入当前kv
			if (values[index] == null) {
				// Found empty slot, use it.
				keys[index] = key;
				values[index] = toInternal(value);
				growSize();
				return null;
			}

      当前key已存在,用新value覆盖原值
			if (keys[index] == key) {
				// Found existing entry with this key, just replace the value.
				V previousValue = values[index];
				values[index] = toInternal(value);
				return toExternal(previousValue);
			}

			继续找下个非空的value索引
			if ((index = probeNext(index)) == startIndex) {
				// Can only happen if the map was full at MAX_ARRAY_SIZE and couldn't grow.
				throw new IllegalStateException("Unable to insert");
			}
		}
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends V> sourceMap) {
		if (sourceMap instanceof IntObjectHashMap) {
			优化版本
			@SuppressWarnings("unchecked")
			IntObjectHashMap<V> source = (IntObjectHashMap<V>) sourceMap;
			for (int i = 0; i < source.values.length; ++i) {
				V sourceValue = source.values[i];
				if (sourceValue != null) {
           逐个put
					put(source.keys[i], sourceValue);
				}
			}
			return;
		}

		逐个添加Entry
		for (Entry<? extends Integer, ? extends V> entry : sourceMap.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}


  1. 如果key对应value不存在,返回null
  2. 找到对应的索引
	@Override
	public V remove(int key) {
		int index = indexOf(key);
		if (index == -1) {
			return null;
		}
    找到目标值
		V prev = values[index];
		removeAt(index);
		return toExternal(prev);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

  数组清0
	@Override
	public void clear() {
		Arrays.fill(keys, 0);
		Arrays.fill(values, null);
		size = 0;
	}

	@Override
	public boolean containsKey(int key) {
		return indexOf(key) >= 0;
	}

	@Override
	public boolean containsValue(Object value) {
		@SuppressWarnings("unchecked")
		V v1 = toInternal((V) value);
		for (V v2 : values) {
			// The map supports null values; this will be matched as NULL_VALUE.equals(NULL_VALUE).
			if (v2 != null && v2.equals(v1)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterable<PrimitiveEntry<V>> entries() {
		return entries;
	}

	@Override
	public Collection<V> values() {
		return new AbstractCollection<V>() {
			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					final PrimitiveIterator iter = new PrimitiveIterator();

					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public V next() {
						return iter.next().value();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public int size() {
				return size;
			}
		};
	}

	@Override
	public int hashCode() {
		// Hashcode is based on all non-zero, valid keys. We have to scan the whole keys
		// array, which may have different lengths for two maps of same size(), so the
		// capacity cannot be used as input for hashing but the size can.
		int hash = size;
		for (int key : keys) {
			// 0 can be a valid key or unused slot, but won't impact the hashcode in either case.
			// This way we can use a cheap loop without conditionals, or hard-to-unroll operations,
			// or the devastatingly bad memory locality of visiting value objects.
			// Also, it's important to use a hash function that does not depend on the ordering
			// of terms, only their values; since the map is an unordered collection and
			// entries can end up in different positions in different maps that have the same
			// elements, but with different history of puts/removes, due to conflicts.
			hash ^= hashCode(key);
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IntObjectMap)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		IntObjectMap other = (IntObjectMap) obj;
		if (size != other.size()) {
			return false;
		}
		for (int i = 0; i < values.length; ++i) {
			V value = values[i];
			if (value != null) {
				int key = keys[i];
				Object otherValue = other.get(key);
				if (value == NULL_VALUE) {
					if (otherValue != null) {
						return false;
					}
				} else if (!value.equals(otherValue)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean containsKey(Object key) {
		return containsKey(objectToKey(key));
	}

	@Override
	public V get(Object key) {
		return get(objectToKey(key));
	}

	@Override
	public V put(Integer key, V value) {
		return put(objectToKey(key), value);
	}

	@Override
	public V remove(Object key) {
		return remove(objectToKey(key));
	}

	@Override
	public Set<Integer> keySet() {
		return keySet;
	}

	@Override
	public Set<Entry<Integer, V>> entrySet() {
		return entrySet;
	}

	private int objectToKey(Object key) {
		return ((Integer) key).intValue();
	}


  根据Key定位具体的索引, 采用双Hash来探测,返回Key对应的下标索引，如果当前Keys中还没有Key,则返回-1
	private int indexOf(int key) {
    默认情况下的初始索引
		int startIndex = hashIndex(key);
		int index = startIndex;

		for (;;) {
			if (values[index] == null) {
				当前Key对应的索引，其关联的Value为空，说明此位置未产生冲突,返回-1.表示此索引可用
				return -1;
			}
      当前key已经存在，返回对应的索引
			if (key == keys[index]) {
				return index;
			}

			key冲突，根据当前Key的索引找到下一个非空的索引
			if ((index = probeNext(index)) == startIndex) {
				return -1;
			}
		}
	}

	根据Key计算Key在数组的下标
	private int hashIndex(int key) {
		// The array lengths are always a power of two, so we can use a bitmask to stay inside the array bounds.
		return hashCode(key) & mask;
	}

	int类型的Key其Hash值为原生值
	private static int hashCode(int key) {
		return key;
	}


  探测下一个索引位置,
	private int probeNext(int index) {
		// The array lengths are always a power of two, so we can use a bitmask to stay inside the array bounds.
		return (index + 1) & mask;
	}

	新增Key-Value后，递增Size.如果超过阈值,做扩容
	private void growSize() {
		size++;

		if (size > maxSize) {
			if (keys.length == Integer.MAX_VALUE) {
				throw new IllegalStateException("Max capacity reached at size=" + size);
			}

			基于当前数组大小的2倍
			rehash(keys.length << 1);
		}
	}


  删除索引对应的Key,Value, 如果存在冲突，则打破冲突位置
	private boolean removeAt(final int index) {
		--size;
    建议清除key。
		keys[index] = 0;
		values[index] = null;

		boolean movedBack = false;
		int nextFree = index;
		for (int i = probeNext(index); values[i] != null; i = probeNext(i)) {
			int bucket = hashIndex(keys[i]);
      当前的删除的index右边还存在冲突的Key, 该Key需要向左移动.目的是降低冲突的位置
			if (i < bucket && (bucket <= nextFree || nextFree <= i) || bucket <= nextFree && nextFree <= i) {
				// Move the displaced entry "back" to the first available position.
				keys[nextFree] = keys[i];
				values[nextFree] = values[i];
				movedBack = true;
				// Put the first entry after the displaced entry
				keys[i] = 0;
				values[i] = null;
				nextFree = i;
			}
		}
		return movedBack;
	}

	在rehash之前计算最大的数组实际容量
	private int calcMaxSize(int capacity) {
    调整一下大小,以避免loadFactor为1,保证还有可用的槽
		int upperBound = capacity - 1;
		return Math.min(upperBound, (int) (capacity * loadFactor));
	}

	rehash数组,转移数据,扩容或缩容
	private void rehash(int newCapacity) {
		int[] oldKeys = keys;
		V[] oldVals = values;

		keys = new int[newCapacity];
		@SuppressWarnings({ "unchecked" })
		V[] temp = (V[]) new Object[newCapacity];
		values = temp;

		maxSize = calcMaxSize(newCapacity);
		mask = newCapacity - 1;


		for (int i = 0; i < oldVals.length; ++i) {
			V oldVal = oldVals[i];
			if (oldVal != null) {
				// Inlined put(), but much simpler: we don't need to worry about
				// duplicated keys, growing/rehashing, or failing to insert.
        内联put方法的逻辑并作冲突检测
				int oldKey = oldKeys[i];
				int index = hashIndex(oldKey);

				for (;;) {
					if (values[index] == null) {
						keys[index] = oldKey;
						values[index] = oldVal;
						break;
					}
          在新数组中存在冲突, 寻址下一个位置
					// Conflict, keep probing. Can wrap around, but never reaches startIndex again.
					index = probeNext(index);
				}
			}
		}
	}

	@Override
	public String toString() {
		if (isEmpty()) {
			return "{}";
		}
		StringBuilder sb = new StringBuilder(4 * size);
		sb.append('{');
		boolean first = true;
		for (int i = 0; i < values.length; ++i) {
			V value = values[i];
			if (value != null) {
				if (!first) {
					sb.append(", ");
				}
				sb.append(keyToString(keys[i])).append('=').append(value == this ? "(this Map)" : toExternal(value));
				first = false;
			}
		}
		return sb.append('}').toString();
	}

	/**
	 * Helper method called by {@link #toString()} in order to convert a single map key into a string. This is protected
	 * to allow subclasses to override the appearance of a given key.
	 */
	protected String keyToString(int key) {
		return Integer.toString(key);
	}

	/**
	 * Set implementation for iterating over the entries of the map.
	 */
	private final class EntrySet extends AbstractSet<Entry<Integer, V>> {
		@Override
		public Iterator<Entry<Integer, V>> iterator() {
			return new MapIterator();
		}

		@Override
		public int size() {
			return IntObjectHashMap.this.size();
		}
	}

	/**
	 * Set implementation for iterating over the keys.
	 */
	private final class KeySet extends AbstractSet<Integer> {
		@Override
		public int size() {
			return IntObjectHashMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return IntObjectHashMap.this.containsKey(o);
		}

		@Override
		public boolean remove(Object o) {
			return IntObjectHashMap.this.remove(o) != null;
		}

		@Override
		public boolean retainAll(Collection<?> retainedKeys) {
			boolean changed = false;
			for (Iterator<PrimitiveEntry<V>> iter = entries().iterator(); iter.hasNext();) {
				PrimitiveEntry<V> entry = iter.next();
				if (!retainedKeys.contains(entry.key())) {
					changed = true;
					iter.remove();
				}
			}
			return changed;
		}

		@Override
		public void clear() {
			IntObjectHashMap.this.clear();
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				private final Iterator<Entry<Integer, V>> iter = entrySet.iterator();

				@Override
				public boolean hasNext() {
					return iter.hasNext();
				}

				@Override
				public Integer next() {
					return iter.next().getKey();
				}

				@Override
				public void remove() {
					iter.remove();
				}
			};
		}
	}

	/**
	 * Iterator over primitive entries. Entry key/values are overwritten by each call to {@link #next()}.
	 */
	private final class PrimitiveIterator implements Iterator<PrimitiveEntry<V>>, PrimitiveEntry<V> {
		private int prevIndex = -1;
		private int nextIndex = -1;
		private int entryIndex = -1;

		private void scanNext() {
			for (;;) {
				if (++nextIndex == values.length || values[nextIndex] != null) {
					break;
				}
			}
		}

		@Override
		public boolean hasNext() {
			if (nextIndex == -1) {
				scanNext();
			}
			return nextIndex < keys.length;
		}

		@Override
		public PrimitiveEntry<V> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			prevIndex = nextIndex;
			scanNext();

			// Always return the same Entry object, just change its index each time.
			entryIndex = prevIndex;
			return this;
		}

		@Override
		public void remove() {
			if (prevIndex < 0) {
				throw new IllegalStateException("next must be called before each remove.");
			}
			if (removeAt(prevIndex)) {
				// removeAt may move elements "back" in the array if they have been displaced because their spot in the
				// array was occupied when they were inserted. If this occurs then the nextIndex is now invalid and
				// should instead point to the prevIndex which now holds an element which was "moved back".
				nextIndex = prevIndex;
			}
			prevIndex = -1;
		}

		// Entry implementation. Since this implementation uses a single Entry, we coalesce that
		// into the Iterator object (potentially making loop optimization much easier).

		@Override
		public int key() {
			return keys[entryIndex];
		}

		@Override
		public V value() {
			return toExternal(values[entryIndex]);
		}

		@Override
		public void setValue(V value) {
			values[entryIndex] = toInternal(value);
		}
	}

	/**
	 * Iterator used by the {@link Map} interface.
	 */
	private final class MapIterator implements Iterator<Entry<Integer, V>> {
		private final PrimitiveIterator iter = new PrimitiveIterator();

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Entry<Integer, V> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			iter.next();

			return new MapEntry(iter.entryIndex);
		}

		@Override
		public void remove() {
			iter.remove();
		}
	}

	/**
	 * A single entry in the map.
	 */
	final class MapEntry implements Entry<Integer, V> {
		private final int entryIndex;

		MapEntry(int entryIndex) {
			this.entryIndex = entryIndex;
		}

		@Override
		public Integer getKey() {
			verifyExists();
			return keys[entryIndex];
		}

		@Override
		public V getValue() {
			verifyExists();
			return toExternal(values[entryIndex]);
		}

		@Override
		public V setValue(V value) {
			verifyExists();
			V prevValue = toExternal(values[entryIndex]);
			values[entryIndex] = toInternal(value);
			return prevValue;
		}

		private void verifyExists() {
			if (values[entryIndex] == null) {
				throw new IllegalStateException("The map entry has been removed");
			}
		}
	}
}

```
