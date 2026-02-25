package com.quickstack.branch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a new area within a branch.
 * <p>
 * Area name uniqueness within the branch is validated at the service layer.
 * The branchId in the request is validated against the path variable in the controller.
 */
public record AreaCreateRequest(

    @NotNull(message = "Branch ID is required")
    UUID branchId,

    @NotBlank(message = "Area name is required")
    @Size(max = 100, message = "Area name must not exceed 100 characters")
    String name,

    String description,

    Integer sortOrder
) {
    /**
     * Returns the effective sort order, defaulting to 0 when not specified.
     */
    public int effectiveSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}
