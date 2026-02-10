package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when request validation fails.
 */
public class ValidationException extends ApiException {

    private final List<FieldError> fieldErrors;

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        this.fieldErrors = List.of();
    }

    public ValidationException(String message, List<FieldError> fieldErrors) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        this.fieldErrors = fieldErrors;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public record FieldError(String field, String message) {}
}
