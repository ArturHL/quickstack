package com.quickstack.user.service;

import com.quickstack.common.config.properties.PasswordProperties;
import com.quickstack.common.exception.PasswordValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
// import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for secure password hashing and verification using Argon2id.
 * <p>
 * Security features:
 * - Argon2id algorithm (OWASP recommended)
 * - Pepper application for defense in depth (ASVS V2.4.5)
 * - Timing-safe comparison to prevent timing attacks
 * - Pepper versioning for secure rotation
 * <p>
 * ASVS Requirements:
 * - V2.4.1: Passwords stored using approved hashing (Argon2id)
 * - V2.4.4: Argon2id with memory ≥ 19MB, iterations ≥ 2, parallelism ≥ 1
 * - V2.4.5: Additional pepper used for defense in depth
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private final PasswordProperties properties;
    private final Argon2PasswordEncoder encoder;
    private final Map<Integer, byte[]> pepperVersions;

    public PasswordService(PasswordProperties properties) {
        this.properties = properties;
        this.encoder = createEncoder(properties);
        this.pepperVersions = loadPepperVersions(properties);

        log.info("PasswordService initialized with Argon2id: memory={}KB, iterations={}, parallelism={}",
            properties.getArgon2().getMemory(),
            properties.getArgon2().getIterations(),
            properties.getArgon2().getParallelism());
    }

    /**
     * Hash a password with pepper and Argon2id.
     * <p>
     * The pepper is prepended to the password before hashing.
     * The pepper version is stored with the hash for rotation support.
     *
     * @param plainPassword the password to hash (never logged)
     * @return the hash in format "v{version}${argon2hash}"
     * @throws PasswordValidationException if password doesn't meet policy
     */
    public String hashPassword(String plainPassword) {
        validatePasswordPolicy(plainPassword);

        int pepperVersion = properties.getPepper().getVersion();
        String pepperedPassword = applyPepper(plainPassword, pepperVersion);
        String hash = encoder.encode(pepperedPassword);

        // Prefix with pepper version for future rotation support
        return "v" + pepperVersion + "$" + hash;
    }

    /**
     * Verify a password against a stored hash.
     * <p>
     * Uses timing-safe comparison to prevent timing attacks.
     * Supports verification against previous pepper versions.
     *
     * @param plainPassword the password to verify (never logged)
     * @param storedHash the stored hash (format: "v{version}${argon2hash}")
     * @return true if password matches, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            // Constant time operation to prevent timing attacks on null inputs
            performDummyHash();
            return false;
        }

        try {
            // Parse pepper version from stored hash
            PepperVersionedHash parsed = parseHash(storedHash);

            // Apply pepper with the version used when hash was created
            String pepperedPassword = applyPepper(plainPassword, parsed.pepperVersion());

            // Use Spring Security's timing-safe comparison
            return encoder.matches(pepperedPassword, parsed.hash());
        } catch (Exception e) {
            // Log error but don't expose details
            log.warn("Password verification failed due to hash parsing error");
            performDummyHash();
            return false;
        }
    }

    /**
     * Check if a password meets the security policy.
     *
     * @param password the password to validate
     * @throws PasswordValidationException if validation fails
     */
    public void validatePasswordPolicy(String password) {
        if (password == null || password.isEmpty()) {
            throw PasswordValidationException.tooShort(properties.getMinLength());
        }

        int length = password.length();
        int minLength = properties.getMinLength();
        int maxLength = properties.getMaxLength();

        if (length < minLength) {
            throw PasswordValidationException.tooShort(minLength);
        }

        if (length > maxLength) {
            throw PasswordValidationException.tooLong(maxLength);
        }

        // ASVS V2.1.3: No composition rules - we don't require uppercase, numbers, etc.
        // Length and breach detection are sufficient
    }

    /**
     * Check if a stored hash needs to be rehashed with updated parameters.
     * <p>
     * Used during login to upgrade old hashes transparently.
     *
     * @param storedHash the stored hash
     * @return true if rehash is recommended
     */
    public boolean needsRehash(String storedHash) {
        if (storedHash == null) {
            return false;
        }

        try {
            PepperVersionedHash parsed = parseHash(storedHash);

            // Rehash if using old pepper version
            if (parsed.pepperVersion() < properties.getPepper().getVersion()) {
                return true;
            }

            // Spring Security's Argon2 encoder handles parameter upgrades
            return encoder.upgradeEncoding(parsed.hash());
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Argon2PasswordEncoder createEncoder(PasswordProperties props) {
        var argon2 = props.getArgon2();
        return new Argon2PasswordEncoder(
            argon2.getSaltLength(),
            argon2.getHashLength(),
            argon2.getParallelism(),
            argon2.getMemory(),
            argon2.getIterations()
        );
    }

    private Map<Integer, byte[]> loadPepperVersions(PasswordProperties props) {
        Map<Integer, byte[]> versions = new HashMap<>();

        // Load current pepper
        var pepperConfig = props.getPepper();
        if (pepperConfig.isConfigured()) {
            versions.put(pepperConfig.getVersion(), hexToBytes(pepperConfig.getValue()));
        }

        // Load previous pepper versions for verification
        String previousVersions = pepperConfig.getPreviousVersions();
        if (previousVersions != null && !previousVersions.isBlank()) {
            for (String entry : previousVersions.split(",")) {
                String[] parts = entry.trim().split(":");
                if (parts.length == 2) {
                    try {
                        int version = Integer.parseInt(parts[0].trim());
                        byte[] pepper = hexToBytes(parts[1].trim());
                        versions.put(version, pepper);
                    } catch (Exception e) {
                        log.warn("Invalid previous pepper version format: {}", entry);
                    }
                }
            }
        }

        return versions;
    }

    private String applyPepper(String password, int pepperVersion) {
        byte[] pepper = pepperVersions.get(pepperVersion);
        if (pepper == null) {
            // No pepper configured - use password as-is
            // This is valid for development but should warn
            if (pepperVersions.isEmpty()) {
                log.warn("No pepper configured - passwords will be hashed without pepper");
            } else {
                throw new IllegalStateException("Unknown pepper version: " + pepperVersion);
            }
            return password;
        }

        // Prepend pepper to password
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[pepper.length + passwordBytes.length];
        System.arraycopy(pepper, 0, combined, 0, pepper.length);
        System.arraycopy(passwordBytes, 0, combined, pepper.length, passwordBytes.length);

        return new String(combined, StandardCharsets.UTF_8);
    }

    private PepperVersionedHash parseHash(String storedHash) {
        if (!storedHash.startsWith("v")) {
            // Legacy hash without pepper version - assume version 1
            return new PepperVersionedHash(1, storedHash);
        }

        int dollarPos = storedHash.indexOf('$');
        if (dollarPos < 2) {
            throw new IllegalArgumentException("Invalid hash format");
        }

        int version = Integer.parseInt(storedHash.substring(1, dollarPos));
        String hash = storedHash.substring(dollarPos + 1);

        return new PepperVersionedHash(version, hash);
    }

    /**
     * Perform a dummy hash operation to maintain constant time.
     * This prevents timing attacks that could reveal whether a user exists.
     */
    private void performDummyHash() {
        encoder.encode("dummy-password-for-timing-safety");
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Record to hold parsed pepper version and hash.
     */
    private record PepperVersionedHash(int pepperVersion, String hash) {}
}
