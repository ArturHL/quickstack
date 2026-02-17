package com.quickstack.user.service;

import com.quickstack.common.config.properties.RateLimitProperties;
import com.quickstack.common.exception.AccountLockedException;
import com.quickstack.user.entity.LoginAttempt;
import com.quickstack.user.repository.LoginAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for tracking and enforcing login attempt limits.
 * <p>
 * Security features:
 * - Account lockout after N failed attempts (default: 5)
 * - Auto-unlock after lockout duration (default: 15 minutes)
 * - Failed attempt tracking with time window
 * - IP-based rate limiting support
 * <p>
 * ASVS Compliance:
 * - V2.2.1: Anti-automation controls (account lockout)
 * - V7.1.1: Login failure logging
 */
@Service
@Transactional(readOnly = true)
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final LoginAttemptRepository loginAttemptRepository;
    private final RateLimitProperties rateLimitProperties;
    private final Clock clock;

    public LoginAttemptService(
            LoginAttemptRepository loginAttemptRepository,
            RateLimitProperties rateLimitProperties,
            Clock clock
    ) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.rateLimitProperties = rateLimitProperties;
        this.clock = clock;
    }

    /**
     * Check if the account is locked due to too many failed attempts.
     *
     * @param email the email being authenticated
     * @param tenantId the tenant context
     * @throws AccountLockedException if the account is currently locked
     */
    public void checkAccountLock(String email, UUID tenantId) {
        if (!rateLimitProperties.getLockout().isEnabled()) {
            return;
        }

        RateLimitProperties.LockoutConfig lockoutConfig = rateLimitProperties.getLockout();
        Instant windowStart = clock.instant().minus(lockoutConfig.getAttemptWindow());

        long failedAttempts = loginAttemptRepository.countFailedAttemptsByEmailSince(
                email, tenantId, windowStart
        );

        if (failedAttempts >= lockoutConfig.getMaxAttempts()) {
            // Check if there was a successful login since the failed attempts
            if (loginAttemptRepository.hasSuccessfulLoginSince(email, tenantId, windowStart)) {
                // Reset the counter implicitly - a successful login clears the lockout
                return;
            }

            // Get the most recent failed attempt to calculate lockout expiry
            LoginAttempt lastFailure = loginAttemptRepository.findMostRecentFailedAttempt(email, tenantId);
            if (lastFailure != null) {
                Instant lockedUntil = lastFailure.getCreatedAt().plus(lockoutConfig.getLockoutDuration());

                if (clock.instant().isBefore(lockedUntil)) {
                    log.warn("Account locked for email {} in tenant {} until {}",
                            maskEmail(email), tenantId, lockedUntil);
                    throw new AccountLockedException(lockedUntil);
                }
            }
        }
    }

    /**
     * Record a successful login attempt and reset failed counter.
     *
     * @param email the email that logged in
     * @param userId the user ID
     * @param tenantId the tenant context
     * @param ipAddress the client IP address
     * @param userAgent the client user agent
     */
    @Transactional
    public void recordSuccessfulLogin(
            String email,
            UUID userId,
            UUID tenantId,
            String ipAddress,
            String userAgent
    ) {
        LoginAttempt attempt = LoginAttempt.successful(email, userId, tenantId, ipAddress, userAgent);
        loginAttemptRepository.save(attempt);

        log.info("Successful login for user {} from IP {}", userId, ipAddress);
    }

    /**
     * Record a failed login attempt.
     *
     * @param email the email that failed
     * @param userId the user ID (may be null if user not found)
     * @param tenantId the tenant context (may be null)
     * @param ipAddress the client IP address
     * @param userAgent the client user agent
     * @param failureReason the reason for failure
     * @return the number of failed attempts in the current window
     */
    @Transactional
    public long recordFailedLogin(
            String email,
            UUID userId,
            UUID tenantId,
            String ipAddress,
            String userAgent,
            String failureReason
    ) {
        LoginAttempt attempt = LoginAttempt.failed(
                email, userId, tenantId, ipAddress, userAgent, failureReason
        );
        loginAttemptRepository.save(attempt);

        // Count failed attempts in window
        RateLimitProperties.LockoutConfig lockoutConfig = rateLimitProperties.getLockout();
        Instant windowStart = clock.instant().minus(lockoutConfig.getAttemptWindow());

        long failedAttempts = tenantId != null
            ? loginAttemptRepository.countFailedAttemptsByEmailSince(email, tenantId, windowStart)
            : 0;

        log.warn("Failed login attempt for {} from IP {} - reason: {} - attempts in window: {}",
                maskEmail(email), ipAddress, failureReason, failedAttempts);

        return failedAttempts;
    }

    /**
     * Check if an IP address has exceeded the rate limit.
     *
     * @param ipAddress the IP to check
     * @return true if rate limited
     */
    public boolean isIpRateLimited(String ipAddress) {
        if (!rateLimitProperties.isEnabled()) {
            return false;
        }

        Instant windowStart = clock.instant().minus(rateLimitProperties.getIp().getRefillPeriod());
        long attempts = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, windowStart);

        return attempts >= rateLimitProperties.getIp().getCapacity();
    }

    /**
     * Get the number of remaining login attempts before lockout.
     *
     * @param email the email to check
     * @param tenantId the tenant context
     * @return remaining attempts, or -1 if lockout is disabled
     */
    public int getRemainingAttempts(String email, UUID tenantId) {
        if (!rateLimitProperties.getLockout().isEnabled()) {
            return -1;
        }

        RateLimitProperties.LockoutConfig lockoutConfig = rateLimitProperties.getLockout();
        Instant windowStart = clock.instant().minus(lockoutConfig.getAttemptWindow());

        long failedAttempts = loginAttemptRepository.countFailedAttemptsByEmailSince(
                email, tenantId, windowStart
        );

        return Math.max(0, lockoutConfig.getMaxAttempts() - (int) failedAttempts);
    }

    /**
     * Mask email for logging (privacy protection).
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
