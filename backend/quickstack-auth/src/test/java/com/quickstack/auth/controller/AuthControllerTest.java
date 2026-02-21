package com.quickstack.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickstack.common.config.properties.CookieProperties;
import com.quickstack.common.config.properties.JwtProperties;
import com.quickstack.common.exception.AccountLockedException;
import com.quickstack.common.exception.AuthenticationException;
import com.quickstack.common.exception.InvalidTokenException;
import com.quickstack.common.exception.PasswordCompromisedException;
import com.quickstack.common.exception.PasswordValidationException;
import com.quickstack.auth.dto.request.ForgotPasswordRequest;
import com.quickstack.auth.dto.request.LoginRequest;
import com.quickstack.auth.dto.request.ResetPasswordRequest;
import com.quickstack.user.entity.User;
import com.quickstack.user.repository.UserRepository;
import com.quickstack.auth.service.LoginAttemptService;
import com.quickstack.auth.service.PasswordResetService;
import com.quickstack.common.security.PasswordService;
import com.quickstack.auth.service.RefreshTokenService;
import com.quickstack.user.service.UserService;
import com.quickstack.auth.dto.request.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 *
 * These tests directly invoke controller methods to verify core logic without
 * full Spring context. Integration tests with MockMvc would be added later in
 * quickstack-app module where full Spring Boot context is available.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController")
class AuthControllerTest {

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

    private JwtProperties jwtProperties;
    private CookieProperties cookieProperties;
    private AuthController.JwtServiceAdapter jwtServiceAdapter;
    private AuthController authController;
    private ObjectMapper objectMapper;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final String EMAIL = "test@example.com";
    private static final String PASSWORD = "SecurePassword123!";
    private static final String REFRESH_TOKEN = "mock-refresh-token";

    private User testUser;

    @BeforeEach
    void setUp() {
        // Configure JWT properties
        jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenExpiration(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExpiration(Duration.ofDays(7));

        // Configure cookie properties
        cookieProperties = new CookieProperties();
        CookieProperties.RefreshTokenCookie rtCookie = new CookieProperties.RefreshTokenCookie();
        rtCookie.setName("__Host-refresh_token");
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(true);
        rtCookie.setPath("/api/v1/auth");
        rtCookie.setMaxAge(Duration.ofDays(7));
        rtCookie.setSameSite(CookieProperties.SameSiteMode.STRICT);
        cookieProperties.setRefreshToken(rtCookie);

        // Mock JWT service
        jwtServiceAdapter = user -> "mock-access-token-" + user.getId();

        // Create controller
        authController = new AuthController(
                userRepository,
                userService,
                passwordService,
                passwordResetService,
                loginAttemptService,
                refreshTokenService,
                jwtProperties,
                cookieProperties,
                jwtServiceAdapter
        );

        objectMapper = new ObjectMapper();

        // Setup test user
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setTenantId(TENANT_ID);
        testUser.setEmail(EMAIL);
        testUser.setPasswordHash("hashed-password");
        testUser.setFullName("John Doe");
        testUser.setRoleId(ROLE_ID);
        testUser.setActive(true);
    }

    private MockHttpServletRequest createMockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Test/1.0");
        return request;
    }

