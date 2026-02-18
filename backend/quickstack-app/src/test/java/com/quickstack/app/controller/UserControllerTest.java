package com.quickstack.app.controller;

import com.quickstack.app.security.JwtAuthenticationFilter.JwtAuthenticationPrincipal;
import com.quickstack.user.dto.response.SessionResponse;
import com.quickstack.user.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    @Mock
    private SessionService sessionService;

    private UserController userController;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal principal;

    @BeforeEach
    void setUp() {
        userController = new UserController(sessionService);
        principal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "user@example.com");
    }

    @Nested
    @DisplayName("GET /me/sessions")
    class GetSessionsTests {

        @Test
        @DisplayName("should return list of active sessions")
        void shouldReturnActiveSessions() {
            UUID sessionId = UUID.randomUUID();
            SessionService.SessionInfo sessionInfo = new SessionService.SessionInfo(
                    sessionId, "192.168.1.1", "Mozilla/5.0",
                    Instant.parse("2026-02-18T09:00:00Z"),
                    Instant.parse("2026-02-25T09:00:00Z")
            );
            when(sessionService.getActiveSessions(USER_ID)).thenReturn(List.of(sessionInfo));

            ResponseEntity<?> response = userController.getSessions(principal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should return empty list when no active sessions")
        void shouldReturnEmptyList() {
            when(sessionService.getActiveSessions(USER_ID)).thenReturn(List.of());

            ResponseEntity<?> response = userController.getSessions(principal);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("should use userId from principal to fetch sessions")
        void shouldUseUserIdFromPrincipal() {
            when(sessionService.getActiveSessions(USER_ID)).thenReturn(List.of());

            userController.getSessions(principal);

            verify(sessionService).getActiveSessions(USER_ID);
        }
    }

    @Nested
    @DisplayName("DELETE /me/sessions/{id}")
    class RevokeSessionTests {

        @Test
        @DisplayName("should return 204 when session is revoked successfully")
        void shouldReturn204OnSuccess() {
            UUID sessionId = UUID.randomUUID();
            doNothing().when(sessionService).revokeSession(USER_ID, sessionId);

            ResponseEntity<Void> response = userController.revokeSession(principal, sessionId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("should delegate revocation with correct userId and sessionId")
        void shouldDelegateWithCorrectIds() {
            UUID sessionId = UUID.randomUUID();
            doNothing().when(sessionService).revokeSession(USER_ID, sessionId);

            userController.revokeSession(principal, sessionId);

            verify(sessionService).revokeSession(USER_ID, sessionId);
        }

        @Test
        @DisplayName("should propagate SessionNotFoundException when session not found")
        void shouldPropagateNotFound() {
            UUID sessionId = UUID.randomUUID();
            doThrow(new SessionService.SessionNotFoundException(sessionId))
                    .when(sessionService).revokeSession(USER_ID, sessionId);

            assertThatThrownBy(() -> userController.revokeSession(principal, sessionId))
                    .isInstanceOf(SessionService.SessionNotFoundException.class);
        }

        @Test
        @DisplayName("should propagate SessionAccessDeniedException when session belongs to another user")
        void shouldPropagateAccessDenied() {
            UUID sessionId = UUID.randomUUID();
            doThrow(new SessionService.SessionAccessDeniedException())
                    .when(sessionService).revokeSession(USER_ID, sessionId);

            assertThatThrownBy(() -> userController.revokeSession(principal, sessionId))
                    .isInstanceOf(SessionService.SessionAccessDeniedException.class);
        }
    }
}
