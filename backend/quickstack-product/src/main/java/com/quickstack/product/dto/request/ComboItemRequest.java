package com.quickstack.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for a single item within a combo.
 * Used both in combo creation and update operations.
 */
public record ComboItemRequest(

        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be >= 1")
        Integer quantity,

        Boolean allowSubstitutes,

        @Size(max = 50, message = "substituteGroup must not exceed 50 characters")
        String substituteGroup,

        Integer sortOrder
) {
    /**
     * Returns the effective allow_substitutes value, defaulting to false when not specified.
     */
    public boolean effectiveAllowSubstitutes() {
        return allowSubstitutes != null && allowSubstitutes;
    }

    /**
     * Returns the effective sort order, defaulting to 0 when not specified.
     */
    public int effectiveSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
