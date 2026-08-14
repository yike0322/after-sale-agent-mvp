package com.yike.aftersaleagent.common.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    DEMO_USER_REQUIRED("DEMO_USER_REQUIRED", "Demo user is required", HttpStatus.UNAUTHORIZED),
    DEMO_USER_NOT_FOUND("DEMO_USER_NOT_FOUND", "Demo user was not found", HttpStatus.FORBIDDEN),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "Invalid account or password", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "Authentication token is invalid", HttpStatus.UNAUTHORIZED),
    AUTH_ROLE_FORBIDDEN("AUTH_ROLE_FORBIDDEN", "This operation requires supervisor access", HttpStatus.FORBIDDEN),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "Session was not found", HttpStatus.NOT_FOUND),
    TOOL_NOT_ALLOWED("TOOL_NOT_ALLOWED", "This tool is not allowed for the current request", HttpStatus.FORBIDDEN),
    BUSINESS_REFERENCE_REQUIRED("BUSINESS_REFERENCE_REQUIRED", "A valid business reference is required", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND_OR_FORBIDDEN("ORDER_NOT_FOUND_OR_FORBIDDEN", "Order was not found", HttpStatus.NOT_FOUND),
    COUPON_NOT_FOUND_OR_FORBIDDEN("COUPON_NOT_FOUND_OR_FORBIDDEN", "Coupon was not found", HttpStatus.NOT_FOUND),
    TOOL_TIMEOUT("TOOL_TIMEOUT", "The tool request timed out", HttpStatus.GATEWAY_TIMEOUT),
    TOOL_EXECUTION_FAILED("TOOL_EXECUTION_FAILED", "The tool request could not be completed", HttpStatus.INTERNAL_SERVER_ERROR),
    TOOL_AUDIT_FAILED("TOOL_AUDIT_FAILED", "The tool request could not be audited", HttpStatus.INTERNAL_SERVER_ERROR),
    ILLEGAL_TASK_TRANSITION("ILLEGAL_TASK_TRANSITION", "The ticket task transition is not allowed", HttpStatus.CONFLICT),
    TICKET_NOT_FOUND_OR_FORBIDDEN("TICKET_NOT_FOUND_OR_FORBIDDEN", "Ticket was not found", HttpStatus.NOT_FOUND),
    TICKET_PERSISTENCE_FAILED("TICKET_PERSISTENCE_FAILED", "The ticket could not be persisted", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("COMMON_BAD_REQUEST", "Invalid request", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
