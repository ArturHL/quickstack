package com.quickstack.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for updating a combo. All fields are optional — only non-null
 * values are applied.
 * <p>
 * If {@code items} is non-null, the entire list replaces the existing combo items.
 * An empty list is not allowed (min 2 items required when updating items).
 */
public record ComboUpdateRequest(

        @Size(max = 255, message = "Combo name must not exceed 255 characters")
        String name,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        @DecimalMin(value = "0.00", message = "price must be >= 0")
        BigDecimal price,

        Boolean isActive,

        @Size(min = 2, message = "Combo must have at least 2 items when updating items")
        List<@Valid ComboItemRequest> items,

        Integer sortOrder
) {}
