package com.quickstack.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for creating a new product variant.
 */
public record VariantCreateRequest(
    @NotBlank(message = "NAME_REQUIRED")
    @Size(max = 100, message = "NAME_TOO_LONG")
    String name,

    @Size(max = 50, message = "SKU_TOO_LONG")
    @Pattern(regexp = "^[A-Z0-9_-]{0,50}$", message = "INVALID_SKU_FORMAT")
    String sku,

    @NotNull(message = "PRICE_ADJUSTMENT_REQUIRED")
    BigDecimal priceAdjustment,

    boolean isDefault,

    Integer sortOrder
) {}
