curl: command line URL viewer
是一种命令行工具，作用是发出网络请求，然后得到和提取数据，显示在"标准输出"（stdout）上面.

## 查看网页源码
```
curl https://www.taobao.com
```

### 带302的响应
```
curl -L taobao.com
```
## 显示头信息
`-i`参数可以显示http response的头信息，连同网页代码一起。
```
curl -i www.taobao.com
```

## 显示通信过程
`-v`参数可以显示一次http通信的整个过程，包括端口连接和http request头信息。
```
curl taobao.com

更详细：
curl --trace output.txt www.sina.com
curl --trace-ascii output.txt www.sina.com

```

## 发送表单信息
发送表单信息有GET和POST两种方法。GET方法相对简单，只要把数据附在网址后面就行。
```
curl example.com/form.cgi?data=xxx
```
POST方法必须把数据和网址分开，curl就要用到--data参数。
```
 curl -X POST --data "data=xxx" example.com/form.cgi
```

如果你的数据没有经过表单编码，还可以让curl为你编码，参数是`--data-urlencode`。
```
curl -X POST--data-urlencode "date=April 1" example.com/form.cgi
```

## Referer字段
有时你需要在http request头信息中，提供一个referer字段，表示你是从哪里跳转过来的。
```
curl --referer http://www.example.com http://www.example.com
```

## User Agent字段
这个字段是用来表示客户端的设备信息。服务器有时会根据这个字段，针对不同设备，返回不同格式的网页，比如手机版和桌面版。
```
curl --user-agent "[User Agent]" [URL]
```
## cookie
使用`--cookie`参数，可以让curl发送cookie。

```
curl --cookie "name=xxx" www.example.com
```
## 增加头信息
有时需要在http request之中，自行增加一个头信息。`--header`参数就可以起到这个作用。

```
curl --header "Content-Type:application/json" http://example.com
```
