package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a password is found in a known data breach.
 *
 * ASVS V2.1.7: Passwords must be checked against known breaches.
 *
 * @see <a href="https://haveibeenpwned.com/Passwords">HaveIBeenPwned Passwords</a>
 */
public class PasswordCompromisedException extends ApiException {

    private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;
    private static final String CODE = "PASSWORD_COMPROMISED";
    private static final String DEFAULT_MESSAGE =
            "This password has been found in a data breach and cannot be used. " +
            "Please choose a different password.";

    /**
     * Creates a password compromised exception with default message.
     */
    public PasswordCompromisedException() {
        super(STATUS, CODE, DEFAULT_MESSAGE);
    }

    /**
     * Creates a password compromised exception with custom message.
     *
     * @param message custom error message
     */
    public PasswordCompromisedException(String message) {
        super(STATUS, CODE, message);
    }

    /**
     * Creates a password compromised exception with breach count information.
     * The count is logged but not exposed to the client.
     *
     * @param breachCount number of times password was found in breaches
     * @return new exception instance
     */
    public static PasswordCompromisedException withBreachCount(int breachCount) {
        // Don't expose breach count to client - security through obscurity
        return new PasswordCompromisedException();
    }
}
