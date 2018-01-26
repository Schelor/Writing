# 官方文档
http://logging.apache.org/log4j/2.x/articles.html

# 配置
以Configuration为根节点，有一个status属性，这个属性表示log4j2本身的日志信息打印级别。如果把status改为TRACE再执行测试代码，可以看到控制台中打印了一些log4j加载插件、组装logger等调试信息。
日志级别从低到高分为TRACE < DEBUG < INFO < WARN < ERROR < FATAL，如果设置为WARN，则低于WARN的信息都不会输出。对于Loggers中level的定义同样适用。
下面是Appender配置，Appender可以理解为日志的输出目的地，这里配置了一个类型为Console的Appender，也就是输出到控制台。Console节点中的PatternLayout定义了输出日志时的格式：
%d{HH:mm:ss.SSS} 表示输出到毫秒的时间
%t 输出当前线程名称
%-5level 输出日志级别，-5表示左对齐并且固定输出5个字符，如果不足在右边补0
%logger 输出logger名称，因为Root Logger没有名称，所以没有输出
%msg 日志文本
%n 换行
其他常用的占位符有：
%F 输出所在的类文件名，如Client.java
%L 输出行号
%M 输出所在方法名
%l  输出语句所在的行数, 包括类名、方法名、文件名、行数
最后是Logger的配置，这里只配置了一个Root Logger。

additivity表示向父Logger中Append.
