package com.quickstack.product.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for updating an existing product variant.
 */
public record VariantUpdateRequest(
    @Size(max = 100, message = "NAME_TOO_LONG")
    String name,

    @Size(max = 50, message = "SKU_TOO_LONG")
    @Pattern(regexp = "^[A-Z0-9_-]{0,50}$", message = "INVALID_SKU_FORMAT")
    String sku,

    BigDecimal priceAdjustment,

    Boolean isDefault,

    Integer sortOrder,

    Boolean isActive
) {}
