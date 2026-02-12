package com.quickstack.common.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates cryptographically secure random tokens.
 *
 * Used for:
 * - Refresh tokens
 * - Password reset tokens
 * - Email verification tokens
 * - JWT ID (jti) claims
 *
 * ASVS V2.6.2: Tokens must have sufficient randomness (112+ bits of entropy).
 * ASVS V3.2.2: Session tokens must have at least 64 bits of entropy.
 */
public final class SecureTokenGenerator {

    /**
     * Default token length in bytes (32 bytes = 256 bits of entropy).
     * Exceeds ASVS minimum of 112 bits.
     */
    public static final int DEFAULT_TOKEN_LENGTH = 32;

    /**
     * Minimum token length in bytes (16 bytes = 128 bits).
     */
    public static final int MIN_TOKEN_LENGTH = 16;

    /**
     * Thread-safe SecureRandom instance.
     * Uses the platform's strongest available source of randomness.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Base64 URL encoder without padding for URL-safe tokens.
     */
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecureTokenGenerator() {
        // Utility class, prevent instantiation
    }

    /**
     * Generates a secure random token with default length (32 bytes).
     *
     * @return URL-safe Base64 encoded token
     */
    public static String generate() {
        return generate(DEFAULT_TOKEN_LENGTH);
    }

    /**
     * Generates a secure random token with specified length.
     *
     * @param lengthInBytes number of random bytes (minimum 16)
     * @return URL-safe Base64 encoded token
     * @throws IllegalArgumentException if length is less than minimum
     */
    public static String generate(int lengthInBytes) {
        if (lengthInBytes < MIN_TOKEN_LENGTH) {
            throw new IllegalArgumentException(
                    "Token length must be at least " + MIN_TOKEN_LENGTH + " bytes for security"
            );
        }

        byte[] randomBytes = new byte[lengthInBytes];
        SECURE_RANDOM.nextBytes(randomBytes);
        return URL_ENCODER.encodeToString(randomBytes);
    }

    /**
     * Generates raw random bytes for cases where Base64 encoding is not needed.
     *
     * @param lengthInBytes number of random bytes
     * @return array of random bytes
     */
    public static byte[] generateBytes(int lengthInBytes) {
        if (lengthInBytes < MIN_TOKEN_LENGTH) {
            throw new IllegalArgumentException(
                    "Token length must be at least " + MIN_TOKEN_LENGTH + " bytes for security"
            );
        }

        byte[] randomBytes = new byte[lengthInBytes];
        SECURE_RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Generates a hex-encoded token.
     * Useful for tokens that need to be case-insensitive in URLs.
     *
     * @param lengthInBytes number of random bytes
     * @return hex-encoded token (lowercase)
     */
    public static String generateHex(int lengthInBytes) {
        byte[] randomBytes = generateBytes(lengthInBytes);
        return bytesToHex(randomBytes);
    }

    /**
     * Generates a hex-encoded token with default length.
     *
     * @return hex-encoded token (lowercase)
     */
    public static String generateHex() {
        return generateHex(DEFAULT_TOKEN_LENGTH);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
