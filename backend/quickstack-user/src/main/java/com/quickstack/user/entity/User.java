package com.quickstack.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * User entity for authentication and authorization.
 * <p>
 * Security features:
 * - Password stored as Argon2id hash (never plaintext)
 * - Account lockout after failed login attempts
 * - Email verification support
 * - Multi-tenant isolation via tenant_id
 * <p>
 * ASVS Compliance:
 * - V2.4: Credential storage with Argon2id
 * - V2.2: Account lockout (failed_login_attempts, locked_until)
 * - V2.1: Email verification
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    // Identity
    @Column(nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    // Authentication
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    // Account Security
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_failed_login_at")
    private Instant lastFailedLoginAt;

    // Email Verification
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "email_verification_expires_at")
    private Instant emailVerificationExpiresAt;

    // Status
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.passwordChangedAt == null) {
            this.passwordChangedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Check if the account is currently locked.
     */
    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    /**
     * Check if the account can be used for login.
     */
    public boolean canLogin() {
        return active && !isLocked() && deletedAt == null;
    }

    /**
     * Record a failed login attempt.
     *
     * @param lockoutThreshold number of failed attempts before lockout
     * @param lockoutDurationSeconds how long to lock the account
     */
    public void recordFailedLogin(int lockoutThreshold, long lockoutDurationSeconds) {
        this.failedLoginAttempts++;
        this.lastFailedLoginAt = Instant.now();

        if (this.failedLoginAttempts >= lockoutThreshold) {
            this.lockedUntil = Instant.now().plusSeconds(lockoutDurationSeconds);
        }
    }

    /**
     * Reset failed login attempts on successful login.
     */
    public void recordSuccessfulLogin(String ipAddress) {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ipAddress;
    }

    /**
     * Soft delete the user.
     */
    public void softDelete(UUID deletedByUserId) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUserId;
        this.active = false;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getLastFailedLoginAt() {
        return lastFailedLoginAt;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public Instant getEmailVerificationExpiresAt() {
        return emailVerificationExpiresAt;
    }

    public void setEmailVerificationExpiresAt(Instant emailVerificationExpiresAt) {
        this.emailVerificationExpiresAt = emailVerificationExpiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }
}
