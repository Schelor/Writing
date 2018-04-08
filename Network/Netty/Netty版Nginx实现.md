## 前言
Nginx典型的作用是作为反向代理服务器，最近想了解Nginx反向代理的原理，因此尝试自己动手实践。技术上直接采用Netty来实现，Netty作为高性能的网络框架，提供了异步的，事件驱动的程序开发库，同时还集成了epoll网络模型实现，Http编解码的处理器等非常便利的工具API.  因此有了Netty，开发反向代理服务器便简易了许多。

功能上对标Nginx的核心与常用功能，支持配置自定义，支持通配符的域名配置，支持Http包的编解码，支持HTTP请求头，请求URI、请求体的转发，支持上游upstream服务器的负载均衡控制等，最后还需支持代理服务器内部处理的日志配置与请求日志记录。

## 参考Nginx的JSON格式配置

```java
{
  "user": "admin",
  "readClientTimeout":5000,
  "connectionTimeout":5000,
  "events": {
    "use": "epoll",
    "workerConnections": 2048
  },
  "http": {
    "index": "index.htm index.htm",
    "include": "",
    "errorPages": [{
        "code": 500,
        "uri": "http://err.taobao.com/error1.html"
      }
    ],
    "accessLog": "logs/access_log.log",
    "servers": [
      {
        "listen": 7777,
        "serverName": "*.taobao.net",
        "defaultServer": true,
        "locations": [
          {
            "uriPattern": "/",
            "proxyPass": "http://11.163.209.52:7001"
          },
          {
            "uriPattern": "/login",
            "proxyPass": "http://11.163.209.52:7001"
          }
        ]
      },
      {
        "listen": 7777,
        "serverName": "login.daily.taobao.*",
        "locations": [
          {
            "uriPattern": "/hello/",
            "proxyPass": "http://login_daily"
          }
        ]
      },
      {
        "listen": 7777,
        "serverName": "login.daily.taobao.net",
        "locations": [
          {
            "uriPattern": "/login/oauth2",
            "proxyPass": "http://login_daily"
          }
        ]
      }
    ],
    "upstream":[
      {
        "name": "login_daily",
        "servers": [
          "11.162.252.212:80",
          "11.163.209.52:80"
        ]
      }
    ]
  }
}
```

## 配置解析与运行时初始化

根据json的格式，定义main，event, http，server等配置块，如：

```Java
// 相当于Nginx的main
public class MainBlockConf implements Serializable {
    private String user;
    private int readClientTimeout = -1;
    private int connectionTimeout = -1;
    // 事件模型配置块
    private EventBlockConf events;
    // http 配置块
    private HttpBlockConf http;
```

### 解析配置：

```Java
public final class ConfLoader {
    private static MainBlockConf mainConf;
    static {
        mainConf = load();
    }
    public static MainBlockConf load() {
        ClassLoader clc = Thread.currentThread().getContextClassLoader();
        InputStream in = clc.getResourceAsStream("conf.json");
        try {
            MainBlockConf conf = JSON.parseObject(in, new TypeReference<MainBlockConf>() {
            }.getType());
            log.info("load conf: {}", JSON.toJSONString(conf,true));
            return conf;
        } catch (IOException e) {
            throw new IllegalArgumentException("load conf error");
        }
    }
    public static MainBlockConf getMainConf() {
        if (mainConf == null) {
            throw new IllegalArgumentException("there maybe a missing conf.json");
        }
        return mainConf;
    }
    public static EventBlockConf getEventConf() {
        EventBlockConf events = getMainConf().getEvents();
        if (events == null) {
            throw new IllegalArgumentException("conf error, missing event conf");
        }
        return events;
    }
    public static HttpBlockConf getHttpConf() {

        HttpBlockConf http = getMainConf().getHttp();
        if (http == null) {
            throw new IllegalArgumentException("conf error, missing http conf");
        }
        return http;
    }
    public static List<ServerBlockConf> getServerConf() {
        List<ServerBlockConf> servers = getHttpConf().getServers();
        if (servers == null) {
            throw new IllegalArgumentException("conf error, missing server conf");
        }
        return servers;
    }
}
```

### 初始化配置的日志路径

采用Log4j2动态创建Logger,并配置默认的日志输出格式,Rolling策略：

