package com.quickstack.app;

import com.quickstack.app.security.JwtService;
import com.quickstack.user.entity.User;
import com.quickstack.user.service.PasswordBreachChecker;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Base class for integration tests.
 * <p>
 * Provides:
 * <ul>
 *   <li>PostgreSQL 16 via Testcontainers (shared across all subclasses in same JVM)</li>
 *   <li>Flyway migrations applied automatically</li>
 *   <li>RSA key pair generated at test startup for JWT signing</li>
 *   <li>HIBP mocked via {@link MockBean} — all passwords pass by default</li>
 *   <li>RestAssured pre-configured with base URI and port</li>
 * </ul>
 * <p>
 * Subclasses should use {@link #generateAccessToken} to obtain Bearer tokens
 * and {@link #authHeader} for request headers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("quickstack_test")
            .withUsername("test")
            .withPassword("test");

    private static final KeyPair TEST_KEY_PAIR = generateTestKeyPair();

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        String privateKeyB64 = Base64.getEncoder()
                .encodeToString(TEST_KEY_PAIR.getPrivate().getEncoded());
        String publicKeyB64 = Base64.getEncoder()
                .encodeToString(TEST_KEY_PAIR.getPublic().getEncoded());

        registry.add("quickstack.jwt.private-key-base64", () -> privateKeyB64);
        registry.add("quickstack.jwt.public-key-base64", () -> publicKeyB64);
    }

    @LocalServerPort
    protected int port;

    /**
     * HIBP is mocked so all passwords pass breach checks by default.
     * Tests that need a compromised password can stub this bean.
     */
    @MockBean
    protected PasswordBreachChecker breachChecker;

    @Autowired
    protected JwtService jwtService;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
    }

    /**
     * Generates a signed JWT access token for use in test requests.
     */
    protected String generateAccessToken(UUID userId, UUID tenantId, UUID roleId, String email) {
        User user = new User();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setRoleId(roleId);
        user.setEmail(email);
        user.setActive(true);
        return jwtService.generateAccessToken(user);
    }

    /**
     * Returns the Authorization header value for a given user.
     */
    protected String authHeader(UUID userId, UUID tenantId, UUID roleId, String email) {
        return "Bearer " + generateAccessToken(userId, tenantId, roleId, email);
    }

    private static KeyPair generateTestKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA test key pair", e);
        }
    }
}
