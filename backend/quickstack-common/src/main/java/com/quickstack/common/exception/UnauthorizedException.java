package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails or is missing.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required");
    }
}
