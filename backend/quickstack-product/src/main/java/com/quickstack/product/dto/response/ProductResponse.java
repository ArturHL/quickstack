package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response DTO for a product, including its variants and category reference.
 */
public record ProductResponse(
    UUID id,
    String name,
    String description,
    String sku,
    BigDecimal basePrice,
    BigDecimal costPrice,
    String imageUrl,
    ProductType productType,
    boolean isActive,
    boolean isAvailable,
    int sortOrder,
    CategorySummaryResponse category,
    List<VariantResponse> variants
) {
    /**
     * Maps a Product entity and Category entity to a ProductResponse.
     *
     * @param product the product entity
     * @param category the category entity (can be null)
     * @return the detailed product response DTO
     */
    public static ProductResponse from(Product product, CategorySummaryResponse category) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getSku(),
            product.getBasePrice(),
            product.getCostPrice(),
            product.getImageUrl(),
            product.getProductType(),
            product.isActive(),
            product.isAvailable(),
            product.getSortOrder(),
            category,
            product.getVariants() != null ? 
                product.getVariants().stream()
                    .filter(v -> v.getDeletedAt() == null)
                    .map(v -> VariantResponse.from(v, product.getBasePrice()))
                    .toList() : 
                List.of()
        );
    }
}
