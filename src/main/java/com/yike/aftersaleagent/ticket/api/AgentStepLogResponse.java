package com.yike.aftersaleagent.ticket.api;

import java.time.LocalDateTime;

public record AgentStepLogResponse(
        int stepNo,
        String stepName,
        String status,
        String inputSummary,
        String outputSummary,
        String errorCode,
        LocalDateTime startedAt,
        LocalDateTime endedAt) { }
