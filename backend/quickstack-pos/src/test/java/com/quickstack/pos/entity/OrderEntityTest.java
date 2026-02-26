package com.quickstack.pos.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Order, OrderItem, and OrderItemModifier entity business logic.
 * No Spring context — pure Java.
 */
@DisplayName("Order entity")
class OrderEntityTest {

    // -------------------------------------------------------------------------
    // Order default values
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("1. New order has correct default values")
        void newOrderHasCorrectDefaults() {
            Order order = new Order();

            assertThat(order.getStatusId()).isEqualTo(OrderStatusConstants.PENDING);
            assertThat(order.getSource()).isEqualTo(OrderSource.POS);
            assertThat(order.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getTax()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getItems()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("12. Order source enum defaults to POS")
        void orderSourceDefaultsToPOS() {
            Order order = new Order();

            assertThat(order.getSource()).isEqualTo(OrderSource.POS);
        }
    }

    // -------------------------------------------------------------------------
    // isTerminal()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isTerminal()")
    class IsTerminal {

        @Test
        @DisplayName("2. isTerminal() returns false for PENDING status")
        void isTerminalReturnsFalseForPending() {
            Order order = new Order();
            order.setStatusId(OrderStatusConstants.PENDING);

            assertThat(order.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("3. isTerminal() returns true for COMPLETED status")
        void isTerminalReturnsTrueForCompleted() {
            Order order = new Order();
            order.setStatusId(OrderStatusConstants.COMPLETED);

            assertThat(order.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("4. isTerminal() returns true for CANCELLED status")
        void isTerminalReturnsTrueForCancelled() {
            Order order = new Order();
            order.setStatusId(OrderStatusConstants.CANCELLED);

            assertThat(order.isTerminal()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // isModifiable()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isModifiable()")
    class IsModifiable {

        @Test
        @DisplayName("5. isModifiable() returns true for PENDING status")
        void isModifiableReturnsTrueForPending() {
            Order order = new Order();
            order.setStatusId(OrderStatusConstants.PENDING);

            assertThat(order.isModifiable()).isTrue();
        }

        @Test
        @DisplayName("6. isModifiable() returns false for IN_PROGRESS status")
        void isModifiableReturnsFalseForInProgress() {
            Order order = new Order();
            order.setStatusId(OrderStatusConstants.IN_PROGRESS);

            assertThat(order.isModifiable()).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // addItem()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addItem()")
    class AddItem {

        @Test
        @DisplayName("7. addItem() sets orderId and tenantId on the item and adds to list")
        void addItemWiresBidirectionalRelationship() {
            UUID tenantId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();

            Order order = new Order();
            order.setId(orderId);
            order.setTenantId(tenantId);

            OrderItem item = new OrderItem();
            item.setProductName("Tacos");
            item.setUnitPrice(new BigDecimal("50.00"));

            order.addItem(item);

            assertThat(order.getItems()).hasSize(1);
            assertThat(item.getOrderId()).isEqualTo(orderId);
            assertThat(item.getTenantId()).isEqualTo(tenantId);
            assertThat(item.getOrder()).isSameAs(order);
        }
    }

    // -------------------------------------------------------------------------
    // OrderItem defaults
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("OrderItem defaults")
    class OrderItemDefaults {

        @Test
        @DisplayName("8. New OrderItem has correct default values")
        void newOrderItemHasCorrectDefaults() {
            OrderItem item = new OrderItem();

            assertThat(item.getQuantity()).isEqualTo(1);
            assertThat(item.getKdsStatus()).isEqualTo(KdsStatus.PENDING);
            assertThat(item.getModifiersTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.getModifiers()).isNotNull().isEmpty();
            assertThat(item.getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("9. OrderItem lineTotal field exists and is null when not persisted")
        void orderItemLineTotalIsNullBeforePersistence() {
            OrderItem item = new OrderItem();

            // lineTotal is a DB generated column — Java side is null until read from DB
            assertThat(item.getLineTotal()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // OrderItemModifier defaults
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("OrderItemModifier defaults")
    class OrderItemModifierDefaults {

        @Test
        @DisplayName("10. New OrderItemModifier has correct default values")
        void newOrderItemModifierHasCorrectDefaults() {
            OrderItemModifier modifier = new OrderItemModifier();

            assertThat(modifier.getQuantity()).isEqualTo(1);
            assertThat(modifier.getPriceAdjustment()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // -------------------------------------------------------------------------
    // Enum roundtrip
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Enum handling")
    class EnumHandling {

        @Test
        @DisplayName("11. Order serviceType enum stores and retrieves correctly")
        void orderServiceTypeEnumRoundtrip() {
            Order order = new Order();
            order.setServiceType(ServiceType.DINE_IN);

            assertThat(order.getServiceType()).isEqualTo(ServiceType.DINE_IN);

            order.setServiceType(ServiceType.DELIVERY);
            assertThat(order.getServiceType()).isEqualTo(ServiceType.DELIVERY);
        }
    }
}
