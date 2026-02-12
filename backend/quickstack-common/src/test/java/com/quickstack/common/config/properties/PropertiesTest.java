package com.quickstack.common.config.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for configuration properties classes.
 */
class PropertiesTest {

    @Nested
    @DisplayName("JwtProperties")
    class JwtPropertiesTest {

        @Test
        @DisplayName("should have secure defaults")
        void shouldHaveSecureDefaults() {
            JwtProperties props = new JwtProperties();

            assertThat(props.getIssuer()).isEqualTo("quickstack-pos");
            assertThat(props.getAccessTokenExpiration()).isEqualTo(Duration.ofMinutes(15));
            assertThat(props.getRefreshTokenExpiration()).isEqualTo(Duration.ofDays(7));
            assertThat(props.getKeySize()).isGreaterThanOrEqualTo(2048);
            assertThat(props.getClockSkew()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("should return expiration in milliseconds")
        void shouldReturnExpirationInMillis() {
            JwtProperties props = new JwtProperties();

            assertThat(props.getAccessTokenExpirationMillis()).isEqualTo(15 * 60 * 1000);
            assertThat(props.getRefreshTokenExpirationMillis()).isEqualTo(7 * 24 * 60 * 60 * 1000L);
        }
    }

    @Nested
    @DisplayName("PasswordProperties")
    class PasswordPropertiesTest {

        @Test
        @DisplayName("should have ASVS-compliant defaults")
        void shouldHaveAsvsCompliantDefaults() {
            PasswordProperties props = new PasswordProperties();

            // ASVS V2.1.1: At least 12 characters
            assertThat(props.getMinLength()).isGreaterThanOrEqualTo(12);
            // ASVS V2.1.2: Allow at least 64, deny over 128
            assertThat(props.getMaxLength()).isLessThanOrEqualTo(128);
        }

        @Test
        @DisplayName("should have secure Argon2 defaults")
        void shouldHaveSecureArgon2Defaults() {
            PasswordProperties.Argon2Config argon2 = new PasswordProperties.Argon2Config();

            // OWASP recommendations for 2024
            assertThat(argon2.getMemory()).isGreaterThanOrEqualTo(16384); // At least 16 MB
            assertThat(argon2.getIterations()).isGreaterThanOrEqualTo(2);
            assertThat(argon2.getParallelism()).isGreaterThanOrEqualTo(1);
            // ASVS V2.4.2: Salt at least 32 bits (we use 128 bits)
            assertThat(argon2.getSaltLength()).isGreaterThanOrEqualTo(16);
            assertThat(argon2.getHashLength()).isGreaterThanOrEqualTo(32);
        }

        @Test
        @DisplayName("should have HIBP enabled by default with block on failure")
        void shouldHaveHibpEnabledWithBlockOnFailure() {
            PasswordProperties.HibpConfig hibp = new PasswordProperties.HibpConfig();

            assertThat(hibp.isEnabled()).isTrue();
            assertThat(hibp.isBlockOnFailure()).isTrue();
            assertThat(hibp.getTimeoutMillis()).isLessThanOrEqualTo(5000);
            assertThat(hibp.getRetries()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("pepper should report configuration status")
        void pepperShouldReportConfigurationStatus() {
            PasswordProperties.PepperConfig pepper = new PasswordProperties.PepperConfig();

            assertThat(pepper.isConfigured()).isFalse();

            pepper.setValue("abc123");
            assertThat(pepper.isConfigured()).isTrue();

            pepper.setValue("");
            assertThat(pepper.isConfigured()).isFalse();

            pepper.setValue("   ");
            assertThat(pepper.isConfigured()).isFalse();
        }
    }

    @Nested
    @DisplayName("RateLimitProperties")
    class RateLimitPropertiesTest {

        @Test
        @DisplayName("should have rate limiting enabled by default")
        void shouldHaveRateLimitingEnabled() {
            RateLimitProperties props = new RateLimitProperties();

            assertThat(props.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should have IP rate limit of 10 per minute")
        void shouldHaveIpRateLimitTenPerMinute() {
            RateLimitProperties props = new RateLimitProperties();

            assertThat(props.getIp().getCapacity()).isEqualTo(10);
            assertThat(props.getIp().getRefillPeriod()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("should have email rate limit of 5 per minute")
        void shouldHaveEmailRateLimitFivePerMinute() {
            RateLimitProperties props = new RateLimitProperties();

            assertThat(props.getEmail().getCapacity()).isEqualTo(5);
            assertThat(props.getEmail().getRefillPeriod()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("should have account lockout after 5 attempts for 15 minutes")
        void shouldHaveAccountLockout() {
            RateLimitProperties.LockoutConfig lockout = new RateLimitProperties.LockoutConfig();

            assertThat(lockout.isEnabled()).isTrue();
            assertThat(lockout.getMaxAttempts()).isEqualTo(5);
            assertThat(lockout.getLockoutDuration()).isEqualTo(Duration.ofMinutes(15));
            assertThat(lockout.getLockoutDurationMinutes()).isEqualTo(15);
        }

        @Test
        @DisplayName("bucket should return refill period in seconds")
        void bucketShouldReturnRefillPeriodInSeconds() {
            RateLimitProperties.BucketConfig bucket = new RateLimitProperties.BucketConfig(10, Duration.ofMinutes(1));

            assertThat(bucket.getRefillPeriodSeconds()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("CookieProperties")
    class CookiePropertiesTest {

        @Test
        @DisplayName("should have secure defaults for refresh token cookie")
        void shouldHaveSecureDefaults() {
            CookieProperties.RefreshTokenCookie cookie = new CookieProperties.RefreshTokenCookie();

            // ASVS V3.4.4: Use __Host- prefix
            assertThat(cookie.getName()).startsWith("__Host-");
            // ASVS V3.4.1: Secure flag
            assertThat(cookie.isSecure()).isTrue();
            // ASVS V3.4.2: HttpOnly flag
            assertThat(cookie.isHttpOnly()).isTrue();
            // ASVS V3.4.3: SameSite attribute
            assertThat(cookie.getSameSite()).isEqualTo(CookieProperties.SameSiteMode.STRICT);
        }

        @Test
        @DisplayName("should restrict path to auth endpoints")
        void shouldRestrictPathToAuthEndpoints() {
            CookieProperties.RefreshTokenCookie cookie = new CookieProperties.RefreshTokenCookie();

            assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        }

        @Test
        @DisplayName("should report secure configuration status")
        void shouldReportSecureConfigurationStatus() {
            CookieProperties.RefreshTokenCookie cookie = new CookieProperties.RefreshTokenCookie();

            assertThat(cookie.isSecureConfiguration()).isTrue();

            cookie.setSecure(false);
            assertThat(cookie.isSecureConfiguration()).isFalse();
        }

        @Test
        @DisplayName("should return max age in seconds")
        void shouldReturnMaxAgeInSeconds() {
            CookieProperties.RefreshTokenCookie cookie = new CookieProperties.RefreshTokenCookie();

            assertThat(cookie.getMaxAgeSeconds()).isEqualTo(7 * 24 * 60 * 60);
        }

        @Test
        @DisplayName("SameSiteMode should have correct string values")
        void sameSiteModeShouldHaveCorrectValues() {
            assertThat(CookieProperties.SameSiteMode.STRICT.getValue()).isEqualTo("Strict");
            assertThat(CookieProperties.SameSiteMode.LAX.getValue()).isEqualTo("Lax");
            assertThat(CookieProperties.SameSiteMode.NONE.getValue()).isEqualTo("None");
        }
    }
}
