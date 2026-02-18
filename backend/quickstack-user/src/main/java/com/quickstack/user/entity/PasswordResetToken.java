package com.quickstack.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Password reset token entity for secure password recovery.
 * <p>
 * Security features:
 * - Token stored as SHA-256 hash (never plaintext)
 * - Single-use (usedAt marks token as consumed)
 * - Time-limited (1 hour default)
 * <p>
 * ASVS Compliance:
 * - V2.5.1: Secure password recovery
 * - V2.5.2: Token single-use
 * - V2.5.4: Token time-limited
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_ip")
    private String createdIp;

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Check if the token is valid (not expired and not used).
     */
    public boolean isValid() {
        return usedAt == null && Instant.now().isBefore(expiresAt);
    }

    /**
     * Check if the token has been used.
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Check if the token has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Mark the token as used.
     */
    public void markAsUsed() {
        this.usedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Builder-style factory method
    // -------------------------------------------------------------------------

    /**
     * Create a new password reset token.
     *
     * @param userId the user ID
     * @param tokenHash SHA-256 hash of the token
     * @param expiresAt expiration time
     * @param createdIp IP address that requested the reset
     * @return new PasswordResetToken
     */
    public static PasswordResetToken create(
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            String createdIp
    ) {
        PasswordResetToken token = new PasswordResetToken();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.createdIp = createdIp;
        return token;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedIp() {
        return createdIp;
    }

    public void setCreatedIp(String createdIp) {
        this.createdIp = createdIp;
    }
}
