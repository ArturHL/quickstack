package com.quickstack.branch.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing branch.
 * <p>
 * All fields are optional — only non-null values are applied.
 */
public record BranchUpdateRequest(

    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    String name,

    @Size(max = 20, message = "Branch code must not exceed 20 characters")
    String code,

    @Size(max = 255, message = "Address must not exceed 255 characters")
    String address,

    @Size(max = 100, message = "City must not exceed 100 characters")
    String city,

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    String phone,

    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    Boolean isActive
) {
}
