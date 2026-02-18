package com.quickstack.app.controller;

import com.quickstack.app.security.JwtAuthenticationFilter.JwtAuthenticationPrincipal;
import com.quickstack.common.dto.ApiResponse;
import com.quickstack.user.dto.response.SessionResponse;
import com.quickstack.user.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user management operations.
 * <p>
 * Endpoints:
 * - GET  /api/v1/users/me/sessions  - List active sessions
 * - DELETE /api/v1/users/me/sessions/{id} - Revoke a specific session
 * <p>
 * ASVS Compliance:
 * - V3.3.1: Users can view active sessions
 * - V3.3.4: Users can log out of all sessions
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final SessionService sessionService;

    public UserController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Lists all active sessions for the authenticated user.
     *
     * @param principal the authenticated user's principal from JWT
     * @return list of active session metadata
     */
    @GetMapping("/me/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal
    ) {
        log.debug("Listing sessions for user {}", principal.userId());

        List<SessionResponse> sessions = sessionService.getActiveSessions(principal.userId())
                .stream()
                .map(SessionResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Revokes a specific session for the authenticated user.
     * <p>
     * Verifies that the session belongs to the authenticated user.
     *
     * @param principal the authenticated user's principal from JWT
     * @param id        the session ID to revoke
     * @return 204 No Content on success
     */
    @DeleteMapping("/me/sessions/{id}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable UUID id
    ) {
        log.info("User {} revoking session {}", principal.userId(), id);

        sessionService.revokeSession(principal.userId(), id);

        return ResponseEntity.noContent().build();
    }
}
