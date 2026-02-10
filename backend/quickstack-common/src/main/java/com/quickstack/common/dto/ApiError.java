package com.quickstack.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Standard API error structure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String code,
    String message,
    List<FieldError> details
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError of(String code, String message, List<FieldError> details) {
        return new ApiError(code, message, details);
    }
}
