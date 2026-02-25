package com.quickstack.auth.controller;

import com.quickstack.common.config.properties.CookieProperties;
import com.quickstack.common.config.properties.JwtProperties;
import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.exception.AuthenticationException;
import com.quickstack.common.security.IpAddressExtractor;
import com.quickstack.auth.dto.request.ForgotPasswordRequest;
import com.quickstack.auth.dto.request.LoginRequest;
import com.quickstack.auth.dto.request.RegisterRequest;
import com.quickstack.auth.dto.request.ResetPasswordRequest;
import com.quickstack.auth.dto.response.AuthResponse;
import com.quickstack.user.dto.response.UserResponse;
import com.quickstack.auth.entity.LoginAttempt;
import com.quickstack.auth.service.LoginAttemptService;
import com.quickstack.auth.service.PasswordResetService;
import com.quickstack.common.security.PasswordService;
import com.quickstack.auth.service.RefreshTokenService;
import com.quickstack.user.entity.User;
import com.quickstack.user.repository.UserRepository;
import com.quickstack.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for authentication operations.
 * <p>
 * Endpoints:
 * - POST /api/v1/auth/login - Authenticate and get tokens
 * - POST /api/v1/auth/refresh - Refresh access token
 * - POST /api/v1/auth/logout - Revoke tokens
 * - POST /api/v1/auth/forgot-password - Initiate password reset
 * - POST /api/v1/auth/reset-password - Complete password reset
 * <p>
 * Security features:
 * - Account lockout after failed attempts
 * - Refresh token rotation
 * - HttpOnly secure cookies for refresh tokens
 * <p>
 * ASVS Compliance:
 * - V2.2.1: Account lockout
 * - V2.5.1: Secure password recovery mechanism
 * - V2.5.2: Password reset token is single-use
 * - V2.5.4: Password reset token is time-limited
 * - V2.5.7: Password reset doesn't reveal account existence
 * - V3.4: Secure cookie attributes
 * - V3.5: Token rotation
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordService passwordService;
    private final PasswordResetService passwordResetService;
    private final LoginAttemptService loginAttemptService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;
    private final JwtServiceAdapter jwtService;

    /**
     * Adapter interface for JwtService to avoid circular dependency.
     * JwtService is in quickstack-app, this controller is in quickstack-user.
     */
    public interface JwtServiceAdapter {
        String generateAccessToken(User user);
    }

    public AuthController(
            UserRepository userRepository,
            UserService userService,
            PasswordService passwordService,
            PasswordResetService passwordResetService,
            LoginAttemptService loginAttemptService,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties,
            CookieProperties cookieProperties,
            JwtServiceAdapter jwtService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordService = passwordService;
        this.passwordResetService = passwordResetService;
        this.loginAttemptService = loginAttemptService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
        this.cookieProperties = cookieProperties;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user for the given tenant.
     * <p>
     * Validates:
     * - Email uniqueness within the tenant
     * - Password policy (12-128 characters)
     * - Password not in breach database (HIBP)
     *
     * @param request registration data
     * @return 201 Created with user info
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UUID tenantId = UUID.fromString(request.tenantId());
        UUID roleId = UUID.fromString(request.roleId());
        UUID branchId = request.branchId() != null ? UUID.fromString(request.branchId()) : null;

        log.info("Registration attempt for tenant {}", tenantId);

        UserService.RegisterUserCommand command = new UserService.RegisterUserCommand(
                tenantId,
                request.email(),
                request.password(),
                request.fullName(),
                roleId,
                branchId,
                request.phone());

        User user = userService.registerUser(command);

        log.info("User registered successfully: userId={}", user.getId());
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(UserResponse.from(user)));
    }

    /**
     * Authenticates a user and returns access token.
     * Refresh token is set as HttpOnly cookie.
     *
     * @param request      login credentials
     * @param httpRequest  for extracting client IP
     * @param httpResponse for setting refresh token cookie
     * @return access token and user info
     */
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String ipAddress = IpAddressExtractor.extract(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        UUID tenantId = UUID.fromString(request.tenantId());

        log.info("Login attempt for {} from IP {}", request.maskedEmail(), ipAddress);

        // Check if account is locked
        loginAttemptService.checkAccountLock(request.email(), tenantId);

        // Find user
        User user = userRepository.findByTenantIdAndEmail(tenantId, request.email())
                .orElse(null);

        // User not found - record attempt and throw generic error
        if (user == null) {
            loginAttemptService.recordFailedLogin(
                    request.email(), null, tenantId, ipAddress, userAgent,
                    LoginAttempt.REASON_USER_NOT_FOUND);
            throw new AuthenticationException();
        }

        // Check if user can login (not locked, active, not deleted)
        if (!user.canLogin()) {
            String reason = user.isLocked()
                    ? LoginAttempt.REASON_ACCOUNT_LOCKED
                    : LoginAttempt.REASON_ACCOUNT_INACTIVE;
            loginAttemptService.recordFailedLogin(
                    request.email(), user.getId(), tenantId, ipAddress, userAgent, reason);
            throw new AuthenticationException();
        }

        // Verify password
        if (!passwordService.verifyPassword(request.password(), user.getPasswordHash())) {
            loginAttemptService.recordFailedLogin(
                    request.email(), user.getId(), tenantId, ipAddress, userAgent,
                    LoginAttempt.REASON_INVALID_CREDENTIALS);
            throw new AuthenticationException();
        }

        // Successful login
        loginAttemptService.recordSuccessfulLogin(
                request.email(), user.getId(), tenantId, ipAddress, userAgent);

        // Update user login info
        user.recordSuccessfulLogin(ipAddress);
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), ipAddress, userAgent);

        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(httpResponse, refreshToken);

        // Build response
        AuthResponse authResponse = AuthResponse.of(
                accessToken,
                jwtProperties.getAccessTokenExpiration().toSeconds(),
                AuthResponse.UserInfo.from(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getTenantId().toString(),
                        user.getRoleId().toString(),
                        user.getBranchId() != null ? user.getBranchId().toString() : null,
                        user.getLastLoginAt()));

        log.info("Login successful for user {} from IP {}", user.getId(), ipAddress);
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    /**
     * Refreshes an access token using the refresh token from cookie.
     *
     * @param httpRequest  for reading refresh token cookie
     * @param httpResponse for setting new refresh token cookie
     * @return new access token
     */
    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String ipAddress = IpAddressExtractor.extract(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Get refresh token from cookie
        String refreshToken = getRefreshTokenFromCookie(httpRequest);
        if (refreshToken == null) {
            log.debug("Refresh token cookie not found");
            throw new AuthenticationException();
        }

        // Rotate token
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotateToken(
                refreshToken, ipAddress, userAgent);

        // Get user
        User user = userRepository.findById(java.util.Objects.requireNonNull(rotation.userId()))
                .orElseThrow(AuthenticationException::new);

        if (!user.canLogin()) {
            refreshTokenService.revokeAllUserTokens(user.getId(), "account_disabled");
            throw new AuthenticationException();
        }

        // Generate new access token
        String accessToken = jwtService.generateAccessToken(user);

        // Set new refresh token cookie
        setRefreshTokenCookie(httpResponse, rotation.newToken());

        AuthResponse authResponse = AuthResponse.of(
                accessToken,
                jwtProperties.getAccessTokenExpiration().toSeconds(),
                AuthResponse.UserInfo.from(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getTenantId().toString(),
                        user.getRoleId().toString(),
                        user.getBranchId() != null ? user.getBranchId().toString() : null,
                        user.getLastLoginAt()));

        log.debug("Token refreshed for user {}", user.getId());
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    /**
     * Logs out the user by revoking the refresh token.
     *
     * @param httpRequest  for reading refresh token cookie
     * @param httpResponse for clearing the cookie
     * @return 204 No Content
     */
    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = getRefreshTokenFromCookie(httpRequest);

        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken, "logout");
        }

        // Clear the cookie
        clearRefreshTokenCookie(httpResponse);

        log.debug("User logged out");
        return ResponseEntity.noContent().build();
    }

    /**
     * Initiates a password reset.
     * <p>
     * This endpoint is timing-safe: it returns the same response whether
     * the email exists or not. This prevents enumeration attacks.
     * <p>
     * The actual password reset token is NOT returned in the response.
     * In production, it would be sent via email.
     *
     * @param request     contains the email and tenant ID
     * @param httpRequest for extracting client IP
     * @return generic success message (doesn't reveal if user exists)
     */
    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = IpAddressExtractor.extract(httpRequest);
        UUID tenantId = UUID.fromString(request.tenantId());

        log.info("Password reset requested for {} from IP {}", request.maskedEmail(), ipAddress);

        // Initiate reset (timing-safe - returns same result for existing/non-existing)
        PasswordResetService.ResetInitiationResult result = passwordResetService.initiateReset(
                request.normalizedEmail(),
                tenantId,
                ipAddress);

        // In production, send email if result.success() is true
        // For security, we return the same response regardless of result
        if (result.success()) {
            log.info("Password reset token generated for user {} (token would be emailed)", result.userId());
        }

        // Always return success message (timing-safe)
        return ResponseEntity.ok(ApiResponse.success(new ForgotPasswordResponse(
                "If the email exists, you will receive a password reset link.")));
    }

    /**
     * Completes a password reset using a valid token.
     * <p>
     * This endpoint:
     * 1. Validates the reset token (single-use, time-limited)
     * 2. Validates the new password against policy
     * 3. Checks the new password against breach database (HIBP)
     * 4. Updates the password
     * 5. Revokes all refresh tokens (forces re-login)
     *
     * @param request contains the token and new password
     * @return success message
     */
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.debug("Password reset attempt with token");

        passwordResetService.resetPassword(request.token(), request.newPassword());

        log.info("Password reset completed successfully");

        return ResponseEntity.ok(ApiResponse.success(new ResetPasswordResponse(
                "Password has been reset successfully. Please login with your new password.")));
    }

    // -------------------------------------------------------------------------
    // Response Records
    // -------------------------------------------------------------------------

    /**
     * Response for forgot-password endpoint.
     */
    public record ForgotPasswordResponse(String message) {
    }

    /**
     * Response for reset-password endpoint.
     */
    public record ResetPasswordResponse(String message) {
    }

    // -------------------------------------------------------------------------
    // Cookie Helpers
    // -------------------------------------------------------------------------

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        CookieProperties.RefreshTokenCookie config = cookieProperties.getRefreshToken();

        Cookie cookie = new Cookie(config.getName(), token);
        cookie.setHttpOnly(config.isHttpOnly());
        cookie.setSecure(config.isSecure());
        cookie.setPath(config.getPath());
        cookie.setMaxAge((int) config.getMaxAgeSeconds());

        // SameSite is set via header since Cookie class doesn't support it directly
        String sameSite = config.getSameSite().getValue();
        String cookieHeader = buildCookieHeader(cookie, sameSite);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        CookieProperties.RefreshTokenCookie config = cookieProperties.getRefreshToken();

        Cookie cookie = new Cookie(config.getName(), "");
        cookie.setHttpOnly(config.isHttpOnly());
        cookie.setSecure(config.isSecure());
        cookie.setPath(config.getPath());
        cookie.setMaxAge(0); // Expire immediately

        String sameSite = config.getSameSite().getValue();
        String cookieHeader = buildCookieHeader(cookie, sameSite);
        response.addHeader("Set-Cookie", cookieHeader);
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String cookieName = cookieProperties.getRefreshToken().getName();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String buildCookieHeader(Cookie cookie, String sameSite) {
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append("=").append(cookie.getValue());

        if (cookie.getMaxAge() >= 0) {
            sb.append("; Max-Age=").append(cookie.getMaxAge());
        }

        if (cookie.getPath() != null) {
            sb.append("; Path=").append(cookie.getPath());
        }

        if (cookie.getSecure()) {
            sb.append("; Secure");
        }

        if (cookie.isHttpOnly()) {
            sb.append("; HttpOnly");
        }

        sb.append("; SameSite=").append(sameSite);

        return sb.toString();
    }
}