```Java
public class ServerLoggerFactory {
    private static final LoggerContext lc = (LoggerContext) LogManager.getContext(false);
    private static final Configuration loggerConf = lc.getConfiguration();

    /**
     * 创建Logger
     * @param logger
     * @param loggerPath
     * @param loggerName
     */
    private static void createLogger(String logger, String loggerPath, String loggerName) {
        if (loggerConf.getLoggers().containsKey(loggerName)) {
            return ;
        }
        //创建一个展示的样式：PatternLayout
        PatternLayout layout = PatternLayout.newBuilder()
                .withConfiguration(loggerConf)
                .withPattern("%d{yyyy-MM-dd HH:mm:ss:SSS} %-5level %class{36}[%L] %M - %msg%xEx%n")
                .build();

        //单个日志文件大小
        TimeBasedTriggeringPolicy tbtp = TimeBasedTriggeringPolicy.createPolicy(null, null);
        TriggeringPolicy tp = SizeBasedTriggeringPolicy.createPolicy("10M");
        CompositeTriggeringPolicy policyComposite = CompositeTriggeringPolicy.createPolicy(tbtp, tp);

        DefaultRolloverStrategy strategy = DefaultRolloverStrategy.createStrategy(
                "7", "1", null, null, null, false, loggerConf);
        //日志路径
        final String loggerPathPrefix = loggerPath  + loggerName;
        RollingFileAppender appender = RollingFileAppender.newBuilder()
                .withFileName(loggerPathPrefix)
                .withFilePattern(loggerPathPrefix + ".%d{yyyy-MM-dd}.%i")
                .withAppend(true)
                .withStrategy(strategy)
                .withName(logger)
                .withPolicy(policyComposite)
                .withLayout(layout)
                .setConfiguration(loggerConf)
                .build();
        appender.start();

        loggerConf.addAppender(appender);

        AppenderRef ref = AppenderRef.createAppenderRef(logger, null, null);
        AppenderRef[] refs = new AppenderRef[]{ref};
        LoggerConfig loggerConfig = LoggerConfig.createLogger(false,
                Level.ALL, logger, "true", refs, null, loggerConf, null);
        loggerConfig.addAppender(appender, null, null);

        loggerConf.addLogger(logger, loggerConfig);
        lc.updateLoggers();
    }
    /**获取Logger*/
    public static Logger getLogger(String logger) {
        synchronized (loggerConf) {
            if (!loggerConf.getLoggers().containsKey(logger)) {
                throw new IllegalArgumentException("please create logger file");
            }
        }
        return LogManager.getLogger(logger);
    }
}
```

### 初始化带通配符的域名

参考Nginx的域名配置规则，支持全匹配，前缀匹配，后缀匹配，规则实现策略如下:

- 1）如配置的serverName不带星号(`*`) , 则走全匹配，如`hello.login.daily.taobao.net`, 存储映射为：
`hello.login.daily.taobao.net -> hello.login.daily.taobao.net`

- 2) 如配置的serverName前缀部分带有星号(`*`), 则走前缀匹配, 如 `.*.login.daily.taobao.net`, 存储映射为：
`.login.daily.taobao.net -> *.login.daily.taobao.net`

- 3) 如果配置的serverName后缀部分带有星号(`*`), 则走后缀匹配，如`login.daily.taobao.*` ,存储映射为：
`login.daily.taobao. -> login.daily.taobao.*`

- 4) 不支持中间部分有星号(`*`),不支持有两个星号(`*`)

```Java
// 初始化带通配符的虚拟server name
for (ServerBlockConf server : ConfLoader.getServerConf()) {
    StoredUpstreamServer.storeServer(server);
    ACCESS_LOGGER.info("init server name done, the pattern is: {}", server.getServerName());

    // 统计需要监听的端口
    int listen = server.getListen();
    if (listen < 0) {
        throw new IllegalArgumentException(
            "illegal server port which should be greater than 0 and better be 80 or 443");
    }
    RuntimeContext.addWorkingPort(listen);

    ACCESS_LOGGER.info("init server port done, the port is: {}", listen);
}

 public static void storeServer(ServerBlockConf serverConf) {
        String serverName = serverConf.getServerName();
        emptyServerConf(serverConf);
        if (!serverName.contains(STAR_SYMBOL)) { // 不带*通配符
            store.put(serverName,
                UpstreamWrapper.create(serverName, Pattern.compile(serverName), serverConf.getLocations()));
            return;
        }
        // 只支持前缀和后缀通配符
        List<String> splittedServer = Splitter.on(STAR_SYMBOL).splitToList(serverName);
        invalidServerName(splittedServer);

        if ("".equals(splittedServer.get(0))) {
            store.put(splittedServer.get(1), UpstreamWrapper
                .create(serverName, Pattern.compile(serverName.replace(STAR_SYMBOL, ".*")), serverConf.getLocations()));
        }
        if ("".equals(splittedServer.get(1))) {
            store.put(splittedServer.get(0), UpstreamWrapper
                .create(serverName, Pattern.compile(serverName.replace(STAR_SYMBOL, "*")), serverConf.getLocations()));
        }

  }
```

