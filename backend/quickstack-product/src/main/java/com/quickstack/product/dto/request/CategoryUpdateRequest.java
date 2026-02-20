package com.quickstack.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

/**
 * Request DTO for updating an existing category.
 * <p>
 * All fields are optional — only non-null fields will be applied to the category.
 * If {@code name} is provided, it must not be blank.
 */
public record CategoryUpdateRequest(

    @NotBlank(message = "Category name must not be blank if provided")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    String name,

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @URL(message = "Image URL must be a valid URL")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    String imageUrl,

    UUID parentId,

    Integer sortOrder,

    Boolean isActive
) {}
