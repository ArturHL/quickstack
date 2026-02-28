package com.quickstack.pos.dto.request;

import com.quickstack.pos.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for registering a payment against an order.
 * <p>
 * MVP Phase 1: only CASH payments are supported.
 * The amount represents the cash received from the customer.
 * Change is calculated automatically as: amount - order.total.
 */
public record PaymentRequest(

    @NotNull
    UUID orderId,

    @NotNull
    PaymentMethod paymentMethod,

    @NotNull
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    BigDecimal amount,

    String notes

) {}
