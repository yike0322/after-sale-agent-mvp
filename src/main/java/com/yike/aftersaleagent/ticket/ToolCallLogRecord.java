package com.yike.aftersaleagent.ticket;

public record ToolCallLogRecord(
        String toolName,
        boolean success,
        long costTimeMs,
        String requestSummary,
        String responseSummary,
        String errorCode) { }
