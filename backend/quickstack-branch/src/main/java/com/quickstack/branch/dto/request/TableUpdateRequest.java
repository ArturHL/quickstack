package com.quickstack.branch.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing table.
 * <p>
 * All fields are optional — only non-null values are applied.
 * To update status, use the dedicated PATCH /tables/{id}/status endpoint.
 */
public record TableUpdateRequest(

    @Size(max = 20, message = "Table number must not exceed 20 characters")
    String number,

    @Size(max = 100, message = "Table name must not exceed 100 characters")
    String name,

    @Positive(message = "Capacity must be a positive number")
    Integer capacity,

    Integer sortOrder,

    Integer positionX,

    Integer positionY,

    Boolean isActive
) {
}
