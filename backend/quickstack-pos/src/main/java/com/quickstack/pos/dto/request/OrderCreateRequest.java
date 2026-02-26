package com.quickstack.pos.dto.request;

import com.quickstack.pos.entity.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new order.
 * <p>
 * Service type determines which optional fields become required:
 * - DINE_IN: tableId is mandatory (validated by @AssertTrue)
 * - DELIVERY: customerId is mandatory (validated by @AssertTrue)
 * - COUNTER / TAKEOUT: neither tableId nor customerId is required
 * <p>
 * Business Rules:
 * - branchId must belong to the tenant (validated at service layer)
 * - tableId must belong to branchId via its area (validated at service layer)
 * - customerId must belong to the tenant (validated at service layer)
 * - Orders must have at least one item
 * <p>
 * ASVS Compliance:
 * - tenantId is NEVER accepted from the request body — always extracted from JWT
 */
public record OrderCreateRequest(

    @NotNull(message = "Branch ID is required")
    UUID branchId,

    @NotNull(message = "Service type is required")
    ServiceType serviceType,

    // Required for DINE_IN — cross-field validation below
    UUID tableId,

    // Required for DELIVERY — cross-field validation below
    UUID customerId,

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    List<OrderItemRequest> items,

    String notes,

    String kitchenNotes
) {

    /**
     * Cross-field validation: tableId is required when serviceType is DINE_IN.
     */
    @AssertTrue(message = "tableId is required for DINE_IN service type")
    public boolean isTableIdValidForDineIn() {
        return serviceType != ServiceType.DINE_IN || tableId != null;
    }

    /**
     * Cross-field validation: customerId is required when serviceType is DELIVERY.
     */
    @AssertTrue(message = "customerId is required for DELIVERY service type")
    public boolean isCustomerIdValidForDelivery() {
        return serviceType != ServiceType.DELIVERY || customerId != null;
    }
}
