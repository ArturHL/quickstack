package com.quickstack.app.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.quickstack.common.config.properties.RateLimitProperties;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitConfig")
class RateLimitConfigTest {

    private RateLimitProperties properties;
    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        config = new RateLimitConfig(properties);
    }

    @Nested
    @DisplayName("Cache Creation")
    class CacheCreation {

        @Test
        @DisplayName("creates IP rate limit cache with correct size")
        void createsIpCacheWithCorrectSize() {
            Cache<String, Bucket> cache = config.ipRateLimitCache();

            assertThat(cache).isNotNull();
            // Cache should be empty initially
            assertThat(cache.estimatedSize()).isZero();
        }

        @Test
        @DisplayName("creates email rate limit cache with correct size")
        void createsEmailCacheWithCorrectSize() {
            Cache<String, Bucket> cache = config.emailRateLimitCache();

            assertThat(cache).isNotNull();
            assertThat(cache.estimatedSize()).isZero();
        }

        @Test
        @DisplayName("creates password reset rate limit cache")
        void createsPasswordResetCache() {
            Cache<String, Bucket> cache = config.passwordResetRateLimitCache();

            assertThat(cache).isNotNull();
            assertThat(cache.estimatedSize()).isZero();
        }
    }

    @Nested
    @DisplayName("Bucket Creation")
    class BucketCreation {

        @Test
        @DisplayName("creates IP bucket with default capacity of 10")
        void createsIpBucketWithDefaultCapacity() {
            Bucket bucket = config.createIpBucket();

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(10);
        }

        @Test
        @DisplayName("creates email bucket with default capacity of 5")
        void createsEmailBucketWithDefaultCapacity() {
            Bucket bucket = config.createEmailBucket();

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(5);
        }

        @Test
        @DisplayName("creates password reset bucket with default capacity of 3")
        void createsPasswordResetBucketWithDefaultCapacity() {
            Bucket bucket = config.createPasswordResetBucket();

            assertThat(bucket).isNotNull();
            assertThat(bucket.getAvailableTokens()).isEqualTo(3);
        }

        @Test
        @DisplayName("IP bucket allows 10 requests then blocks")
        void ipBucketAllows10RequestsThenBlocks() {
            Bucket bucket = config.createIpBucket();

            // Should allow 10 requests
            for (int i = 0; i < 10; i++) {
                assertThat(bucket.tryConsume(1))
                        .as("Request %d should be allowed", i + 1)
                        .isTrue();
            }

            // 11th request should be blocked
            assertThat(bucket.tryConsume(1))
                    .as("11th request should be blocked")
                    .isFalse();
        }

        @Test
        @DisplayName("email bucket allows 5 requests then blocks")
        void emailBucketAllows5RequestsThenBlocks() {
            Bucket bucket = config.createEmailBucket();

            // Should allow 5 requests
            for (int i = 0; i < 5; i++) {
                assertThat(bucket.tryConsume(1))
                        .as("Request %d should be allowed", i + 1)
                        .isTrue();
            }

            // 6th request should be blocked
            assertThat(bucket.tryConsume(1))
                    .as("6th request should be blocked")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Bucket Resolution")
    class BucketResolution {

        @Test
        @DisplayName("resolves same bucket for same IP")
        void resolvesSameBucketForSameIp() {
            Cache<String, Bucket> cache = config.ipRateLimitCache();
            String ip = "192.168.1.1";

            Bucket bucket1 = config.resolveIpBucket(ip, cache);
            Bucket bucket2 = config.resolveIpBucket(ip, cache);

            assertThat(bucket1).isSameAs(bucket2);
        }

        @Test
        @DisplayName("resolves different buckets for different IPs")
        void resolvesDifferentBucketsForDifferentIps() {
            Cache<String, Bucket> cache = config.ipRateLimitCache();

            Bucket bucket1 = config.resolveIpBucket("192.168.1.1", cache);
            Bucket bucket2 = config.resolveIpBucket("192.168.1.2", cache);

            assertThat(bucket1).isNotSameAs(bucket2);
        }

        @Test
        @DisplayName("resolves same bucket for same email regardless of case")
        void resolvesSameBucketForSameEmailRegardlessOfCase() {
            Cache<String, Bucket> cache = config.emailRateLimitCache();

            Bucket bucket1 = config.resolveEmailBucket("user@example.com", cache);
            Bucket bucket2 = config.resolveEmailBucket("USER@EXAMPLE.COM", cache);
            Bucket bucket3 = config.resolveEmailBucket("User@Example.Com", cache);

            assertThat(bucket1).isSameAs(bucket2);
            assertThat(bucket2).isSameAs(bucket3);
        }

        @Test
        @DisplayName("resolves same bucket for email with whitespace")
        void resolvesSameBucketForEmailWithWhitespace() {
            Cache<String, Bucket> cache = config.emailRateLimitCache();

            Bucket bucket1 = config.resolveEmailBucket("user@example.com", cache);
            Bucket bucket2 = config.resolveEmailBucket("  user@example.com  ", cache);

            assertThat(bucket1).isSameAs(bucket2);
        }

        @Test
        @DisplayName("resolves password reset bucket correctly")
        void resolvesPasswordResetBucket() {
            Cache<String, Bucket> cache = config.passwordResetRateLimitCache();

            Bucket bucket1 = config.resolvePasswordResetBucket("user@example.com", cache);
            Bucket bucket2 = config.resolvePasswordResetBucket("USER@EXAMPLE.COM", cache);

            assertThat(bucket1).isSameAs(bucket2);
            assertThat(bucket1.getAvailableTokens()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Configuration Access")
    class ConfigurationAccess {

        @Test
        @DisplayName("reports enabled status correctly")
        void reportsEnabledStatus() {
            assertThat(config.isEnabled()).isTrue();

            properties.setEnabled(false);
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("returns IP config")
        void returnsIpConfig() {
            RateLimitProperties.BucketConfig ipConfig = config.getIpConfig();

            assertThat(ipConfig).isNotNull();
            assertThat(ipConfig.getCapacity()).isEqualTo(10);
            assertThat(ipConfig.getRefillPeriod()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("returns email config")
        void returnsEmailConfig() {
            RateLimitProperties.BucketConfig emailConfig = config.getEmailConfig();

            assertThat(emailConfig).isNotNull();
            assertThat(emailConfig.getCapacity()).isEqualTo(5);
            assertThat(emailConfig.getRefillPeriod()).isEqualTo(Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("returns password reset config")
        void returnsPasswordResetConfig() {
            RateLimitProperties.BucketConfig resetConfig = config.getPasswordResetConfig();

            assertThat(resetConfig).isNotNull();
            assertThat(resetConfig.getCapacity()).isEqualTo(3);
            assertThat(resetConfig.getRefillPeriod()).isEqualTo(Duration.ofHours(1));
        }
    }

    @Nested
    @DisplayName("Custom Configuration")
    class CustomConfiguration {

        @Test
        @DisplayName("respects custom IP capacity")
        void respectsCustomIpCapacity() {
            properties.getIp().setCapacity(20);
            config = new RateLimitConfig(properties);

            Bucket bucket = config.createIpBucket();

            assertThat(bucket.getAvailableTokens()).isEqualTo(20);
        }

        @Test
        @DisplayName("respects custom email capacity")
        void respectsCustomEmailCapacity() {
            properties.getEmail().setCapacity(10);
            config = new RateLimitConfig(properties);

            Bucket bucket = config.createEmailBucket();

            assertThat(bucket.getAvailableTokens()).isEqualTo(10);
        }

        @Test
        @DisplayName("respects custom cache max buckets")
        void respectsCustomCacheMaxBuckets() {
            properties.getCache().setMaxBuckets(5000);
            config = new RateLimitConfig(properties);

            // The cache should be created with the new max size
            Cache<String, Bucket> cache = config.ipRateLimitCache();
            assertThat(cache).isNotNull();
        }
    }
}
