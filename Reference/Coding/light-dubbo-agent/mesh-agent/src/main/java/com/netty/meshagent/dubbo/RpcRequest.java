package com.netty.meshagent.dubbo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -3088346785317132117L;
    private static final AtomicLong INVOKE_ID = new AtomicLong();

    private Long id;

    private String interfaceName;

    private String methodName;

    private String parameterTypes;

    private String parameter;

    private Map<String, String> attachments;


    public static long newId() {
        return INVOKE_ID.incrementAndGet();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }

    public String getAttachment(String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }

    public String getAttachment(String key, String defaultValue) {
        if (attachments == null) {
            return defaultValue;
        }
        String value = attachments.get(key);
        if (value == null || value.length() == 0) {
            return defaultValue;
        }
        return value;
    }

    public void setAttachment(String key, String value) {
        if (attachments == null) {
            attachments = new HashMap<>();
        }
        attachments.put(key, value);
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "id=" + id +
                ", interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes='" + parameterTypes + '\'' +
                ", parameter='" + parameter + '\'' +
                ", attachments=" + attachments +
                '}';
    }
}
