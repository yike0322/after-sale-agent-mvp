package com.yike.aftersaleagent.tool;

public record AgentExecutionContext(
        String requestId,
        long userId,
        String sessionId,
        String userMessage) { }
