package com.quickstack.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for adding a modifier to a modifier group.
 */
public record ModifierCreateRequest(

        @NotNull(message = "modifierGroupId is required")
        UUID modifierGroupId,

        @NotBlank(message = "Modifier name is required")
        @Size(max = 100, message = "Modifier name must not exceed 100 characters")
        String name,

        @NotNull(message = "priceAdjustment is required")
        BigDecimal priceAdjustment,

        Boolean isDefault,

        Integer sortOrder
) {
    /**
     * Returns whether this modifier should be the default selection.
     * Defaults to false when not specified.
     */
    public boolean effectiveIsDefault() {
        return isDefault != null && isDefault;
    }

    /**
     * Returns the effective sort order, defaulting to 0 when not specified.
     */
    public int effectiveSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
