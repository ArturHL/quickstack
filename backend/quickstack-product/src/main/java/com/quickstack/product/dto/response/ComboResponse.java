package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Combo;
import com.quickstack.product.entity.ComboItem;
import com.quickstack.product.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a combo, including its list of items with product names.
 */
public record ComboResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        String imageUrl,
        BigDecimal price,
        boolean isActive,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        List<ComboItemResponse> items
) {
    /**
     * Maps a Combo entity, its items, and a product lookup map to a ComboResponse.
     *
     * @param combo      the combo entity
     * @param items      the list of combo item entities
     * @param productMap map from productId to Product entity (for name resolution)
     * @return the response DTO
     */
    public static ComboResponse from(Combo combo, List<ComboItem> items, Map<UUID, Product> productMap) {
        List<ComboItemResponse> itemResponses = items.stream()
                .map(item -> ComboItemResponse.from(item, productMap.get(item.getProductId())))
                .toList();

        return new ComboResponse(
                combo.getId(),
                combo.getTenantId(),
                combo.getName(),
                combo.getDescription(),
                combo.getImageUrl(),
                combo.getPrice(),
                combo.isActive(),
                combo.getSortOrder(),
                combo.getCreatedAt(),
                combo.getUpdatedAt(),
                itemResponses
        );
    }
}
