package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when rate limit is exceeded.
 *
 * ASVS V2.2.1: Implement anti-automation controls.
 * ASVS V2.2.2: Use rate limiting to prevent brute force attacks.
 */
public class RateLimitExceededException extends ApiException {

    private static final HttpStatus STATUS = HttpStatus.TOO_MANY_REQUESTS;
    private static final String CODE = "RATE_LIMIT_EXCEEDED";

    private final long retryAfterSeconds;

    /**
     * Creates a rate limit exception with retry information.
     *
     * @param retryAfterSeconds seconds until the rate limit resets
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        super(STATUS, CODE, "Too many requests. Please try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Creates a rate limit exception with custom message.
     *
     * @param message custom error message
     * @param retryAfterSeconds seconds until the rate limit resets
     */
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(STATUS, CODE, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Returns the number of seconds until the rate limit resets.
     * This should be used for the Retry-After HTTP header.
     *
     * @return seconds until rate limit reset
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
