package com.quickstack.product.dto.request;

import com.quickstack.product.entity.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new product.
 */
public record ProductCreateRequest(
    @NotBlank(message = "NAME_REQUIRED")
    @Size(max = 255, message = "NAME_TOO_LONG")
    String name,

    @Size(max = 2000, message = "DESCRIPTION_TOO_LONG")
    String description,

    @NotNull(message = "CATEGORY_REQUIRED")
    UUID categoryId,

    @Size(max = 50, message = "SKU_TOO_LONG")
    @Pattern(regexp = "^[A-Z0-9_-]{0,50}$", message = "INVALID_SKU_FORMAT")
    String sku,

    @NotNull(message = "BASE_PRICE_REQUIRED")
    @DecimalMin(value = "0.00", message = "INVALID_PRICE")
    BigDecimal basePrice,

    @DecimalMin(value = "0.00", message = "INVALID_COST_PRICE")
    BigDecimal costPrice,

    @URL(message = "INVALID_IMAGE_URL")
    @Size(max = 500, message = "IMAGE_URL_TOO_LONG")
    String imageUrl,

    @NotNull(message = "PRODUCT_TYPE_REQUIRED")
    ProductType productType,

    Integer sortOrder,

    @Valid
    List<VariantCreateRequest> variants
) {}
