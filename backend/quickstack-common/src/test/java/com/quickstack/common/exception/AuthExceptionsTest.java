package com.quickstack.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for authentication exception classes.
 */
class AuthExceptionsTest {

    @Nested
    @DisplayName("AuthenticationException")
    class AuthenticationExceptionTest {

        @Test
        @DisplayName("should have 401 status and generic message")
        void shouldHave401StatusAndGenericMessage() {
            AuthenticationException ex = new AuthenticationException();

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getCode()).isEqualTo("AUTHENTICATION_FAILED");
            // Message should be generic to avoid revealing which credential was wrong
            assertThat(ex.getMessage()).doesNotContain("email", "password", "user");
        }

        @Test
        @DisplayName("should allow custom message")
        void shouldAllowCustomMessage() {
            AuthenticationException ex = new AuthenticationException("Custom message");

            assertThat(ex.getMessage()).isEqualTo("Custom message");
        }
    }

    @Nested
    @DisplayName("RateLimitExceededException")
    class RateLimitExceededExceptionTest {

        @Test
        @DisplayName("should have 429 status with retry information")
        void shouldHave429StatusWithRetryInfo() {
            RateLimitExceededException ex = new RateLimitExceededException(60);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(ex.getCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
            assertThat(ex.getRetryAfterSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("should have generic message for security")
        void shouldHaveGenericMessage() {
            RateLimitExceededException ex = new RateLimitExceededException(60);

            // Should not reveal rate limit details
            assertThat(ex.getMessage()).doesNotContain("10", "minute");
        }
    }

    @Nested
    @DisplayName("AccountLockedException")
    class AccountLockedExceptionTest {

        @Test
        @DisplayName("should have 423 status with unlock timestamp")
        void shouldHave423StatusWithUnlockTimestamp() {
            Instant unlockTime = Instant.now().plus(15, ChronoUnit.MINUTES);
            AccountLockedException ex = new AccountLockedException(unlockTime);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.LOCKED);
            assertThat(ex.getCode()).isEqualTo("ACCOUNT_LOCKED");
            assertThat(ex.getLockedUntil()).isEqualTo(unlockTime);
        }

        @Test
        @DisplayName("should calculate remaining lockout seconds")
        void shouldCalculateRemainingLockoutSeconds() {
            Instant unlockTime = Instant.now().plus(5, ChronoUnit.MINUTES);
            AccountLockedException ex = new AccountLockedException(unlockTime);

            // Should be approximately 300 seconds (5 minutes)
            assertThat(ex.getRemainingLockoutSeconds())
                    .isBetween(295L, 305L);
        }

        @Test
        @DisplayName("should return zero for past unlock time")
        void shouldReturnZeroForPastUnlockTime() {
            Instant pastTime = Instant.now().minus(1, ChronoUnit.MINUTES);
            AccountLockedException ex = new AccountLockedException(pastTime);

            assertThat(ex.getRemainingLockoutSeconds()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("InvalidTokenException")
    class InvalidTokenExceptionTest {

        @Test
        @DisplayName("should have 401 status with token type and reason")
        void shouldHave401StatusWithTypeAndReason() {
            InvalidTokenException ex = new InvalidTokenException(
                    InvalidTokenException.TokenType.ACCESS_TOKEN,
                    InvalidTokenException.InvalidationReason.EXPIRED
            );

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getCode()).isEqualTo("INVALID_TOKEN");
            assertThat(ex.getTokenType()).isEqualTo(InvalidTokenException.TokenType.ACCESS_TOKEN);
            assertThat(ex.getReason()).isEqualTo(InvalidTokenException.InvalidationReason.EXPIRED);
        }

        @Test
        @DisplayName("should have generic messages for security")
        void shouldHaveGenericMessages() {
            // Expired token can reveal expiration
            InvalidTokenException expired = InvalidTokenException.expired(
                    InvalidTokenException.TokenType.REFRESH_TOKEN);
            assertThat(expired.getMessage()).isEqualTo("Token has expired");

            // Malformed should be generic
            InvalidTokenException malformed = InvalidTokenException.malformed(
                    InvalidTokenException.TokenType.ACCESS_TOKEN);
            assertThat(malformed.getMessage()).isEqualTo("Invalid token");

            // Algorithm attack should be generic
            InvalidTokenException wrongAlg = InvalidTokenException.wrongAlgorithm(
                    InvalidTokenException.TokenType.ACCESS_TOKEN);
            assertThat(wrongAlg.getMessage()).isEqualTo("Invalid token");
        }

        @Test
        @DisplayName("should provide factory methods for common cases")
        void shouldProvideFactoryMethods() {
            assertThat(InvalidTokenException.expired(InvalidTokenException.TokenType.ACCESS_TOKEN))
                    .isNotNull();
            assertThat(InvalidTokenException.revoked(InvalidTokenException.TokenType.REFRESH_TOKEN))
                    .isNotNull();
            assertThat(InvalidTokenException.alreadyUsed(InvalidTokenException.TokenType.PASSWORD_RESET_TOKEN))
                    .isNotNull();
            assertThat(InvalidTokenException.invalidSignature(InvalidTokenException.TokenType.ACCESS_TOKEN))
                    .isNotNull();
            assertThat(InvalidTokenException.wrongAlgorithm(InvalidTokenException.TokenType.ACCESS_TOKEN))
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("PasswordCompromisedException")
    class PasswordCompromisedExceptionTest {

        @Test
        @DisplayName("should have 400 status with helpful message")
        void shouldHave400StatusWithHelpfulMessage() {
            PasswordCompromisedException ex = new PasswordCompromisedException();

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getCode()).isEqualTo("PASSWORD_COMPROMISED");
            // Message should guide user without revealing security details
            assertThat(ex.getMessage())
                    .contains("data breach")
                    .contains("different password");
        }
    }

    @Nested
    @DisplayName("PasswordValidationException")
    class PasswordValidationExceptionTest {

        @Test
        @DisplayName("should create exception for password too short")
        void shouldCreateExceptionForTooShort() {
            PasswordValidationException ex = PasswordValidationException.tooShort(12);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getCode()).isEqualTo("PASSWORD_TOO_SHORT");
            assertThat(ex.getMessage()).contains("12");
            assertThat(ex.getFailure()).isEqualTo(
                    PasswordValidationException.ValidationFailure.TOO_SHORT);
        }

        @Test
        @DisplayName("should create exception for password too long")
        void shouldCreateExceptionForTooLong() {
            PasswordValidationException ex = PasswordValidationException.tooLong(128);

            assertThat(ex.getCode()).isEqualTo("PASSWORD_TOO_LONG");
            assertThat(ex.getMessage()).contains("128");
        }

        @Test
        @DisplayName("should create exception for breach check unavailable")
        void shouldCreateExceptionForBreachCheckUnavailable() {
            PasswordValidationException ex = PasswordValidationException.breachCheckUnavailable();

            assertThat(ex.getCode()).isEqualTo("PASSWORD_BREACH_CHECK_UNAVAILABLE");
            assertThat(ex.getMessage()).contains("temporarily unavailable");
        }

        @Test
        @DisplayName("should create exception for same as current")
        void shouldCreateExceptionForSameAsCurrent() {
            PasswordValidationException ex = PasswordValidationException.sameAsCurrent();

            assertThat(ex.getCode()).isEqualTo("PASSWORD_SAME_AS_CURRENT");
        }
    }
}
