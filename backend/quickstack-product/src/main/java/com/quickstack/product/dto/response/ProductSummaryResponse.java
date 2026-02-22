package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight response DTO for product listings and search results.
 */
public record ProductSummaryResponse(
    UUID id,
    String name,
    BigDecimal basePrice,
    ProductType productType,
    boolean isAvailable,
    boolean isActive,
    UUID categoryId,
    String imageUrl,
    int sortOrder
) {
    /**
     * Maps a Product entity to a ProductSummaryResponse.
     *
     * @param product the entity to map
     * @return the summary response DTO
     */
    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(
            product.getId(),
            product.getName(),
            product.getBasePrice(),
            product.getProductType(),
            product.isAvailable(),
            product.isActive(),
            product.getCategoryId(),
            product.getImageUrl(),
            product.getSortOrder()
        );
    }
}
