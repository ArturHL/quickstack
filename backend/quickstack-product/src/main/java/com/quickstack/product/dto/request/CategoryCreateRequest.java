package com.quickstack.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

/**
 * Request DTO for creating a new category.
 * <p>
 * Name uniqueness is validated at the service layer (within same tenant and parent level).
 */
public record CategoryCreateRequest(

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @URL(message = "Image URL must be a valid URL")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    String imageUrl,

    UUID parentId,

    Integer sortOrder
) {
    /**
     * Returns the effective sort order, defaulting to 0 when not specified.
     */
    public int effectiveSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
