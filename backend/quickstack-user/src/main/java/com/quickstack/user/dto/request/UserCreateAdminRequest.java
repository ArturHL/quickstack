package com.quickstack.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a user by an OWNER administrator.
 * <p>
 * ASVS V2.1.1: Minimum 12 character passwords enforced at both request level and service level.
 */
public record UserCreateAdminRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotNull UUID roleId,
        UUID branchId,
        @Size(max = 20) String phone
) {}
