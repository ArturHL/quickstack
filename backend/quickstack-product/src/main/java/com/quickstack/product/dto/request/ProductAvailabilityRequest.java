package com.quickstack.product.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing product availability.
 */
public record ProductAvailabilityRequest(
    @NotNull(message = "AVAILABILITY_STATUS_REQUIRED")
    Boolean isAvailable
) {}
