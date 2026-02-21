package com.quickstack.auth.repository;

import com.quickstack.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LoginAttempt entity operations.
 * <p>
 * Provides queries for rate limiting and security analysis.
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Count failed login attempts for an email within a time window.
     * Used for account lockout detection.
     */
    @Query("""
        SELECT COUNT(la) FROM LoginAttempt la
        WHERE la.email = :email
        AND la.tenantId = :tenantId
        AND la.success = false
        AND la.createdAt > :since
        """)
    long countFailedAttemptsByEmailSince(
        @Param("email") String email,
        @Param("tenantId") UUID tenantId,
        @Param("since") Instant since
    );

    /**
     * Count failed login attempts from an IP address within a time window.
     * Used for IP-based rate limiting.
     */
    @Query("""
        SELECT COUNT(la) FROM LoginAttempt la
        WHERE la.ipAddress = :ipAddress
        AND la.success = false
        AND la.createdAt > :since
        """)
    long countFailedAttemptsByIpSince(
        @Param("ipAddress") String ipAddress,
        @Param("since") Instant since
    );

    /**
     * Get the most recent failed login attempt for an email.
     * Used to determine lockout start time.
     */
    @Query("""
        SELECT la FROM LoginAttempt la
        WHERE la.email = :email
        AND la.tenantId = :tenantId
        AND la.success = false
        ORDER BY la.createdAt DESC
        LIMIT 1
        """)
    LoginAttempt findMostRecentFailedAttempt(
        @Param("email") String email,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Get recent login attempts for a user (for security audit).
     */
    @Query("""
        SELECT la FROM LoginAttempt la
        WHERE la.userId = :userId
        ORDER BY la.createdAt DESC
        """)
    List<LoginAttempt> findRecentByUserId(
        @Param("userId") UUID userId,
        org.springframework.data.domain.Pageable pageable
    );

    /**
     * Check if there was a successful login after last failed attempt.
     * Used to determine if lockout counter should reset.
     */
    @Query("""
        SELECT COUNT(la) > 0 FROM LoginAttempt la
        WHERE la.email = :email
        AND la.tenantId = :tenantId
        AND la.success = true
        AND la.createdAt > :since
        """)
    boolean hasSuccessfulLoginSince(
        @Param("email") String email,
        @Param("tenantId") UUID tenantId,
        @Param("since") Instant since
    );

    /**
     * Get failed attempts for an email within a time window (for analysis).
     */
    @Query("""
        SELECT la FROM LoginAttempt la
        WHERE la.email = :email
        AND la.tenantId = :tenantId
        AND la.success = false
        AND la.createdAt > :since
        ORDER BY la.createdAt DESC
        """)
    List<LoginAttempt> findFailedAttemptsByEmailSince(
        @Param("email") String email,
        @Param("tenantId") UUID tenantId,
        @Param("since") Instant since
    );

    /**
     * Delete old login attempts (cleanup job).
     * Keep attempts for 90 days for security analysis.
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.createdAt < :cutoff")
    int deleteOldAttempts(@Param("cutoff") Instant cutoff);
}
