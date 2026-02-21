package com.quickstack.common.security;

import com.quickstack.common.exception.PasswordCompromisedException;
import com.quickstack.common.exception.PasswordValidationException;

/**
 * Interface for checking passwords against breach databases.
 * <p>
 * Implemented by HibpClient in quickstack-app module.
 * This interface allows UserService in quickstack-user to use
 * breach checking without depending on quickstack-app.
 * <p>
 * ASVS V2.1.7: Passwords must be checked against known breaches.
 */
@FunctionalInterface
public interface PasswordBreachChecker {

    /**
     * Check if a password has been found in known data breaches.
     *
     * @param password the password to check (never logged or stored)
     * @throws PasswordCompromisedException if password found in breach database
     * @throws PasswordValidationException if breach check fails and blocking is enabled
     */
    void checkPassword(String password);

    /**
     * No-op implementation for testing or when breach checking is disabled.
     */
    static PasswordBreachChecker noop() {
        return password -> {};
    }
}
