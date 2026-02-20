package com.quickstack.product.dto.response;

import com.quickstack.product.entity.Category;

import java.util.UUID;

/**
 * Lightweight response DTO for category listings and references.
 * <p>
 * Contains only the fields needed for display in lists or dropdowns.
 */
public record CategorySummaryResponse(
    UUID id,
    String name,
    int sortOrder,
    boolean isActive,
    UUID parentId
) {
    /**
     * Maps a Category entity to a CategorySummaryResponse.
     *
     * @param category the entity to map
     * @return the summary response DTO
     */
    public static CategorySummaryResponse from(Category category) {
        return new CategorySummaryResponse(
            category.getId(),
            category.getName(),
            category.getSortOrder(),
            category.isActive(),
            category.getParentId()
        );
    }
}
