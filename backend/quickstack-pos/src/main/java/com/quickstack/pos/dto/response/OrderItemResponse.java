package com.quickstack.pos.dto.response;

import com.quickstack.pos.entity.KdsStatus;
import com.quickstack.pos.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a single line item within an order.
 * All price fields are historical snapshots — they reflect values at order creation time.
 */
public record OrderItemResponse(
    UUID id,
    UUID tenantId,
    UUID orderId,
    UUID productId,
    UUID variantId,
    UUID comboId,
    String productName,
    String variantName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal modifiersTotal,
    BigDecimal lineTotal,
    KdsStatus kdsStatus,
    Instant kdsSentAt,
    Instant kdsReadyAt,
    String notes,
    int sortOrder,
    List<OrderItemModifierResponse> modifiers,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Maps an OrderItem entity to a response DTO, including its modifiers.
     *
     * @param item the entity to map
     * @return the response DTO
     */
    public static OrderItemResponse from(OrderItem item) {
        List<OrderItemModifierResponse> modifierResponses = item.getModifiers().stream()
                .map(OrderItemModifierResponse::from)
                .toList();

        return new OrderItemResponse(
            item.getId(),
            item.getTenantId(),
            item.getOrderId(),
            item.getProductId(),
            item.getVariantId(),
            item.getComboId(),
            item.getProductName(),
            item.getVariantName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getModifiersTotal(),
            item.getLineTotal(),
            item.getKdsStatus(),
            item.getKdsSentAt(),
            item.getKdsReadyAt(),
            item.getNotes(),
            item.getSortOrder(),
            modifierResponses,
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
