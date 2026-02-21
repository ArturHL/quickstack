package com.quickstack.common.security;

import com.quickstack.common.config.properties.PasswordProperties;
import com.quickstack.common.exception.PasswordValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PasswordService.
 * <p>
 * Tests cover:
 * - Argon2id hashing with correct parameters
 * - Unique hashes for same password (salt)
 * - Password verification
 * - Timing attack mitigation
 * - Pepper application and versioning
 * - Password policy validation
 */
class PasswordServiceTest {

    private PasswordService passwordService;
    private PasswordProperties properties;

    @BeforeEach
    void setUp() {
        properties = createDefaultProperties();
        passwordService = new PasswordService(properties);
    }

    @Nested
    @DisplayName("Password Hashing")
    class HashingTests {

        @Test
        @DisplayName("should hash password with Argon2id")
        void shouldHashPasswordWithArgon2id() {
            String password = "SecurePassword123!";

            String hash = passwordService.hashPassword(password);

            assertThat(hash).isNotBlank();
            // Should contain pepper version prefix
            assertThat(hash).startsWith("v1$");
            // Should contain Argon2id identifier
            assertThat(hash).contains("$argon2id$");
        }

        @Test
        @DisplayName("should generate unique hashes for same password (salt)")
        void shouldGenerateUniqueHashes() {
            String password = "SamePasswordTest123";
            Set<String> hashes = new HashSet<>();

            // Generate 100 hashes of the same password
            for (int i = 0; i < 100; i++) {
                hashes.add(passwordService.hashPassword(password));
            }

            // All hashes should be unique due to random salt
            assertThat(hashes).hasSize(100);
        }

        @Test
        @DisplayName("should include pepper version in hash")
        void shouldIncludePepperVersion() {
            String hash = passwordService.hashPassword("TestPassword123!");

            // Format: v{version}${argon2hash}
            assertThat(hash).matches("v\\d+\\$\\$argon2id\\$.+");
        }

        @Test
        @DisplayName("should hash minimum length password")
        void shouldHashMinimumLengthPassword() {
            String password = "A".repeat(12); // Minimum length

            String hash = passwordService.hashPassword(password);

            assertThat(hash).isNotBlank();
        }

        @Test
        @DisplayName("should hash maximum length password")
        void shouldHashMaximumLengthPassword() {
            String password = "A".repeat(128); // Maximum length

            String hash = passwordService.hashPassword(password);

            assertThat(hash).isNotBlank();
        }

        @Test
        @DisplayName("hash operation should complete in reasonable time")
        void hashShouldCompleteInReasonableTime() {
            String password = "PerformanceTest123!";

            long start = System.currentTimeMillis();
            passwordService.hashPassword(password);
            long elapsed = System.currentTimeMillis() - start;

            // Should complete in less than 200ms (ASVS requirement)
            assertThat(elapsed).isLessThan(200);
        }
    }

    @Nested
    @DisplayName("Password Verification")
    class VerificationTests {

