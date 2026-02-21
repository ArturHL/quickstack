package com.quickstack.auth.service;

import com.quickstack.auth.entity.RefreshToken;
import com.quickstack.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService")
class SessionServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private SessionService sessionService;

    private static final Instant FIXED_NOW = Instant.parse("2026-02-18T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        sessionService = new SessionService(refreshTokenRepository, clock);
    }

    @Nested
    @DisplayName("getActiveSessions")
    class GetActiveSessionsTests {

        @Test
        @DisplayName("should return session info without token hash")
        void shouldReturnSessionInfoWithoutTokenHash() {
            RefreshToken token = createToken(UUID.randomUUID(), USER_ID);
            when(refreshTokenRepository.findActiveByUserId(USER_ID, FIXED_NOW))
                    .thenReturn(List.of(token));

            List<SessionService.SessionInfo> sessions = sessionService.getActiveSessions(USER_ID);

            assertThat(sessions).hasSize(1);
            SessionService.SessionInfo info = sessions.get(0);
            assertThat(info.id()).isEqualTo(token.getId());
            assertThat(info.ipAddress()).isEqualTo(token.getIpAddress());
            assertThat(info.userAgent()).isEqualTo(token.getUserAgent());
            assertThat(info.createdAt()).isEqualTo(token.getCreatedAt());
            assertThat(info.expiresAt()).isEqualTo(token.getExpiresAt());
        }

        @Test
        @DisplayName("should return empty list when no active sessions")
        void shouldReturnEmptyListWhenNoActiveSessions() {
            when(refreshTokenRepository.findActiveByUserId(USER_ID, FIXED_NOW))
                    .thenReturn(List.of());

            List<SessionService.SessionInfo> sessions = sessionService.getActiveSessions(USER_ID);

            assertThat(sessions).isEmpty();
        }

        @Test
        @DisplayName("should return multiple sessions")
        void shouldReturnMultipleSessions() {
            List<RefreshToken> tokens = List.of(
                    createToken(UUID.randomUUID(), USER_ID),
                    createToken(UUID.randomUUID(), USER_ID)
            );
            when(refreshTokenRepository.findActiveByUserId(USER_ID, FIXED_NOW)).thenReturn(tokens);

            List<SessionService.SessionInfo> sessions = sessionService.getActiveSessions(USER_ID);

            assertThat(sessions).hasSize(2);
        }
    }

    @Nested
    @DisplayName("revokeSession")
    class RevokeSessionTests {

        @Test
        @DisplayName("should revoke session when it belongs to the user")
        void shouldRevokeSessionWhenBelongsToUser() {
            UUID sessionId = UUID.randomUUID();
            RefreshToken token = createToken(sessionId, USER_ID);
            when(refreshTokenRepository.findById(sessionId)).thenReturn(Optional.of(token));

            sessionService.revokeSession(USER_ID, sessionId);

            assertThat(token.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("should throw 404 when session does not exist")
        void shouldThrow404WhenSessionDoesNotExist() {
            UUID sessionId = UUID.randomUUID();
            when(refreshTokenRepository.findById(sessionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.revokeSession(USER_ID, sessionId))
                    .isInstanceOf(SessionService.SessionNotFoundException.class);
        }

        @Test
        @DisplayName("should throw 403 when session belongs to another user")
        void shouldThrow403WhenSessionBelongsToAnotherUser() {
            UUID sessionId = UUID.randomUUID();
            RefreshToken token = createToken(sessionId, OTHER_USER_ID);
            when(refreshTokenRepository.findById(sessionId)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> sessionService.revokeSession(USER_ID, sessionId))
                    .isInstanceOf(SessionService.SessionAccessDeniedException.class);
        }

        @Test
        @DisplayName("should not re-revoke an already revoked session")
        void shouldNotReRevokeAlreadyRevokedSession() {
            UUID sessionId = UUID.randomUUID();
            RefreshToken token = createToken(sessionId, USER_ID);
            token.revoke(RefreshToken.REASON_LOGOUT);
            when(refreshTokenRepository.findById(sessionId)).thenReturn(Optional.of(token));

            sessionService.revokeSession(USER_ID, sessionId);

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("revokeAllSessions")
    class RevokeAllSessionsTests {

        @Test
        @DisplayName("should revoke all active sessions when no exception")
        void shouldRevokeAllActiveSessions() {
            RefreshToken token1 = createToken(UUID.randomUUID(), USER_ID);
            RefreshToken token2 = createToken(UUID.randomUUID(), USER_ID);
            when(refreshTokenRepository.findActiveByUserId(USER_ID, FIXED_NOW))
                    .thenReturn(List.of(token1, token2));

            sessionService.revokeAllSessions(USER_ID, null);

            assertThat(token1.isRevoked()).isTrue();
            assertThat(token2.isRevoked()).isTrue();
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("should revoke all except current session")
        void shouldRevokeAllExceptCurrentSession() {
            UUID currentSessionId = UUID.randomUUID();
            RefreshToken current = createToken(currentSessionId, USER_ID);
            RefreshToken other = createToken(UUID.randomUUID(), USER_ID);
            when(refreshTokenRepository.findActiveByUserId(USER_ID, FIXED_NOW))
                    .thenReturn(List.of(current, other));

            sessionService.revokeAllSessions(USER_ID, currentSessionId);

            assertThat(current.isRevoked()).isFalse();
            assertThat(other.isRevoked()).isTrue();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(other.getId());
        }

        @Test
        @DisplayName("should do nothing when no active sessions")
        void shouldDoNothingWhenNoActiveSessions() {
            when(refreshTokenRepository.findActiveByUserId(USER_ID, FIXED_NOW))
                    .thenReturn(List.of());

            sessionService.revokeAllSessions(USER_ID, null);

            verify(refreshTokenRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RefreshToken createToken(UUID id, UUID userId) {
        Instant expiresAt = FIXED_NOW.plusSeconds(7 * 24 * 3600);
        RefreshToken token = RefreshToken.create(
                userId,
                "hash-" + id,
                UUID.randomUUID(),
                expiresAt,
                "192.168.1.1",
                "Mozilla/5.0"
        );
        // Use reflection to set generated/lifecycle fields not available in unit tests
        ReflectionTestUtils.setField(token, "id", id);
        ReflectionTestUtils.setField(token, "createdAt", FIXED_NOW);
        return token;
    }
}
