package com.quickstack.auth.dto.request;

import jakarta.validation.constraints.*;

/**
 * Request DTO for user registration.
 * <p>
 * Deliberately does not accept role elevation or tenant override
 * to prevent mass assignment attacks.
 */
public record RegisterRequest(

        @NotBlank(message = "Tenant ID is required")
        String tenantId,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotBlank(message = "Role ID is required")
        String roleId,

        String branchId,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone
) {}