        @Test
        @DisplayName("should verify correct password")
        void shouldVerifyCorrectPassword() {
            String password = "CorrectPassword123!";
            String hash = passwordService.hashPassword(password);

            boolean result = passwordService.verifyPassword(password, hash);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should reject incorrect password")
        void shouldRejectIncorrectPassword() {
            String password = "CorrectPassword123!";
            String hash = passwordService.hashPassword(password);

            boolean result = passwordService.verifyPassword("WrongPassword123!", hash);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject null password")
        void shouldRejectNullPassword() {
            String hash = passwordService.hashPassword("SomePassword123!");

            boolean result = passwordService.verifyPassword(null, hash);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject null hash")
        void shouldRejectNullHash() {
            boolean result = passwordService.verifyPassword("SomePassword123!", null);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should reject malformed hash")
        void shouldRejectMalformedHash() {
            boolean result = passwordService.verifyPassword("Password123!", "invalid-hash");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should verify password with legacy hash (no pepper version)")
        void shouldVerifyLegacyHash() {
            // Create a service without pepper to simulate legacy hash
            PasswordProperties noPepperProps = createDefaultProperties();
            noPepperProps.getPepper().setValue(null);
            PasswordService legacyService = new PasswordService(noPepperProps);

            String password = "LegacyPassword123!";
            String hash = legacyService.hashPassword(password);

            // Should still verify (assumes pepper version 1)
            boolean result = legacyService.verifyPassword(password, hash);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Timing Attack Mitigation")
    class TimingTests {

        @Test
        @DisplayName("verification time should be similar for valid and invalid passwords")
        void verificationTimeShouldBeSimilar() {
            String password = "TimingTestPassword123!";
            String hash = passwordService.hashPassword(password);

            // Warmup
            for (int i = 0; i < 5; i++) {
                passwordService.verifyPassword(password, hash);
                passwordService.verifyPassword("WrongPassword123!", hash);
            }

            // Measure correct password verification
            long correctStart = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                passwordService.verifyPassword(password, hash);
            }
            long correctTime = System.nanoTime() - correctStart;

            // Measure incorrect password verification
            long incorrectStart = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                passwordService.verifyPassword("WrongPassword12345!", hash);
            }
            long incorrectTime = System.nanoTime() - incorrectStart;

            // Times should be within 50% of each other
            double ratio = (double) correctTime / incorrectTime;
            assertThat(ratio).isBetween(0.5, 2.0);
        }

        @Test
        @DisplayName("verification should take similar time for null inputs")
        void verificationShouldTakeSimilarTimeForNullInputs() {
            String password = "ValidPassword123!";
            String hash = passwordService.hashPassword(password);

            // Warmup
            for (int i = 0; i < 3; i++) {
                passwordService.verifyPassword(password, hash);
                passwordService.verifyPassword(null, hash);
            }

            // Measure valid verification
            long validStart = System.nanoTime();
            for (int i = 0; i < 5; i++) {
                passwordService.verifyPassword(password, hash);
            }
            long validTime = System.nanoTime() - validStart;

            // Measure null password verification (should still take time due to dummy hash)
            long nullStart = System.nanoTime();
            for (int i = 0; i < 5; i++) {
                passwordService.verifyPassword(null, hash);
            }
            long nullTime = System.nanoTime() - nullStart;

            // Times should be within same order of magnitude
            double ratio = (double) validTime / nullTime;
            assertThat(ratio).isBetween(0.1, 10.0);
        }
    }

    @Nested
    @DisplayName("Password Policy Validation")
    class PolicyValidationTests {

        @Test
        @DisplayName("should reject password shorter than minimum")
        void shouldRejectShortPassword() {
            String shortPassword = "Short123!"; // 9 chars, less than 12

            assertThatThrownBy(() -> passwordService.hashPassword(shortPassword))
                .isInstanceOf(PasswordValidationException.class)
                .satisfies(ex -> {
                    PasswordValidationException pve = (PasswordValidationException) ex;
                    assertThat(pve.getFailure()).isEqualTo(PasswordValidationException.ValidationFailure.TOO_SHORT);
                });
        }

        @Test
        @DisplayName("should reject password longer than maximum")
        void shouldRejectLongPassword() {
            String longPassword = "A".repeat(129); // 129 chars, more than 128

            assertThatThrownBy(() -> passwordService.hashPassword(longPassword))
                .isInstanceOf(PasswordValidationException.class)
                .satisfies(ex -> {
                    PasswordValidationException pve = (PasswordValidationException) ex;
                    assertThat(pve.getFailure()).isEqualTo(PasswordValidationException.ValidationFailure.TOO_LONG);
                });
        }

        @Test
        @DisplayName("should reject null password")
        void shouldRejectNullPassword() {
            assertThatThrownBy(() -> passwordService.hashPassword(null))
                .isInstanceOf(PasswordValidationException.class)
                .satisfies(ex -> {
                    PasswordValidationException pve = (PasswordValidationException) ex;
                    assertThat(pve.getFailure()).isEqualTo(PasswordValidationException.ValidationFailure.TOO_SHORT);
                });
        }

        @Test
        @DisplayName("should reject empty password")
        void shouldRejectEmptyPassword() {
            assertThatThrownBy(() -> passwordService.hashPassword(""))
                .isInstanceOf(PasswordValidationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "password1234",           // all lowercase
            "PASSWORD1234",           // all uppercase
            "123456789012",           // all numbers
            "abcdefghijkl",           // no numbers/symbols
            "Simple Password"         // with space, no special chars
        })
        @DisplayName("should accept passwords without composition rules (ASVS V2.1.3)")
        void shouldAcceptPasswordsWithoutCompositionRules(String password) {
            // ASVS V2.1.3: No composition rules required
            // Length and breach detection are sufficient
            String hash = passwordService.hashPassword(password);

            assertThat(hash).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Pepper Handling")
    class PepperTests {

        @Test
        @DisplayName("should apply pepper to password")
        void shouldApplyPepper() {
            // Create two services with different peppers
            PasswordProperties props1 = createDefaultProperties();
            props1.getPepper().setValue("0123456789abcdef0123456789abcdef");
            PasswordService service1 = new PasswordService(props1);

            PasswordProperties props2 = createDefaultProperties();
            props2.getPepper().setValue("fedcba9876543210fedcba9876543210");
            PasswordService service2 = new PasswordService(props2);

            String password = "SamePassword123!";
            String hash1 = service1.hashPassword(password);
            String hash2 = service2.hashPassword(password);

            // Hashes with different peppers should not verify against each other
            assertThat(service1.verifyPassword(password, hash1)).isTrue();
            assertThat(service2.verifyPassword(password, hash2)).isTrue();

            // Cross-verification should fail
            assertThat(service1.verifyPassword(password, hash2)).isFalse();
        }

        @Test
        @DisplayName("should support pepper versioning")
        void shouldSupportPepperVersioning() {
            // Create service with pepper version 2
            PasswordProperties props = createDefaultProperties();
            props.getPepper().setVersion(2);
            props.getPepper().setValue("fedcba9876543210fedcba9876543210");
            PasswordService service = new PasswordService(props);

            String hash = service.hashPassword("TestPassword123!");

            // Hash should include version 2
            assertThat(hash).startsWith("v2$");
        }

        @Test
        @DisplayName("should verify password with previous pepper version")
        void shouldVerifyWithPreviousPepperVersion() {
            // Create hash with pepper version 1
            PasswordProperties props1 = createDefaultProperties();
            props1.getPepper().setVersion(1);
            props1.getPepper().setValue("0123456789abcdef0123456789abcdef");
            PasswordService service1 = new PasswordService(props1);

            String password = "MigrationPassword123!";
            String hashV1 = service1.hashPassword(password);

            // Create service with pepper version 2 that knows about version 1
            PasswordProperties props2 = createDefaultProperties();
            props2.getPepper().setVersion(2);
            props2.getPepper().setValue("fedcba9876543210fedcba9876543210");
            props2.getPepper().setPreviousVersions("1:0123456789abcdef0123456789abcdef");
            PasswordService service2 = new PasswordService(props2);

            // Should still verify old hash
            assertThat(service2.verifyPassword(password, hashV1)).isTrue();
        }
    }

    @Nested
    @DisplayName("Rehash Detection")
    class RehashTests {

        @Test
        @DisplayName("should detect hash needing upgrade due to old pepper")
        void shouldDetectOldPepperVersion() {
            // Create hash with old pepper version
            PasswordProperties propsV1 = createDefaultProperties();
            propsV1.getPepper().setVersion(1);
            PasswordService serviceV1 = new PasswordService(propsV1);
            String hashV1 = serviceV1.hashPassword("TestPassword123!");

            // Check with service using new pepper version
            PasswordProperties propsV2 = createDefaultProperties();
            propsV2.getPepper().setVersion(2);
            propsV2.getPepper().setPreviousVersions("1:" + propsV1.getPepper().getValue());
            PasswordService serviceV2 = new PasswordService(propsV2);

            assertThat(serviceV2.needsRehash(hashV1)).isTrue();
        }

        @Test
        @DisplayName("should not need rehash for current pepper version")
        void shouldNotNeedRehashForCurrentVersion() {
            String hash = passwordService.hashPassword("TestPassword123!");

            assertThat(passwordService.needsRehash(hash)).isFalse();
        }

        @Test
        @DisplayName("should handle null hash gracefully")
        void shouldHandleNullHash() {
            assertThat(passwordService.needsRehash(null)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Test Helpers
    // -------------------------------------------------------------------------

    private PasswordProperties createDefaultProperties() {
        PasswordProperties props = new PasswordProperties();
        props.setMinLength(12);
        props.setMaxLength(128);

        // Argon2 config - using lower values for test speed
        var argon2 = new PasswordProperties.Argon2Config();
        argon2.setMemory(16384); // 16 MB (faster for tests)
        argon2.setIterations(2);
        argon2.setParallelism(1);
        argon2.setSaltLength(16);
        argon2.setHashLength(32);
        props.setArgon2(argon2);

        // Pepper config
        var pepper = new PasswordProperties.PepperConfig();
        pepper.setVersion(1);
        pepper.setValue("0123456789abcdef0123456789abcdef"); // 32 hex chars = 16 bytes
        props.setPepper(pepper);

        // HIBP config (not used in password service directly)
        var hibp = new PasswordProperties.HibpConfig();
        hibp.setEnabled(true);
        props.setHibp(hibp);

        return props;
    }
}
