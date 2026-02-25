package com.quickstack.branch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a new table within an area.
 * <p>
 * Table number uniqueness within the area is validated at the service layer.
 */
public record TableCreateRequest(

    @NotNull(message = "Area ID is required")
    UUID areaId,

    @NotBlank(message = "Table number is required")
    @Size(max = 20, message = "Table number must not exceed 20 characters")
    String number,

    @Size(max = 100, message = "Table name must not exceed 100 characters")
    String name,

    @Positive(message = "Capacity must be a positive number")
    Integer capacity,

    Integer sortOrder
) {
    /**
     * Returns the effective sort order, defaulting to 0 when not specified.
     */
    public int effectiveSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
