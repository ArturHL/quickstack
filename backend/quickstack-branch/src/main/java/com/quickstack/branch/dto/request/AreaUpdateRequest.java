package com.quickstack.branch.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing area.
 * <p>
 * All fields are optional — only non-null values are applied.
 */
public record AreaUpdateRequest(

    @Size(max = 100, message = "Area name must not exceed 100 characters")
    String name,

    String description,

    Integer sortOrder,

    Boolean isActive
) {
}