    // -------------------------------------------------------------------------
    // Login Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("should return tokens on successful login")
        void shouldReturnTokensOnSuccess() {
            LoginRequest request = new LoginRequest(TENANT_ID.toString(), EMAIL, PASSWORD);
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(passwordService.verifyPassword(PASSWORD, "hashed-password"))
                    .thenReturn(true);
            when(refreshTokenService.createRefreshToken(eq(USER_ID), anyString(), anyString()))
                    .thenReturn(REFRESH_TOKEN);
            when(userRepository.save(any(User.class)))
                    .thenReturn(testUser);

            var response = authController.login(request, httpRequest, httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error() == null).isTrue();
            assertThat(response.getBody().data().accessToken()).contains("mock-access-token");
            assertThat(response.getBody().data().tokenType()).isEqualTo("Bearer");
            assertThat(response.getBody().data().expiresIn()).isEqualTo(900);
            assertThat(response.getBody().data().user().id()).isEqualTo(USER_ID.toString());

            // Verify cookie is set
            String setCookie = httpResponse.getHeader("Set-Cookie");
            assertThat(setCookie).contains("__Host-refresh_token=" + REFRESH_TOKEN);
            assertThat(setCookie).contains("HttpOnly");
            assertThat(setCookie).contains("Secure");
            assertThat(setCookie).contains("SameSite=Strict");

            // Verify successful login recorded
            verify(loginAttemptService).recordSuccessfulLogin(
                    eq(EMAIL), eq(USER_ID), eq(TENANT_ID), anyString(), anyString()
            );
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should throw AuthenticationException when user not found")
        void shouldThrowWhenUserNotFound() {
            LoginRequest request = new LoginRequest(TENANT_ID.toString(), EMAIL, PASSWORD);
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authController.login(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationException.class);

            // Verify failed attempt recorded
            verify(loginAttemptService).recordFailedLogin(
                    eq(EMAIL), isNull(), eq(TENANT_ID), anyString(), anyString(), eq("user_not_found")
            );
        }

        @Test
        @DisplayName("should throw AuthenticationException when password is invalid")
        void shouldThrowWhenPasswordInvalid() {
            LoginRequest request = new LoginRequest(TENANT_ID.toString(), EMAIL, "wrongpassword");
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL))
                    .thenReturn(Optional.of(testUser));
            when(passwordService.verifyPassword("wrongpassword", "hashed-password"))
                    .thenReturn(false);

            assertThatThrownBy(() -> authController.login(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationException.class);

            // Verify failed attempt recorded
            verify(loginAttemptService).recordFailedLogin(
                    eq(EMAIL), eq(USER_ID), eq(TENANT_ID), anyString(), anyString(), eq("invalid_credentials")
            );
        }

        @Test
        @DisplayName("should propagate AccountLockedException when account is locked")
        void shouldPropagateAccountLockedException() {
            LoginRequest request = new LoginRequest(TENANT_ID.toString(), EMAIL, PASSWORD);
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();
            Instant lockedUntil = Instant.now().plusSeconds(900);

            doThrow(new AccountLockedException(lockedUntil))
                    .when(loginAttemptService).checkAccountLock(EMAIL, TENANT_ID);

            assertThatThrownBy(() -> authController.login(request, httpRequest, httpResponse))
                    .isInstanceOf(AccountLockedException.class)
                    .satisfies(ex -> {
                        AccountLockedException ale = (AccountLockedException) ex;
                        assertThat(ale.getLockedUntil()).isEqualTo(lockedUntil);
                    });
        }

