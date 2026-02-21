package com.quickstack.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for completing password reset.
 * <p>
 * Security notes:
 * - Token is single-use and time-limited (1 hour)
 * - Password is validated against policy and breach database
 * - Password is never logged
 * <p>
 * ASVS Compliance:
 * - V2.5.2: Password reset token is single-use
 * - V2.5.4: Password reset token is time-limited
 * - V2.1.7: Password checked against breach database
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Token is required")
    @Size(max = 128, message = "Token cannot exceed 128 characters")
    String token,

    @NotBlank(message = "New password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    String newPassword
) {
    // Password is never logged or exposed
}