## 启动Server端
### HTTP连接器
http连接器的功能主要是对HTTP包作编解码, 注册转发处理器：
```Java
	 /**
     * 编解码handler
     */
    public static final String CODEC_HANDLER_NAME = "codec_handler";

    /**
     * 压缩数据handler
     */
    public static final String COMPRESSOR_HANDLER_NAME = "compressor_handler";

    /**
     * Http包聚合handler
     */
    public static final String AGGREGATOR_HANDLER_NAME = "aggregator_handler";

    /**
     * http请求处理handler
     */
    public static final String HTTP_REQUEST_HANDLER_NAME = "http_request_handler";

    public ChannelInitializer<?> getChannelInitializer() {
        return new ChannelInitializer<NioSocketChannel>() {

            protected void initChannel(final NioSocketChannel ch) throws Exception {

                ch.pipeline()
                        .addLast(CODEC_HANDLER_NAME, new HttpServerCodec())
                        .addLast(AGGREGATOR_HANDLER_NAME, new HttpObjectAggregator(10 * 1024 * 1024))
                        .addLast(HTTP_REQUEST_HANDLER_NAME, new NettyClientUpstreamHandler())
                        .addAfter(CODEC_HANDLER_NAME, COMPRESSOR_HANDLER_NAME, new HttpContentCompressor())
                ;


            }
        };
    }
```

### 监听端口(可能有多个)

```Java
public class NettyHttpServer implements HttpServer {

    private HttpServerConnector connector = new HttpServerConnector();

    private final EventLoopGroup masterGroup;
    private final EventLoopGroup slaveGroup;

    public NettyHttpServer() {
        // 主线程池，作为Acceptor线程池，并转发到IO线程池处理
        masterGroup = new NioEventLoopGroup(RuntimeContext.getWorkingPort().size());
        slaveGroup = new NioEventLoopGroup(RuntimeContext.getIoThreads());
    }

    public void start() {
        addShutdownHook();
        initConf();
        startServer();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
            shutdown();
        }));
    }

    private void initConf() {
        ContextInitializer.init();
    }

    private void startServer() {
        ServerBootstrap bootstrap = new ServerBootstrap()
            .group(masterGroup, slaveGroup);
        // 选择io模型, linux支持epoll, nio底层为select 或poll
        if (RuntimeContext.getEventMode() == EventMode.EPOLL) {
            bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            bootstrap.channel(NioServerSocketChannel.class);
        }
        bootstrap.childHandler(connector.getChannelInitializer());
        bootstrap.option(ChannelOption.SO_BACKLOG, RuntimeContext.getWorkerConnections()); // tcp接受连接的队列大小
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        try {
            Set<Integer> workingPort = RuntimeContext.getWorkingPort();
            for (Integer port : workingPort) {
                ChannelFuture future = bootstrap.bind(port).sync();
                future.channel().closeFuture().sync();
            }
        } catch (InterruptedException e) {
            log.error("start http server error", e);
        }
    }

```

## 请求转发处理

### 接收请求

由于已在HttpConnector中注册了编解码的handler:HttpServerCodec，因此只需要继承入栈的Handler如SimpleChannelInboundHandler，在管道中流向自定义的Handler时，请求体已经被解码成封装好的http请求：

```Java
public class NettyClientUpstreamHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        log.info("接收到的请求信息：{}", httpRequest.toString());
        final String host = getHostFromHeader(httpRequest);
        // 根据host查找转发的server地址
        String upstreamHost = StoredUpstreamServer.getUpstreamHost(host, httpRequest.getUri());
        if (StoredUpstreamServer.supportUpstream(upstreamHost)) {
            doExecuteProxy(ctx, upstreamHost, httpRequest);
        } else {
           ctx.writeAndFlush(createBadRequestResponse());
        }
    }


    private void doExecuteProxy(ChannelHandlerContext ctx, final String upstreamHost, FullHttpRequest httpRequest) throws IOException {

        try {
            // 使用Netty转发到上游服务器
            // 解析path + queryString
            NettyClientHttpProxyRequest request = factory.createUpstreamRequest(upstreamHost);
            ListenableFuture<FullHttpResponse> listenableFuture = request.executeInternal(httpRequest);

            FullHttpResponse proxyResponse = listenableFuture.get();
            log.info("代理转发收到的响应为: {}", proxyResponse);

            ctx.writeAndFlush(proxyResponse);

        } catch (Exception e) {
            log.error("转发请求出现异常", e);
            ctx.writeAndFlush(create502BadGatewayResponse());
        }

    }
}
```

