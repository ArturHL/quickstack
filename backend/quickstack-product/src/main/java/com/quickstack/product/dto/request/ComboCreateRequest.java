package com.quickstack.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a combo.
 * <p>
 * Validation rules:
 * - name is required (max 255 chars)
 * - price is required and must be >= 0
 * - items must have at least 2 entries, each with a valid productId and quantity
 */
public record ComboCreateRequest(

        @NotBlank(message = "Combo name is required")
        @Size(max = 255, message = "Combo name must not exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", message = "price must be >= 0")
        BigDecimal price,

        @NotNull(message = "items are required")
        @Size(min = 2, message = "Combo must have at least 2 items")
        List<@Valid ComboItemRequest> items,

        Integer sortOrder
) {
    /**
     * Returns the effective sort order, defaulting to 0 when not specified.
     */
    public int effectiveSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
