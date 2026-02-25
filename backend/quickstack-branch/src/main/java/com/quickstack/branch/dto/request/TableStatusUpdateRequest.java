package com.quickstack.branch.dto.request;

import com.quickstack.branch.entity.TableStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating the status of a table.
 * <p>
 * Used by the PATCH /api/v1/tables/{id}/status endpoint.
 * Status transitions are managed by the POS and staff.
 */
public record TableStatusUpdateRequest(

    @NotNull(message = "Status is required")
    TableStatus status
) {
}
