package com.quickstack.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for security utility classes.
 */
class SecurityUtilsTest {

    @Nested
    @DisplayName("SecureTokenGenerator")
    class SecureTokenGeneratorTest {

        @Test
        @DisplayName("should generate token with default length (32 bytes)")
        void shouldGenerateTokenWithDefaultLength() {
            String token = SecureTokenGenerator.generate();

            // Base64 URL encoding: 32 bytes = ~43 characters
            assertThat(token).isNotBlank();
            assertThat(token.length()).isGreaterThanOrEqualTo(42);
        }

        @Test
        @DisplayName("should generate URL-safe Base64 tokens")
        void shouldGenerateUrlSafeTokens() {
            String token = SecureTokenGenerator.generate();

            // Should only contain URL-safe characters
            assertThat(token).matches("[A-Za-z0-9_-]+");
            // Should not have padding
            assertThat(token).doesNotContain("=");
        }

        @Test
        @DisplayName("should generate unique tokens")
        void shouldGenerateUniqueTokens() {
            Set<String> tokens = new HashSet<>();

            // Generate 1000 tokens and verify all are unique
            for (int i = 0; i < 1000; i++) {
                tokens.add(SecureTokenGenerator.generate());
            }

            assertThat(tokens).hasSize(1000);
        }

        @Test
        @DisplayName("should reject tokens shorter than minimum length")
        void shouldRejectShortTokens() {
            assertThatThrownBy(() -> SecureTokenGenerator.generate(8))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("16 bytes");
        }

        @Test
        @DisplayName("should generate token with custom length")
        void shouldGenerateTokenWithCustomLength() {
            String token = SecureTokenGenerator.generate(64);

            // 64 bytes Base64 = ~86 characters
            assertThat(token.length()).isGreaterThanOrEqualTo(85);
        }

        @Test
        @DisplayName("should generate raw bytes")
        void shouldGenerateRawBytes() {
            byte[] bytes = SecureTokenGenerator.generateBytes(32);

            assertThat(bytes).hasSize(32);
        }

        @Test
        @DisplayName("should generate hex tokens")
        void shouldGenerateHexTokens() {
            String hex = SecureTokenGenerator.generateHex(16);

            // 16 bytes = 32 hex chars
            assertThat(hex).hasSize(32);
            assertThat(hex).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("generated tokens should have sufficient entropy")
        void generatedTokensShouldHaveSufficientEntropy() {
            // Generate token and decode
            String token = SecureTokenGenerator.generate();
            byte[] decoded = Base64.getUrlDecoder().decode(token);

            // Should have at least 256 bits of entropy (32 bytes)
            assertThat(decoded.length).isGreaterThanOrEqualTo(32);
        }
    }

    @Nested
    @DisplayName("IpAddressExtractor")
    class IpAddressExtractorTest {

        @Test
        @DisplayName("should extract IP from X-Forwarded-For header")
        void shouldExtractFromXForwardedFor() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195");

            String ip = IpAddressExtractor.extract(request);

            assertThat(ip).isEqualTo("203.0.113.195");
        }

        @Test
        @DisplayName("should extract first IP from X-Forwarded-For with multiple IPs")
        void shouldExtractFirstFromXForwardedForChain() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For"))
                    .thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");

            String ip = IpAddressExtractor.extract(request);

            // Should return the first (client) IP
            assertThat(ip).isEqualTo("203.0.113.195");
        }

        @Test
        @DisplayName("should extract IP from X-Real-IP header")
        void shouldExtractFromXRealIp() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.178");

            String ip = IpAddressExtractor.extract(request);

            assertThat(ip).isEqualTo("198.51.100.178");
        }

        @Test
        @DisplayName("should fallback to remote address")
        void shouldFallbackToRemoteAddr() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getHeader("X-Real-IP")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.0.2.1");

            String ip = IpAddressExtractor.extract(request);

            assertThat(ip).isEqualTo("192.0.2.1");
        }

        @Test
        @DisplayName("should return unknown for null request")
        void shouldReturnUnknownForNullRequest() {
            String ip = IpAddressExtractor.extract(null);

            assertThat(ip).isEqualTo(IpAddressExtractor.UNKNOWN_IP);
        }

        @Test
        @DisplayName("should reject invalid IP in X-Forwarded-For (header injection)")
        void shouldRejectInvalidIpInXForwardedFor() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            // Attempt header injection
            when(request.getHeader("X-Forwarded-For"))
                    .thenReturn("1.2.3.4\r\nX-Injected: malicious");
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");

            String ip = IpAddressExtractor.extract(request);

            // Should fall back to remote address since X-Forwarded-For is invalid
            assertThat(ip).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("should reject script injection attempts")
        void shouldRejectScriptInjection() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getHeader("X-Forwarded-For"))
                    .thenReturn("<script>alert('xss')</script>");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            String ip = IpAddressExtractor.extract(request);

            assertThat(ip).isEqualTo("127.0.0.1");
            assertThat(ip).doesNotContain("<script>");
        }

        @ParameterizedTest
        @ValueSource(strings = {"192.168.1.1", "10.0.0.1", "172.16.0.1", "127.0.0.1"})
        @DisplayName("should validate IPv4 addresses")
        void shouldValidateIpv4(String ip) {
            assertThat(IpAddressExtractor.isValidIp(ip)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"::1", "2001:db8::1", "fe80::1"})
        @DisplayName("should validate IPv6 addresses")
        void shouldValidateIpv6(String ip) {
            assertThat(IpAddressExtractor.isValidIp(ip)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"not-an-ip", "256.1.1.1", "1.2.3", "", "   "})
        @DisplayName("should reject invalid IP formats")
        void shouldRejectInvalidFormats(String ip) {
            assertThat(IpAddressExtractor.isValidIp(ip)).isFalse();
        }

        @Test
        @DisplayName("should identify loopback addresses")
        void shouldIdentifyLoopback() {
            assertThat(IpAddressExtractor.isLoopback("127.0.0.1")).isTrue();
            assertThat(IpAddressExtractor.isLoopback("127.0.0.5")).isTrue();
            assertThat(IpAddressExtractor.isLoopback("::1")).isTrue();
            assertThat(IpAddressExtractor.isLoopback("192.168.1.1")).isFalse();
        }

        @Test
        @DisplayName("should identify private addresses")
        void shouldIdentifyPrivate() {
            assertThat(IpAddressExtractor.isPrivate("10.0.0.1")).isTrue();
            assertThat(IpAddressExtractor.isPrivate("192.168.1.1")).isTrue();
            assertThat(IpAddressExtractor.isPrivate("172.16.0.1")).isTrue();
            assertThat(IpAddressExtractor.isPrivate("172.31.255.255")).isTrue();
            assertThat(IpAddressExtractor.isPrivate("8.8.8.8")).isFalse();
        }

        @Test
        @DisplayName("should handle very long header values")
        void shouldHandleLongHeaders() {
            HttpServletRequest request = mock(HttpServletRequest.class);
            // Very long value that could cause DoS
            String longValue = "1.2.3.4" + ",5.6.7.8".repeat(1000);
            when(request.getHeader("X-Forwarded-For")).thenReturn(longValue);

            String ip = IpAddressExtractor.extract(request);

            // Should extract first IP without issues
            assertThat(ip).isEqualTo("1.2.3.4");
        }
    }
}
