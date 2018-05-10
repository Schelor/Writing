package com.netty.meshagent.agent;

import java.util.Optional;

/**
 * @author xiele
 * @date 2018/04/17
 */
public class MsgpackResponse implements AgentResponse {

    private String text;

    public MsgpackResponse(String text) {
        this.text = text;
    }

    public static MsgpackResponse text(String text) {
        return new MsgpackResponse(text);
    }

    public static MsgpackResponse empty() {
        return new MsgpackResponse(null);
    }

    @Override
    public Optional<String> message() {
        return Optional.ofNullable(text);
    }
}
