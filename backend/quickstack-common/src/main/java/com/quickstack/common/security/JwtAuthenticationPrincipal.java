package com.quickstack.common.security;

import java.util.UUID;

/**
 * Principal object containing authenticated user context extracted from JWT claims.
 * <p>
 * Stored in the Spring Security context after JWT validation. Available in
 * controllers via {@code @AuthenticationPrincipal JwtAuthenticationPrincipal}.
 * <p>
 * Fields match JWT claims populated by JwtService:
 * - sub  → userId
 * - tid  → tenantId
 * - rid  → roleId
 * - bid  → branchId (nullable)
 * - email → email
 */
public record JwtAuthenticationPrincipal(
        UUID userId,
        UUID tenantId,
        UUID roleId,
        UUID branchId,
        String email
) {
    /**
     * Returns the user ID as the principal name (required by Spring Security).
     */
    public String getName() {
        return userId.toString();
    }
}
