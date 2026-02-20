package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Category;

import java.time.Instant;
import java.util.UUID;

/**
 * Full response DTO for a category, including audit fields and product count.
 * <p>
 * Returned by all category read and write operations.
 */
public record CategoryResponse(
    UUID id,
    UUID tenantId,
    UUID parentId,
    String name,
    String description,
    String imageUrl,
    int sortOrder,
    boolean isActive,
    int productCount,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
    /**
     * Maps a Category entity to a CategoryResponse.
     *
     * @param category the entity to map
     * @param productCount the number of active products in this category
     * @return the response DTO
     */
    public static CategoryResponse from(Category category, long productCount) {
        return new CategoryResponse(
            category.getId(),
            category.getTenantId(),
            category.getParentId(),
            category.getName(),
            category.getDescription(),
            category.getImageUrl(),
            category.getSortOrder(),
            category.isActive(),
            (int) productCount,
            category.getCreatedAt(),
            category.getUpdatedAt(),
            category.getCreatedBy(),
            category.getUpdatedBy()
        );
    }
}
