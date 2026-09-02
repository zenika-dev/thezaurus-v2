package com.zenika.thezaurus.model;

public record AgentPayload(String class_method, ReasoningEngineInput input) {
    public static AgentPayload asyncStreamQuery(String messageJson) {
        return new AgentPayload("async_stream_query", new ReasoningEngineInput(messageJson, "12345"));
    }
}
