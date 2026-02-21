package com.quickstack.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetToken")
class PasswordResetTokenTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TOKEN_HASH = "abc123hash";
    private static final String IP_ADDRESS = "192.168.1.1";

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("creates token with all fields")
        void createsTokenWithAllFields() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

            PasswordResetToken token = PasswordResetToken.create(
                    USER_ID,
                    TOKEN_HASH,
                    expiresAt,
                    IP_ADDRESS
            );

            assertThat(token.getUserId()).isEqualTo(USER_ID);
            assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
            assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
            assertThat(token.getCreatedIp()).isEqualTo(IP_ADDRESS);
            assertThat(token.getUsedAt()).isNull();
        }

        @Test
        @DisplayName("creates token without IP address")
        void createsTokenWithoutIpAddress() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

            PasswordResetToken token = PasswordResetToken.create(
                    USER_ID,
                    TOKEN_HASH,
                    expiresAt,
                    null
            );

            assertThat(token.getCreatedIp()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation Methods")
    class ValidationMethods {

        @Test
        @DisplayName("isValid returns true for unused non-expired token")
        void isValidReturnsTrueForUnusedNonExpiredToken() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
            PasswordResetToken token = PasswordResetToken.create(USER_ID, TOKEN_HASH, expiresAt, IP_ADDRESS);

            assertThat(token.isValid()).isTrue();
        }

        @Test
        @DisplayName("isValid returns false for used token")
        void isValidReturnsFalseForUsedToken() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
            PasswordResetToken token = PasswordResetToken.create(USER_ID, TOKEN_HASH, expiresAt, IP_ADDRESS);
            token.markAsUsed();

            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("isValid returns false for expired token")
        void isValidReturnsFalseForExpiredToken() {
            Instant expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
            PasswordResetToken token = PasswordResetToken.create(USER_ID, TOKEN_HASH, expiresAt, IP_ADDRESS);

            assertThat(token.isValid()).isFalse();
        }

        @Test
        @DisplayName("isUsed returns correct status")
        void isUsedReturnsCorrectStatus() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
            PasswordResetToken token = PasswordResetToken.create(USER_ID, TOKEN_HASH, expiresAt, IP_ADDRESS);

            assertThat(token.isUsed()).isFalse();

            token.markAsUsed();

            assertThat(token.isUsed()).isTrue();
        }

        @Test
        @DisplayName("isExpired returns correct status")
        void isExpiredReturnsCorrectStatus() {
            // Not expired
            Instant futureExpiry = Instant.now().plus(1, ChronoUnit.HOURS);
            PasswordResetToken validToken = PasswordResetToken.create(USER_ID, TOKEN_HASH, futureExpiry, IP_ADDRESS);
            assertThat(validToken.isExpired()).isFalse();

            // Expired
            Instant pastExpiry = Instant.now().minus(1, ChronoUnit.HOURS);
            PasswordResetToken expiredToken = PasswordResetToken.create(USER_ID, "other", pastExpiry, IP_ADDRESS);
            assertThat(expiredToken.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("Mark As Used")
    class MarkAsUsed {

        @Test
        @DisplayName("sets usedAt timestamp")
        void setsUsedAtTimestamp() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
            PasswordResetToken token = PasswordResetToken.create(USER_ID, TOKEN_HASH, expiresAt, IP_ADDRESS);

            Instant beforeMark = Instant.now();
            token.markAsUsed();
            Instant afterMark = Instant.now();

            assertThat(token.getUsedAt())
                    .isNotNull()
                    .isAfterOrEqualTo(beforeMark)
                    .isBeforeOrEqualTo(afterMark);
        }

        @Test
        @DisplayName("marking as used makes token invalid")
        void markingAsUsedMakesTokenInvalid() {
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
            PasswordResetToken token = PasswordResetToken.create(USER_ID, TOKEN_HASH, expiresAt, IP_ADDRESS);

            assertThat(token.isValid()).isTrue();

            token.markAsUsed();

            assertThat(token.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersAndSetters {

        @Test
        @DisplayName("id can be set and retrieved")
        void idCanBeSetAndRetrieved() {
            PasswordResetToken token = new PasswordResetToken();
            UUID id = UUID.randomUUID();

            token.setId(id);

            assertThat(token.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("userId can be set and retrieved")
        void userIdCanBeSetAndRetrieved() {
            PasswordResetToken token = new PasswordResetToken();

            token.setUserId(USER_ID);

            assertThat(token.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("tokenHash can be set and retrieved")
        void tokenHashCanBeSetAndRetrieved() {
            PasswordResetToken token = new PasswordResetToken();

            token.setTokenHash(TOKEN_HASH);

            assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
        }

        @Test
        @DisplayName("expiresAt can be set and retrieved")
        void expiresAtCanBeSetAndRetrieved() {
            PasswordResetToken token = new PasswordResetToken();
            Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);

            token.setExpiresAt(expiresAt);

            assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        }

        @Test
        @DisplayName("createdIp can be set and retrieved")
        void createdIpCanBeSetAndRetrieved() {
            PasswordResetToken token = new PasswordResetToken();

            token.setCreatedIp(IP_ADDRESS);

            assertThat(token.getCreatedIp()).isEqualTo(IP_ADDRESS);
        }
    }
}
