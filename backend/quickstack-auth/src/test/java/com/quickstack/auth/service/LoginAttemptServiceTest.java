package com.quickstack.auth.service;

import com.quickstack.common.config.properties.RateLimitProperties;
import com.quickstack.common.exception.AccountLockedException;
import com.quickstack.auth.entity.LoginAttempt;
import com.quickstack.auth.repository.LoginAttemptRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    private RateLimitProperties rateLimitProperties;
    private Clock clock;
    private LoginAttemptService loginAttemptService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "test@example.com";
    private static final String IP_ADDRESS = "192.168.1.1";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final Instant FIXED_NOW = Instant.parse("2026-02-16T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_NOW, ZoneId.of("UTC"));
        rateLimitProperties = createDefaultProperties();
        loginAttemptService = new LoginAttemptService(
                loginAttemptRepository, rateLimitProperties, clock
        );
    }

    private RateLimitProperties createDefaultProperties() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);

        RateLimitProperties.LockoutConfig lockout = new RateLimitProperties.LockoutConfig();
        lockout.setEnabled(true);
        lockout.setMaxAttempts(5);
        lockout.setLockoutDuration(Duration.ofMinutes(15));
        lockout.setAttemptWindow(Duration.ofMinutes(15));
        props.setLockout(lockout);

        RateLimitProperties.BucketConfig ipBucket = new RateLimitProperties.BucketConfig(10, Duration.ofMinutes(1));
        props.setIp(ipBucket);

        return props;
    }

    // -------------------------------------------------------------------------
    // Account Lockout Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Account Lockout")
    class AccountLockoutTests {

        @Test
        @DisplayName("should not throw when failed attempts below threshold")
        void shouldNotThrowWhenBelowThreshold() {
            when(loginAttemptRepository.countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(4L);

            loginAttemptService.checkAccountLock(EMAIL, TENANT_ID);

            // No exception thrown
            verify(loginAttemptRepository).countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any());
        }

        @Test
        @DisplayName("should throw AccountLockedException when threshold reached")
        void shouldThrowWhenThresholdReached() {
            Instant lastFailureTime = FIXED_NOW.minusSeconds(60);
            LoginAttempt lastFailure = createFailedAttempt(lastFailureTime);

            when(loginAttemptRepository.countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(5L);
            when(loginAttemptRepository.hasSuccessfulLoginSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(false);
            when(loginAttemptRepository.findMostRecentFailedAttempt(EMAIL, TENANT_ID))
                    .thenReturn(lastFailure);

            Instant expectedLockedUntil = lastFailureTime.plus(Duration.ofMinutes(15));

            assertThatThrownBy(() -> loginAttemptService.checkAccountLock(EMAIL, TENANT_ID))
                    .isInstanceOf(AccountLockedException.class)
                    .satisfies(ex -> {
                        AccountLockedException ale = (AccountLockedException) ex;
                        assertThat(ale.getLockedUntil()).isEqualTo(expectedLockedUntil);
                    });
        }

        @Test
        @DisplayName("should not throw when lockout duration has passed")
        void shouldNotThrowWhenLockoutExpired() {
            // Last failure was 20 minutes ago - lockout (15 min) has expired
            Instant lastFailureTime = FIXED_NOW.minus(Duration.ofMinutes(20));
            LoginAttempt lastFailure = createFailedAttempt(lastFailureTime);

            when(loginAttemptRepository.countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(5L);
            when(loginAttemptRepository.hasSuccessfulLoginSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(false);
            when(loginAttemptRepository.findMostRecentFailedAttempt(EMAIL, TENANT_ID))
                    .thenReturn(lastFailure);

            // Should not throw - lockout has expired
            loginAttemptService.checkAccountLock(EMAIL, TENANT_ID);
        }

        @Test
        @DisplayName("should not throw when successful login resets counter")
        void shouldNotThrowWhenSuccessfulLoginResets() {
            when(loginAttemptRepository.countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(5L);
            when(loginAttemptRepository.hasSuccessfulLoginSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(true);

            // Should not throw - successful login resets the counter
            loginAttemptService.checkAccountLock(EMAIL, TENANT_ID);

            verify(loginAttemptRepository, never()).findMostRecentFailedAttempt(any(), any());
        }

        @Test
        @DisplayName("should skip check when lockout is disabled")
        void shouldSkipWhenLockoutDisabled() {
            rateLimitProperties.getLockout().setEnabled(false);

            loginAttemptService.checkAccountLock(EMAIL, TENANT_ID);

            verify(loginAttemptRepository, never()).countFailedAttemptsByEmailSince(any(), any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Record Login Attempts Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Record Login Attempts")
    class RecordLoginAttemptsTests {

        @Test
        @DisplayName("should record successful login")
        void shouldRecordSuccessfulLogin() {
            loginAttemptService.recordSuccessfulLogin(EMAIL, USER_ID, TENANT_ID, IP_ADDRESS, USER_AGENT);

            ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
            verify(loginAttemptRepository).save(captor.capture());

            LoginAttempt saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.isSuccess()).isTrue();
            assertThat(saved.getIpAddress()).isEqualTo(IP_ADDRESS);
            assertThat(saved.getUserAgent()).isEqualTo(USER_AGENT);
            assertThat(saved.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("should record failed login with reason")
        void shouldRecordFailedLogin() {
            when(loginAttemptRepository.countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(3L);

            long attempts = loginAttemptService.recordFailedLogin(
                    EMAIL, USER_ID, TENANT_ID, IP_ADDRESS, USER_AGENT,
                    LoginAttempt.REASON_INVALID_CREDENTIALS
            );

            assertThat(attempts).isEqualTo(3L);

            ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
            verify(loginAttemptRepository).save(captor.capture());

            LoginAttempt saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.isSuccess()).isFalse();
            assertThat(saved.getFailureReason()).isEqualTo(LoginAttempt.REASON_INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("should handle null tenantId for user not found")
        void shouldHandleNullTenantId() {
            long attempts = loginAttemptService.recordFailedLogin(
                    EMAIL, null, null, IP_ADDRESS, USER_AGENT,
                    LoginAttempt.REASON_USER_NOT_FOUND
            );

            assertThat(attempts).isEqualTo(0L);

            ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
            verify(loginAttemptRepository).save(captor.capture());

            LoginAttempt saved = captor.getValue();
            assertThat(saved.getUserId()).isNull();
            assertThat(saved.getTenantId()).isNull();
            assertThat(saved.getFailureReason()).isEqualTo(LoginAttempt.REASON_USER_NOT_FOUND);
        }
    }

    // -------------------------------------------------------------------------
    // IP Rate Limiting Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("IP Rate Limiting")
    class IpRateLimitingTests {

        @Test
        @DisplayName("should detect IP rate limit when threshold exceeded")
        void shouldDetectIpRateLimit() {
            when(loginAttemptRepository.countFailedAttemptsByIpSince(eq(IP_ADDRESS), any()))
                    .thenReturn(10L);

            boolean isRateLimited = loginAttemptService.isIpRateLimited(IP_ADDRESS);

            assertThat(isRateLimited).isTrue();
        }

        @Test
        @DisplayName("should not rate limit when below threshold")
        void shouldNotRateLimitBelowThreshold() {
            when(loginAttemptRepository.countFailedAttemptsByIpSince(eq(IP_ADDRESS), any()))
                    .thenReturn(5L);

            boolean isRateLimited = loginAttemptService.isIpRateLimited(IP_ADDRESS);

            assertThat(isRateLimited).isFalse();
        }

        @Test
        @DisplayName("should skip IP rate limiting when disabled")
        void shouldSkipWhenDisabled() {
            rateLimitProperties.setEnabled(false);

            boolean isRateLimited = loginAttemptService.isIpRateLimited(IP_ADDRESS);

            assertThat(isRateLimited).isFalse();
            verify(loginAttemptRepository, never()).countFailedAttemptsByIpSince(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Remaining Attempts Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Remaining Attempts")
    class RemainingAttemptsTests {

        @Test
        @DisplayName("should return remaining attempts correctly")
        void shouldReturnRemainingAttempts() {
            when(loginAttemptRepository.countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(3L);

            int remaining = loginAttemptService.getRemainingAttempts(EMAIL, TENANT_ID);

            assertThat(remaining).isEqualTo(2); // 5 max - 3 used = 2 remaining
        }

        @Test
        @DisplayName("should return zero when all attempts used")
        void shouldReturnZeroWhenExhausted() {
            when(loginAttemptRepository.countFailedAttemptsByEmailSince(eq(EMAIL), eq(TENANT_ID), any()))
                    .thenReturn(5L);

            int remaining = loginAttemptService.getRemainingAttempts(EMAIL, TENANT_ID);

            assertThat(remaining).isEqualTo(0);
        }

        @Test
        @DisplayName("should return -1 when lockout disabled")
        void shouldReturnNegativeOneWhenDisabled() {
            rateLimitProperties.getLockout().setEnabled(false);

            int remaining = loginAttemptService.getRemainingAttempts(EMAIL, TENANT_ID);

            assertThat(remaining).isEqualTo(-1);
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private LoginAttempt createFailedAttempt(Instant createdAt) {
        LoginAttempt attempt = LoginAttempt.failed(
                EMAIL, USER_ID, TENANT_ID, IP_ADDRESS, USER_AGENT,
                LoginAttempt.REASON_INVALID_CREDENTIALS
        );
        // Use reflection or a setter to set createdAt for testing
        try {
            var field = LoginAttempt.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(attempt, createdAt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt", e);
        }
        return attempt;
    }
}
