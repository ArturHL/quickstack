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
        String phone,
        String tenantId,
        String roleId,
        String roleCode,
        String branchId,
        boolean isActive,
        Instant createdAt
) {
    public static UserResponse from(User user, String roleCode) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getTenantId().toString(),
                user.getRoleId().toString(),
                roleCode,
                user.getBranchId() != null ? user.getBranchId().toString() : null,
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
