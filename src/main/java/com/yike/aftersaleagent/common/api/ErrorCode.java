package com.yike.aftersaleagent.common.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    DEMO_USER_REQUIRED("DEMO_USER_REQUIRED", "Demo user is required", HttpStatus.UNAUTHORIZED),
    DEMO_USER_NOT_FOUND("DEMO_USER_NOT_FOUND", "Demo user was not found", HttpStatus.FORBIDDEN),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "Session was not found", HttpStatus.NOT_FOUND),
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
