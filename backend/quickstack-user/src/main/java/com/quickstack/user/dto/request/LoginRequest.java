package com.quickstack.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user login.
 * <p>
 * Security notes:
 * - Email is validated but not logged in plaintext
 * - Password is never logged or stored
 * - tenantId is required for multi-tenant isolation
 */
public record LoginRequest(
    @NotBlank(message = "Tenant ID is required")
    String tenantId,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password must be between 1 and 128 characters")
    String password
) {
    /**
     * Returns a masked version of email for logging.
     */
    public String maskedEmail() {
        if (email == null || email.length() < 3) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
