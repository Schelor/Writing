package com.alibaba.tianchi.dubbo.meshagent.dubbo;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RpcInvocation {

    private String methodName;

    private String parameterTypes;

    private byte[] arguments;

    private Map<String, String> attachments;


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

    public byte[] getArguments() {
        return arguments;
    }

    public void setArguments(byte[] arguments) {
        this.arguments = arguments;
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
        return "RpcInvocation{" +
                "methodName='" + methodName + '\'' +
                ", parameterTypes='" + parameterTypes + '\'' +
                ", arguments=" + Arrays.toString(arguments) +
                ", attachments=" + attachments +
                '}';
    }
}