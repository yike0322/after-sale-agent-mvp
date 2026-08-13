package com.yike.aftersaleagent.common.api;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String requestId) {

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>(true, "SUCCESS", "success", data, requestId);
    }

    public static <T> ApiResponse<T> failure(String code, String message, String requestId) {
        return new ApiResponse<>(false, code, message, null, requestId);
    }
}
