package com.quickstack.pos.dto.response;

import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderSource;
import com.quickstack.pos.entity.ServiceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for an order, including all financial data and line items.
 * <p>
 * All price fields are denormalized snapshots from the time of order creation,
 * ensuring historical accuracy even when catalog prices change later.
 */
public record OrderResponse(
    UUID id,
    UUID tenantId,
    UUID branchId,
    UUID tableId,
    UUID customerId,
    String orderNumber,
    int dailySequence,
    ServiceType serviceType,
    UUID statusId,
    BigDecimal subtotal,
    BigDecimal taxRate,
    BigDecimal tax,
    BigDecimal discount,
    BigDecimal total,
    OrderSource source,
    String notes,
    String kitchenNotes,
    Instant openedAt,
    Instant closedAt,
    UUID createdBy,
    UUID updatedBy,
    Instant createdAt,
    Instant updatedAt,
    List<OrderItemResponse> items
) {

    /**
     * Maps an Order entity to a response DTO, including all its items and their modifiers.
     *
     * @param order the entity to map
     * @return the response DTO
     */
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
            order.getId(),
            order.getTenantId(),
            order.getBranchId(),
            order.getTableId(),
            order.getCustomerId(),
            order.getOrderNumber(),
            order.getDailySequence(),
            order.getServiceType(),
            order.getStatusId(),
            order.getSubtotal(),
            order.getTaxRate(),
            order.getTax(),
            order.getDiscount(),
            order.getTotal(),
            order.getSource(),
            order.getNotes(),
            order.getKitchenNotes(),
            order.getOpenedAt(),
            order.getClosedAt(),
            order.getCreatedBy(),
            order.getUpdatedBy(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            itemResponses
        );
    }
}
