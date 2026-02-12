package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails.
 * This is a generic authentication error that does not reveal
 * whether the failure was due to invalid email or password.
 *
 * ASVS V2.2.3: Error messages must not reveal which part of credentials was incorrect.
 */
public class AuthenticationException extends ApiException {

    private static final HttpStatus STATUS = HttpStatus.UNAUTHORIZED;
    private static final String DEFAULT_CODE = "AUTHENTICATION_FAILED";
    private static final String DEFAULT_MESSAGE = "Invalid credentials";

    public AuthenticationException() {
        super(STATUS, DEFAULT_CODE, DEFAULT_MESSAGE);
    }

    public AuthenticationException(String message) {
        super(STATUS, DEFAULT_CODE, message);
    }

    public AuthenticationException(String code, String message) {
        super(STATUS, code, message);
    }

    public AuthenticationException(String code, String message, Throwable cause) {
        super(STATUS, code, message, cause);
    }
}
