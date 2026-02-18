package com.quickstack.user.dto.response;

import com.quickstack.user.entity.User;

import java.time.Instant;

/**
 * Response DTO for user information.
 * <p>
 * Never includes sensitive fields like password hash.
 */
public record UserResponse(
        String id,
        String email,
        String fullName,
        String tenantId,
        String roleId,
        String branchId,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getTenantId().toString(),
                user.getRoleId().toString(),
                user.getBranchId() != null ? user.getBranchId().toString() : null,
                user.getCreatedAt()
        );
    }
}
