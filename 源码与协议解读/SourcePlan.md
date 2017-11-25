## 库收集
- Apache Commons Util (http://commons.apache.org/ commons-io commons-collections commons-lang)
- FastUtil（https://github.com/vigna/fastutil）
- SpringSide-Util（https://github.com/springside/springside4/wiki/SpringSide-Utils-Overview）
- Google Guava (https://github.com/google/guava)
- Disruptor (http://lmax-exchange.github.io/disruptor/)
- Quartz (http://www.quartz-scheduler.org/)
- Netty
- Lombok

## 源码计划
### 文本
1. 重用StringBuilder: org.springside.modules.utils.text.StringBuilderHolder
2. 常用正则表达式：TextValidator
3. MoreStringUtil

### 集合
#### Map簇
1. Jdk8之ConcurrentHashMap
2. 枚举Map:EnumMap
3. 特殊Map: IntOjbectHashMap LongObjectHashMap
4. 一Key对应多Value的Map: ArrayListMultimap(内部先取出List,再add)

#### 集合Util
来自SpringSide: MapUtil, ListUtil

### 并发
1. 并发计数器： LongAdder
2. SpringSide: org.springside.modules.utils.concurrent.threadpool.ThreadPoolUtil

### 异常
1. Guava: com.google.common.base.Throwables
2. SpringSide: org.springside.modules.utils.base.ExceptionUtil

### 日期
1. DateFormatUtil
日期与String相互转换时，JDK的SimpleDateFormat，又慢，又非线程安全。
在不能全面转为Joda Time时，使用Common Lang的FastDateFormat，又快，又线程安全，还能缓存实例。
2. CachingDateFormatter
FastDateFormat再快，日期格式化还是个消耗很大的事情。
参考Logback和Log4j2，在打印当前时间的场景里，将同一时刻的结果缓存。

## Referto
https://github.com/jiangxincode/cnblogs
https://www.marcobehler.com/2014/12/27/marco-behlers-2014-ultimate-java-developer-library-tool-people-list/
