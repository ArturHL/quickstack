package com.quickstack.pos.service;

import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderItem;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.entity.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OrderCalculationService.
 * No Spring context — pure business logic verification.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCalculationService")
class OrderCalculationServiceTest {

    private OrderCalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new OrderCalculationService();
    }

    // -------------------------------------------------------------------------
    // calculateItemTotal()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("calculateItemTotal()")
    class CalculateItemTotal {

        @Test
        @DisplayName("1. Simple case — qty=2, price=10.00, modifiers=0 → 20.00")
        void simpleItemTotal() {
            OrderItem item = buildItem(2, "10.00", "0.00");

            BigDecimal result = calculationService.calculateItemTotal(item);

            assertThat(result).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("2. With modifiers — qty=1, price=50.00, modifiers=5.00 → 55.00")
        void itemTotalWithModifiers() {
            OrderItem item = buildItem(1, "50.00", "5.00");

            BigDecimal result = calculationService.calculateItemTotal(item);

            assertThat(result).isEqualByComparingTo("55.00");
        }

        @Test
        @DisplayName("3. Fractional result rounds HALF_UP — qty=3, price=10.005 → 30.02")
        void itemTotalRoundsHalfUp() {
            // 3 * (10.005 + 0) = 30.015 → rounds HALF_UP to 30.02
            OrderItem item = buildItem(3, "10.005", "0.00");

            BigDecimal result = calculationService.calculateItemTotal(item);

            assertThat(result).isEqualByComparingTo("30.02");
        }
    }

    // -------------------------------------------------------------------------
    // calculateSubtotal()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("calculateSubtotal()")
    class CalculateSubtotal {

        @Test
        @DisplayName("4. Sums multiple items correctly")
        void sumsMultipleItemsCorrectly() {
            List<OrderItem> items = List.of(
                    buildItem(2, "10.00", "0.00"),  // 20.00
                    buildItem(1, "50.00", "5.00"),   // 55.00
                    buildItem(3, "15.00", "2.00")    // 51.00
            );

            BigDecimal result = calculationService.calculateSubtotal(items);

            assertThat(result).isEqualByComparingTo("126.00");
        }

        @Test
        @DisplayName("5. Returns ZERO for empty list")
        void returnsZeroForEmptyList() {
            BigDecimal result = calculationService.calculateSubtotal(List.of());

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("6. Single item returns its line total")
        void singleItemReturnsItsLineTotal() {
            List<OrderItem> items = List.of(buildItem(1, "99.99", "0.01"));

            BigDecimal result = calculationService.calculateSubtotal(items);

            // 1 * (99.99 + 0.01) = 100.00
            assertThat(result).isEqualByComparingTo("100.00");
        }
    }

    // -------------------------------------------------------------------------
    // calculateTax()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("calculateTax()")
    class CalculateTax {

        @Test
        @DisplayName("7. 16% tax on 100.00 = 16.00")
        void taxAtSixteenPercent() {
            BigDecimal result = calculationService.calculateTax(
                    new BigDecimal("100.00"), new BigDecimal("0.1600"));

            assertThat(result).isEqualByComparingTo("16.00");
        }

        @Test
        @DisplayName("8. Zero tax rate returns 0.00")
        void zeroTaxRateReturnsZero() {
            BigDecimal result = calculationService.calculateTax(
                    new BigDecimal("100.00"), BigDecimal.ZERO);

            assertThat(result).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("9. Fractional tax result rounds correctly — 100.00 * 0.16 = 16.00")
        void fractionalTaxRoundsCorrectly() {
            // 99.99 * 0.16 = 15.9984 → rounds HALF_UP to 16.00
            BigDecimal result = calculationService.calculateTax(
                    new BigDecimal("99.99"), new BigDecimal("0.1600"));

            assertThat(result).isEqualByComparingTo("16.00");
        }
    }

    // -------------------------------------------------------------------------
    // calculateTotal()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("calculateTotal()")
    class CalculateTotal {

        @Test
        @DisplayName("10. total = subtotal + tax - discount")
        void totalEqualsSubtotalPlusTaxMinusDiscount() {
            BigDecimal result = calculationService.calculateTotal(
                    new BigDecimal("100.00"),
                    new BigDecimal("16.00"),
                    new BigDecimal("10.00"));

            assertThat(result).isEqualByComparingTo("106.00");
        }

        @Test
        @DisplayName("11. Discount can make total go negative (validation is caller's responsibility)")
        void discountCanMakeTotalNegative() {
            // OrderCalculationService is a pure math service — it does not validate business rules
            BigDecimal result = calculationService.calculateTotal(
                    new BigDecimal("10.00"),
                    new BigDecimal("1.60"),
                    new BigDecimal("50.00"));

            assertThat(result).isEqualByComparingTo("-38.40");
        }
    }

    // -------------------------------------------------------------------------
    // recalculateOrder()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("recalculateOrder()")
    class RecalculateOrder {

        @Test
        @DisplayName("12. recalculateOrder sets subtotal, tax, and total on the order correctly")
        void recalculateOrderSetsAllTotals() {
            Order order = buildOrderWithItems();

            calculationService.recalculateOrder(order);

            // Items: 2*10.00=20.00, 1*50.00=50.00 → subtotal=70.00
            // tax = 70.00 * 0.16 = 11.20
            // total = 70.00 + 11.20 - 5.00 (discount) = 76.20
            assertThat(order.getSubtotal()).isEqualByComparingTo("70.00");
            assertThat(order.getTax()).isEqualByComparingTo("11.20");
            assertThat(order.getTotal()).isEqualByComparingTo("76.20");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private OrderItem buildItem(int quantity, String unitPrice, String modifiersTotal) {
        OrderItem item = new OrderItem();
        item.setProductName("Test Product");
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setModifiersTotal(new BigDecimal(modifiersTotal));
        return item;
    }

    private Order buildOrderWithItems() {
        UUID tenantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setId(orderId);
        order.setTenantId(tenantId);
        order.setBranchId(UUID.randomUUID());
        order.setOrderNumber("ORD-20260225-001");
        order.setDailySequence(1);
        order.setServiceType(ServiceType.COUNTER);
        order.setStatusId(OrderStatusConstants.PENDING);
        order.setTaxRate(new BigDecimal("0.1600"));
        order.setDiscount(new BigDecimal("5.00"));

        OrderItem item1 = buildItem(2, "10.00", "0.00");
        OrderItem item2 = buildItem(1, "50.00", "0.00");

        order.addItem(item1);
        order.addItem(item2);

        return order;
    }
}
