package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user doesn't have permission to access a resource.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied");
    }
}
