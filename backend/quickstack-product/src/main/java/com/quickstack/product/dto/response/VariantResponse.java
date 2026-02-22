package com.quickstack.product.dto.response;

import com.quickstack.product.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detailed response DTO for a product variant.
 */
public record VariantResponse(
    UUID id,
    String name,
    String sku,
    BigDecimal priceAdjustment,
    BigDecimal effectivePrice,
    boolean isDefault,
    boolean isActive,
    int sortOrder
) {
    /**
     * Maps a ProductVariant entity and product base price to a VariantResponse.
     *
     * @param variant the variant entity
     * @param basePrice the base price of the parent product
     * @return the variant response DTO
     */
    public static VariantResponse from(ProductVariant variant, BigDecimal basePrice) {
        return new VariantResponse(
            variant.getId(),
            variant.getName(),
            variant.getSku(),
            variant.getPriceAdjustment(),
            variant.getEffectivePrice(basePrice),
            variant.isDefault(),
            variant.isActive(),
            variant.getSortOrder()
        );
    }
}
