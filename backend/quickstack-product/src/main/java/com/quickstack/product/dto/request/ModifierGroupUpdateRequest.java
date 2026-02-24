package com.quickstack.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a modifier group. All fields are optional — only non-null
 * values are applied. Cross-field validations run against the effective (merged) state
 * in the service layer.
 */
public record ModifierGroupUpdateRequest(

        @Size(max = 100, message = "Modifier group name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Min(value = 0, message = "minSelections must be >= 0")
        Integer minSelections,

        Integer maxSelections,

        Boolean isRequired,

        Integer sortOrder
) {}
