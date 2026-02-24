package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuProductItem(
    UUID id,
    String name,
    BigDecimal basePrice,
    String imageUrl,
    boolean isAvailable,
    ProductType productType,
    List<MenuVariantItem> variants
) {
    public static MenuProductItem from(Product product, List<MenuVariantItem> variants) {
        return new MenuProductItem(
            product.getId(),
            product.getName(),
            product.getBasePrice(),
            product.getImageUrl(),
            product.isAvailable(),
            product.getProductType(),
            variants
        );
    }
}
