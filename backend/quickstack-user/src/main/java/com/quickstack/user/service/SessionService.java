package com.quickstack.user.service;

import com.quickstack.common.exception.ApiException;
import com.quickstack.user.entity.RefreshToken;
import com.quickstack.user.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user sessions (active refresh tokens).
 * <p>
 * Provides session metadata to users without exposing actual token values.
 * Enforces ownership checks to prevent IDOR attacks.
 * <p>
 * ASVS Compliance:
 * - V3.3.1: Users can view and revoke active sessions
 * - V3.3.4: Users can log out of all sessions
 */
@Service
@Transactional(readOnly = true)
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public SessionService(RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    /**
     * Gets all active sessions for a user.
     * <p>
     * Returns session metadata only - never the actual token hash.
     *
     * @param userId the user ID
     * @return list of active session info
     */
    public List<SessionInfo> getActiveSessions(UUID userId) {
        return refreshTokenRepository.findActiveByUserId(userId, clock.instant())
                .stream()
                .map(SessionInfo::from)
                .toList();
    }

    /**
     * Revokes a specific session.
     * <p>
     * Verifies that the session belongs to the requesting user (IDOR prevention).
     *
     * @param userId    the authenticated user ID
     * @param sessionId the session (refresh token) ID to revoke
     * @throws SessionNotFoundException if session does not exist
     * @throws SessionAccessDeniedException if session belongs to another user
     */
    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (!token.getUserId().equals(userId)) {
            log.warn("SECURITY: User {} attempted to revoke session {} belonging to user {}",
                    userId, sessionId, token.getUserId());
            throw new SessionAccessDeniedException();
        }

        if (!token.isRevoked()) {
            token.revoke(RefreshToken.REASON_LOGOUT);
            refreshTokenRepository.save(token);
            log.info("Session {} revoked by user {}", sessionId, userId);
        }
    }

    /**
     * Revokes all sessions for a user, optionally excluding the current session.
     *
     * @param userId          the user ID
     * @param exceptSessionId session to preserve (e.g., current session), or null to revoke all
     */
    @Transactional
    public void revokeAllSessions(UUID userId, UUID exceptSessionId) {
        List<RefreshToken> active = refreshTokenRepository.findActiveByUserId(userId, clock.instant());

        int revokedCount = 0;
        for (RefreshToken token : active) {
            if (exceptSessionId == null || !token.getId().equals(exceptSessionId)) {
                token.revoke(RefreshToken.REASON_LOGOUT);
                refreshTokenRepository.save(token);
                revokedCount++;
            }
        }

        log.info("Revoked {} sessions for user {} (excluded: {})", revokedCount, userId, exceptSessionId);
    }

    // -------------------------------------------------------------------------
    // Session Info record (no actual token exposed)
    // -------------------------------------------------------------------------

    /**
     * Session metadata for display to the user.
     * Never includes the actual token or its hash.
     */
    public record SessionInfo(
            UUID id,
            String ipAddress,
            String userAgent,
            Instant createdAt,
            Instant expiresAt
    ) {
        public static SessionInfo from(RefreshToken token) {
            return new SessionInfo(
                    token.getId(),
                    token.getIpAddress(),
                    token.getUserAgent(),
                    token.getCreatedAt(),
                    token.getExpiresAt()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static class SessionNotFoundException extends ApiException {
        public SessionNotFoundException(UUID sessionId) {
            super(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found");
        }
    }

    public static class SessionAccessDeniedException extends ApiException {
        public SessionAccessDeniedException() {
            super(HttpStatus.FORBIDDEN, "SESSION_ACCESS_DENIED", "Access to this session is denied");
        }
    }
}
