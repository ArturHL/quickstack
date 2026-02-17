package com.quickstack.app.config;

import com.quickstack.common.config.properties.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for JWT RSA key management.
 * <p>
 * Supports loading keys from:
 * - Base64-encoded environment variables (preferred for production)
 * - PEM files (for development)
 * <p>
 * ASVS Compliance:
 * - V6.2.1: RSA keys must be at least 2048 bits
 * - V3.5.3: Supports key rotation with previous public keys
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
    private static final int MIN_RSA_KEY_SIZE_BITS = 2048;
    private static final String RSA_ALGORITHM = "RSA";

    private final JwtProperties properties;
    private final Environment environment;

    public JwtConfig(JwtProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * RSA private key for signing JWT tokens.
     * <p>
     * Loaded from either:
     * 1. Base64-encoded string (quickstack.jwt.private-key-base64)
     * 2. PEM file path (quickstack.jwt.private-key-path)
     *
     * @return RSA private key
     * @throws IllegalStateException if key cannot be loaded or is invalid
     */
    @Bean
    public RSAPrivateKey rsaPrivateKey() {
        String base64Key = properties.getPrivateKeyBase64();
        String keyPath = properties.getPrivateKeyPath();

        byte[] keyBytes;

        if (StringUtils.hasText(base64Key)) {
            log.debug("Loading RSA private key from Base64 configuration");
            keyBytes = decodeBase64Key(base64Key);
        } else if (StringUtils.hasText(keyPath)) {
            log.debug("Loading RSA private key from file: {}", keyPath);
            keyBytes = loadKeyFromPemFile(keyPath, true);
        } else {
            throw new IllegalStateException(
                    "JWT private key not configured. Set either quickstack.jwt.private-key-base64 " +
                    "or quickstack.jwt.private-key-path");
        }

        RSAPrivateKey privateKey = parsePrivateKey(keyBytes);
        validateKeySize(privateKey.getModulus().bitLength(), "private");

        return privateKey;
    }

    /**
     * RSA public key for verifying JWT tokens.
     *
     * @return RSA public key
     * @throws IllegalStateException if key cannot be loaded or is invalid
     */
    @Bean
    public RSAPublicKey rsaPublicKey() {
        String base64Key = properties.getPublicKeyBase64();
        String keyPath = properties.getPublicKeyPath();

        byte[] keyBytes;

        if (StringUtils.hasText(base64Key)) {
            log.debug("Loading RSA public key from Base64 configuration");
            keyBytes = decodeBase64Key(base64Key);
        } else if (StringUtils.hasText(keyPath)) {
            log.debug("Loading RSA public key from file: {}", keyPath);
            keyBytes = loadKeyFromPemFile(keyPath, false);
        } else {
            throw new IllegalStateException(
                    "JWT public key not configured. Set either quickstack.jwt.public-key-base64 " +
                    "or quickstack.jwt.public-key-path");
        }

        RSAPublicKey publicKey = parsePublicKey(keyBytes);
        validateKeySize(publicKey.getModulus().bitLength(), "public");

        return publicKey;
    }

    /**
     * List of previous public keys for token validation during key rotation.
     * <p>
     * Tokens signed with previous keys will still be valid until they expire.
     * This enables seamless key rotation without invalidating existing tokens.
     *
     * @return list of previous RSA public keys (may be empty)
     */
    @Bean
    public List<RSAPublicKey> previousPublicKeys() {
        String previousKeysBase64 = properties.getPreviousPublicKeysBase64();

        if (!StringUtils.hasText(previousKeysBase64)) {
            return Collections.emptyList();
        }

        List<RSAPublicKey> previousKeys = new ArrayList<>();
        String[] keyStrings = previousKeysBase64.split(",");

        for (int i = 0; i < keyStrings.length; i++) {
            String keyBase64 = keyStrings[i].trim();
            if (!StringUtils.hasText(keyBase64)) {
                continue;
            }

            try {
                byte[] keyBytes = decodeBase64Key(keyBase64);
                RSAPublicKey key = parsePublicKey(keyBytes);
                validateKeySize(key.getModulus().bitLength(), "previous public [" + i + "]");
                previousKeys.add(key);
                log.debug("Loaded previous public key {} for rotation support", i);
            } catch (Exception e) {
                log.warn("Failed to load previous public key at index {}: {}", i, e.getMessage());
                // Continue loading other keys - don't fail completely
            }
        }

        if (!previousKeys.isEmpty()) {
            log.info("Loaded {} previous public key(s) for rotation support", previousKeys.size());
        }

        return Collections.unmodifiableList(previousKeys);
    }

    /**
     * Decodes a Base64-encoded key.
     * Supports both standard and URL-safe Base64.
     */
    private byte[] decodeBase64Key(String base64Key) {
        String cleanKey = base64Key.trim();

        try {
            // Try standard Base64 first
            return Base64.getDecoder().decode(cleanKey);
        } catch (IllegalArgumentException e) {
            // Fall back to URL-safe Base64
            try {
                return Base64.getUrlDecoder().decode(cleanKey);
            } catch (IllegalArgumentException e2) {
                throw new IllegalStateException("Invalid Base64 encoding for key", e2);
            }
        }
    }

    /**
     * Loads a key from a PEM file, stripping headers and decoding Base64.
     */
    private byte[] loadKeyFromPemFile(String path, boolean isPrivate) {
        try {
            String pemContent = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            return parsePemContent(pemContent, isPrivate);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read key file: " + path, e);
        }
    }

    /**
     * Parses PEM content, removing headers/footers and decoding Base64.
     */
    private byte[] parsePemContent(String pemContent, boolean isPrivate) {
        String beginMarker = isPrivate ? "-----BEGIN PRIVATE KEY-----" : "-----BEGIN PUBLIC KEY-----";
        String endMarker = isPrivate ? "-----END PRIVATE KEY-----" : "-----END PUBLIC KEY-----";

        // Also support RSA-specific headers
        String rsaBeginMarker = isPrivate ? "-----BEGIN RSA PRIVATE KEY-----" : "-----BEGIN RSA PUBLIC KEY-----";
        String rsaEndMarker = isPrivate ? "-----END RSA PRIVATE KEY-----" : "-----END RSA PUBLIC KEY-----";

        String content = pemContent
                .replace(beginMarker, "")
                .replace(endMarker, "")
                .replace(rsaBeginMarker, "")
                .replace(rsaEndMarker, "")
                .replaceAll("\\s", "");

        if (content.isEmpty()) {
            throw new IllegalStateException("PEM file appears to be empty or has invalid format");
        }

        return Base64.getDecoder().decode(content);
    }

    /**
     * Parses raw bytes into an RSA private key.
     */
    private RSAPrivateKey parsePrivateKey(byte[] keyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid RSA private key format. Ensure key is in PKCS#8 format", e);
        }
    }

    /**
     * Parses raw bytes into an RSA public key.
     */
    private RSAPublicKey parsePublicKey(byte[] keyBytes) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid RSA public key format. Ensure key is in X.509 format", e);
        }
    }

    /**
     * Validates that the key meets minimum size requirements.
     * ASVS V6.2.1: RSA keys must be at least 2048 bits.
     */
    private void validateKeySize(int keySizeBits, String keyType) {
        if (keySizeBits < MIN_RSA_KEY_SIZE_BITS) {
            throw new IllegalStateException(
                    String.format("RSA %s key size (%d bits) is below minimum required (%d bits). " +
                                  "ASVS V6.2.1 requires at least 2048-bit RSA keys",
                            keyType, keySizeBits, MIN_RSA_KEY_SIZE_BITS));
        }
        log.debug("Validated RSA {} key: {} bits", keyType, keySizeBits);
    }
}
