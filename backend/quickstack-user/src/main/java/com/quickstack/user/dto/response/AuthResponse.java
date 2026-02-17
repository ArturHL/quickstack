package com.quickstack.user.dto.response;

import java.time.Instant;

/**
 * Response DTO for authentication operations.
 * <p>
 * Note: Refresh token is NOT included in the response body.
 * It is sent as an HttpOnly cookie for security.
 */
public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserInfo user
) {
    /**
     * Creates an AuthResponse with the given access token and user info.
     *
     * @param accessToken the JWT access token
     * @param expiresInSeconds token expiration time in seconds
     * @param user user information
     * @return AuthResponse instance
     */
    public static AuthResponse of(String accessToken, long expiresInSeconds, UserInfo user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }

    /**
     * Basic user information included in auth response.
     */
    public record UserInfo(
        String id,
        String email,
        String fullName,
        String tenantId,
        String roleId,
        String branchId,
        Instant lastLoginAt
    ) {
        public static UserInfo from(
                String id,
                String email,
                String fullName,
                String tenantId,
                String roleId,
                String branchId,
                Instant lastLoginAt
        ) {
            return new UserInfo(id, email, fullName, tenantId, roleId, branchId, lastLoginAt);
        }
    }
}
