package com.quickstack.user.dto.request;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating a user by an OWNER administrator.
 * <p>
 * All fields are optional — only non-null fields are applied.
 * Email and password cannot be changed via this endpoint.
 */
public record UserUpdateAdminRequest(
        @Size(max = 255) String fullName,
        @Size(max = 20) String phone,
        UUID roleId
) {}
