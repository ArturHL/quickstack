package com.quickstack.pos.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Customer entity business logic.
 */
@DisplayName("Customer entity")
class CustomerEntityTest {

    @Test
    @DisplayName("1. New customer has correct default values")
    void newCustomerHasDefaultValues() {
        Customer customer = new Customer();

        assertThat(customer.getTotalOrders()).isEqualTo(0);
        assertThat(customer.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(customer.getPreferences()).isEqualTo("{}");
        assertThat(customer.isDeleted()).isFalse();
        assertThat(customer.getDeletedAt()).isNull();
        assertThat(customer.getDeletedBy()).isNull();
        assertThat(customer.getLastOrderAt()).isNull();
    }

    @Test
    @DisplayName("2. softDelete sets deletedAt and deletedBy")
    void softDeleteSetsFieldsCorrectly() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        UUID deletedBy = UUID.randomUUID();

        customer.softDelete(deletedBy);

        assertThat(customer.isDeleted()).isTrue();
        assertThat(customer.getDeletedAt()).isNotNull();
        assertThat(customer.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("3. isDeleted returns true when deletedAt is set")
    void isDeletedReturnsTrueWhenDeletedAtIsSet() {
        Customer customer = new Customer();
        assertThat(customer.isDeleted()).isFalse();

        customer.softDelete(UUID.randomUUID());

        assertThat(customer.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("4. incrementOrderStats accumulates totals correctly across multiple calls")
    void incrementOrderStatsAccumulatesCorrectly() {
        Customer customer = new Customer();

        customer.incrementOrderStats(new BigDecimal("150.00"));
        customer.incrementOrderStats(new BigDecimal("75.50"));

        assertThat(customer.getTotalOrders()).isEqualTo(2);
        assertThat(customer.getTotalSpent()).isEqualByComparingTo(new BigDecimal("225.50"));
    }

    @Test
    @DisplayName("5. incrementOrderStats sets lastOrderAt to a non-null instant")
    void incrementOrderStatsSetsLastOrderAt() {
        Customer customer = new Customer();
        assertThat(customer.getLastOrderAt()).isNull();

        customer.incrementOrderStats(new BigDecimal("100.00"));

        assertThat(customer.getLastOrderAt()).isNotNull();
    }

    @Test
    @DisplayName("6. All contact and address fields are nullable by default")
    void allContactAndAddressFieldsAreNullableByDefault() {
        Customer customer = new Customer();

        assertThat(customer.getName()).isNull();
        assertThat(customer.getPhone()).isNull();
        assertThat(customer.getEmail()).isNull();
        assertThat(customer.getWhatsapp()).isNull();
        assertThat(customer.getAddressLine1()).isNull();
        assertThat(customer.getAddressLine2()).isNull();
        assertThat(customer.getCity()).isNull();
        assertThat(customer.getPostalCode()).isNull();
        assertThat(customer.getDeliveryNotes()).isNull();
    }
}
