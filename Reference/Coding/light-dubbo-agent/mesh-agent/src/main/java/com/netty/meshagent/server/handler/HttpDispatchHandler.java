package com.netty.meshagent.server.handler;

import com.netty.meshagent.agent.AgentClient;
import com.netty.meshagent.agent.MsgpackRequest;
import com.netty.meshagent.client.MsgpackAgentClient;
import com.netty.meshagent.util.ResponseBuilder;
import com.netty.meshagent.agent.AgentRequest;
import com.netty.meshagent.agent.AgentResponse;
import com.netty.meshagent.config.ConnectionConfig;
import com.netty.meshagent.lb.LoadBalanceFactory;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * @author xiele
 * @date 2018/04/14
 */
public class HttpDispatchHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger logger = LogManager.getLogger(HttpDispatchHandler.class);

    private static AgentClient agentClient = new MsgpackAgentClient();

    private static ListeningExecutorService listeningExecutor = MoreExecutors.listeningDecorator(Executors
            .newFixedThreadPool(ConnectionConfig.PROCESSOR_NUM,
                    new ThreadFactoryBuilder().setNameFormat("HttpDispatchWorkerListener").build()));

    public HttpDispatchHandler() {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Optional<Map<String, String>> optional = parameter(request);
        logger.info("received http request,method: {}, uri: {}, parameter: {}",
            request.method(), request.uri(), optional);
        if (!optional.isPresent() || optional.get().isEmpty()) {
            ctx.writeAndFlush(ResponseBuilder.badRequest("not support the http method or the parameter is empty"));
            return;
        }
        forward(ctx, request, optional.get());
        //mockResponse(ctx);
    }

    private void mockResponse(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(ResponseBuilder.mockSuccess());
    }
    private AgentRequest forwardRequest(URI uri, Map<String, String> parameter) {
        return MsgpackRequest.create(uri).append(parameter);
    }

    /**
     * 还可修改为异步监听回调机制 TODO
     *
     * @return
     */
    private void forward(ChannelHandlerContext ctx, FullHttpRequest request, Map<String, String> parameter) {
        try {
            // 负载均衡选择
            Optional<URI> uri = LoadBalanceFactory.random();
            if (uri.isPresent()) {
                ListenableFuture<AgentResponse> future = agentClient.executeAsync(forwardRequest(uri.get(), parameter));
                Futures.addCallback(future, new FutureCallback<AgentResponse>() {
                    @Override
                    public void onSuccess(@Nullable AgentResponse result) {
                        Optional<String> opt = result.message();
                        logger.info("success callback,  response: {}", result.message());
                        handleResponse(ctx, request, opt.get());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        logger.info("failure callback response: {}", t);
                        handleResponse(ctx, request, null);
                    }
                }, null);
            } else {
                logger.error("the http request execution error");
                ctx.writeAndFlush(ResponseBuilder.noSurvivor());
            }
        } catch (Exception e) {
            logger.error("forward the request error", e);
            ctx.writeAndFlush(ResponseBuilder.badGateWay());
        }

    }

    private Optional<Map<String, String>> parameter(FullHttpRequest request) throws IOException {
        // 解析http请求
        if (HttpMethod.GET == request.method()) {
            // 是GET请求
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            Map<String, String> parameter = Maps.newHashMapWithExpectedSize(decoder.parameters().size());
            decoder.parameters().entrySet().forEach(entry -> {
                // entry.getValue()是一个List, 只取第一个元素
                parameter.put(entry.getKey(), entry.getValue().get(0));
            });
            return Optional.of(parameter);
        } else if (HttpMethod.POST == request.method()) {
            // 是POST请求
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
            decoder.offer(request);
            Map<String, String> parameter = Maps.newHashMapWithExpectedSize(decoder.getBodyHttpDatas().size());
            List<InterfaceHttpData> list = decoder.getBodyHttpDatas();
            for (InterfaceHttpData data : list) {
                Attribute attr = (Attribute) data;
                parameter.put(attr.getName(), attr.getValue());
            }
            decoder.destroy();
            return Optional.of(parameter);
        } else {
            // 不支持其它方法
            return Optional.empty();
        }
    }

    private void handleResponse(ChannelHandlerContext ctx, FullHttpRequest request, String content) {
        // 响应HTTP
        FullHttpResponse response =
                ResponseBuilder.response(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("http dispatcher handler caught error, response bad gateway", cause);
        ctx.writeAndFlush(ResponseBuilder.badGateWay());
    }

}
