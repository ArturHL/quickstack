package com.quickstack.product.dto.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for updating a modifier. All fields are optional — only non-null
 * values are applied.
 */
public record ModifierUpdateRequest(

        @Size(max = 100, message = "Modifier name must not exceed 100 characters")
        String name,

        BigDecimal priceAdjustment,

        Boolean isDefault,

        Boolean isActive,

        Integer sortOrder
) {}