### 构建转发请求
#### 引入Factory创建代理请求

```Java
public class NettyClientHttpRequestFactory implements InitializingBean, DisposableBean  {
    public NettyClientHttpRequestFactory() {
        int ioWorkerCount = Runtime.getRuntime().availableProcessors() * 2;
        this.ioEventLoopGroup = new NioEventLoopGroup(ioWorkerCount);
        this.defaultEventLoopGroup = true;
    }

    private Bootstrap getBootstrap() {
        if (this.bootstrap == null) {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(this.ioEventLoopGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            configureChannel(channel.config());
                            ChannelPipeline pipeline = channel.pipeline();
                            configureSSL(channel);
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(maxResponseSize));
                            if (readTimeout > 0) {
                                pipeline.addLast(new ReadTimeoutHandler(readTimeout,
                                        TimeUnit.MILLISECONDS));
                            }
                        }
                    });
            this.bootstrap = bootstrap;
        }
        return this.bootstrap;
    }

    protected void configureChannel(SocketChannelConfig config) {
        if (this.connectTimeout >= 0) {
            config.setConnectTimeoutMillis(this.connectTimeout);
        }
    }

    // 创建代理请求，复用client端的Channel和线程池
    public NettyClientHttpProxyRequest createUpstreamRequest(String host) throws IOException {

        return new NettyClientHttpProxyRequest(getBootstrap(), getUpstreamUri(host));
    }

    public URI getUpstreamUri(String host) {
        try {
            return new URI(host);
        } catch (URISyntaxException e) {
            log.error("解析URI出现异常", e);
            throw new IllegalArgumentException("Invalid URL after inserting base URL: " + host, e);
        }
    }
}
```

### 异步执行转发

```Java
public class NettyClientHttpProxyRequest {
    private final Bootstrap bootstrap;
    private final URI uri;

    public NettyClientHttpProxyRequest(Bootstrap bootstrap, URI uri) {
        this.bootstrap = bootstrap;
        this.uri = uri;
    }

    public ListenableFuture<FullHttpResponse> executeInternal(final FullHttpRequest request) {
        final SettableListenableFuture<FullHttpResponse> responseFuture =
                new SettableListenableFuture<>();
        ChannelFutureListener connectionListener = future -> {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                channel.pipeline().addLast(new UpstreamRequestExecuteHandler(responseFuture));
                channel.writeAndFlush(createFullHttpRequest(request));
            } else {
                log.error("Upstream转发请求出现异常", future.cause());
                responseFuture.set(NettyClientUpstreamHandler.create502BadGatewayResponse());
            }
        };
        // 此处应该设置为：上游服务器的URI 和端口
        ChannelFuture channelFuture = this.bootstrap.connect(this.uri.getHost(), getPort(this.uri));
        channelFuture.addListener(connectionListener);
        return responseFuture;
    }

    private static int getPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                port = 80;
            }
            else if ("https".equalsIgnoreCase(uri.getScheme())) {
                port = 443;
            }
        }
        return port;
    }

    // 转换请求
    private FullHttpRequest createFullHttpRequest(final FullHttpRequest request) {

        FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
                request.getMethod(), request.getUri(), request.content());
        for (Map.Entry<String, String> entry : request.headers().entries()) {
            nettyRequest.headers().add(entry.getKey(), entry.getValue());
        }
        nettyRequest.headers().set(io.netty.handler.codec.http.HttpHeaders.Names.HOST, uri.getHost());
        nettyRequest.headers().set(io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION,
                io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE);
        log.debug("转发到：{}, header：{}", this.uri, nettyRequest.headers().entries());
        return nettyRequest;
    }
}
```



### 获取上游Server的响应

```Java
private static class UpstreamRequestExecuteHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final SettableListenableFuture<FullHttpResponse> responseFuture;

        public UpstreamRequestExecuteHandler(SettableListenableFuture<FullHttpResponse> responseFuture) {
            this.responseFuture = responseFuture;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, FullHttpResponse response) throws Exception {

         	// 把上游server返回的响应放入Future中, 由外部通过get方法获取
            // 由于涉及到不同的ChannelContext,因此需要手动保留reponse的引用计数
            this.responseFuture.set(response.retain());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
            log.error("转发请求Handler处理响应数据出现异常", cause);
            this.responseFuture.set(NettyClientUpstreamHandler.create502BadGatewayResponse());
        }
}
```



## 最后

谢谢阅读, 如有兴趣，欢迎讨论。
