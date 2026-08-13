package com.yike.aftersaleagent.ticket.api;

public record ToolCallLogResponse(
        String toolName,
        boolean success,
        long costTimeMs,
        String requestSummary,
        String responseSummary,
        String errorCode) { }
