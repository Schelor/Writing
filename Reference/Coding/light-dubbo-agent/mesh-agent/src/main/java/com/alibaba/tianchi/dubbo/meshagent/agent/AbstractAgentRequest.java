package com.alibaba.tianchi.dubbo.meshagent.agent;

import com.alibaba.tianchi.dubbo.meshagent.dubbo.RpcRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.Map;

/**
 * @author xiele
 * @date 2018/04/17
 */
public abstract class AbstractAgentRequest implements AgentRequest {

    protected static final Logger logger = LogManager.getLogger(AbstractAgentRequest.class);

    protected URI uri;
    protected Map<String, String> parameter;

    public AbstractAgentRequest(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("illegal request uri");
        }
        this.uri = uri;
    }

    public AgentRequest append(Map<String, String> parameter) {
        if (parameter == null || parameter.isEmpty()) {
            throw new IllegalArgumentException("illegal request parameter");
        }
        this.parameter = parameter;
        return this;
    }

    public RpcRequest rpcRequest() {
        RpcRequest request = new RpcRequest();
        request.setId(RpcRequest.newId());
        request.setInterfaceName(this.parameter.get("interface"));
        request.setMethodName(this.parameter.get("method"));
        request.setParameterTypes(this.parameter.get("parameterTypesString"));
        request.setParameter(this.parameter.get("parameter"));
        request.setAttachment("path", request.getInterfaceName());
        request.setAttachment("version", "0.0.0");
        return request;
    }

}
