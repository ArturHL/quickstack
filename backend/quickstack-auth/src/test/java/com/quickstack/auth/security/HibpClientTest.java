package com.quickstack.auth.security;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.quickstack.common.config.properties.PasswordProperties;
import com.quickstack.common.exception.PasswordCompromisedException;
import com.quickstack.common.exception.PasswordValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for HibpClient.
 * Uses WireMock to simulate Have I Been Pwned API responses.
 */
@WireMockTest
class HibpClientTest {

    private HibpClient hibpClient;

    // Test password: "password" -> SHA-1: 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
    // Prefix: 5BAA6, Suffix: 1E4C9B93F3F0682250B6CF8331B7EE68FD8
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_HASH_PREFIX = "5BAA6";
    private static final String TEST_HASH_SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

    // Safe password: "MySecureP@ssw0rd2024!" -> different hash
    private static final String SAFE_PASSWORD = "MySecureP@ssw0rd2024!";

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        PasswordProperties properties = createTestProperties(wmRuntimeInfo.getHttpBaseUrl());
        hibpClient = new HibpClient(properties);
    }

    @Nested
    @DisplayName("Password Checking")
    class PasswordCheckingTests {

        @Test
        @DisplayName("should throw exception when password is compromised")
        void shouldThrowWhenPasswordCompromised(WireMockRuntimeInfo wmRuntimeInfo) {
            // Mock HIBP response containing the test password hash suffix
            stubFor(get(urlEqualTo("/" + TEST_HASH_PREFIX))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(buildHibpResponse(TEST_HASH_SUFFIX, 3861493))));

            assertThatThrownBy(() -> hibpClient.checkPassword(TEST_PASSWORD))
                .isInstanceOf(PasswordCompromisedException.class);
        }

        @Test
        @DisplayName("should not throw when password is not compromised")
        void shouldNotThrowWhenPasswordNotCompromised(WireMockRuntimeInfo wmRuntimeInfo) {
            // Mock HIBP response NOT containing the password hash suffix
            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(buildHibpResponse("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 100))));

            // Should not throw
            hibpClient.checkPassword(SAFE_PASSWORD);
        }

        @Test
        @DisplayName("should return true for compromised password via isCompromised")
        void shouldReturnTrueForCompromisedPassword(WireMockRuntimeInfo wmRuntimeInfo) {
            stubFor(get(urlEqualTo("/" + TEST_HASH_PREFIX))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(buildHibpResponse(TEST_HASH_SUFFIX, 1000))));

            assertThat(hibpClient.isCompromised(TEST_PASSWORD)).isTrue();
        }

        @Test
        @DisplayName("should return false for safe password via isCompromised")
        void shouldReturnFalseForSafePassword(WireMockRuntimeInfo wmRuntimeInfo) {
            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(buildHibpResponse("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 50))));

            assertThat(hibpClient.isCompromised(SAFE_PASSWORD)).isFalse();
        }

        @Test
        @DisplayName("should handle null password gracefully")
        void shouldHandleNullPassword() {
            hibpClient.checkPassword(null);
            // No exception, no API call
        }

        @Test
        @DisplayName("should handle empty password gracefully")
        void shouldHandleEmptyPassword() {
            hibpClient.checkPassword("");
            // No exception, no API call
        }
    }

    @Nested
    @DisplayName("k-Anonymity")
    class KAnonymityTests {

        @Test
        @DisplayName("should only send first 5 characters of hash")
        void shouldOnlySendHashPrefix(WireMockRuntimeInfo wmRuntimeInfo) {
            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(buildHibpResponse("0000000000000000000000000000000000", 1))));

            hibpClient.checkPassword(TEST_PASSWORD);

            // Verify only prefix was sent (5 chars)
            verify(getRequestedFor(urlEqualTo("/" + TEST_HASH_PREFIX)));
        }

        @Test
        @DisplayName("should not send full password hash")
        void shouldNotSendFullHash(WireMockRuntimeInfo wmRuntimeInfo) {
            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA:1")));

            hibpClient.checkPassword(TEST_PASSWORD);

            // Verify only the prefix (5 chars) was sent, not the full hash suffix (35 chars)
            verify(getRequestedFor(urlEqualTo("/" + TEST_HASH_PREFIX)));
        }
    }

    @Nested
    @DisplayName("API Failure Handling")
    class ApiFailureTests {

        @Test
        @DisplayName("should throw when API is unavailable and blockOnFailure is true")
        void shouldThrowWhenApiUnavailableAndBlockOnFailure(WireMockRuntimeInfo wmRuntimeInfo) {
            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(500)));

            assertThatThrownBy(() -> hibpClient.checkPassword(TEST_PASSWORD))
                .isInstanceOf(PasswordValidationException.class);
        }

        @Test
        @DisplayName("should not throw when API unavailable and blockOnFailure is false")
        void shouldNotThrowWhenApiUnavailableAndNoBlock(WireMockRuntimeInfo wmRuntimeInfo) {
            PasswordProperties props = createTestProperties(wmRuntimeInfo.getHttpBaseUrl());
            props.getHibp().setBlockOnFailure(false);
            HibpClient client = new HibpClient(props);

            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(500)));

            // Should not throw when blockOnFailure is false
            client.checkPassword(TEST_PASSWORD);
        }

        @Test
        @DisplayName("should retry on failure")
        void shouldRetryOnFailure(WireMockRuntimeInfo wmRuntimeInfo) {
            // First two requests fail, third succeeds
            stubFor(get(urlEqualTo("/" + TEST_HASH_PREFIX))
                .inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("FirstFailed"));

            stubFor(get(urlEqualTo("/" + TEST_HASH_PREFIX))
                .inScenario("retry")
                .whenScenarioStateIs("FirstFailed")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("SecondFailed"));

            stubFor(get(urlEqualTo("/" + TEST_HASH_PREFIX))
                .inScenario("retry")
                .whenScenarioStateIs("SecondFailed")
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA:1")));

            // Should succeed after retries
            hibpClient.checkPassword(TEST_PASSWORD);

            // Verify 3 requests were made (initial + 2 retries)
            verify(3, getRequestedFor(urlEqualTo("/" + TEST_HASH_PREFIX)));
        }

        @Test
        @DisplayName("should handle timeout")
        void shouldHandleTimeout(WireMockRuntimeInfo wmRuntimeInfo) {
            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withFixedDelay(5000))); // 5 second delay, timeout is 3 seconds

            assertThatThrownBy(() -> hibpClient.checkPassword(TEST_PASSWORD))
                .isInstanceOf(PasswordValidationException.class);
        }
    }

    @Nested
    @DisplayName("Response Parsing")
    class ResponseParsingTests {

        @Test
        @DisplayName("should handle multiline response")
        void shouldHandleMultilineResponse(WireMockRuntimeInfo wmRuntimeInfo) {
            String response = String.join("\r\n",
                "0018A45C4D1DEF81644B54AB7F969B88D65:21",
                TEST_HASH_SUFFIX + ":3861493",
                "00D4F6E8FA6EECAD2A3AA415EEC418D38EC:2",
                "011053FD0102E94D6AE2F8B83D76FAF94F6:2");

            stubFor(get(urlEqualTo("/" + TEST_HASH_PREFIX))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(response)));

            assertThat(hibpClient.isCompromised(TEST_PASSWORD)).isTrue();
        }

        @Test
        @DisplayName("should treat no matching suffix as not compromised")
        void shouldTreatNoMatchAsNotCompromised(WireMockRuntimeInfo wmRuntimeInfo) {
            // Response with a different suffix than our password's hash
            stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC:500")));

            assertThat(hibpClient.isCompromised(TEST_PASSWORD)).isFalse();
        }

        @Test
        @DisplayName("should be case insensitive when matching hash suffix")
        void shouldBeCaseInsensitive(WireMockRuntimeInfo wmRuntimeInfo) {
            // Response with lowercase suffix
            String response = TEST_HASH_SUFFIX.toLowerCase() + ":100";

            stubFor(get(urlEqualTo("/" + TEST_HASH_PREFIX))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withBody(response)));

            assertThat(hibpClient.isCompromised(TEST_PASSWORD)).isTrue();
        }
    }

    @Nested
    @DisplayName("Disabled Mode")
    class DisabledModeTests {

        @Test
        @DisplayName("should not call API when disabled")
        void shouldNotCallApiWhenDisabled(WireMockRuntimeInfo wmRuntimeInfo) {
            PasswordProperties props = createTestProperties(wmRuntimeInfo.getHttpBaseUrl());
            props.getHibp().setEnabled(false);
            HibpClient client = new HibpClient(props);

            client.checkPassword(TEST_PASSWORD);

            // Verify no API calls were made
            verify(0, getRequestedFor(urlPathMatching("/.*")));
        }
    }

    // -------------------------------------------------------------------------
    // Test Helpers
    // -------------------------------------------------------------------------

    private PasswordProperties createTestProperties(String baseUrl) {
        PasswordProperties props = new PasswordProperties();

        var hibp = new PasswordProperties.HibpConfig();
        hibp.setEnabled(true);
        hibp.setApiUrl(baseUrl + "/");
        hibp.setTimeoutMillis(3000);
        hibp.setRetries(2);
        hibp.setBlockOnFailure(true);
        props.setHibp(hibp);

        return props;
    }

    private String buildHibpResponse(String hashSuffix, int breachCount) {
        // HIBP response format: SUFFIX:COUNT
        return hashSuffix + ":" + breachCount;
    }
}