        @Test
        @DisplayName("should throw AuthenticationException when user is inactive")
        void shouldThrowWhenUserInactive() {
            LoginRequest request = new LoginRequest(TENANT_ID.toString(), EMAIL, PASSWORD);
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();
            testUser.setActive(false); // Make user inactive

            when(userRepository.findByTenantIdAndEmail(TENANT_ID, EMAIL))
                    .thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authController.login(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationException.class);

            verify(loginAttemptService).recordFailedLogin(
                    eq(EMAIL), eq(USER_ID), eq(TENANT_ID), anyString(), any(), eq("account_inactive")
            );
        }
    }

    // -------------------------------------------------------------------------
    // Refresh Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Refresh")
    class RefreshTests {

        @Test
        @DisplayName("should return new tokens on successful refresh")
        void shouldReturnNewTokensOnSuccess() {
            String newRefreshToken = "new-refresh-token";
            RefreshTokenService.RotationResult rotation = new RefreshTokenService.RotationResult(
                    newRefreshToken, USER_ID
            );

            MockHttpServletRequest httpRequest = createMockRequest();
            httpRequest.setCookies(new Cookie("__Host-refresh_token", REFRESH_TOKEN));
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            when(refreshTokenService.rotateToken(eq(REFRESH_TOKEN), anyString(), anyString()))
                    .thenReturn(rotation);
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(testUser));

            var response = authController.refresh(httpRequest, httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().error() == null).isTrue();
            assertThat(response.getBody().data().accessToken()).contains("mock-access-token");

            // Verify new refresh token cookie is set
            String setCookie = httpResponse.getHeader("Set-Cookie");
            assertThat(setCookie).contains("__Host-refresh_token=" + newRefreshToken);
        }

        @Test
        @DisplayName("should throw AuthenticationException when cookie is missing")
        void shouldThrowWhenCookieMissing() {
            MockHttpServletRequest httpRequest = createMockRequest();
            // No cookie set
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            assertThatThrownBy(() -> authController.refresh(httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("should throw when refresh token is invalid")
        void shouldThrowWhenTokenInvalid() {
            MockHttpServletRequest httpRequest = createMockRequest();
            httpRequest.setCookies(new Cookie("__Host-refresh_token", "invalid-token"));
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            when(refreshTokenService.rotateToken(eq("invalid-token"), anyString(), anyString()))
                    .thenThrow(InvalidTokenException.expired(InvalidTokenException.TokenType.REFRESH_TOKEN));

            assertThatThrownBy(() -> authController.refresh(httpRequest, httpResponse))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("should revoke all tokens when user cannot login")
        void shouldRevokeTokensWhenUserInactive() {
            RefreshTokenService.RotationResult rotation = new RefreshTokenService.RotationResult(
                    "new-token", USER_ID
            );
            testUser.setActive(false);

            MockHttpServletRequest httpRequest = createMockRequest();
            httpRequest.setCookies(new Cookie("__Host-refresh_token", REFRESH_TOKEN));
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            when(refreshTokenService.rotateToken(eq(REFRESH_TOKEN), anyString(), anyString()))
                    .thenReturn(rotation);
            when(userRepository.findById(USER_ID))
                    .thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authController.refresh(httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationException.class);

            verify(refreshTokenService).revokeAllUserTokens(USER_ID, "account_disabled");
        }
    }

    // -------------------------------------------------------------------------
    // Logout Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("should return 204 and revoke token on successful logout")
        void shouldReturn204OnSuccess() {
            MockHttpServletRequest httpRequest = createMockRequest();
            httpRequest.setCookies(new Cookie("__Host-refresh_token", REFRESH_TOKEN));
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            var response = authController.logout(httpRequest, httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Verify token revoked
            verify(refreshTokenService).revokeToken(REFRESH_TOKEN, "logout");

            // Verify cookie cleared
            String setCookie = httpResponse.getHeader("Set-Cookie");
            assertThat(setCookie).contains("__Host-refresh_token=");
            assertThat(setCookie).contains("Max-Age=0");
        }

        @Test
        @DisplayName("should return 204 even without cookie")
        void shouldReturn204WithoutCookie() {
            MockHttpServletRequest httpRequest = createMockRequest();
            MockHttpServletResponse httpResponse = new MockHttpServletResponse();

            var response = authController.logout(httpRequest, httpResponse);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Should not throw, just complete without revoking
            verify(refreshTokenService, never()).revokeToken(anyString(), anyString());
        }
    }

    // -------------------------------------------------------------------------
    // Forgot Password Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Forgot Password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should return success message for existing user")
        void shouldReturnSuccessForExistingUser() {
            ForgotPasswordRequest request = new ForgotPasswordRequest(TENANT_ID.toString(), EMAIL);
            MockHttpServletRequest httpRequest = createMockRequest();

            PasswordResetService.ResetInitiationResult result =
                    PasswordResetService.ResetInitiationResult.success("reset-token", USER_ID, EMAIL);

            when(passwordResetService.initiateReset(eq(EMAIL), eq(TENANT_ID), anyString()))
                    .thenReturn(result);

            var response = authController.forgotPassword(request, httpRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().message())
                    .contains("If the email exists");

            verify(passwordResetService).initiateReset(eq(EMAIL), eq(TENANT_ID), anyString());
        }

        @Test
        @DisplayName("should return same success message for non-existing user (timing-safe)")
        void shouldReturnSuccessForNonExistingUser() {
            ForgotPasswordRequest request = new ForgotPasswordRequest(TENANT_ID.toString(), "nonexistent@example.com");
            MockHttpServletRequest httpRequest = createMockRequest();

            PasswordResetService.ResetInitiationResult result =
                    PasswordResetService.ResetInitiationResult.userNotFound();

            when(passwordResetService.initiateReset(eq("nonexistent@example.com"), eq(TENANT_ID), anyString()))
                    .thenReturn(result);

            var response = authController.forgotPassword(request, httpRequest);

            // Response should be the same (timing-safe)
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().message())
                    .contains("If the email exists");
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeEmail() {
            ForgotPasswordRequest request = new ForgotPasswordRequest(TENANT_ID.toString(), "USER@EXAMPLE.COM");
            MockHttpServletRequest httpRequest = createMockRequest();

            when(passwordResetService.initiateReset(eq("user@example.com"), eq(TENANT_ID), anyString()))
                    .thenReturn(PasswordResetService.ResetInitiationResult.userNotFound());

            authController.forgotPassword(request, httpRequest);

            verify(passwordResetService).initiateReset(eq("user@example.com"), eq(TENANT_ID), anyString());
        }
    }

    // -------------------------------------------------------------------------
    // Reset Password Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Reset Password")
    class ResetPasswordTests {

        @Test
        @DisplayName("should return success on valid reset")
        void shouldReturnSuccessOnValidReset() {
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newSecurePassword123!");

            doNothing().when(passwordResetService).resetPassword("valid-token", "newSecurePassword123!");

            var response = authController.resetPassword(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data().message())
                    .contains("Password has been reset successfully");

            verify(passwordResetService).resetPassword("valid-token", "newSecurePassword123!");
        }

        @Test
        @DisplayName("should throw InvalidTokenException for invalid token")
        void shouldThrowForInvalidToken() {
            ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newSecurePassword123!");

            doThrow(InvalidTokenException.expired(InvalidTokenException.TokenType.PASSWORD_RESET_TOKEN))
                    .when(passwordResetService).resetPassword("invalid-token", "newSecurePassword123!");

            assertThatThrownBy(() -> authController.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("should throw PasswordValidationException for weak password")
        void shouldThrowForWeakPassword() {
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "weak");

            doThrow(PasswordValidationException.tooShort(12))
                    .when(passwordResetService).resetPassword("valid-token", "weak");

            assertThatThrownBy(() -> authController.resetPassword(request))
                    .isInstanceOf(PasswordValidationException.class);
        }

        @Test
        @DisplayName("should throw PasswordCompromisedException for breached password")
        void shouldThrowForBreachedPassword() {
            ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "password123");

            doThrow(PasswordCompromisedException.withBreachCount(1000))
                    .when(passwordResetService).resetPassword("valid-token", "password123");

            assertThatThrownBy(() -> authController.resetPassword(request))
                    .isInstanceOf(PasswordCompromisedException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Register Tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        private RegisterRequest validRequest;

        @BeforeEach
        void setUpRegisterTests() {
            validRequest = new RegisterRequest(
                    TENANT_ID.toString(),
                    EMAIL,
                    "SecurePassword123!",
                    "John Doe",
                    ROLE_ID.toString(),
                    null,
                    null
            );
        }

        @Test
        @DisplayName("should return 201 with user info on successful registration")
        void shouldReturn201OnSuccessfulRegistration() {
            when(userService.registerUser(any(UserService.RegisterUserCommand.class)))
                    .thenReturn(testUser);

            var response = authController.register(validRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data()).isNotNull();
        }

        @Test
        @DisplayName("should propagate EmailAlreadyExistsException for duplicate email")
        void shouldPropagateEmailAlreadyExistsException() {
            doThrow(new UserService.EmailAlreadyExistsException())
                    .when(userService).registerUser(any(UserService.RegisterUserCommand.class));

            assertThatThrownBy(() -> authController.register(validRequest))
                    .isInstanceOf(UserService.EmailAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should propagate PasswordCompromisedException for breached password")
        void shouldPropagatePasswordCompromisedException() {
            doThrow(PasswordCompromisedException.withBreachCount(100))
                    .when(userService).registerUser(any(UserService.RegisterUserCommand.class));

            assertThatThrownBy(() -> authController.register(validRequest))
                    .isInstanceOf(PasswordCompromisedException.class);
        }

        @Test
        @DisplayName("should create RegisterUserCommand with correct tenant and role")
        void shouldCreateCommandWithCorrectTenantAndRole() {
            when(userService.registerUser(any(UserService.RegisterUserCommand.class)))
                    .thenReturn(testUser);

            authController.register(validRequest);

            var captor = org.mockito.ArgumentCaptor.forClass(UserService.RegisterUserCommand.class);
            verify(userService).registerUser(captor.capture());

            UserService.RegisterUserCommand command = captor.getValue();
            assertThat(command.tenantId()).isEqualTo(TENANT_ID);
            assertThat(command.email()).isEqualTo(EMAIL);
            assertThat(command.roleId()).isEqualTo(ROLE_ID);
            assertThat(command.branchId()).isNull();
        }
    }
}
