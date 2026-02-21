package com.quickstack.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token entity for session management.
 * <p>
 * Security features:
 * - Token stored as SHA-256 hash (never plaintext)
 * - Family ID for detecting token reuse attacks
 * - Soft revocation with reason tracking
 * <p>
 * ASVS Compliance:
 * - V3.5: Refresh token rotation
 * - V3.3: Token revocation
 * - V2.2.6: Replay resistance via family tracking
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Revocation reasons (constants)
    // -------------------------------------------------------------------------

    public static final String REASON_ROTATED = "rotated";
    public static final String REASON_LOGOUT = "logout";
    public static final String REASON_PASSWORD_CHANGE = "password_change";
    public static final String REASON_SUSPICIOUS_REUSE = "suspicious_reuse";
    public static final String REASON_ADMIN_REVOKED = "admin_revoked";

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
     * Check if the token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return revokedAt == null && Instant.now().isBefore(expiresAt);
    }

    /**
     * Check if the token has been revoked.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Check if the token has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Revoke this token with a reason.
     */
    public void revoke(String reason) {
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }

    // -------------------------------------------------------------------------
    // Builder-style factory method
    // -------------------------------------------------------------------------

    /**
     * Create a new refresh token.
     */
    public static RefreshToken create(
            UUID userId,
            String tokenHash,
            UUID familyId,
            Instant expiresAt,
            String ipAddress,
            String userAgent
    ) {
        RefreshToken token = new RefreshToken();
        token.userId = userId;
        token.tokenHash = tokenHash;
        token.familyId = familyId;
        token.expiresAt = expiresAt;
        token.ipAddress = ipAddress;
        token.userAgent = userAgent;
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

    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
