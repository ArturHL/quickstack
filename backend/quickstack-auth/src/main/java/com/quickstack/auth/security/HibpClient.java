package com.quickstack.auth.security;

import com.quickstack.common.config.properties.PasswordProperties;
import com.quickstack.common.exception.PasswordCompromisedException;
import com.quickstack.common.exception.PasswordValidationException;
import com.quickstack.common.security.PasswordBreachChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * Client for Have I Been Pwned (HIBP) Passwords API.
 * <p>
 * Uses k-Anonymity model to check if a password has been found in data breaches
 * without sending the actual password to the API.
 * <p>
 * Security features:
 * - Only the first 5 characters of the SHA-1 hash are sent (k-Anonymity)
 * - Timeout protection to prevent hanging
 * - Retry with exponential backoff
 * - Blocks registration if API is unavailable (configurable)
 * <p>
 * ASVS V2.1.7: Check passwords against known breaches.
 *
 * @see <a href="https://haveibeenpwned.com/API/v3#SearchingPwnedPasswordsByRange">HIBP API Documentation</a>
 */
@Component
public class HibpClient implements PasswordBreachChecker {

    private static final Logger log = LoggerFactory.getLogger(HibpClient.class);

    private static final int HASH_PREFIX_LENGTH = 5;
    private static final String USER_AGENT = "QuickStack-POS";

    private final PasswordProperties.HibpConfig config;
    private final RestClient restClient;

    public HibpClient(PasswordProperties properties) {
        this.config = properties.getHibp();
        this.restClient = createRestClient();

        log.info("HibpClient initialized: apiUrl={}, enabled={}, blockOnFailure={}",
            config.getApiUrl(), config.isEnabled(), config.isBlockOnFailure());
    }

    /**
     * Check if a password has been found in known data breaches.
     * <p>
     * Uses k-Anonymity: only sends first 5 chars of SHA-1 hash.
     *
     * @param password the password to check (never logged or sent in full)
     * @throws PasswordCompromisedException if password found in breach database
     * @throws PasswordValidationException if breach check fails and blockOnFailure is true
     */
    @Override
    public void checkPassword(String password) {
        if (!config.isEnabled()) {
            log.debug("HIBP check disabled, skipping");
            return;
        }

        if (password == null || password.isEmpty()) {
            return;
        }

        String sha1Hash = sha1Hash(password);
        String hashPrefix = sha1Hash.substring(0, HASH_PREFIX_LENGTH);
        String hashSuffix = sha1Hash.substring(HASH_PREFIX_LENGTH);

        log.debug("Checking password hash prefix: {}...", hashPrefix);

        try {
            String response = fetchHashSuffixes(hashPrefix);
            int breachCount = findBreachCount(response, hashSuffix);

            if (breachCount > 0) {
                log.warn("Password found in {} breach(es)", breachCount);
                throw PasswordCompromisedException.withBreachCount(breachCount);
            }

            log.debug("Password not found in breach database");
        } catch (PasswordCompromisedException e) {
            throw e; // Re-throw compromised exception
        } catch (Exception e) {
            handleApiFailure(e);
        }
    }

    /**
     * Check if a password is compromised.
     *
     * @param password the password to check
     * @return true if password is compromised, false otherwise
     */
    public boolean isCompromised(String password) {
        try {
            checkPassword(password);
            return false;
        } catch (PasswordCompromisedException e) {
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private RestClient createRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()));
        factory.setReadTimeout(Duration.ofMillis(config.getTimeoutMillis()));

        return RestClient.builder()
            .baseUrl(config.getApiUrl())
            .requestFactory(factory)
            .defaultHeader("User-Agent", USER_AGENT)
            .defaultHeader("Add-Padding", "true") // Prevents response size analysis
            .build();
    }

    private String fetchHashSuffixes(String hashPrefix) {
        int maxRetries = config.getRetries();
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                sleepWithBackoff(attempt);
            }

            try {
                String response = restClient.get()
                    .uri(hashPrefix)
                    .retrieve()
                    .body(String.class);

                // Empty string is a valid response (no matches for this prefix)
                return response != null ? response : "";
            } catch (RestClientException e) {
                lastException = e;
                log.warn("HIBP API request failed (attempt {}/{}): {}",
                    attempt + 1, maxRetries + 1, e.getMessage());
            }
        }

        throw new RuntimeException("HIBP API unavailable after " + (maxRetries + 1) + " attempts", lastException);
    }

    private int findBreachCount(String response, String hashSuffix) {
        // Response format: SUFFIX:COUNT\r\n
        // Example: 0018A45C4D1DEF81644B54AB7F969B88D65:21
        String upperSuffix = hashSuffix.toUpperCase();

        for (String line : response.split("\r?\n")) {
            if (line.isEmpty()) continue;

            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String suffix = line.substring(0, colonIndex).trim();
                if (suffix.equalsIgnoreCase(upperSuffix)) {
                    try {
                        return Integer.parseInt(line.substring(colonIndex + 1).trim());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid breach count format: {}", line);
                        return 1; // Assume at least 1 breach if format is wrong
                    }
                }
            }
        }

        return 0; // Not found in breach database
    }

    private void handleApiFailure(Exception e) {
        log.error("HIBP API check failed: {}", e.getMessage());

        if (config.isBlockOnFailure()) {
            throw PasswordValidationException.breachCheckUnavailable();
        }

        // If blockOnFailure is false, allow registration despite API failure
        log.warn("HIBP check skipped due to API failure (blockOnFailure=false)");
    }

    private void sleepWithBackoff(int attempt) {
        try {
            // Exponential backoff: 100ms, 200ms, 400ms, etc.
            long sleepMs = 100L * (1L << (attempt - 1));
            Thread.sleep(Math.min(sleepMs, 1000)); // Cap at 1 second
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String sha1Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1"); // nosemgrep: java.lang.security.audit.crypto.use-of-sha1.use-of-sha1
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
