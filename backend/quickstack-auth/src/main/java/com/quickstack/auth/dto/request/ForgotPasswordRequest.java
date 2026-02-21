package com.quickstack.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for initiating password reset.
 * <p>
 * Security notes:
 * - Email is normalized to lowercase
 * - Response is timing-safe (same for existing/non-existing users)
 * - Rate limited per IP and email
 * <p>
 * ASVS Compliance:
 * - V2.5.7: Password reset doesn't reveal account existence
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Tenant ID is required")
    String tenantId,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    String email
) {
    /**
     * Returns the email normalized to lowercase.
     */
    public String normalizedEmail() {
        return email != null ? email.toLowerCase().trim() : null;
    }

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
