package com.quickstack.pos.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Payment entity.
 * Verifies field defaults, assignment, and audit invariants.
 */
@DisplayName("Payment entity")
class PaymentTest {

    @Test
    @DisplayName("default status is COMPLETED")
    void defaultStatusIsCompleted() {
        Payment payment = new Payment();
        assertThat(payment.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("fields are correctly set via setters")
    void fieldsSetCorrectly() {
        UUID tenantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        BigDecimal changeGiven = new BigDecimal("0.00");

        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setOrderId(orderId);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setAmount(amount);
        payment.setAmountReceived(amount);
        payment.setChangeGiven(changeGiven);
        payment.setCreatedBy(userId);
        payment.setNotes("Test note");

        assertThat(payment.getTenantId()).isEqualTo(tenantId);
        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment.getAmount()).isEqualByComparingTo("150.00");
        assertThat(payment.getAmountReceived()).isEqualByComparingTo("150.00");
        assertThat(payment.getChangeGiven()).isEqualByComparingTo("0.00");
        assertThat(payment.getCreatedBy()).isEqualTo(userId);
        assertThat(payment.getNotes()).isEqualTo("Test note");
    }

    @Test
    @DisplayName("PaymentMethod enum contains CASH, CARD, TRANSFER, OTHER")
    void paymentMethodEnumValues() {
        assertThat(PaymentMethod.values())
                .contains(PaymentMethod.CASH, PaymentMethod.CARD,
                        PaymentMethod.TRANSFER, PaymentMethod.OTHER);
    }

    @Test
    @DisplayName("change given is correctly calculated externally and stored")
    void changeGivenStored() {
        BigDecimal orderTotal = new BigDecimal("89.00");
        BigDecimal amountReceived = new BigDecimal("100.00");
        BigDecimal expectedChange = amountReceived.subtract(orderTotal);

        Payment payment = new Payment();
        payment.setAmount(amountReceived);
        payment.setAmountReceived(amountReceived);
        payment.setChangeGiven(expectedChange);

        assertThat(payment.getChangeGiven()).isEqualByComparingTo("11.00");
    }

    @Test
    @DisplayName("payment has no deletedAt field — never deleted")
    void paymentHasNoDeletedAtField() {
        // Verifies by checking the class does not have a deletedAt getter
        // (structural test — ensures the domain rule is encoded)
        Payment payment = new Payment();
        assertThat(payment.getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().contains("deleted") || f.getName().contains("Deleted"));
    }
}
