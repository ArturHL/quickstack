package com.quickstack.user.dto.response;

import com.quickstack.user.service.SessionService;

import java.time.Instant;

/**
 * Response DTO for session information.
 * <p>
 * Contains session metadata visible to the user.
 * Never includes the actual refresh token or its hash.
 */
public record SessionResponse(
        String id,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant expiresAt
) {
    public static SessionResponse from(SessionService.SessionInfo info) {
        return new SessionResponse(
                info.id().toString(),
                info.ipAddress(),
                info.userAgent(),
                info.createdAt(),
                info.expiresAt()
        );
    }
}
