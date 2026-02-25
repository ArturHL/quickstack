package com.quickstack.auth.service;

import com.quickstack.common.config.properties.JwtProperties;
import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.exception.InvalidTokenException.InvalidationReason;
import com.quickstack.common.exception.InvalidTokenException.TokenType;
import com.quickstack.common.security.SecureTokenGenerator;
import com.quickstack.auth.entity.RefreshToken;
import com.quickstack.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing refresh tokens with rotation and reuse detection.
 * <p>
 * Security features:
 * - Token stored as SHA-256 hash (never plaintext)
 * - Token rotation on each refresh (ASVS V3.5)
 * - Family tracking for detecting token reuse attacks
 * - Automatic revocation of entire family on reuse detection
 * <p>
 * ASVS Compliance:
 * - V3.5.1: Refresh token rotation
 * - V3.5.2: Token binding to user session
 * - V2.2.6: Replay resistance via family tracking
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    /**
     * Creates a new refresh token for a user.
     * <p>
     * A new family ID is generated for initial token creation.
     * Subsequent rotations maintain the same family ID.
     *
     * @param userId    the user ID
     * @param ipAddress client IP address
     * @param userAgent client user agent
     * @return the generated plain-text refresh token (store securely!)
     */
    @Transactional
    public String createRefreshToken(UUID userId, String ipAddress, String userAgent) {
        return createRefreshToken(userId, UUID.randomUUID(), ipAddress, userAgent);
    }

    /**
     * Creates a new refresh token with a specific family ID.
     * Used for token rotation to maintain family tracking.
     */
    @Transactional
    public String createRefreshToken(UUID userId, UUID familyId, String ipAddress, String userAgent) {
        String plainToken = SecureTokenGenerator.generate();
        String tokenHash = hashToken(plainToken);
        Instant expiresAt = clock.instant().plus(jwtProperties.getRefreshTokenExpiration());

        RefreshToken refreshToken = RefreshToken.create(
                userId, tokenHash, familyId, expiresAt, ipAddress, userAgent);

        refreshTokenRepository.save(java.util.Objects.requireNonNull(refreshToken));

        log.debug("Created refresh token for user {} with family {}", userId, familyId);
        return plainToken;
    }

    /**
     * Validates a refresh token and returns the associated entity.
     * <p>
     * This method detects token reuse attacks:
     * - If a token is found but already revoked, the entire family is invalidated
     * - This protects against attackers who have captured old tokens
     *
     * @param plainToken the plain-text refresh token
     * @return the validated RefreshToken entity
     * @throws InvalidTokenException if the token is invalid, expired, or revoked
     */
    public RefreshToken validateToken(String plainToken) {
        String tokenHash = hashToken(plainToken);

        // First, check if this token exists at all (including revoked)
        var existingToken = refreshTokenRepository.findByTokenHash(tokenHash);

        if (existingToken.isEmpty()) {
            log.warn("Refresh token not found");
            throw new InvalidTokenException(TokenType.REFRESH_TOKEN, InvalidationReason.NOT_FOUND);
        }

        RefreshToken token = existingToken.get();

        // Check for token reuse attack
        if (token.isRevoked()) {
            log.error("SECURITY ALERT: Refresh token reuse detected! Family: {}", token.getFamilyId());
            // Revoke entire family - someone is trying to use an old token
            revokeFamily(token.getFamilyId(), RefreshToken.REASON_SUSPICIOUS_REUSE);
            throw InvalidTokenException.revoked(TokenType.REFRESH_TOKEN);
        }

        // Check expiration
        if (token.isExpired(clock.instant())) {
            log.debug("Refresh token expired for family {}", token.getFamilyId());
            throw InvalidTokenException.expired(TokenType.REFRESH_TOKEN);
        }

        return token;
    }

    /**
     * Rotates a refresh token.
     * <p>
     * The old token is revoked and a new token is generated with the same family
     * ID.
     * This implements ASVS V3.5.1 refresh token rotation.
     *
     * @param plainToken the current plain-text refresh token
     * @param ipAddress  new client IP address
     * @param userAgent  new client user agent
     * @return result containing the new token and user ID
     * @throws InvalidTokenException if the token is invalid
     */
    @Transactional
    public RotationResult rotateToken(String plainToken, String ipAddress, String userAgent) {
        RefreshToken currentToken = validateToken(plainToken);

        // Revoke the current token
        currentToken.revoke(RefreshToken.REASON_ROTATED);
        refreshTokenRepository.save(currentToken);

        // Create new token in the same family
        String newPlainToken = createRefreshToken(
                currentToken.getUserId(),
                currentToken.getFamilyId(),
                ipAddress,
                userAgent);

        log.debug("Rotated refresh token for user {} in family {}",
                currentToken.getUserId(), currentToken.getFamilyId());

        return new RotationResult(newPlainToken, currentToken.getUserId());
    }

    /**
     * Revokes a specific refresh token (e.g., on logout).
     *
     * @param plainToken the plain-text token to revoke
     * @param reason     the revocation reason
     */
    @Transactional
    public void revokeToken(String plainToken, String reason) {
        String tokenHash = hashToken(plainToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke(reason);
                refreshTokenRepository.save(token);
                log.info("Revoked refresh token for user {} - reason: {}", token.getUserId(), reason);
            }
        });
    }

    /**
     * Revokes an entire token family.
     * <p>
     * Used when detecting token reuse attacks or on password change.
     *
     * @param familyId the family ID to revoke
     * @param reason   the revocation reason
     */
    @Transactional
    public void revokeFamily(UUID familyId, String reason) {
        int revoked = refreshTokenRepository.revokeFamily(familyId, clock.instant(), reason);
        log.info("Revoked {} tokens in family {} - reason: {}", revoked, familyId, reason);
    }

    /**
     * Revokes all refresh tokens for a user (logout from all devices).
     *
     * @param userId the user ID
     * @param reason the revocation reason
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId, String reason) {
        int revoked = refreshTokenRepository.revokeAllByUserId(userId, clock.instant(), reason);
        log.info("Revoked {} refresh tokens for user {} - reason: {}", revoked, userId, reason);
    }

    /**
     * Gets all active sessions for a user.
     *
     * @param userId the user ID
     * @return list of active refresh tokens (metadata only, no actual tokens)
     */
    public List<RefreshToken> getActiveSessions(UUID userId) {
        return refreshTokenRepository.findActiveByUserId(userId, clock.instant());
    }

    /**
     * Counts active sessions for a user.
     *
     * @param userId the user ID
     * @return number of active sessions
     */
    public long countActiveSessions(UUID userId) {
        return refreshTokenRepository.countActiveByUserId(userId, clock.instant());
    }

    /**
     * Cleans up expired and revoked tokens.
     * Should be called periodically by a scheduled job.
     *
     * @param retentionDays days to keep old tokens for auditing
     * @return number of deleted tokens
     */
    @Transactional
    public int cleanupExpiredTokens(int retentionDays) {
        Instant cutoff = clock.instant().minusSeconds(retentionDays * 86400L);
        int deleted = refreshTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Cleaned up {} expired/revoked refresh tokens", deleted);
        return deleted;
    }

    /**
     * Hashes a token using SHA-256.
     * <p>
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
     * Result of a token rotation operation.
     */
    public record RotationResult(String newToken, UUID userId) {
    }
}
