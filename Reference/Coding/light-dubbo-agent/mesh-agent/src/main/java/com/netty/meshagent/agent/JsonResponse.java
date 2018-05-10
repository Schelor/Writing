package com.netty.meshagent.agent;

import java.util.Optional;

/**
 * @author xiele
 * @date 2018/04/15
 */
public class JsonResponse implements AgentResponse {

    private String json;

    public JsonResponse (String json) {
        this.json = json;
    }

    public static JsonResponse json(String json) {
        return new JsonResponse(json);
    }

    public static JsonResponse empty() {
        return new JsonResponse(null);
    }

    @Override
    public Optional<String> message() {
        return Optional.ofNullable(json);
    }


}
