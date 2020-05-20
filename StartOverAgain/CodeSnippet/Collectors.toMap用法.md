控制不抛Duplicate Key 异常的用法:
需要调用：

```Java
public static <T, K, U>
Collector<T, ?, Map<K,U>> toMap(Function<? super T, ? extends K> keyMapper,
                                Function<? super T, ? extends U> valueMapper,
                                BinaryOperator<U> mergeFunction) {
    return toMap(keyMapper, valueMapper, mergeFunction, HashMap::new);
}

   @Test
   public void toMap() {

       List<Pair<String, Long>> list = new ArrayList<>();

       list.add(new Pair<>("v", 1L));
       list.add(new Pair<>("v", 2L));
       list.add(new Pair<>("v", 3L));

       list.stream()
           .collect(Collectors.toMap(k -> k.getKey(),
               v -> v.getValue()));

       Map<String, Long> map = list.stream()
           .collect(Collectors.toMap(k -> k.getKey(),
               v -> v.getValue(), (v1, v2) -> {
                   System.out.println(v1);
                   System.out.println(v2);
                   return v2;
               }));

       System.out.println(map);
   }
```
