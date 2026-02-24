package com.quickstack.product.dto.response;

import com.quickstack.product.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuVariantItem(
    UUID id,
    String name,
    BigDecimal priceAdjustment,
    BigDecimal effectivePrice,
    boolean isDefault,
    int sortOrder
) {
    public static MenuVariantItem from(ProductVariant variant, BigDecimal basePrice) {
        return new MenuVariantItem(
            variant.getId(),
            variant.getName(),
            variant.getPriceAdjustment(),
            variant.getEffectivePrice(basePrice),
            variant.isDefault(),
            variant.getSortOrder()
        );
    }
}
