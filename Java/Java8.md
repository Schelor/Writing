# Java8使用总结

## Lambda表达式
用法:
无参数：() -> {}
带参数：(a, b) -> {}

只有一行表达式可省略return语句和括号
有多行代码块，需要用括号包起来


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
