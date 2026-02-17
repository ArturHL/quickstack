package com.quickstack.user.service;

import com.quickstack.common.config.properties.JwtProperties;
import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.exception.InvalidTokenException.InvalidationReason;
import com.quickstack.user.entity.RefreshToken;
import com.quickstack.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private JwtProperties jwtProperties;
    private Clock clock;
    private RefreshTokenService refreshTokenService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String IP_ADDRESS = "192.168.1.1";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final Instant FIXED_NOW = Instant.parse("2026-02-16T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        jwtProperties = createDefaultProperties();
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository, jwtProperties, clock
        );
    }

    private JwtProperties createDefaultProperties() {
        JwtProperties props = new JwtProperties();
        props.setRefreshTokenExpiration(Duration.ofDays(7));
        return props;
    }

    // -------------------------------------------------------------------------
    // Token Creation Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Token Creation")
    class TokenCreationTests {

        @Test
        @DisplayName("should create refresh token with hash stored in database")
        void shouldCreateTokenWithHash() {
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String plainToken = refreshTokenService.createRefreshToken(USER_ID, IP_ADDRESS, USER_AGENT);

            assertThat(plainToken).isNotBlank();
            assertThat(plainToken).hasSize(43); // Base64URL encoded 32 bytes

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getTokenHash()).isNotEqualTo(plainToken); // Hash, not plain token
            assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex
            assertThat(saved.getFamilyId()).isNotNull();
            assertThat(saved.getIpAddress()).isEqualTo(IP_ADDRESS);
            assertThat(saved.getUserAgent()).isEqualTo(USER_AGENT);
            assertThat(saved.getExpiresAt()).isEqualTo(FIXED_NOW.plus(Duration.ofDays(7)));
        }

        @Test
        @DisplayName("should generate unique tokens for each call")
        void shouldGenerateUniqueTokens() {
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String token1 = refreshTokenService.createRefreshToken(USER_ID, IP_ADDRESS, USER_AGENT);
            String token2 = refreshTokenService.createRefreshToken(USER_ID, IP_ADDRESS, USER_AGENT);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should create token with specific family ID for rotation")
        void shouldCreateWithFamilyId() {
            UUID familyId = UUID.randomUUID();
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            refreshTokenService.createRefreshToken(USER_ID, familyId, IP_ADDRESS, USER_AGENT);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            assertThat(captor.getValue().getFamilyId()).isEqualTo(familyId);
        }
    }

    // -------------------------------------------------------------------------
    // Token Validation Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should validate valid token successfully")
        void shouldValidateValidToken() {
            String plainToken = "test-token-value";
            RefreshToken storedToken = createValidToken(plainToken);

            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(storedToken));

            RefreshToken result = refreshTokenService.validateToken(plainToken);

            assertThat(result).isEqualTo(storedToken);
        }

        @Test
        @DisplayName("should throw when token not found")
        void shouldThrowWhenNotFound() {
            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validateToken("unknown-token"))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("should throw when token expired")
        void shouldThrowWhenExpired() {
            String plainToken = "expired-token";
            RefreshToken expiredToken = createExpiredToken(plainToken);

            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> refreshTokenService.validateToken(plainToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.EXPIRED);
                    });
        }

        @Test
        @DisplayName("should detect token reuse and revoke family")
        void shouldDetectReuseAndRevokeFamily() {
            String plainToken = "reused-token";
            RefreshToken revokedToken = createRevokedToken(plainToken);

            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> refreshTokenService.validateToken(plainToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.REVOKED);
                    });

            // Verify that entire family was revoked
            verify(refreshTokenRepository).revokeFamily(
                    eq(revokedToken.getFamilyId()),
                    any(Instant.class),
                    eq(RefreshToken.REASON_SUSPICIOUS_REUSE)
            );
        }
    }

    // -------------------------------------------------------------------------
    // Token Rotation Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Token Rotation")
    class TokenRotationTests {

        @Test
        @DisplayName("should rotate token successfully")
        void shouldRotateToken() {
            String oldPlainToken = "old-token";
            RefreshToken oldToken = createValidToken(oldPlainToken);

            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(oldToken));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            RefreshTokenService.RotationResult result = refreshTokenService.rotateToken(
                    oldPlainToken, IP_ADDRESS, USER_AGENT
            );

            assertThat(result.newToken()).isNotBlank();
            assertThat(result.newToken()).isNotEqualTo(oldPlainToken);
            assertThat(result.userId()).isEqualTo(USER_ID);

            // Verify old token was revoked
            assertThat(oldToken.isRevoked()).isTrue();
            assertThat(oldToken.getRevokedReason()).isEqualTo(RefreshToken.REASON_ROTATED);

            // Verify two saves: one for revoke, one for new token
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("should maintain family ID across rotation")
        void shouldMaintainFamilyId() {
            String oldPlainToken = "old-token";
            UUID familyId = UUID.randomUUID();
            RefreshToken oldToken = createValidTokenWithFamily(oldPlainToken, familyId);

            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(oldToken));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            refreshTokenService.rotateToken(oldPlainToken, IP_ADDRESS, USER_AGENT);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository, times(2)).save(captor.capture());

            // The second save should be the new token with same family ID
            RefreshToken newToken = captor.getAllValues().get(1);
            assertThat(newToken.getFamilyId()).isEqualTo(familyId);
        }
    }

    // -------------------------------------------------------------------------
    // Token Revocation Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Token Revocation")
    class TokenRevocationTests {

        @Test
        @DisplayName("should revoke single token")
        void shouldRevokeSingleToken() {
            String plainToken = "token-to-revoke";
            RefreshToken token = createValidToken(plainToken);

            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(token));

            refreshTokenService.revokeToken(plainToken, RefreshToken.REASON_LOGOUT);

            assertThat(token.isRevoked()).isTrue();
            assertThat(token.getRevokedReason()).isEqualTo(RefreshToken.REASON_LOGOUT);
            verify(refreshTokenRepository).save(token);
        }

        @Test
        @DisplayName("should revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            when(refreshTokenRepository.revokeAllByUserId(eq(USER_ID), any(Instant.class), any()))
                    .thenReturn(5);

            refreshTokenService.revokeAllUserTokens(USER_ID, RefreshToken.REASON_PASSWORD_CHANGE);

            verify(refreshTokenRepository).revokeAllByUserId(
                    eq(USER_ID),
                    eq(FIXED_NOW),
                    eq(RefreshToken.REASON_PASSWORD_CHANGE)
            );
        }

        @Test
        @DisplayName("should revoke entire token family")
        void shouldRevokeFamilyOnReuse() {
            UUID familyId = UUID.randomUUID();
            when(refreshTokenRepository.revokeFamily(eq(familyId), any(Instant.class), any()))
                    .thenReturn(3);

            refreshTokenService.revokeFamily(familyId, RefreshToken.REASON_SUSPICIOUS_REUSE);

            verify(refreshTokenRepository).revokeFamily(
                    eq(familyId),
                    eq(FIXED_NOW),
                    eq(RefreshToken.REASON_SUSPICIOUS_REUSE)
            );
        }
    }

    // -------------------------------------------------------------------------
    // Session Management Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Session Management")
    class SessionManagementTests {

        @Test
        @DisplayName("should get active sessions")
        void shouldGetActiveSessions() {
            List<RefreshToken> sessions = List.of(
                    createValidToken("token1"),
                    createValidToken("token2")
            );
            when(refreshTokenRepository.findActiveByUserId(eq(USER_ID), any(Instant.class)))
                    .thenReturn(sessions);

            List<RefreshToken> result = refreshTokenService.getActiveSessions(USER_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should count active sessions")
        void shouldCountActiveSessions() {
            when(refreshTokenRepository.countActiveByUserId(eq(USER_ID), any(Instant.class)))
                    .thenReturn(3L);

            long count = refreshTokenService.countActiveSessions(USER_ID);

            assertThat(count).isEqualTo(3L);
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Cleanup")
    class CleanupTests {

        @Test
        @DisplayName("should cleanup expired tokens")
        void shouldCleanupExpiredTokens() {
            when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class)))
                    .thenReturn(100);

            int deleted = refreshTokenService.cleanupExpiredTokens(30);

            assertThat(deleted).isEqualTo(100);
            verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private RefreshToken createValidToken(String plainToken) {
        return createValidTokenWithFamily(plainToken, UUID.randomUUID());
    }

    private RefreshToken createValidTokenWithFamily(String plainToken, UUID familyId) {
        RefreshToken token = RefreshToken.create(
                USER_ID,
                hashToken(plainToken),
                familyId,
                FIXED_NOW.plus(Duration.ofDays(7)),
                IP_ADDRESS,
                USER_AGENT
        );
        return token;
    }

    private RefreshToken createExpiredToken(String plainToken) {
        return RefreshToken.create(
                USER_ID,
                hashToken(plainToken),
                UUID.randomUUID(),
                FIXED_NOW.minus(Duration.ofHours(1)), // Expired 1 hour ago
                IP_ADDRESS,
                USER_AGENT
        );
    }

    private RefreshToken createRevokedToken(String plainToken) {
        RefreshToken token = createValidToken(plainToken);
        token.revoke(RefreshToken.REASON_ROTATED);
        return token;
    }

    private String hashToken(String plainToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
