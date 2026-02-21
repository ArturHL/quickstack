package com.quickstack.auth.security;

import com.quickstack.common.config.properties.JwtProperties;
import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.exception.InvalidTokenException.InvalidationReason;
import com.quickstack.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for JwtService token generation and validation.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private JwtProperties properties;
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private JwtService jwtService;
    private Clock fixedClock;

    // Test key pairs
    private KeyPair currentKeyPair;
    private KeyPair previousKeyPair;
    private KeyPair otherKeyPair;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        // Generate test keys
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        currentKeyPair = generator.generateKeyPair();
        previousKeyPair = generator.generateKeyPair();
        otherKeyPair = generator.generateKeyPair();

        privateKey = (RSAPrivateKey) currentKeyPair.getPrivate();
        publicKey = (RSAPublicKey) currentKeyPair.getPublic();

        // Configure properties
        properties = new JwtProperties();
        properties.setIssuer("quickstack-pos");
        properties.setAccessTokenExpiration(Duration.ofMinutes(15));
        properties.setClockSkew(Duration.ofSeconds(30));

        // Fixed clock for deterministic tests
        fixedClock = Clock.fixed(Instant.parse("2024-01-15T10:00:00Z"), ZoneId.of("UTC"));

        // Create service
        jwtService = new JwtService(properties, privateKey, publicKey, Collections.emptyList(), fixedClock);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setTenantId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        user.setRoleId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        user.setBranchId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        user.setEmail("test@example.com");
        return user;
    }

    @Nested
    @DisplayName("Token generation")
    class TokenGeneration {

        @Test
        @DisplayName("generates valid JWT token")
        void generatesValidJwtToken() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = jwtService.generateAccessToken(user);

            // Assert
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
        }

        @Test
        @DisplayName("includes correct subject claim")
        void includesCorrectSubjectClaim() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        }

        @Test
        @DisplayName("includes email claim")
        void includesEmailClaim() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(jwtService.getEmail(claims)).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("includes tenant_id claim")
        void includesTenantIdClaim() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(jwtService.getTenantId(claims))
                    .isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        }

        @Test
        @DisplayName("includes role_id claim")
        void includesRoleIdClaim() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(jwtService.getRoleId(claims))
                    .isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        }

        @Test
        @DisplayName("includes branch_id claim when present")
        void includesBranchIdClaimWhenPresent() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(jwtService.getBranchId(claims))
                    .isEqualTo(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        }

        @Test
        @DisplayName("omits branch_id claim when null")
        void omitsBranchIdClaimWhenNull() {
            // Arrange
            User user = createTestUser();
            user.setBranchId(null);

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(jwtService.getBranchId(claims)).isNull();
        }

        @Test
        @DisplayName("includes unique jti claim")
        void includesUniqueJtiClaim() {
            // Arrange
            User user = createTestUser();

            // Act
            String token1 = jwtService.generateAccessToken(user);
            String token2 = jwtService.generateAccessToken(user);

            Claims claims1 = jwtService.validateToken(token1);
            Claims claims2 = jwtService.validateToken(token2);

            // Assert
            assertThat(claims1.getId()).isNotBlank();
            assertThat(claims2.getId()).isNotBlank();
            assertThat(claims1.getId()).isNotEqualTo(claims2.getId());
        }

        @Test
        @DisplayName("includes correct issuer claim")
        void includesCorrectIssuerClaim() {
            // Arrange
            User user = createTestUser();

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(claims.getIssuer()).isEqualTo("quickstack-pos");
        }

        @Test
        @DisplayName("includes correct expiration claim")
        void includesCorrectExpirationClaim() {
            // Arrange
            User user = createTestUser();
            Instant expectedExpiration = fixedClock.instant().plus(Duration.ofMinutes(15));

            // Act
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(claims.getExpiration().toInstant()).isEqualTo(expectedExpiration);
        }
    }

    @Nested
    @DisplayName("Token validation")
    class TokenValidation {

        @Test
        @DisplayName("validates correct token successfully")
        void validatesCorrectTokenSuccessfully() {
            // Arrange
            User user = createTestUser();
            String token = jwtService.generateAccessToken(user);

            // Act
            Claims claims = jwtService.validateToken(token);

            // Assert
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        }

        @Test
        @DisplayName("rejects expired token")
        void rejectsExpiredToken() {
            // Arrange - create token that expires in 15 minutes
            User user = createTestUser();
            String token = jwtService.generateAccessToken(user);

            // Create new service with clock 20 minutes in the future
            Clock futureClock = Clock.fixed(
                    fixedClock.instant().plus(Duration.ofMinutes(20)),
                    ZoneId.of("UTC")
            );
            JwtService futureService = new JwtService(
                    properties, privateKey, publicKey, Collections.emptyList(), futureClock);

            // Act & Assert
            assertThatThrownBy(() -> futureService.validateToken(token))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.EXPIRED);
                    });
        }

        @Test
        @DisplayName("accepts token within clock skew")
        void acceptsTokenWithinClockSkew() {
            // Arrange - token expires in 15 minutes
            User user = createTestUser();
            String token = jwtService.generateAccessToken(user);

            // Create service with clock 15 minutes + 20 seconds in future (within 30s skew)
            Clock futureClockWithinSkew = Clock.fixed(
                    fixedClock.instant().plus(Duration.ofMinutes(15)).plus(Duration.ofSeconds(20)),
                    ZoneId.of("UTC")
            );
            JwtService futureService = new JwtService(
                    properties, privateKey, publicKey, Collections.emptyList(), futureClockWithinSkew);

            // Act & Assert - should not throw
            Claims claims = futureService.validateToken(token);
            assertThat(claims).isNotNull();
        }

        @Test
        @DisplayName("rejects token with invalid signature")
        void rejectsTokenWithInvalidSignature() {
            // Arrange - create token with different key
            JwtService otherService = new JwtService(
                    properties,
                    (RSAPrivateKey) otherKeyPair.getPrivate(),
                    (RSAPublicKey) otherKeyPair.getPublic(),
                    Collections.emptyList(),
                    fixedClock
            );
            User user = createTestUser();
            String token = otherService.generateAccessToken(user);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateToken(token))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.SIGNATURE_INVALID);
                    });
        }

        @Test
        @DisplayName("rejects malformed token")
        void rejectsMalformedToken() {
            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateToken("not.a.valid.token"))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.MALFORMED);
                    });
        }

        @Test
        @DisplayName("rejects empty token")
        void rejectsEmptyToken() {
            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateToken(""))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.MALFORMED);
                    });
        }

        @Test
        @DisplayName("rejects token with wrong issuer")
        void rejectsTokenWithWrongIssuer() {
            // Arrange - create token with different issuer
            JwtProperties otherProperties = new JwtProperties();
            otherProperties.setIssuer("other-issuer");
            otherProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
            otherProperties.setClockSkew(Duration.ofSeconds(30));

            JwtService otherIssuerService = new JwtService(
                    otherProperties, privateKey, publicKey, Collections.emptyList(), fixedClock);

            User user = createTestUser();
            String token = otherIssuerService.generateAccessToken(user);

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateToken(token))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        assertThat(ite.getReason()).isEqualTo(InvalidationReason.ISSUER_MISMATCH);
                    });
        }
    }

    @Nested
    @DisplayName("Algorithm confusion attack prevention")
    class AlgorithmConfusionPrevention {

        @Test
        @DisplayName("rejects token signed with HS256 using public key as secret")
        void rejectsTokenSignedWithHs256() {
            // Arrange - create malicious token using public key as HMAC secret
            // This simulates algorithm confusion attack (RS256 -> HS256)
            byte[] publicKeyBytes = publicKey.getEncoded();
            SecretKey hmacKey = new javax.crypto.spec.SecretKeySpec(publicKeyBytes, "HmacSHA256");

            String maliciousToken = Jwts.builder()
                    .subject("hacker")
                    .issuer(properties.getIssuer())
                    .issuedAt(Date.from(fixedClock.instant()))
                    .expiration(Date.from(fixedClock.instant().plus(Duration.ofMinutes(15))))
                    .signWith(hmacKey, Jwts.SIG.HS256)
                    .compact();

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateToken(maliciousToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        // JJWT will reject as wrong algorithm or signature
                        assertThat(ite.getReason()).isIn(
                                InvalidationReason.WRONG_ALGORITHM,
                                InvalidationReason.SIGNATURE_INVALID
                        );
                    });
        }

        @Test
        @DisplayName("rejects unsigned token (none algorithm)")
        void rejectsUnsignedToken() {
            // Arrange - create token without signature manually
            // JJWT v0.12+ won't let us create unsigned tokens, so we build it manually
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(("{\"sub\":\"hacker\",\"iss\":\"" + properties.getIssuer() + "\"}").getBytes());
            String unsignedToken = header + "." + payload + ".";

            // Act & Assert
            assertThatThrownBy(() -> jwtService.validateToken(unsignedToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .satisfies(ex -> {
                        InvalidTokenException ite = (InvalidTokenException) ex;
                        // Should be rejected for wrong algorithm or malformed
                        assertThat(ite.getReason()).isIn(
                                InvalidationReason.WRONG_ALGORITHM,
                                InvalidationReason.MALFORMED,
                                InvalidationReason.SIGNATURE_INVALID
                        );
                    });
        }
    }

    @Nested
    @DisplayName("Key rotation support")
    class KeyRotationSupport {

        @Test
        @DisplayName("validates token signed with previous key")
        void validatesTokenSignedWithPreviousKey() {
            // Arrange - create token with previous key
            JwtService previousKeyService = new JwtService(
                    properties,
                    (RSAPrivateKey) previousKeyPair.getPrivate(),
                    (RSAPublicKey) previousKeyPair.getPublic(),
                    Collections.emptyList(),
                    fixedClock
            );

            User user = createTestUser();
            String token = previousKeyService.generateAccessToken(user);

            // Create service with current key but previous key in rotation list
            JwtService rotationService = new JwtService(
                    properties,
                    privateKey,
                    publicKey,
                    List.of((RSAPublicKey) previousKeyPair.getPublic()),
                    fixedClock
            );

            // Act
            Claims claims = rotationService.validateToken(token);

            // Assert
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        }

        @Test
        @DisplayName("tries current key first before previous keys")
        void triesCurrentKeyFirstBeforePreviousKeys() {
            // Arrange - create token with current key
            User user = createTestUser();
            String token = jwtService.generateAccessToken(user);

            // Service with previous keys
            JwtService rotationService = new JwtService(
                    properties,
                    privateKey,
                    publicKey,
                    List.of((RSAPublicKey) previousKeyPair.getPublic()),
                    fixedClock
            );

            // Act
            Claims claims = rotationService.validateToken(token);

            // Assert - should work without falling back
            assertThat(claims).isNotNull();
        }

        @Test
        @DisplayName("validates with second previous key if first fails")
        void validatesWithSecondPreviousKeyIfFirstFails() throws NoSuchAlgorithmException {
            // Arrange - generate a third key pair
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair thirdKeyPair = generator.generateKeyPair();

            // Create token with third key
            JwtService thirdKeyService = new JwtService(
                    properties,
                    (RSAPrivateKey) thirdKeyPair.getPrivate(),
                    (RSAPublicKey) thirdKeyPair.getPublic(),
                    Collections.emptyList(),
                    fixedClock
            );

            User user = createTestUser();
            String token = thirdKeyService.generateAccessToken(user);

            // Create service with current key and two previous keys
            // Third key is second in the list
            JwtService rotationService = new JwtService(
                    properties,
                    privateKey,
                    publicKey,
                    List.of(
                            (RSAPublicKey) previousKeyPair.getPublic(),
                            (RSAPublicKey) thirdKeyPair.getPublic()
                    ),
                    fixedClock
            );

            // Act
            Claims claims = rotationService.validateToken(token);

            // Assert
            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        }
    }

    @Nested
    @DisplayName("Claim extraction helpers")
    class ClaimExtractionHelpers {

        @Test
        @DisplayName("getUserId extracts user UUID from claims")
        void getUserIdExtractsUserUuid() {
            // Arrange
            User user = createTestUser();
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Act
            UUID userId = jwtService.getUserId(claims);

            // Assert
            assertThat(userId).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("getTenantId extracts tenant UUID from claims")
        void getTenantIdExtractsTenantUuid() {
            // Arrange
            User user = createTestUser();
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Act
            UUID tenantId = jwtService.getTenantId(claims);

            // Assert
            assertThat(tenantId).isEqualTo(user.getTenantId());
        }

        @Test
        @DisplayName("getRoleId extracts role UUID from claims")
        void getRoleIdExtractsRoleUuid() {
            // Arrange
            User user = createTestUser();
            String token = jwtService.generateAccessToken(user);
            Claims claims = jwtService.validateToken(token);

            // Act
            UUID roleId = jwtService.getRoleId(claims);

            // Assert
            assertThat(roleId).isEqualTo(user.getRoleId());
        }
    }
}
