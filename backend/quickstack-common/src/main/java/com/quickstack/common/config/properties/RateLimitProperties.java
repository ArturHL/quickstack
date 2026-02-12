package com.quickstack.common.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for rate limiting with Bucket4j.
 *
 * ASVS V2.2.1: Implement anti-automation controls for authentication.
 * ASVS V2.2.2: Use rate limiting to prevent brute force attacks.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#protect-against-automated-attacks">OWASP Auth Cheat Sheet</a>
 */
@ConfigurationProperties(prefix = "quickstack.rate-limit")
@Validated
public class RateLimitProperties {

    /**
     * Enable rate limiting. Default: true.
     */
    private boolean enabled = true;

    /**
     * Rate limit configuration by IP address.
     */
    private BucketConfig ip = new BucketConfig(10, Duration.ofMinutes(1));

    /**
     * Rate limit configuration by email address.
     * More restrictive to prevent credential stuffing.
     */
    private BucketConfig email = new BucketConfig(5, Duration.ofMinutes(1));

    /**
     * Rate limit for password reset requests.
     */
    private BucketConfig passwordReset = new BucketConfig(3, Duration.ofHours(1));

    /**
     * Account lockout configuration.
     */
    private LockoutConfig lockout = new LockoutConfig();

    /**
     * Cache configuration for rate limit buckets.
     */
    private CacheConfig cache = new CacheConfig();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BucketConfig getIp() {
        return ip;
    }

    public void setIp(BucketConfig ip) {
        this.ip = ip;
    }

    public BucketConfig getEmail() {
        return email;
    }

    public void setEmail(BucketConfig email) {
        this.email = email;
    }

    public BucketConfig getPasswordReset() {
        return passwordReset;
    }

    public void setPasswordReset(BucketConfig passwordReset) {
        this.passwordReset = passwordReset;
    }

    public LockoutConfig getLockout() {
        return lockout;
    }

    public void setLockout(LockoutConfig lockout) {
        this.lockout = lockout;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    /**
     * Token bucket configuration.
     */
    public static class BucketConfig {

        /**
         * Maximum number of tokens (requests) allowed.
         */
        @Min(value = 1, message = "Bucket capacity must be at least 1")
        private int capacity;

        /**
         * Time window for token refill.
         */
        private Duration refillPeriod;

        /**
         * Number of tokens to add on each refill. Default: same as capacity.
         */
        @Min(value = 1, message = "Refill tokens must be at least 1")
        private int refillTokens;

        public BucketConfig() {
            this.capacity = 10;
            this.refillPeriod = Duration.ofMinutes(1);
            this.refillTokens = 10;
        }

        public BucketConfig(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
            this.refillTokens = capacity;
        }

        // Getters and Setters

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public Duration getRefillPeriod() {
            return refillPeriod;
        }

        public void setRefillPeriod(Duration refillPeriod) {
            this.refillPeriod = refillPeriod;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        /**
         * Returns refill period in seconds for Retry-After header.
         */
        public long getRefillPeriodSeconds() {
            return refillPeriod.toSeconds();
        }
    }

    /**
     * Account lockout configuration.
     * ASVS V2.2.1: Implement account lockout after failed attempts.
     */
    public static class LockoutConfig {

        /**
         * Enable account lockout. Default: true.
         */
        private boolean enabled = true;

        /**
         * Maximum failed login attempts before lockout.
         */
        @Min(value = 3, message = "Max attempts must be at least 3")
        @Max(value = 10, message = "Max attempts cannot exceed 10")
        private int maxAttempts = 5;

        /**
         * Duration of account lockout.
         */
        private Duration lockoutDuration = Duration.ofMinutes(15);

        /**
         * Time window to count failed attempts.
         */
        private Duration attemptWindow = Duration.ofMinutes(15);

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getLockoutDuration() {
            return lockoutDuration;
        }

        public void setLockoutDuration(Duration lockoutDuration) {
            this.lockoutDuration = lockoutDuration;
        }

        public Duration getAttemptWindow() {
            return attemptWindow;
        }

        public void setAttemptWindow(Duration attemptWindow) {
            this.attemptWindow = attemptWindow;
        }

        /**
         * Returns lockout duration in minutes.
         */
        public long getLockoutDurationMinutes() {
            return lockoutDuration.toMinutes();
        }
    }

    /**
     * Cache configuration for storing rate limit buckets.
     */
    public static class CacheConfig {

        /**
         * Maximum number of buckets to keep in cache.
         * Prevents memory exhaustion from distributed attacks.
         */
        @Min(value = 1000, message = "Max buckets must be at least 1000")
        private int maxBuckets = 10000;

        /**
         * Time to live for cache entries.
         */
        private Duration ttl = Duration.ofHours(1);

        // Getters and Setters

        public int getMaxBuckets() {
            return maxBuckets;
        }

        public void setMaxBuckets(int maxBuckets) {
            this.maxBuckets = maxBuckets;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
