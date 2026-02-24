package com.quickstack.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quickstack.auth.config.RateLimitConfig;
import com.quickstack.common.config.properties.RateLimitProperties;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    private RateLimitProperties properties;
    private RateLimitConfig config;
    private Cache<String, Bucket> ipCache;
    private Cache<String, Bucket> emailCache;
    private Cache<String, Bucket> passwordResetCache;
    private ObjectMapper objectMapper;
    private RateLimitFilter filter;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        config = new RateLimitConfig(properties);

        ipCache = Caffeine.newBuilder().maximumSize(1000).build();
        emailCache = Caffeine.newBuilder().maximumSize(1000).build();
        passwordResetCache = Caffeine.newBuilder().maximumSize(1000).build();

        objectMapper = new ObjectMapper();

        filter = new RateLimitFilter(
                config,
                ipCache,
                emailCache,
                passwordResetCache,
                objectMapper);
    }

    @Nested
    @DisplayName("Non-Rate-Limited Paths")
    class NonRateLimitedPaths {

        @Test
        @DisplayName("allows requests to non-auth endpoints without rate limiting")
        void allowsNonAuthEndpoints() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        @Test
        @DisplayName("allows requests when rate limiting is disabled")
        void allowsWhenDisabled() throws Exception {
            properties.setEnabled(false);
            config = new RateLimitConfig(properties);
            filter = new RateLimitFilter(config, ipCache, emailCache, passwordResetCache, objectMapper);

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Rate-Limited Paths")
    class RateLimitedPaths {

        @Test
        @DisplayName("allows login requests within rate limit")
        void allowsLoginWithinLimit() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
        }

        @Test
        @DisplayName("blocks requests exceeding IP rate limit")
        void blocksExceedingIpLimit() throws Exception {
            String clientIp = "192.168.1.100";

            // Exhaust the rate limit (default 10 requests)
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // 11th request should be blocked
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getHeader("Retry-After")).isNotNull();
            verify(filterChain, times(10)).doFilter(any(), any());
        }

        @Test
        @DisplayName("allows requests from different IPs")
        void allowsDifferentIps() throws Exception {
            // Exhaust rate limit for one IP
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
                request.setRemoteAddr("192.168.1.1");
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // Request from different IP should work
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.168.1.2");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            verify(filterChain, times(11)).doFilter(any(), any());
        }

        @Test
        @DisplayName("rate limits register endpoint")
        void rateLimitsRegister() throws Exception {
            String clientIp = "192.168.1.200";

            // Exhaust the rate limit
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // Next request should be blocked
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    @Nested
    @DisplayName("Password Reset Rate Limiting")
    class PasswordResetRateLimiting {

        @Test
        @DisplayName("applies stricter rate limit to forgot-password endpoint")
        void appliesStricterLimitToForgotPassword() throws Exception {
            String clientIp = "192.168.1.50";

            // Default password reset limit is 3 per hour
            for (int i = 0; i < 3; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/forgot-password");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // 4th request should be blocked by password reset limit
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/forgot-password");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            verify(filterChain, times(3)).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("X-Forwarded-For Header")
    class XForwardedForHeader {

        @Test
        @DisplayName("uses X-Forwarded-For header for IP extraction")
        void usesXForwardedFor() throws Exception {
            String realIp = "203.0.113.50";

            // Exhaust rate limit for real IP
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
                request.setRemoteAddr("10.0.0.1");
                request.addHeader("X-Forwarded-For", realIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // Next request from same real IP should be blocked
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", realIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        @Test
        @DisplayName("rejects invalid X-Forwarded-For values")
        void rejectsInvalidXForwardedFor() throws Exception {
            // Invalid IP in X-Forwarded-For should fall back to remote address
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.168.1.1");
            request.addHeader("X-Forwarded-For", "not-an-ip; DROP TABLE users;--");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            // Should still process request (falls back to remoteAddr)
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("handles X-Forwarded-For chain by extracting first IP")
        void handlesXForwardedForChain() throws Exception {
            String realIp = "203.0.113.99";

            // Exhaust rate limit using first IP in chain
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
                request.setRemoteAddr("10.0.0.1");
                request.addHeader("X-Forwarded-For", realIp + ", 10.10.10.10, 172.16.0.1");
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // Next request with same first IP should be blocked
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", realIp + ", 10.10.10.10");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        @Test
        @DisplayName("handles empty X-Forwarded-For header")
        void handlesEmptyXForwardedFor() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.168.1.99");
            request.addHeader("X-Forwarded-For", "");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            // Should fall back to remoteAddr and process request
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Email Rate Limiting")
    class EmailRateLimiting {

        @Test
        @DisplayName("checkEmailRateLimit returns true within limit")
        void checkEmailRateLimitReturnsTrue() {
            assertThat(filter.checkEmailRateLimit("user@example.com")).isTrue();
        }

        @Test
        @DisplayName("checkEmailRateLimit returns false when exceeded")
        void checkEmailRateLimitReturnsFalse() {
            String email = "attacker@example.com";

            // Exhaust email rate limit (default 5)
            for (int i = 0; i < 5; i++) {
                filter.checkEmailRateLimit(email);
            }

            assertThat(filter.checkEmailRateLimit(email)).isFalse();
        }

        @Test
        @DisplayName("email rate limit is case-insensitive")
        void emailRateLimitCaseInsensitive() {
            // Use different cases
            filter.checkEmailRateLimit("User@Example.com");
            filter.checkEmailRateLimit("USER@EXAMPLE.COM");
            filter.checkEmailRateLimit("user@example.com");

            // All should count against same bucket
            long remaining = filter.getEmailRateLimitRemaining("user@example.com");
            assertThat(remaining).isEqualTo(2); // 5 - 3 = 2
        }

        @Test
        @DisplayName("returns correct retry after seconds")
        void returnsRetryAfterSeconds() {
            long retryAfter = filter.getEmailRetryAfterSeconds();
            assertThat(retryAfter).isEqualTo(60); // 1 minute default
        }
    }

    @Nested
    @DisplayName("Response Body")
    class ResponseBody {

        @Test
        @DisplayName("returns JSON error body when rate limited")
        void returnsJsonErrorBody() throws Exception {
            String clientIp = "192.168.1.75";

            // Exhaust rate limit
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // Trigger rate limit
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);

            String content = response.getContentAsString();
            assertThat(content).contains("RATE_LIMIT_EXCEEDED");
            assertThat(content).contains("retryAfter");
            assertThat(response.getContentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("does not leak internal information in error response")
        void doesNotLeakInternalInfo() throws Exception {
            String clientIp = "192.168.1.76";

            // Exhaust rate limit
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // Trigger rate limit
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, filterChain);

            String content = response.getContentAsString();

            // Should NOT contain internal information
            assertThat(content).doesNotContain("stackTrace");
            assertThat(content).doesNotContain("Exception");
            assertThat(content).doesNotContain("bucket");
            assertThat(content).doesNotContain("caffeine");
            assertThat(content).doesNotContain(clientIp); // Should not reveal user's IP back to them
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class SecurityEdgeCases {

        @Test
        @DisplayName("rate limits reset-password endpoint")
        void rateLimitsResetPassword() throws Exception {
            String clientIp = "192.168.1.201";

            // Exhaust the rate limit
            for (int i = 0; i < 10; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/reset-password");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // Next request should be blocked
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/reset-password");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        }

        @Test
        @DisplayName("maintains separate rate limits for IP and password reset buckets")
        void separateRateLimitsForPasswordReset() throws Exception {
            String clientIp = "192.168.1.202";

            // Use 3 forgot-password requests (exhausts password reset limit)
            for (int i = 0; i < 3; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/forgot-password");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, filterChain);
            }

            // 4th forgot-password should be blocked by password reset limit
            MockHttpServletRequest request4 = new MockHttpServletRequest("POST", "/api/v1/auth/forgot-password");
            request4.setRemoteAddr(clientIp);
            MockHttpServletResponse response4 = new MockHttpServletResponse();
            filter.doFilter(request4, response4, filterChain);
            assertThat(response4.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

            // But regular login should still work (different bucket)
            MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            loginRequest.setRemoteAddr(clientIp);
            MockHttpServletResponse loginResponse = new MockHttpServletResponse();
            filter.doFilter(loginRequest, loginResponse, filterChain);

            assertThat(loginResponse.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        @Test
        @DisplayName("email rate limit handles null email")
        void emailRateLimitHandlesNull() {
            // Should return true (allow) for null email
            assertThat(filter.checkEmailRateLimit(null)).isTrue();
        }
    }
}
