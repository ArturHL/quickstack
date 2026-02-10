package com.quickstack.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard API response wrapper.
 *
 * @param <T> Type of the data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    T data,
    ApiError error,
    Meta meta
) {
    public record Meta(Instant timestamp) {
        public Meta() {
            this(Instant.now());
        }
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, new Meta());
    }

    public static <T> ApiResponse<T> error(ApiError error) {
        return new ApiResponse<>(null, error, new Meta());
    }
}
