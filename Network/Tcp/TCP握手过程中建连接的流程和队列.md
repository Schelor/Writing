## 三次握手
![tcp-sync-queue-and-accept-queue](./tcp-sync-queue-and-accept-queue-small-1024x747.jpg)

如上图所示，这里有两个队列：syns queue(半连接队列）；accept queue（全连接队列）
三次握手中，在第一步server收到client的syn后，把相关信息放到半连接队列中，同时回复syn+ack给client（第二步）

第三步的时候server收到client的ack，如果这时全连接队列没满，那么从半连接队列拿出相关信息放入到全连接队列中，否则按tcp_abort_on_overflow指示的执行。

这时如果全连接队列满了并且tcp_abort_on_overflow是0的话，server过一段时间再次发送syn+ack给client（也就是重新走握手的第二步），如果client超时等待比较短，就很容易异常了。centos默认配置值：
```
tcp_abort_on_overflow = 5
```


### syn floods
syn floods 攻击就是针对半连接队列的，攻击方不停地建连接，但是建连接的时候只做第一步，第二步中攻击方收到server的syn+ack后故意扔掉什么也不做，导致server上这个队列满其它正常请求无法进来。

## TCP连接队列溢出
```
1. netstat -s
netstat -s | egrep "listen|LISTEN"
[root@server ~]#  netstat -s | egrep "listen|LISTEN"
667399 times the listen queue of a socket overflowed
667399 SYNs to LISTEN sockets ignored

2. ss 命令
ss -lnt
Recv-Q Send-Q Local Address:Port  Peer Address:Port
0        50               *:3306             *:*
```
上面看到的第二列Send-Q 表示第三列的listen端口上的全连接队列最大为50，第一列Recv-Q为全连接队列当前使用了多少
全连接队列的大小取决于：min(backlog, somaxconn) . backlog是在socket创建的时候传入的，somaxconn是一个os级别的系统参数
半连接队列的大小取决于：max(64, /proc/sys/net/ipv4/tcp_max_syn_backlog)。 不同版本的os会有些差异

## 容器或框架的Accept队列参数
1. Tomcat默认短连接，backlog（在Tomcat里面的术语是Accept count）默认100.
2. Nginx默认为：By default, backlog is set to -1 on FreeBSD, DragonFly BSD, and macOS, and to 511 on other platforms 
3. Netty默认为：Linux and Mac OS X: 128 @see io.netty.util.NetUtil
