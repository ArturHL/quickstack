package com.quickstack.pos.dto.response;

import com.quickstack.pos.entity.Payment;
import com.quickstack.pos.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a payment record.
 * All fields are snapshots from the time of payment registration.
 */
public record PaymentResponse(
    UUID id,
    UUID tenantId,
    UUID orderId,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    BigDecimal amountReceived,
    BigDecimal changeGiven,
    String status,
    String referenceNumber,
    String notes,
    Instant createdAt,
    UUID createdBy
) {

    /**
     * Maps a Payment entity to a response DTO.
     *
     * @param payment the entity to map
     * @return the response DTO
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getTenantId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getPaymentMethod(),
            payment.getAmountReceived(),
            payment.getChangeGiven(),
            payment.getStatus(),
            payment.getReferenceNumber(),
            payment.getNotes(),
            payment.getCreatedAt(),
            payment.getCreatedBy()
        );
    }
}
