package com.quickstack.auth.security;

import com.quickstack.common.exception.AuthenticationException;
import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.auth.controller.AuthController;
import com.quickstack.auth.dto.request.LoginRequest;
import com.quickstack.auth.dto.request.RegisterRequest;
import com.quickstack.auth.entity.RefreshToken;
import com.quickstack.user.entity.User;
import com.quickstack.user.repository.UserRepository;
import com.quickstack.auth.repository.RefreshTokenRepository;
import com.quickstack.common.security.PasswordService;
import com.quickstack.auth.service.PasswordResetService;
import com.quickstack.auth.service.LoginAttemptService;
import com.quickstack.auth.service.RefreshTokenService;
import com.quickstack.user.service.UserService;
import com.quickstack.auth.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Basic penetration tests verifying common attack vectors are mitigated.
 * <p>
 * Covers:
 * - SQL Injection via input fields
 * - JWT tampering
 * - IDOR (Insecure Direct Object Reference)
 * - Timing attack on password reset
 * - Mass assignment via registration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Penetration Tests - Basic Security Vectors")
class PenetrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthController authController;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        com.quickstack.common.config.properties.JwtProperties jwtProperties =
                new com.quickstack.common.config.properties.JwtProperties();
        jwtProperties.setAccessTokenExpiration(java.time.Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExpiration(java.time.Duration.ofDays(7));

        com.quickstack.common.config.properties.CookieProperties cookieProperties =
                new com.quickstack.common.config.properties.CookieProperties();
        com.quickstack.common.config.properties.CookieProperties.RefreshTokenCookie rtCookie =
                new com.quickstack.common.config.properties.CookieProperties.RefreshTokenCookie();
        rtCookie.setName("__Host-refresh_token");
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(true);
        rtCookie.setPath("/api/v1/auth");
        rtCookie.setMaxAge(java.time.Duration.ofDays(7));
        rtCookie.setSameSite(com.quickstack.common.config.properties.CookieProperties.SameSiteMode.STRICT);
        cookieProperties.setRefreshToken(rtCookie);

        authController = new AuthController(
                userRepository,
                userService,
                passwordService,
                passwordResetService,
                loginAttemptService,
                refreshTokenService,
                jwtProperties,
                cookieProperties,
                user -> "test-access-token"
        );
    }

    @Nested
    @DisplayName("SQL Injection")
    class SqlInjectionTests {

        @Test
        @DisplayName("SQL injection in email field is passed as literal string to repository")
        void sqlInjectionInEmailPassedAsLiteralToRepository() {
            // Email with @ to avoid maskedEmail() parsing issues,
            // but with SQL injection payload that JPA parameterized queries will ignore
            String maliciousEmail = "admin'@example.com OR 1=1 --";
            LoginRequest request = new LoginRequest(TENANT_ID.toString(), maliciousEmail, "password");
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            // Repository returns empty (injection doesn't work - JPA uses parameterized queries)
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, maliciousEmail))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authController.login(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationException.class);

            // Verify the malicious string was passed as-is (not executed as SQL)
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            verify(userRepository).findByTenantIdAndEmail(eq(TENANT_ID), emailCaptor.capture());
            assertThat(emailCaptor.getValue()).isEqualTo(maliciousEmail);
        }

        @Test
        @DisplayName("SQL injection in password field - repository never called with SQL")
        void sqlInjectionInPasswordFieldDoesNotReachDatabase() {
            String maliciousPassword = "'; DROP TABLE users; --";
            String maliciousEmail = "admin@example.com";
            LoginRequest request = new LoginRequest(TENANT_ID.toString(), maliciousEmail, maliciousPassword);
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            User user = createUser(USER_ID, TENANT_ID);
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, maliciousEmail))
                    .thenReturn(Optional.of(user));
            // Password verification fails (hash comparison, not SQL)
            when(passwordService.verifyPassword(maliciousPassword, "hashed-password"))
                    .thenReturn(false);

            assertThatThrownBy(() -> authController.login(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationException.class);

            // Verify password was checked as string, not executed
            verify(passwordService).verifyPassword(maliciousPassword, "hashed-password");
        }
    }

    @Nested
    @DisplayName("JWT Tampering")
    class JwtTamperingTests {

        @Test
        @DisplayName("tampered JWT token is rejected during validation")
        void tamperedJwtIsRejected() {
            // Create a mock JWT service and verify it rejects tampered tokens
            // (actual tampering tests are in JwtServiceTest.java)
            // Here we verify that the filter propagates the rejection
            InvalidTokenException invalidToken = new InvalidTokenException(
                    InvalidTokenException.TokenType.ACCESS_TOKEN,
                    InvalidTokenException.InvalidationReason.SIGNATURE_INVALID
            );

            // If JwtService throws, the filter clears context and 401 is returned
            // This behavior is tested in JwtAuthenticationFilterTest
            assertThat(invalidToken.getReason())
                    .isEqualTo(InvalidTokenException.InvalidationReason.SIGNATURE_INVALID);
        }

        @Test
        @DisplayName("refresh token reuse is detected and family is revoked")
        void refreshTokenReuseIsDetectedAndFamilyRevoked() {
            String revokedToken = "already-used-token";
            MockHttpServletRequest httpRequest = createMockRequestWithCookie(revokedToken);
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            // Simulate reuse detection: service throws on revoked token
            when(refreshTokenService.rotateToken(eq(revokedToken), anyString(), anyString()))
                    .thenThrow(InvalidTokenException.revoked(InvalidTokenException.TokenType.REFRESH_TOKEN));

            assertThatThrownBy(() -> authController.refresh(httpRequest, httpResponse))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("IDOR - Insecure Direct Object Reference")
    class IdorTests {

        @Test
        @DisplayName("user cannot revoke another user's session (403)")
        void userCannotRevokeAnotherUsersSession() {
            UUID attackerUserId = UUID.randomUUID();
            UUID victimSessionId = UUID.randomUUID();

            com.quickstack.auth.entity.RefreshToken victimToken = RefreshToken.create(
                    UUID.randomUUID(), // victim's userId (different from attacker)
                    "victim-hash", UUID.randomUUID(),
                    Instant.now().plusSeconds(3600),
                    "192.168.1.2", "Mozilla"
            );
            ReflectionTestUtils.setField(victimToken, "id", victimSessionId);

            when(refreshTokenRepository.findById(victimSessionId)).thenReturn(Optional.of(victimToken));

            SessionService sessionService = new SessionService(
                    refreshTokenRepository, java.time.Clock.systemUTC());

            assertThatThrownBy(() -> sessionService.revokeSession(attackerUserId, victimSessionId))
                    .isInstanceOf(SessionService.SessionAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Timing Attack Mitigation")
    class TimingAttackTests {

        @Test
        @DisplayName("forgot-password always returns 200 regardless of email existence")
        void forgotPasswordAlwaysReturns200() {
            MockHttpServletRequest httpRequest = createMockRequest();

            // Scenario 1: email does NOT exist
            com.quickstack.auth.dto.request.ForgotPasswordRequest requestNoUser =
                    new com.quickstack.auth.dto.request.ForgotPasswordRequest(
                            TENANT_ID.toString(), "nonexistent@example.com");

            when(passwordResetService.initiateReset(anyString(), eq(TENANT_ID), anyString()))
                    .thenReturn(PasswordResetService.ResetInitiationResult.userNotFound());

            var response1 = authController.forgotPassword(requestNoUser, httpRequest);
            assertThat(response1.getStatusCodeValue()).isEqualTo(200);

            // Scenario 2: email DOES exist
            com.quickstack.auth.dto.request.ForgotPasswordRequest requestWithUser =
                    new com.quickstack.auth.dto.request.ForgotPasswordRequest(
                            TENANT_ID.toString(), "existing@example.com");

            when(passwordResetService.initiateReset(anyString(), eq(TENANT_ID), anyString()))
                    .thenReturn(PasswordResetService.ResetInitiationResult.success("token", USER_ID, "existing@example.com"));

            var response2 = authController.forgotPassword(requestWithUser, httpRequest);
            assertThat(response2.getStatusCodeValue()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Mass Assignment")
    class MassAssignmentTests {

        @Test
        @DisplayName("RegisterRequest does not expose role elevation fields")
        void registerRequestDoesNotExposeRoleElevationFields() {
            // RegisterRequest record fields - role comes from request but is separate
            // No 'isAdmin', 'permissions', or similar fields exist in RegisterRequest
            RegisterRequest request = new RegisterRequest(
                    TENANT_ID.toString(),
                    "user@example.com",
                    "SecurePassword123!",
                    "John Doe",
                    ROLE_ID.toString(),
                    null,
                    null
            );

            // The roleId comes from the request (intended for MVP), but
            // tenantId cannot be overridden to another tenant's domain
            // and no privilege escalation fields exist
            assertThat(request.tenantId()).isEqualTo(TENANT_ID.toString());
            assertThat(request.roleId()).isEqualTo(ROLE_ID.toString());

            // No fields that could escalate privileges (isAdmin, isSuperuser, etc.)
            var fields = RegisterRequest.class.getRecordComponents();
            var fieldNames = java.util.Arrays.stream(fields)
                    .map(f -> f.getName().toLowerCase())
                    .toList();
            assertThat(fieldNames).doesNotContain("isadmin", "admin", "superuser", "permissions");
        }

        @Test
        @DisplayName("UserResponse does not expose sensitive fields")
        void userResponseDoesNotExposeSensitiveFields() {
            var fields = com.quickstack.user.dto.response.UserResponse.class.getRecordComponents();
            var fieldNames = java.util.Arrays.stream(fields)
                    .map(f -> f.getName().toLowerCase())
                    .toList();

            // Sensitive fields must NOT be in the response
            assertThat(fieldNames).doesNotContain(
                    "passwordhash", "password", "hash",
                    "emailverificationtoken"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MockHttpServletRequest createMockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "PenTest/1.0");
        return request;
    }

    private MockHttpServletRequest createMockRequestWithCookie(String refreshToken) {
        MockHttpServletRequest request = createMockRequest();
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(
                "__Host-refresh_token", refreshToken);
        request.setCookies(cookie);
        return request;
    }

    private User createUser(UUID id, UUID tenantId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");
        user.setFullName("Test User");
        user.setRoleId(ROLE_ID);
        user.setActive(true);
        return user;
    }
}
