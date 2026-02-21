package com.quickstack.auth.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quickstack.common.config.properties.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for rate limiting using Bucket4j with Caffeine cache.
 * <p>
 * Provides in-memory rate limiting for authentication endpoints.
 * <p>
 * ASVS Compliance:
 * - V2.2.1: Anti-automation controls
 * - V2.2.2: Rate limiting for brute force protection
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    private final RateLimitProperties properties;

    public RateLimitConfig(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Cache for IP-based rate limiting buckets.
     * <p>
     * Configuration:
     * - Max 10,000 entries (configurable)
     * - TTL of 1 hour (configurable)
     * - LRU eviction when full
     */
    @Bean
    public Cache<String, Bucket> ipRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(properties.getCache().getMaxBuckets())
                .expireAfterAccess(properties.getCache().getTtl().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Cache for email-based rate limiting buckets.
     * <p>
     * Separate cache to prevent IP cache pollution from email-based attacks.
     */
    @Bean
    public Cache<String, Bucket> emailRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(properties.getCache().getMaxBuckets())
                .expireAfterAccess(properties.getCache().getTtl().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Cache for password reset rate limiting buckets.
     */
    @Bean
    public Cache<String, Bucket> passwordResetRateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(properties.getCache().getMaxBuckets())
                .expireAfterAccess(properties.getCache().getTtl().toMillis(), TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Creates a new bucket for IP-based rate limiting.
     * Default: 10 requests per minute.
     */
    public Bucket createIpBucket() {
        RateLimitProperties.BucketConfig config = properties.getIp();
        return createBucket(config);
    }

    /**
     * Creates a new bucket for email-based rate limiting.
     * Default: 5 requests per minute.
     */
    public Bucket createEmailBucket() {
        RateLimitProperties.BucketConfig config = properties.getEmail();
        return createBucket(config);
    }

    /**
     * Creates a new bucket for password reset rate limiting.
     * Default: 3 requests per hour.
     */
    public Bucket createPasswordResetBucket() {
        RateLimitProperties.BucketConfig config = properties.getPasswordReset();
        return createBucket(config);
    }

    /**
     * Gets or creates a bucket for the given IP address.
     *
     * @param ip the IP address
     * @param cache the cache to use
     * @return the bucket for rate limiting
     */
    public Bucket resolveIpBucket(String ip, Cache<String, Bucket> cache) {
        return cache.get(ip, k -> createIpBucket());
    }

    /**
     * Gets or creates a bucket for the given email address.
     *
     * @param email the email address (normalized to lowercase)
     * @param cache the cache to use
     * @return the bucket for rate limiting
     */
    public Bucket resolveEmailBucket(String email, Cache<String, Bucket> cache) {
        String normalizedEmail = email.toLowerCase().trim();
        return cache.get(normalizedEmail, k -> createEmailBucket());
    }

    /**
     * Gets or creates a bucket for password reset rate limiting.
     *
     * @param key the rate limit key (IP or email)
     * @param cache the cache to use
     * @return the bucket for rate limiting
     */
    public Bucket resolvePasswordResetBucket(String key, Cache<String, Bucket> cache) {
        return cache.get(key.toLowerCase().trim(), k -> createPasswordResetBucket());
    }

    /**
     * Check if rate limiting is enabled.
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * Get the IP rate limit configuration.
     */
    public RateLimitProperties.BucketConfig getIpConfig() {
        return properties.getIp();
    }

    /**
     * Get the email rate limit configuration.
     */
    public RateLimitProperties.BucketConfig getEmailConfig() {
        return properties.getEmail();
    }

    /**
     * Get the password reset rate limit configuration.
     */
    public RateLimitProperties.BucketConfig getPasswordResetConfig() {
        return properties.getPasswordReset();
    }

    private Bucket createBucket(RateLimitProperties.BucketConfig config) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.getCapacity())
                .refillGreedy(config.getRefillTokens(), config.getRefillPeriod())
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
