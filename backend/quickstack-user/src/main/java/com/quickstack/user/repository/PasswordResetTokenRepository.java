package com.quickstack.user.repository;

import com.quickstack.user.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for password reset tokens.
 * <p>
 * ASVS Compliance:
 * - V2.5.1: Secure password recovery
 * - V2.5.2: Token single-use enforcement
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Find a token by its hash.
     * Used for validating reset tokens.
     *
     * @param tokenHash SHA-256 hash of the token
     * @return the token if found
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Find valid (not used and not expired) token by hash.
     *
     * @param tokenHash SHA-256 hash of the token
     * @param now current time for expiration check
     * @return the token if valid
     */
    @Query("""
        SELECT t FROM PasswordResetToken t
        WHERE t.tokenHash = :tokenHash
        AND t.usedAt IS NULL
        AND t.expiresAt > :now
        """)
    Optional<PasswordResetToken> findValidToken(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now
    );

    /**
     * Delete all expired tokens.
     * Should be run periodically to clean up old tokens.
     *
     * @param expirationTime tokens expiring before this time will be deleted
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :expirationTime")
    int deleteExpiredTokens(@Param("expirationTime") Instant expirationTime);

    /**
     * Delete all tokens for a specific user.
     * Used when initiating a new reset (invalidate previous tokens).
     *
     * @param userId the user ID
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * Check if user has a valid pending reset token.
     *
     * @param userId the user ID
     * @param now current time
     * @return true if user has a valid token
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM PasswordResetToken t
        WHERE t.userId = :userId
        AND t.usedAt IS NULL
        AND t.expiresAt > :now
        """)
    boolean hasValidToken(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Count valid tokens for a user in a time window.
     * Used for rate limiting password reset requests.
     *
     * @param userId the user ID
     * @param since start of time window
     * @return count of tokens created since the given time
     */
    @Query("""
        SELECT COUNT(t) FROM PasswordResetToken t
        WHERE t.userId = :userId
        AND t.createdAt >= :since
        """)
    long countTokensSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
