package com.quickstack.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Login attempt entity for security auditing and rate limiting.
 * <p>
 * Security features:
 * - Tracks both successful and failed login attempts
 * - Records IP address for rate limiting
 * - Records failure reasons for analysis
 * <p>
 * ASVS Compliance:
 * - V2.2.1: Anti-automation controls
 * - V7.1: Security event logging
 * - V2.2: Account lockout tracking
 */
@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Failure reason constants
    // -------------------------------------------------------------------------

    public static final String REASON_INVALID_CREDENTIALS = "invalid_credentials";
    public static final String REASON_ACCOUNT_LOCKED = "account_locked";
    public static final String REASON_ACCOUNT_INACTIVE = "account_inactive";
    public static final String REASON_EMAIL_NOT_VERIFIED = "email_not_verified";
    public static final String REASON_RATE_LIMITED = "rate_limited";
    public static final String REASON_USER_NOT_FOUND = "user_not_found";

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Create a successful login attempt record.
     */
    public static LoginAttempt successful(
            String email,
            UUID userId,
            UUID tenantId,
            String ipAddress,
            String userAgent
    ) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.email = email;
        attempt.userId = userId;
        attempt.tenantId = tenantId;
        attempt.success = true;
        attempt.ipAddress = ipAddress;
        attempt.userAgent = userAgent;
        return attempt;
    }

    /**
     * Create a failed login attempt record.
     */
    public static LoginAttempt failed(
            String email,
            UUID userId,
            UUID tenantId,
            String ipAddress,
            String userAgent,
            String failureReason
    ) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.email = email;
        attempt.userId = userId;
        attempt.tenantId = tenantId;
        attempt.success = false;
        attempt.failureReason = failureReason;
        attempt.ipAddress = ipAddress;
        attempt.userAgent = userAgent;
        return attempt;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
