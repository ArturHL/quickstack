package com.quickstack.pos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for a modifier applied to an order item.
 * <p>
 * Modifier data is snapshotted at order creation time to preserve historical
 * accuracy. The modifierId is optional — it links to the catalog for reference
 * but may be null if the modifier was later deleted from the catalog.
 * <p>
 * Business Rules:
 * - quantity defaults to 1 at the service layer if 0 or null is received
 * - priceAdjustment may be negative (discount), zero (free add-on), or positive (surcharge)
 */
public record OrderItemModifierRequest(

    // Optional link to catalog modifier — null allowed for historical records
    UUID modifierId,

    @NotBlank(message = "Modifier name is required")
    @Size(max = 100, message = "Modifier name must not exceed 100 characters")
    String modifierName,

    @NotNull(message = "Price adjustment is required")
    BigDecimal priceAdjustment,

    // Service layer normalizes 0 to 1 — explicit 0 means caller omitted the field
    int quantity
) {
}
