package com.quickstack.pos.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing customer.
 * <p>
 * All fields are optional (partial update). However, if at least one contact
 * field is present, the cross-field constraint ensures it is non-blank.
 * The service layer resolves whether to apply the change or keep existing values.
 */
public record CustomerUpdateRequest(

    @Size(max = 255, message = "Name must not exceed 255 characters")
    String name,

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    String phone,

    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @Size(max = 20, message = "WhatsApp must not exceed 20 characters")
    String whatsapp,

    @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    String addressLine1,

    @Size(max = 100, message = "Address line 2 must not exceed 100 characters")
    String addressLine2,

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    String postalCode,

    String deliveryNotes
) {

    /**
     * Cross-field validation: at least one contact method must be provided.
     * This ensures updates cannot remove all contact information.
     */
    @AssertTrue(message = "At least one contact method (phone, email, or whatsapp) must be provided")
    public boolean isAtLeastOneContactPresent() {
        return isNonBlank(phone) || isNonBlank(email) || isNonBlank(whatsapp);
    }

    private boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
