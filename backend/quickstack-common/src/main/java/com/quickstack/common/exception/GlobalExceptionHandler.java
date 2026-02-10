package com.quickstack.common.exception;

import com.quickstack.common.dto.ApiError;
import com.quickstack.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Global exception handler for all REST controllers.
 * <p>
 * Ensures consistent error response format and prevents information leakage.
 * ASVS V7: Error handling that doesn't expose sensitive information.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle custom API exceptions.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.warn("API exception: {} - {}", ex.getCode(), ex.getMessage());

        ApiError error = ApiError.of(ex.getCode(), ex.getMessage());
        return ResponseEntity
            .status(ex.getStatus())
            .body(ApiResponse.error(error));
    }

    /**
     * Handle validation exceptions with field details.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        log.warn("Validation exception: {}", ex.getMessage());

        List<ApiError.FieldError> fieldErrors = ex.getFieldErrors().stream()
            .map(fe -> new ApiError.FieldError(fe.field(), fe.message()))
            .toList();

        ApiError error = ApiError.of(ex.getCode(), ex.getMessage(), fieldErrors);
        return ResponseEntity
            .status(ex.getStatus())
            .body(ApiResponse.error(error));
    }

    /**
     * Handle Spring validation exceptions (from @Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());

        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

        ApiError error = ApiError.of("VALIDATION_ERROR", "Validation failed", fieldErrors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle constraint violations (from @Validated on path/query params).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
            .map(cv -> new ApiError.FieldError(
                cv.getPropertyPath().toString(),
                cv.getMessage()
            ))
            .toList();

        ApiError error = ApiError.of("VALIDATION_ERROR", "Validation failed", fieldErrors);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle Spring Security authentication exceptions.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ApiError error = ApiError.of("UNAUTHORIZED", "Authentication required");
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle Spring Security access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiError error = ApiError.of("FORBIDDEN", "Access denied");
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle malformed JSON.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());

        ApiError error = ApiError.of("BAD_REQUEST", "Malformed request body");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle method not allowed.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ApiError error = ApiError.of("METHOD_NOT_ALLOWED", "Method not allowed: " + ex.getMethod());
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle unsupported media type.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        ApiError error = ApiError.of("UNSUPPORTED_MEDIA_TYPE", "Content type not supported");
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        ApiError error = ApiError.of("BAD_REQUEST", "Missing parameter: " + ex.getParameterName());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle type mismatch (e.g., string where number expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiError error = ApiError.of("BAD_REQUEST", "Invalid parameter type: " + ex.getName());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(error));
    }

    /**
     * Handle 404 for static resources.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        ApiError error = ApiError.of("NOT_FOUND", "Resource not found");
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(error));
    }

    /**
     * Catch-all for unexpected exceptions.
     * Never expose internal error details to clients (ASVS V7).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        // Log the full exception for debugging
        log.error("Unexpected error", ex);

        // Return generic message - never expose internal details
        ApiError error = ApiError.of("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(error));
    }
}
