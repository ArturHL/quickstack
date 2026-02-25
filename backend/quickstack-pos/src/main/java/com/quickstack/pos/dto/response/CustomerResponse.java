package com.quickstack.pos.dto.response;

import com.quickstack.pos.entity.Customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a customer, including contact info, address, stats, and audit metadata.
 */
public record CustomerResponse(
    UUID id,
    UUID tenantId,
    String name,
    String phone,
    String email,
    String whatsapp,
    String addressLine1,
    String addressLine2,
    String city,
    String postalCode,
    String deliveryNotes,
    int totalOrders,
    BigDecimal totalSpent,
    Instant lastOrderAt,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Maps a Customer entity to a CustomerResponse.
     *
     * @param customer the entity to map
     * @return the response DTO
     */
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getTenantId(),
            customer.getName(),
            customer.getPhone(),
            customer.getEmail(),
            customer.getWhatsapp(),
            customer.getAddressLine1(),
            customer.getAddressLine2(),
            customer.getCity(),
            customer.getPostalCode(),
            customer.getDeliveryNotes(),
            customer.getTotalOrders(),
            customer.getTotalSpent(),
            customer.getLastOrderAt(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
