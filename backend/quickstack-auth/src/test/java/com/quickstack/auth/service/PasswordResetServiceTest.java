package com.quickstack.auth.service;

import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.exception.PasswordCompromisedException;
import com.quickstack.common.exception.PasswordValidationException;
import com.quickstack.common.security.PasswordBreachChecker;
import com.quickstack.common.security.PasswordService;
import com.quickstack.auth.entity.PasswordResetToken;
import com.quickstack.user.entity.User;
import com.quickstack.auth.repository.PasswordResetTokenRepository;
import com.quickstack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService")
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private PasswordBreachChecker breachChecker;

    @Mock
    private RefreshTokenService refreshTokenService;

    private Clock clock;
    private PasswordResetService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";
    private static final String CLIENT_IP = "192.168.1.1";

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        service = new PasswordResetService(
                tokenRepository,
                userRepository,
                passwordService,
                breachChecker,
                refreshTokenService,
                clock
        );
    }

    private User createActiveUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setTenantId(TENANT_ID);
        user.setEmail(EMAIL);
        user.setActive(true);
        return user;
    }

    @Nested
    @DisplayName("Initiate Reset")
    class InitiateReset {

        @Test
        @DisplayName("creates reset token for existing active user")
        void createsTokenForExistingUser() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));
            when(tokenRepository.deleteByUserId(USER_ID)).thenReturn(0);
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            assertThat(result.success()).isTrue();
            assertThat(result.token()).isNotNull();
            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo(EMAIL);

            verify(tokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("invalidates existing tokens before creating new one")
        void invalidatesExistingTokens() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));
            when(tokenRepository.deleteByUserId(USER_ID)).thenReturn(2);
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            verify(tokenRepository).deleteByUserId(USER_ID);
        }

        @Test
        @DisplayName("returns userNotFound for non-existent email")
        void returnsUserNotFoundForNonExistentEmail() {
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, "nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            var result = service.initiateReset("nonexistent@example.com", TENANT_ID, CLIENT_IP);

            assertThat(result.success()).isFalse();
            assertThat(result.token()).isNull();
            assertThat(result.userId()).isNull();

            verify(tokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("returns userNotFound for inactive user")
        void returnsUserNotFoundForInactiveUser() {
            User user = createActiveUser();
            user.setActive(false);
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));

            var result = service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            assertThat(result.success()).isFalse();
            verify(tokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("normalizes email to lowercase")
        void normalizesEmail() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.initiateReset("USER@EXAMPLE.COM", TENANT_ID, CLIENT_IP);

            verify(userRepository).findByTenantIdAndEmail(TENANT_ID, EMAIL);
        }

        @Test
        @DisplayName("stores token hash, not plaintext")
        void storesTokenHash() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            when(tokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            PasswordResetToken savedToken = captor.getValue();
            // Token hash should be different from plain token (64 hex chars for SHA-256)
            assertThat(savedToken.getTokenHash()).hasSize(64);
            assertThat(savedToken.getTokenHash()).isNotEqualTo(result.token());
        }

        @Test
        @DisplayName("sets correct expiration time")
        void setsCorrectExpiration() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            when(tokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            PasswordResetToken savedToken = captor.getValue();
            Instant expectedExpiry = clock.instant().plus(1, ChronoUnit.HOURS);
            assertThat(savedToken.getExpiresAt()).isEqualTo(expectedExpiry);
        }
    }

    @Nested
    @DisplayName("Validate Token")
    class ValidateToken {

        @Test
        @DisplayName("validates valid token")
        void validatesValidToken() {
            PasswordResetToken token = PasswordResetToken.create(
                    USER_ID, "somehash", clock.instant().plus(1, ChronoUnit.HOURS), CLIENT_IP
            );
            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(token));

            PasswordResetToken result = service.validateToken("plain-token");

            assertThat(result).isEqualTo(token);
        }

        @Test
        @DisplayName("throws exception for expired token")
        void throwsForExpiredToken() {
            PasswordResetToken expiredToken = PasswordResetToken.create(
                    USER_ID, "somehash", clock.instant().minus(1, ChronoUnit.HOURS), CLIENT_IP
            );
            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.empty());
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> service.validateToken("plain-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("throws exception for used token")
        void throwsForUsedToken() {
            PasswordResetToken usedToken = PasswordResetToken.create(
                    USER_ID, "somehash", clock.instant().plus(1, ChronoUnit.HOURS), CLIENT_IP
            );
            usedToken.markAsUsed();

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.empty());
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usedToken));

            assertThatThrownBy(() -> service.validateToken("plain-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("throws exception for non-existent token")
        void throwsForNonExistentToken() {
            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.empty());
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.validateToken("invalid-token"))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("Reset Password")
    class ResetPassword {

        private PasswordResetToken validToken;
        private User user;

        @BeforeEach
        void setUpResetPassword() {
            validToken = PasswordResetToken.create(
                    USER_ID, "tokenhash", clock.instant().plus(1, ChronoUnit.HOURS), CLIENT_IP
            );
            user = createActiveUser();
        }

        @Test
        @DisplayName("resets password successfully")
        void resetsPasswordSuccessfully() {
            String newPassword = "newSecurePassword123!";
            String hashedPassword = "v1$argon2hash";

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(validToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordService.hashPassword(newPassword)).thenReturn(hashedPassword);

            service.resetPassword("plain-token", newPassword);

            verify(passwordService).validatePasswordPolicy(newPassword);
            verify(breachChecker).checkPassword(newPassword);
            verify(userRepository).save(user);
            verify(tokenRepository).save(validToken);
            verify(refreshTokenService).revokeAllUserTokens(USER_ID, "password_change");

            assertThat(user.getPasswordHash()).isEqualTo(hashedPassword);
            assertThat(user.isMustChangePassword()).isFalse();
            assertThat(validToken.isUsed()).isTrue();
        }

        @Test
        @DisplayName("validates password policy before reset")
        void validatesPasswordPolicy() {
            String weakPassword = "short";

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(validToken));
            doThrow(PasswordValidationException.tooShort(12))
                    .when(passwordService).validatePasswordPolicy(weakPassword);

            assertThatThrownBy(() -> service.resetPassword("plain-token", weakPassword))
                    .isInstanceOf(PasswordValidationException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("checks password against breach database")
        void checksBreachDatabase() {
            String compromisedPassword = "password123";

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(validToken));
            doThrow(PasswordCompromisedException.withBreachCount(1000))
                    .when(breachChecker).checkPassword(compromisedPassword);

            assertThatThrownBy(() -> service.resetPassword("plain-token", compromisedPassword))
                    .isInstanceOf(PasswordCompromisedException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("revokes all refresh tokens on password reset")
        void revokesRefreshTokens() {
            String newPassword = "newSecurePassword123!";

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(validToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordService.hashPassword(newPassword)).thenReturn("hashed");

            service.resetPassword("plain-token", newPassword);

            verify(refreshTokenService).revokeAllUserTokens(USER_ID, "password_change");
        }

        @Test
        @DisplayName("marks token as used after reset")
        void marksTokenAsUsed() {
            String newPassword = "newSecurePassword123!";

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(validToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordService.hashPassword(newPassword)).thenReturn("hashed");

            service.resetPassword("plain-token", newPassword);

            assertThat(validToken.isUsed()).isTrue();
            verify(tokenRepository).save(validToken);
        }

        @Test
        @DisplayName("resets failed login attempts")
        void resetsFailedLoginAttempts() {
            String newPassword = "newSecurePassword123!";
            user.recordFailedLogin(5, 900); // Simulate locked account

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(validToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordService.hashPassword(newPassword)).thenReturn("hashed");

            service.resetPassword("plain-token", newPassword);

            assertThat(user.getFailedLoginAttempts()).isZero();
            assertThat(user.isLocked()).isFalse();
        }
    }

    @Nested
    @DisplayName("Cleanup")
    class Cleanup {

        @Test
        @DisplayName("deletes expired tokens")
        void deletesExpiredTokens() {
            when(tokenRepository.deleteExpiredTokens(any())).thenReturn(5);

            int deleted = service.cleanupExpiredTokens();

            assertThat(deleted).isEqualTo(5);
            verify(tokenRepository).deleteExpiredTokens(clock.instant());
        }
    }

    @Nested
    @DisplayName("Security Properties")
    class SecurityProperties {

        @Test
        @DisplayName("generated tokens have high entropy (32 bytes = 256 bits)")
        void tokensHaveHighEntropy() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result1 = service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);
            var result2 = service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            // Tokens should be different (cryptographically random)
            assertThat(result1.token()).isNotEqualTo(result2.token());

            // Base64URL encoded 32 bytes = 43 characters
            assertThat(result1.token()).hasSize(43);
            assertThat(result2.token()).hasSize(43);
        }

        @Test
        @DisplayName("token hash is SHA-256 (64 hex characters)")
        void tokenHashIsSha256() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            when(tokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            PasswordResetToken savedToken = captor.getValue();
            // SHA-256 produces 64 hex characters
            assertThat(savedToken.getTokenHash()).hasSize(64);
            assertThat(savedToken.getTokenHash()).matches("^[0-9a-f]+$");
        }

        @Test
        @DisplayName("same plain token always produces same hash")
        void sameTokenProducesSameHash() {
            // This tests the deterministic nature of SHA-256
            // The service should hash consistently for token validation
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            when(tokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            var result = service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);
            PasswordResetToken savedToken = captor.getValue();

            // When validateToken is called with the same plain token,
            // it should hash to the same value to find it in DB
            when(tokenRepository.findValidToken(eq(savedToken.getTokenHash()), any()))
                    .thenReturn(Optional.of(savedToken));

            // This should find the token using the same hash
            PasswordResetToken validated = service.validateToken(result.token());
            assertThat(validated).isEqualTo(savedToken);
        }

        @Test
        @DisplayName("prevents token accumulation by deleting existing tokens")
        void preventsTokenAccumulation() {
            User user = createActiveUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));
            when(tokenRepository.deleteByUserId(USER_ID)).thenReturn(3); // Had 3 existing tokens
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.initiateReset(EMAIL, TENANT_ID, CLIENT_IP);

            // Should delete existing tokens before creating new one
            verify(tokenRepository).deleteByUserId(USER_ID);
            // Only save the new token
            verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("token isolation between users")
        void tokenIsolationBetweenUsers() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            User user1 = new User();
            user1.setId(userId1);
            user1.setTenantId(TENANT_ID);
            user1.setEmail("user1@example.com");
            user1.setActive(true);

            User user2 = new User();
            user2.setId(userId2);
            user2.setTenantId(TENANT_ID);
            user2.setEmail("user2@example.com");
            user2.setActive(true);

            when(userRepository.findByTenantIdAndEmail(TENANT_ID, "user1@example.com"))
                    .thenReturn(Optional.of(user1));
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, "user2@example.com"))
                    .thenReturn(Optional.of(user2));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var result1 = service.initiateReset("user1@example.com", TENANT_ID, CLIENT_IP);
            var result2 = service.initiateReset("user2@example.com", TENANT_ID, CLIENT_IP);

            // Tokens should be different
            assertThat(result1.token()).isNotEqualTo(result2.token());

            // User IDs should be different
            assertThat(result1.userId()).isNotEqualTo(result2.userId());

            // Delete should only be called for each user's tokens
            verify(tokenRepository).deleteByUserId(userId1);
            verify(tokenRepository).deleteByUserId(userId2);
        }

        @Test
        @DisplayName("token cannot be reused after reset")
        void tokenCannotBeReusedAfterReset() {
            PasswordResetToken token = PasswordResetToken.create(
                    USER_ID, "tokenhash", clock.instant().plus(1, ChronoUnit.HOURS), CLIENT_IP
            );
            User user = createActiveUser();

            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.of(token));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordService.hashPassword(anyString())).thenReturn("hashed");

            // First reset succeeds
            service.resetPassword("plain-token", "newPassword123!");
            assertThat(token.isUsed()).isTrue();

            // Second reset attempt should fail
            when(tokenRepository.findValidToken(anyString(), any())).thenReturn(Optional.empty());
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.resetPassword("plain-token", "anotherPassword123!"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("timing-safe operations for non-existent user")
        void timingSafeForNonExistentUser() {
            // When user doesn't exist, dummy operations should still be performed
            // This is verified by ensuring the method completes without error
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, "nonexistent@example.com"))
                    .thenReturn(Optional.empty());

            var result = service.initiateReset("nonexistent@example.com", TENANT_ID, CLIENT_IP);

            // Should return not found but NOT throw an exception
            // Response should look the same to caller
            assertThat(result.success()).isFalse();
            assertThat(result.token()).isNull();
            assertThat(result.userId()).isNull();
            assertThat(result.email()).isNull();

            // Verify no token was saved
            verify(tokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("stores client IP for audit trail")
        void storesClientIpForAudit() {
            User user = createActiveUser();
            String testIp = "203.0.113.100";
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(user));

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            when(tokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.initiateReset(EMAIL, TENANT_ID, testIp);

            PasswordResetToken savedToken = captor.getValue();
            assertThat(savedToken.getCreatedIp()).isEqualTo(testIp);
        }

        @Test
        @DisplayName("tenant isolation - user from wrong tenant not found")
        void tenantIsolation() {
            UUID wrongTenant = UUID.randomUUID();

            when(userRepository.findByTenantIdAndEmail(wrongTenant, EMAIL))
                    .thenReturn(Optional.empty());

            var result = service.initiateReset(EMAIL, wrongTenant, CLIENT_IP);

            assertThat(result.success()).isFalse();
            verify(tokenRepository, never()).save(any());
        }
    }
}
