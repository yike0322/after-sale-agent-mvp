package com.yike.aftersaleagent.tool;

import com.yike.aftersaleagent.common.api.ErrorCode;

public record ToolAuditRequest(
        String requestId,
        String requestSummary,
        String responseSummary,
        ErrorCode errorCode) {
    private static final int MAX_REQUEST_ID_LENGTH = 100;
    private static final int MAX_SUMMARY_LENGTH = 200;

    public ToolAuditRequest {
        requestId = bounded(requestId, MAX_REQUEST_ID_LENGTH);
        requestSummary = bounded(requestSummary, MAX_SUMMARY_LENGTH);
        responseSummary = responseSummary == null ? null : bounded(responseSummary, MAX_SUMMARY_LENGTH);
    }

    public static ToolAuditRequest success(
            String requestId, String requestSummary, String responseSummary) {
        return new ToolAuditRequest(requestId, requestSummary, responseSummary, null);
    }

    public static ToolAuditRequest failure(
            String requestId, String requestSummary, ErrorCode errorCode) {
        return new ToolAuditRequest(requestId, requestSummary, null, errorCode);
    }

    private static String bounded(String value, int maximumLength) {
        String safeValue = value == null ? "" : value.strip();
        return safeValue.length() <= maximumLength ? safeValue : safeValue.substring(0, maximumLength);
    }
}
