## Lambda表达式语法

用法:
无参数：() -> {}
带参数：(a, b) -> {}

只有一行表达式可省略return语句和括号
有多行代码块，需要用括号包起来

## 函数描述符

函数式接口的抽象方法的签名称为函数描 述符 （函数类型）, Lambda的参数为接口函数参数列表。

```java

// 定义了一个名叫test的抽象方法，它接受泛型 T对象，并返回一个boolean
java.util.function.Predicate<T> {
     boolean test(T t);
}
// 定义了一个名叫accept的抽象方法，它接受泛型T 的对象，没有返回(void)
java.util.function.Consumer<T> {
    void accept(T t);
}
//接口定义了一个叫作apply的方法，它接受一个 泛型T的对象，并返回一个泛型R的对象。
java.util.function.Function<T, R> { 
     R apply(T t);
}

```



## 方法引用

方法引用的本质是重用Lambda表达式

引用静态方法
ContainingClass::staticMethodName
例子: String::valueOf，对应的Lambda：(s) -> String.valueOf(s)
比较容易理解，和静态方法调用相比，只是把.换为::

引用特定对象的实例方法
containingObject::instanceMethodName
例子: x::toString，对应的Lambda：() -> this.toString()
与引用静态方法相比，都换为实例的而已

引用构造函数
ClassName::new
例子: String::new，对应的Lambda：() -> new String()
构造函数本质上是静态方法，只是方法名字比较特殊。

### 示例

```java

		//构造方法引用
        // 引用无参构造函数，返回一个apple对象, Lambda：() -> new Apple();
        Supplier<Apple> s1 = Apple::new;
        Apple apple = s1.get();

        // 引用带参数构造函数，返回Apple对象, 如下为等价
        Function<Integer,Apple> fa = Apple::new;
        Function<Integer,Apple> fb = (w) -> new Apple(w);
        apple = fa.apply(1);

        // 如下为等价
        BiFunction<Integer,String,Apple> fc = Apple::new;
        BiFunction<Integer, String, Apple> fd = (w, c) -> new Apple(w, c);
        Apple green = fc.apply(1, "green");
        Map<String, Function<Integer, Fruit>> map = new HashMap<>();
        map.put("apple", Apple::new);

        // 实例方法引用
        // 创建weight为1的Apple
        Fruit fruit = map.get("apple").apply(1);

        List<Apple> apples = new ArrayList<>();

        // 本质上，根据方法引用推断出实际的Lambda方法描述符
        apples.sort(Comparator.comparing(Apple::getWeight));
        List<String> str = Arrays.asList("a", "b", "c");
        str.sort((s, s2) -> s.compareToIgnoreCase(s2));
        // 需要满足方法描述符的入参与返回值
        str.sort(String::compareToIgnoreCase);
```



