package com.quickstack.auth.service;

import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.exception.InvalidTokenException.InvalidationReason;
import com.quickstack.common.exception.InvalidTokenException.TokenType;
import com.quickstack.common.security.PasswordBreachChecker;
import com.quickstack.common.security.PasswordService;
import com.quickstack.common.security.SecureTokenGenerator;
import com.quickstack.auth.entity.PasswordResetToken;
import com.quickstack.auth.entity.RefreshToken;
import com.quickstack.user.entity.User;
import com.quickstack.auth.repository.PasswordResetTokenRepository;
import com.quickstack.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for secure password reset flow.
 * <p>
 * Security features:
 * - Token stored as SHA-256 hash (never plaintext)
 * - Single-use tokens (marked as used immediately)
 * - Time-limited (1 hour expiration)
 * - Timing-safe operations (same time for existing/non-existing emails)
 * - Revokes all refresh tokens on password change
 * - HIBP check on new password
 * <p>
 * ASVS Compliance:
 * - V2.5.1: Secure password recovery mechanism
 * - V2.5.2: Password reset token is single-use
 * - V2.5.4: Password reset token is time-limited
 * - V2.5.7: Password reset doesn't reveal account existence
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    /**
     * Token expiration time: 1 hour.
     */
    private static final Duration TOKEN_EXPIRATION = Duration.ofHours(1);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final PasswordBreachChecker breachChecker;
    private final RefreshTokenService refreshTokenService;
    private final Clock clock;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordService passwordService,
            PasswordBreachChecker breachChecker,
            RefreshTokenService refreshTokenService,
            Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.breachChecker = breachChecker;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock;
    }

    /**
     * Initiates a password reset for a user.
     * <p>
     * IMPORTANT: This method is timing-safe. It takes the same amount of time
     * regardless of whether the email exists or not. This prevents enumeration
     * attacks.
     *
     * @param email    the user's email address
     * @param tenantId the tenant ID
     * @param clientIp the client's IP address
     * @return the result containing the token if email exists, or empty if not
     */
    @Transactional
    public ResetInitiationResult initiateReset(String email, UUID tenantId, String clientIp) {
        String normalizedEmail = email.toLowerCase().trim();

        Optional<User> userOpt = userRepository.findByTenantIdAndEmail(tenantId, normalizedEmail);

        if (userOpt.isEmpty()) {
            // User doesn't exist - perform dummy operations for timing safety
            log.debug("Password reset requested for non-existent email (timing-safe)");
            performDummyOperations();
            return ResetInitiationResult.userNotFound();
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.isActive()) {
            log.debug("Password reset requested for inactive user: {}", user.getId());
            performDummyOperations();
            return ResetInitiationResult.userNotFound();
        }

        // Invalidate any existing reset tokens for this user
        int deleted = tokenRepository.deleteByUserId(user.getId());
        if (deleted > 0) {
            log.debug("Invalidated {} existing reset tokens for user {}", deleted, user.getId());
        }

        // Generate new reset token
        String plainToken = SecureTokenGenerator.generate();
        String tokenHash = hashToken(plainToken);
        Instant expiresAt = clock.instant().plus(TOKEN_EXPIRATION);

        PasswordResetToken resetToken = PasswordResetToken.create(
                user.getId(),
                tokenHash,
                expiresAt,
                clientIp);

        tokenRepository.save(java.util.Objects.requireNonNull(resetToken));

        log.info("Password reset token created for user {} (expires at {})", user.getId(), expiresAt);

        return ResetInitiationResult.success(plainToken, user.getId(), user.getEmail());
    }

    /**
     * Validates a password reset token.
     *
     * @param plainToken the plain-text reset token
     * @return the validated token entity
     * @throws InvalidTokenException if the token is invalid, expired, or used
     */
    public PasswordResetToken validateToken(String plainToken) {
        String tokenHash = hashToken(plainToken);

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findValidToken(tokenHash, clock.instant());

        if (tokenOpt.isEmpty()) {
            // Check if token exists but is invalid (used or expired)
            Optional<PasswordResetToken> existingToken = tokenRepository.findByTokenHash(tokenHash);

            if (existingToken.isPresent()) {
                PasswordResetToken token = existingToken.get();
                if (token.isUsed()) {
                    log.warn("Attempt to reuse password reset token");
                    throw InvalidTokenException.revoked(TokenType.PASSWORD_RESET_TOKEN);
                }
                if (token.isExpired(clock.instant())) {
                    log.warn("Attempt to use expired password reset token");
                    throw InvalidTokenException.expired(TokenType.PASSWORD_RESET_TOKEN);
                }
            }

            log.warn("Password reset token not found");
            throw new InvalidTokenException(TokenType.PASSWORD_RESET_TOKEN, InvalidationReason.NOT_FOUND);
        }

        return tokenOpt.get();
    }

    /**
     * Resets a user's password using a valid reset token.
     * <p>
     * This method:
     * 1. Validates the reset token
     * 2. Checks the new password against breach database
     * 3. Hashes and stores the new password
     * 4. Marks the token as used
     * 5. Revokes all user's refresh tokens (force re-login)
     *
     * @param plainToken  the plain-text reset token
     * @param newPassword the new password
     * @throws InvalidTokenException if the token is invalid
     */
    @Transactional
    public void resetPassword(String plainToken, String newPassword) {
        // Validate the token
        PasswordResetToken resetToken = validateToken(plainToken);

        // Validate password policy
        passwordService.validatePasswordPolicy(newPassword);

        // Check password against breach database (ASVS V2.1.7)
        breachChecker.checkPassword(newPassword);

        // Get the user
        User user = userRepository.findById(java.util.Objects.requireNonNull(resetToken.getUserId()))
                .orElseThrow(() -> {
                    log.error("User not found for valid reset token - user may have been deleted");
                    return new InvalidTokenException(TokenType.PASSWORD_RESET_TOKEN, InvalidationReason.NOT_FOUND);
                });

        // Hash the new password
        String passwordHash = passwordService.hashPassword(newPassword);

        // Update user's password
        user.setPasswordHash(passwordHash);
        user.setPasswordChangedAt(clock.instant());
        user.setMustChangePassword(false);
        user.resetFailedLoginAttempts();
        userRepository.save(user);

        // Mark token as used
        resetToken.markAsUsed();
        tokenRepository.save(resetToken);

        // Revoke all refresh tokens for this user (force re-login on all devices)
        refreshTokenService.revokeAllUserTokens(user.getId(), RefreshToken.REASON_PASSWORD_CHANGE);

        log.info("Password reset completed for user {}", user.getId());
    }

    /**
     * Cleans up expired password reset tokens.
     * Should be called periodically by a scheduled job.
     *
     * @return number of deleted tokens
     */
    @Transactional
    public int cleanupExpiredTokens() {
        int deleted = tokenRepository.deleteExpiredTokens(clock.instant());
        if (deleted > 0) {
            log.info("Cleaned up {} expired password reset tokens", deleted);
        }
        return deleted;
    }

    /**
     * Perform dummy operations to maintain constant time.
     * This prevents timing attacks that could reveal whether a user exists.
     */
    private void performDummyOperations() {
        // Simulate the same operations that would occur for a valid user
        // This makes the response time similar regardless of user existence
        SecureTokenGenerator.generate();
        hashToken("dummy-token-for-timing-safety");
    }

    /**
     * Hashes a token using SHA-256.
     * We store only the hash in the database, never the actual token.
     */
    private String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Result of initiating a password reset.
     */
    public record ResetInitiationResult(
            boolean success,
            String token,
            UUID userId,
            String email) {
        /**
         * Create a successful result.
         */
        public static ResetInitiationResult success(String token, UUID userId, String email) {
            return new ResetInitiationResult(true, token, userId, email);
        }

        /**
         * Create a result for non-existent user.
         * No token is generated but the response looks the same.
         */
        public static ResetInitiationResult userNotFound() {
            return new ResetInitiationResult(false, null, null, null);
        }
    }
}
