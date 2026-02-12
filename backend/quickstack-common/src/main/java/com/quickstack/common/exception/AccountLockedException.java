package com.quickstack.common.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Exception thrown when an account is temporarily locked due to
 * too many failed login attempts.
 *
 * ASVS V2.2.1: Account lockout after N failed attempts.
 */
public class AccountLockedException extends ApiException {

    private static final HttpStatus STATUS = HttpStatus.LOCKED;
    private static final String CODE = "ACCOUNT_LOCKED";

    private final Instant lockedUntil;

    /**
     * Creates an account locked exception with unlock timestamp.
     *
     * @param lockedUntil timestamp when the account will be unlocked
     */
    public AccountLockedException(Instant lockedUntil) {
        super(STATUS, CODE, "Account is temporarily locked due to too many failed login attempts");
        this.lockedUntil = lockedUntil;
    }

    /**
     * Creates an account locked exception with custom message.
     *
     * @param message custom error message
     * @param lockedUntil timestamp when the account will be unlocked
     */
    public AccountLockedException(String message, Instant lockedUntil) {
        super(STATUS, CODE, message);
        this.lockedUntil = lockedUntil;
    }

    /**
     * Returns the timestamp when the account will be unlocked.
     *
     * @return unlock timestamp, or null if permanently locked
     */
    public Instant getLockedUntil() {
        return lockedUntil;
    }

    /**
     * Returns the remaining lockout time in seconds.
     *
     * @return seconds until unlock, or 0 if already unlocked
     */
    public long getRemainingLockoutSeconds() {
        if (lockedUntil == null) {
            return 0;
        }
        long remaining = lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}
