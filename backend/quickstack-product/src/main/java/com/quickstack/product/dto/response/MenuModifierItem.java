package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Modifier;
import java.math.BigDecimal;
import java.util.UUID;

public record MenuModifierItem(
    UUID id,
    String name,
    BigDecimal priceAdjustment,
    boolean isDefault,
    int sortOrder
) {
    public static MenuModifierItem from(Modifier modifier) {
        return new MenuModifierItem(
            modifier.getId(),
            modifier.getName(),
            modifier.getPriceAdjustment(),
            modifier.isDefault(),
            modifier.getSortOrder()
        );
    }
}
