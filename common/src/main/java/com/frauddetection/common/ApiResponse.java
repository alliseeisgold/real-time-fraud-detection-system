package com.frauddetection.common;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetails error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return failure(code, message, Map.of());
    }

    public static <T> ApiResponse<T> failure(String code, String message, Map<String, String> details) {
        return new ApiResponse<>(false, null, new ErrorDetails(code, message, details), Instant.now());
    }

    public record ErrorDetails(
            String code,
            String message,
            Map<String, String> details
    ) {
    }
}
