package com.quickstack.product.dto.request;

import com.quickstack.product.entity.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an existing product.
 */
public record ProductUpdateRequest(
    @Size(max = 255, message = "NAME_TOO_LONG")
    String name,

    @Size(max = 2000, message = "DESCRIPTION_TOO_LONG")
    String description,

    UUID categoryId,

    @Size(max = 50, message = "SKU_TOO_LONG")
    @Pattern(regexp = "^[A-Z0-9_-]{0,50}$", message = "INVALID_SKU_FORMAT")
    String sku,

    @DecimalMin(value = "0.00", message = "INVALID_PRICE")
    BigDecimal basePrice,

    @DecimalMin(value = "0.00", message = "INVALID_COST_PRICE")
    BigDecimal costPrice,

    @URL(message = "INVALID_IMAGE_URL")
    @Size(max = 500, message = "IMAGE_URL_TOO_LONG")
    String imageUrl,

    ProductType productType,

    Integer sortOrder,

    Boolean isActive,

    @Valid
    List<VariantCreateRequest> variants // For replacing all variants in a VARIANT product
) {}
