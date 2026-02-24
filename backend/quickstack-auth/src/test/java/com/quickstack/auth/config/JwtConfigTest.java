package com.quickstack.auth.config;

import com.quickstack.common.config.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for JwtConfig RSA key loading and validation.
 */
@DisplayName("JwtConfig")
class JwtConfigTest {

    private JwtProperties properties;

    @TempDir
    Path tempDir;

    // Pre-generated test keys
    private KeyPair testKeyPair2048;
    private KeyPair testKeyPair1024; // Intentionally weak for testing rejection

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        properties = new JwtProperties();

        // Generate test keys
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        testKeyPair2048 = generator.generateKeyPair();

        generator.initialize(1024);
        testKeyPair1024 = generator.generateKeyPair();
    }

    @Nested
    @DisplayName("Loading from Base64")
    class LoadFromBase64 {

        @Test
        @DisplayName("loads private key from Base64 configuration")
        void loadsPrivateKeyFromBase64() {
            // Arrange
            String privateKeyBase64 = Base64.getEncoder()
                    .encodeToString(testKeyPair2048.getPrivate().getEncoded());
            String publicKeyBase64 = Base64.getEncoder()
                    .encodeToString(testKeyPair2048.getPublic().getEncoded());

            properties.setPrivateKeyBase64(privateKeyBase64);
            properties.setPublicKeyBase64(publicKeyBase64);

            JwtConfig config = new JwtConfig(properties);

            // Act
            RSAPrivateKey loadedKey = config.rsaPrivateKey();

            // Assert
            assertThat(loadedKey).isNotNull();
            assertThat(loadedKey.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
        }

        @Test
        @DisplayName("loads public key from Base64 configuration")
        void loadsPublicKeyFromBase64() {
            // Arrange
            String privateKeyBase64 = Base64.getEncoder()
                    .encodeToString(testKeyPair2048.getPrivate().getEncoded());
            String publicKeyBase64 = Base64.getEncoder()
                    .encodeToString(testKeyPair2048.getPublic().getEncoded());

            properties.setPrivateKeyBase64(privateKeyBase64);
            properties.setPublicKeyBase64(publicKeyBase64);

            JwtConfig config = new JwtConfig(properties);

            // Act
            RSAPublicKey loadedKey = config.rsaPublicKey();

            // Assert
            assertThat(loadedKey).isNotNull();
            assertThat(loadedKey.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
        }

        @Test
        @DisplayName("supports URL-safe Base64 encoding")
        void supportsUrlSafeBase64() {
            // Arrange - URL-safe Base64 (no padding, - and _ instead of + and /)
            String privateKeyUrlSafe = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(testKeyPair2048.getPrivate().getEncoded());
            String publicKeyUrlSafe = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(testKeyPair2048.getPublic().getEncoded());

            properties.setPrivateKeyBase64(privateKeyUrlSafe);
            properties.setPublicKeyBase64(publicKeyUrlSafe);

            JwtConfig config = new JwtConfig(properties);

            // Act & Assert - should not throw
            RSAPrivateKey privateKey = config.rsaPrivateKey();
            RSAPublicKey publicKey = config.rsaPublicKey();

            assertThat(privateKey).isNotNull();
            assertThat(publicKey).isNotNull();
        }
    }

    @Nested
    @DisplayName("Loading from PEM files")
    class LoadFromPemFiles {

        @Test
        @DisplayName("loads private key from PEM file")
        void loadsPrivateKeyFromPemFile() throws IOException {
            // Arrange
            Path privateKeyPath = writePemFile("private.pem", testKeyPair2048.getPrivate().getEncoded(), true);
            Path publicKeyPath = writePemFile("public.pem", testKeyPair2048.getPublic().getEncoded(), false);

            properties.setPrivateKeyPath(privateKeyPath.toString());
            properties.setPublicKeyPath(publicKeyPath.toString());

            JwtConfig config = new JwtConfig(properties);

            // Act
            RSAPrivateKey loadedKey = config.rsaPrivateKey();

            // Assert
            assertThat(loadedKey).isNotNull();
            assertThat(loadedKey.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
        }

        @Test
        @DisplayName("loads public key from PEM file")
        void loadsPublicKeyFromPemFile() throws IOException {
            // Arrange
            Path privateKeyPath = writePemFile("private.pem", testKeyPair2048.getPrivate().getEncoded(), true);
            Path publicKeyPath = writePemFile("public.pem", testKeyPair2048.getPublic().getEncoded(), false);

            properties.setPrivateKeyPath(privateKeyPath.toString());
            properties.setPublicKeyPath(publicKeyPath.toString());

            JwtConfig config = new JwtConfig(properties);

            // Act
            RSAPublicKey loadedKey = config.rsaPublicKey();

            // Assert
            assertThat(loadedKey).isNotNull();
        }

        @Test
        @DisplayName("fails if PEM file does not exist")
        void failsIfPemFileDoesNotExist() {
            // Arrange
            properties.setPrivateKeyPath("/nonexistent/path/private.pem");

            JwtConfig config = new JwtConfig(properties);

            // Act & Assert
            assertThatThrownBy(config::rsaPrivateKey)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to read key file");
        }

        private Path writePemFile(String filename, byte[] keyBytes, boolean isPrivate) throws IOException {
            String header = isPrivate ? "-----BEGIN PRIVATE KEY-----" : "-----BEGIN PUBLIC KEY-----";
            String footer = isPrivate ? "-----END PRIVATE KEY-----" : "-----END PUBLIC KEY-----";

            String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                    .encodeToString(keyBytes);

            String pemContent = header + "\n" + base64 + "\n" + footer;

            Path path = tempDir.resolve(filename);
            Files.writeString(path, pemContent);
            return path;
        }
    }

    @Nested
    @DisplayName("Key validation")
    class KeyValidation {

        @Test
        @DisplayName("fails if private key is not configured")
        void failsIfPrivateKeyNotConfigured() {
            // Arrange
            JwtConfig config = new JwtConfig(properties);

            // Act & Assert
            assertThatThrownBy(config::rsaPrivateKey)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT private key not configured");
        }

        @Test
        @DisplayName("fails if public key is not configured")
        void failsIfPublicKeyNotConfigured() {
            // Arrange
            JwtConfig config = new JwtConfig(properties);

            // Act & Assert
            assertThatThrownBy(config::rsaPublicKey)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT public key not configured");
        }

        @Test
        @DisplayName("rejects key smaller than 2048 bits")
        void rejectsKeysSmallerThan2048Bits() {
            // Arrange
            String weakPrivateKey = Base64.getEncoder()
                    .encodeToString(testKeyPair1024.getPrivate().getEncoded());

            properties.setPrivateKeyBase64(weakPrivateKey);

            JwtConfig config = new JwtConfig(properties);

            // Act & Assert
            assertThatThrownBy(config::rsaPrivateKey)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("below minimum required")
                    .hasMessageContaining("2048");
        }

        @Test
        @DisplayName("rejects invalid Base64 encoding")
        void rejectsInvalidBase64() {
            // Arrange
            properties.setPrivateKeyBase64("not-valid-base64!!!");

            JwtConfig config = new JwtConfig(properties);

            // Act & Assert
            assertThatThrownBy(config::rsaPrivateKey)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid Base64 encoding");
        }
    }

    @Nested
    @DisplayName("Key rotation support")
    class KeyRotation {

        @Test
        @DisplayName("returns empty list when no previous keys configured")
        void returnsEmptyListWhenNoPreviousKeys() {
            // Arrange
            JwtConfig config = new JwtConfig(properties);

            // Act
            List<RSAPublicKey> previousKeys = config.previousPublicKeys();

            // Assert
            assertThat(previousKeys).isEmpty();
        }

        @Test
        @DisplayName("loads single previous public key")
        void loadsSinglePreviousPublicKey() throws NoSuchAlgorithmException {
            // Arrange
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair previousKeyPair = generator.generateKeyPair();

            String previousKeyBase64 = Base64.getEncoder()
                    .encodeToString(previousKeyPair.getPublic().getEncoded());

            properties.setPreviousPublicKeysBase64(previousKeyBase64);

            JwtConfig config = new JwtConfig(properties);

            // Act
            List<RSAPublicKey> previousKeys = config.previousPublicKeys();

            // Assert
            assertThat(previousKeys).hasSize(1);
        }

        @Test
        @DisplayName("loads multiple previous public keys separated by comma")
        void loadsMultiplePreviousPublicKeys() throws NoSuchAlgorithmException {
            // Arrange
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair previousKeyPair1 = generator.generateKeyPair();
            KeyPair previousKeyPair2 = generator.generateKeyPair();

            String key1Base64 = Base64.getEncoder()
                    .encodeToString(previousKeyPair1.getPublic().getEncoded());
            String key2Base64 = Base64.getEncoder()
                    .encodeToString(previousKeyPair2.getPublic().getEncoded());

            properties.setPreviousPublicKeysBase64(key1Base64 + "," + key2Base64);

            JwtConfig config = new JwtConfig(properties);

            // Act
            List<RSAPublicKey> previousKeys = config.previousPublicKeys();

            // Assert
            assertThat(previousKeys).hasSize(2);
        }

        @Test
        @DisplayName("skips invalid keys in previous keys list")
        void skipsInvalidKeysInPreviousKeysList() throws NoSuchAlgorithmException {
            // Arrange
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair validKeyPair = generator.generateKeyPair();

            String validKeyBase64 = Base64.getEncoder()
                    .encodeToString(validKeyPair.getPublic().getEncoded());

            properties.setPreviousPublicKeysBase64(validKeyBase64 + ",invalid-key-data");

            JwtConfig config = new JwtConfig(properties);

            // Act
            List<RSAPublicKey> previousKeys = config.previousPublicKeys();

            // Assert - should have loaded the valid key, skipped the invalid one
            assertThat(previousKeys).hasSize(1);
        }

        @Test
        @DisplayName("returns immutable list of previous keys")
        void returnsImmutableListOfPreviousKeys() throws NoSuchAlgorithmException {
            // Arrange
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair previousKeyPair = generator.generateKeyPair();

            String previousKeyBase64 = Base64.getEncoder()
                    .encodeToString(previousKeyPair.getPublic().getEncoded());

            properties.setPreviousPublicKeysBase64(previousKeyBase64);

            JwtConfig config = new JwtConfig(properties);

            // Act
            List<RSAPublicKey> previousKeys = config.previousPublicKeys();

            // Assert
            assertThatThrownBy(() -> previousKeys.add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
