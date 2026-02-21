package com.quickstack.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.quickstack.auth.config.RateLimitConfig;
import com.quickstack.common.config.properties.RateLimitProperties;
import com.quickstack.common.security.IpAddressExtractor;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Filter for rate limiting authentication endpoints.
 * <p>
 * Applies IP-based rate limiting to prevent brute force attacks.
 * Optionally applies email-based rate limiting for login/register/forgot-password.
 * <p>
 * ASVS Compliance:
 * - V2.2.1: Anti-automation controls
 * - V2.2.2: Rate limiting for brute force protection
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /**
     * Endpoints subject to rate limiting.
     */
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    );

    /**
     * Endpoints subject to password reset rate limiting (more restrictive).
     */
    private static final Set<String> PASSWORD_RESET_PATHS = Set.of(
            "/api/v1/auth/forgot-password"
    );

    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";

    private final RateLimitConfig rateLimitConfig;
    private final Cache<String, Bucket> ipCache;
    private final Cache<String, Bucket> emailCache;
    private final Cache<String, Bucket> passwordResetCache;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitConfig rateLimitConfig,
            Cache<String, Bucket> ipRateLimitCache,
            Cache<String, Bucket> emailRateLimitCache,
            Cache<String, Bucket> passwordResetRateLimitCache,
            ObjectMapper objectMapper
    ) {
        this.rateLimitConfig = rateLimitConfig;
        this.ipCache = ipRateLimitCache;
        this.emailCache = emailRateLimitCache;
        this.passwordResetCache = passwordResetRateLimitCache;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip if rate limiting is disabled
        if (!rateLimitConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Only apply to rate limited paths
        if (!shouldRateLimit(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = IpAddressExtractor.extract(request);

        // Check IP-based rate limit first
        if (!checkIpRateLimit(clientIp, response)) {
            log.warn("IP rate limit exceeded for {} on path {}", clientIp, path);
            return;
        }

        // For password reset, apply additional stricter rate limit
        if (isPasswordResetPath(path)) {
            if (!checkPasswordResetRateLimit(clientIp, response)) {
                log.warn("Password reset rate limit exceeded for {} on path {}", clientIp, path);
                return;
            }
        }

        // Add remaining tokens header for debugging/client info
        addRateLimitHeaders(response, clientIp);

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the path should be rate limited.
     */
    private boolean shouldRateLimit(String path) {
        return RATE_LIMITED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Check if the path is a password reset path (stricter limits).
     */
    private boolean isPasswordResetPath(String path) {
        return PASSWORD_RESET_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Check IP-based rate limit.
     *
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean checkIpRateLimit(String clientIp, HttpServletResponse response) throws IOException {
        Bucket bucket = rateLimitConfig.resolveIpBucket(clientIp, ipCache);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            sendRateLimitResponse(response, waitSeconds, "IP");
            return false;
        }

        return true;
    }

    /**
     * Check password reset rate limit (more restrictive: 3 per hour).
     *
     * @return true if request is allowed, false if rate limit exceeded
     */
    private boolean checkPasswordResetRateLimit(String clientIp, HttpServletResponse response) throws IOException {
        Bucket bucket = rateLimitConfig.resolvePasswordResetBucket(clientIp, passwordResetCache);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            sendRateLimitResponse(response, waitSeconds, "password_reset");
            return false;
        }

        return true;
    }

    /**
     * Add rate limit information headers.
     */
    private void addRateLimitHeaders(HttpServletResponse response, String clientIp) {
        Bucket bucket = rateLimitConfig.resolveIpBucket(clientIp, ipCache);
        response.setHeader(X_RATE_LIMIT_REMAINING, String.valueOf(bucket.getAvailableTokens()));
    }

    /**
     * Send 429 Too Many Requests response.
     */
    private void sendRateLimitResponse(HttpServletResponse response, long retryAfterSeconds, String limitType)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));

        Map<String, Object> errorBody = Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "code", "RATE_LIMIT_EXCEEDED",
                "message", "Too many requests. Please try again later.",
                "retryAfter", retryAfterSeconds,
                "limitType", limitType
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }

    /**
     * Check email-based rate limit.
     * Can be called by controllers for additional email-based limiting.
     *
     * @param email the email address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean checkEmailRateLimit(String email) {
        if (!rateLimitConfig.isEnabled() || email == null) {
            return true;
        }

        Bucket bucket = rateLimitConfig.resolveEmailBucket(email, emailCache);
        return bucket.tryConsume(1);
    }

    /**
     * Get remaining tokens for an email.
     *
     * @param email the email address
     * @return remaining tokens
     */
    public long getEmailRateLimitRemaining(String email) {
        if (email == null) {
            return 0;
        }
        Bucket bucket = rateLimitConfig.resolveEmailBucket(email, emailCache);
        return bucket.getAvailableTokens();
    }

    /**
     * Get the retry after seconds for email rate limit.
     *
     * @return retry after seconds
     */
    public long getEmailRetryAfterSeconds() {
        RateLimitProperties.BucketConfig config = rateLimitConfig.getEmailConfig();
        return config.getRefillPeriodSeconds();
    }
}
