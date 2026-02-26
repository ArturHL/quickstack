package com.quickstack.pos.dto.response;

import com.quickstack.pos.entity.OrderItemModifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a modifier applied to an order item.
 * All fields are historical snapshots — they reflect values at order creation time.
 */
public record OrderItemModifierResponse(
    UUID id,
    UUID tenantId,
    UUID orderItemId,
    UUID modifierId,
    String modifierName,
    BigDecimal priceAdjustment,
    int quantity,
    Instant createdAt
) {

    /**
     * Maps an OrderItemModifier entity to a response DTO.
     *
     * @param modifier the entity to map
     * @return the response DTO
     */
    public static OrderItemModifierResponse from(OrderItemModifier modifier) {
        return new OrderItemModifierResponse(
            modifier.getId(),
            modifier.getTenantId(),
            modifier.getOrderItemId(),
            modifier.getModifierId(),
            modifier.getModifierName(),
            modifier.getPriceAdjustment(),
            modifier.getQuantity(),
            modifier.getCreatedAt()
        );
    }
}
