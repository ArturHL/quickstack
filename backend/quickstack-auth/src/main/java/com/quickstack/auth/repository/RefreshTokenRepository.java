package com.quickstack.auth.repository;

import com.quickstack.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity operations.
 * <p>
 * Provides queries for token validation, rotation, and revocation.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find a valid (non-revoked, non-expired) refresh token by its hash.
     */
    @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.tokenHash = :tokenHash
        AND rt.revokedAt IS NULL
        AND rt.expiresAt > :now
        """)
    Optional<RefreshToken> findValidByTokenHash(
        @Param("tokenHash") String tokenHash,
        @Param("now") Instant now
    );

    /**
     * Find a refresh token by hash (including revoked/expired for reuse detection).
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all tokens in a family (for reuse detection and family revocation).
     */
    List<RefreshToken> findByFamilyId(UUID familyId);

    /**
     * Find all active (non-revoked) tokens for a user.
     * Used for listing sessions.
     */
    @Query("""
        SELECT rt FROM RefreshToken rt
        WHERE rt.userId = :userId
        AND rt.revokedAt IS NULL
        AND rt.expiresAt > :now
        ORDER BY rt.createdAt DESC
        """)
    List<RefreshToken> findActiveByUserId(
        @Param("userId") UUID userId,
        @Param("now") Instant now
    );

    /**
     * Revoke all tokens in a family (for reuse detection).
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :now, rt.revokedReason = :reason
        WHERE rt.familyId = :familyId
        AND rt.revokedAt IS NULL
        """)
    int revokeFamily(
        @Param("familyId") UUID familyId,
        @Param("now") Instant now,
        @Param("reason") String reason
    );

    /**
     * Revoke all tokens for a user (logout from all devices).
     */
    @Modifying
    @Query("""
        UPDATE RefreshToken rt
        SET rt.revokedAt = :now, rt.revokedReason = :reason
        WHERE rt.userId = :userId
        AND rt.revokedAt IS NULL
        """)
    int revokeAllByUserId(
        @Param("userId") UUID userId,
        @Param("now") Instant now,
        @Param("reason") String reason
    );

    /**
     * Count active sessions for a user.
     */
    @Query("""
        SELECT COUNT(rt) FROM RefreshToken rt
        WHERE rt.userId = :userId
        AND rt.revokedAt IS NULL
        AND rt.expiresAt > :now
        """)
    long countActiveByUserId(
        @Param("userId") UUID userId,
        @Param("now") Instant now
    );

    /**
     * Delete expired and revoked tokens (cleanup job).
     * Tokens are deleted after 30 days of being expired/revoked.
     */
    @Modifying
    @Query("""
        DELETE FROM RefreshToken rt
        WHERE (rt.expiresAt < :cutoff)
        OR (rt.revokedAt IS NOT NULL AND rt.revokedAt < :cutoff)
        """)
    int deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
