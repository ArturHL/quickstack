package com.quickstack.common.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for password hashing with Argon2id.
 *
 * ASVS V2.4.1: Passwords must be stored using approved hashing algorithms.
 * ASVS V2.4.5: Additional pepper should be used for defense in depth.
 *
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html">OWASP Password Storage</a>
 */
@ConfigurationProperties(prefix = "quickstack.password")
@Validated
public class PasswordProperties {

    /**
     * Minimum password length. ASVS V2.1.1: At least 12 characters.
     */
    @Min(value = 12, message = "Minimum password length must be at least 12")
    private int minLength = 12;

    /**
     * Maximum password length. ASVS V2.1.2: Allow at least 64, deny over 128.
     */
    @Max(value = 128, message = "Maximum password length cannot exceed 128")
    private int maxLength = 128;

    /**
     * Argon2id configuration following OWASP recommendations.
     */
    private Argon2Config argon2 = new Argon2Config();

    /**
     * Pepper configuration for additional security layer.
     * ASVS V2.4.5: Pepper must be stored separately from password hashes.
     */
    private PepperConfig pepper = new PepperConfig();

    /**
     * HaveIBeenPwned API configuration for breach detection.
     * ASVS V2.1.7: Check passwords against known breaches.
     */
    private HibpConfig hibp = new HibpConfig();

    // Getters and Setters

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public Argon2Config getArgon2() {
        return argon2;
    }

    public void setArgon2(Argon2Config argon2) {
        this.argon2 = argon2;
    }

    public PepperConfig getPepper() {
        return pepper;
    }

    public void setPepper(PepperConfig pepper) {
        this.pepper = pepper;
    }

    public HibpConfig getHibp() {
        return hibp;
    }

    public void setHibp(HibpConfig hibp) {
        this.hibp = hibp;
    }

    /**
     * Argon2id algorithm configuration.
     * Default values follow OWASP recommendations for 2024.
     */
    public static class Argon2Config {

        /**
         * Memory cost in KB. Default: 65536 (64 MB).
         * Higher values increase resistance to GPU attacks.
         */
        @Min(value = 16384, message = "Argon2 memory must be at least 16 MB (16384 KB)")
        private int memory = 65536;

        /**
         * Number of iterations. Default: 3.
         */
        @Min(value = 2, message = "Argon2 iterations must be at least 2")
        private int iterations = 3;

        /**
         * Degree of parallelism. Default: 4.
         */
        @Min(value = 1, message = "Argon2 parallelism must be at least 1")
        private int parallelism = 4;

        /**
         * Salt length in bytes. Default: 16 (128 bits).
         * ASVS V2.4.2: Salt must be at least 32 bits (4 bytes).
         */
        @Min(value = 16, message = "Salt length must be at least 16 bytes")
        private int saltLength = 16;

        /**
         * Hash length in bytes. Default: 32 (256 bits).
         */
        @Min(value = 32, message = "Hash length must be at least 32 bytes")
        private int hashLength = 32;

        // Getters and Setters

        public int getMemory() {
            return memory;
        }

        public void setMemory(int memory) {
            this.memory = memory;
        }

        public int getIterations() {
            return iterations;
        }

        public void setIterations(int iterations) {
            this.iterations = iterations;
        }

        public int getParallelism() {
            return parallelism;
        }

        public void setParallelism(int parallelism) {
            this.parallelism = parallelism;
        }

        public int getSaltLength() {
            return saltLength;
        }

        public void setSaltLength(int saltLength) {
            this.saltLength = saltLength;
        }

        public int getHashLength() {
            return hashLength;
        }

        public void setHashLength(int hashLength) {
            this.hashLength = hashLength;
        }
    }

    /**
     * Pepper configuration for versioned pepper support.
     */
    public static class PepperConfig {

        /**
         * Current pepper value (hex-encoded, 32 bytes recommended).
         * Must be stored in environment variable, never in code.
         */
        private String value;

        /**
         * Current pepper version for rotation support.
         */
        @Min(value = 1, message = "Pepper version must be positive")
        private int version = 1;

        /**
         * Previous pepper values for migration (format: "version:hex,version:hex").
         */
        private String previousVersions;

        // Getters and Setters

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getPreviousVersions() {
            return previousVersions;
        }

        public void setPreviousVersions(String previousVersions) {
            this.previousVersions = previousVersions;
        }

        /**
         * Check if pepper is configured.
         */
        public boolean isConfigured() {
            return value != null && !value.isBlank();
        }
    }

    /**
     * HaveIBeenPwned API configuration.
     */
    public static class HibpConfig {

        /**
         * Enable HIBP password checking. Default: true.
         */
        private boolean enabled = true;

        /**
         * HIBP API base URL.
         */
        private String apiUrl = "https://api.pwnedpasswords.com/range/";

        /**
         * Connection timeout in milliseconds. Default: 3000 (3 seconds).
         */
        @Min(value = 1000, message = "HIBP timeout must be at least 1 second")
        private int timeoutMillis = 3000;

        /**
         * Number of retry attempts. Default: 2.
         */
        @Min(value = 0)
        @Max(value = 5, message = "HIBP retries cannot exceed 5")
        private int retries = 2;

        /**
         * Block registration if HIBP API is unavailable. Default: true.
         * When true, registration fails if breach check cannot be performed.
         */
        private boolean blockOnFailure = true;

        // Getters and Setters

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

        public boolean isBlockOnFailure() {
            return blockOnFailure;
        }

        public void setBlockOnFailure(boolean blockOnFailure) {
            this.blockOnFailure = blockOnFailure;
        }
    }
}
