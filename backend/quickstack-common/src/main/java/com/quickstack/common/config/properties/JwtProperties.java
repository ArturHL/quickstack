package com.quickstack.common.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for JWT token generation and validation.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html">OWASP JWT Cheat Sheet</a>
 */
@ConfigurationProperties(prefix = "quickstack.jwt")
@Validated
public class JwtProperties {

    /**
     * Issuer claim for JWT tokens.
     */
    @NotBlank(message = "JWT issuer must be configured")
    private String issuer = "quickstack-pos";

    /**
     * Access token expiration time. Default: 15 minutes.
     * ASVS V3.5.3: Tokens should have short expiration.
     */
    private Duration accessTokenExpiration = Duration.ofMinutes(15);

    /**
     * Refresh token expiration time. Default: 7 days.
     */
    private Duration refreshTokenExpiration = Duration.ofDays(7);

    /**
     * RSA key size in bits. Minimum 2048 for security.
     * ASVS V6.2.1: Use approved cryptographic algorithms.
     */
    @Min(value = 2048, message = "RSA key size must be at least 2048 bits")
    private int keySize = 2048;

    /**
     * Path to RSA private key file (PEM format).
     * If not set, keys will be loaded from environment variables.
     */
    private String privateKeyPath;

    /**
     * Path to RSA public key file (PEM format).
     */
    private String publicKeyPath;

    /**
     * Base64-encoded RSA private key (alternative to file path).
     */
    private String privateKeyBase64;

    /**
     * Base64-encoded RSA public key (alternative to file path).
     */
    private String publicKeyBase64;

    /**
     * Comma-separated list of previous public keys for rotation support.
     * Tokens signed with these keys will still be valid during rotation period.
     */
    private String previousPublicKeysBase64;

    /**
     * Clock skew tolerance for token validation. Default: 30 seconds.
     */
    private Duration clockSkew = Duration.ofSeconds(30);

    // Getters and Setters

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(Duration accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(Duration refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }

    public String getPrivateKeyBase64() {
        return privateKeyBase64;
    }

    public void setPrivateKeyBase64(String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public void setPublicKeyBase64(String publicKeyBase64) {
        this.publicKeyBase64 = publicKeyBase64;
    }

    public String getPreviousPublicKeysBase64() {
        return previousPublicKeysBase64;
    }

    public void setPreviousPublicKeysBase64(String previousPublicKeysBase64) {
        this.previousPublicKeysBase64 = previousPublicKeysBase64;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew;
    }

    /**
     * Returns access token expiration in milliseconds.
     */
    public long getAccessTokenExpirationMillis() {
        return accessTokenExpiration.toMillis();
    }

    /**
     * Returns refresh token expiration in milliseconds.
     */
    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpiration.toMillis();
    }
}
