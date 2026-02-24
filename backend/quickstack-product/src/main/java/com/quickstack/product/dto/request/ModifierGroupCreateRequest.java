package com.quickstack.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a modifier group.
 * <p>
 * Cross-field validations:
 * - If isRequired = true, minSelections must be >= 1
 * - If maxSelections is provided, it must be >= minSelections
 */
public record ModifierGroupCreateRequest(

        @NotNull(message = "productId is required")
        UUID productId,

        @NotBlank(message = "Modifier group name is required")
        @Size(max = 100, message = "Modifier group name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "minSelections is required")
        @Min(value = 0, message = "minSelections must be >= 0")
        Integer minSelections,

        Integer maxSelections,

        @NotNull(message = "isRequired is required")
        Boolean isRequired,

        Integer sortOrder
) {
    /**
     * Validates that if isRequired is true, minSelections must be >= 1.
     */
    @jakarta.validation.constraints.AssertTrue(message = "If isRequired is true, minSelections must be >= 1")
    public boolean isValidRequiredConfig() {
        if (isRequired == null || minSelections == null) return true;
        return !isRequired || minSelections >= 1;
    }

    /**
     * Validates that maxSelections >= minSelections when both are provided.
     */
    @jakarta.validation.constraints.AssertTrue(message = "maxSelections must be >= minSelections when specified")
    public boolean isValidSelectionRange() {
        if (maxSelections == null || minSelections == null) return true;
        return maxSelections >= minSelections;
    }

    /**
     * Returns the effective sort order, defaulting to 0 when not specified.
     */
    public int effectiveSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
