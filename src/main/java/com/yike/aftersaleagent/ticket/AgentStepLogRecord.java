package com.yike.aftersaleagent.ticket;

import java.time.LocalDateTime;

public record AgentStepLogRecord(
        int stepNo,
        String stepName,
        String status,
        String inputSummary,
        String outputSummary,
        String errorCode,
        LocalDateTime startedAt,
        LocalDateTime endedAt) { }
