package com.smeloan.platform.common.dto;

import lombok.*;
import java.time.OffsetDateTime;

/**
 * Generic API response wrapper for all REST endpoints.
 *
 * @param <T> the type of the response data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message("OK")
            .data(data)
            .build();
    }

    /**
     * Creates a successful response with a custom message and data.
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    /**
     * Creates an error response with a message.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .build();
    }
}
