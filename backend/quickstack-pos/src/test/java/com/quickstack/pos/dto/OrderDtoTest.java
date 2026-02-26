package com.quickstack.pos.dto;

import com.quickstack.pos.dto.request.OrderCreateRequest;
import com.quickstack.pos.dto.request.OrderItemModifierRequest;
import com.quickstack.pos.dto.request.OrderItemRequest;
import com.quickstack.pos.dto.response.OrderItemModifierResponse;
import com.quickstack.pos.dto.response.OrderItemResponse;
import com.quickstack.pos.dto.response.OrderResponse;
import com.quickstack.pos.entity.KdsStatus;
import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderItem;
import com.quickstack.pos.entity.OrderItemModifier;
import com.quickstack.pos.entity.OrderSource;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.entity.ServiceType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Order request and response DTOs.
 * <p>
 * Validates Bean Validation constraints and cross-field (@AssertTrue) validations.
 * Also verifies that factory methods map all entity fields correctly.
 */
@DisplayName("Order DTOs")
class OrderDtoTest {

    private static Validator validator;

    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID TABLE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // =========================================================================
    // OrderItemModifierRequest
    // =========================================================================

    @Nested
    @DisplayName("OrderItemModifierRequest")
    class OrderItemModifierRequestTests {

        @Test
        @DisplayName("1. Valid modifier request passes validation")
        void validModifierRequestPassesValidation() {
            OrderItemModifierRequest request = new OrderItemModifierRequest(
                    UUID.randomUUID(), "Extra Queso", new BigDecimal("10.00"), 1);

            Set<ConstraintViolation<OrderItemModifierRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("2. Blank modifier name fails validation")
        void blankModifierNameFailsValidation() {
            OrderItemModifierRequest request = new OrderItemModifierRequest(
                    null, "", new BigDecimal("5.00"), 1);

            Set<ConstraintViolation<OrderItemModifierRequest>> violations = validator.validate(request);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("modifierName"));
        }

        @Test
        @DisplayName("3. Null price adjustment fails validation")
        void nullPriceAdjustmentFailsValidation() {
            OrderItemModifierRequest request = new OrderItemModifierRequest(
                    null, "Sin Cebolla", null, 1);

            Set<ConstraintViolation<OrderItemModifierRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("priceAdjustment"));
        }
    }

    // =========================================================================
    // OrderItemRequest
    // =========================================================================

    @Nested
    @DisplayName("OrderItemRequest")
    class OrderItemRequestTests {

        @Test
        @DisplayName("4. Valid item with productId passes validation")
        void validItemWithProductIdPassesValidation() {
            OrderItemRequest request = new OrderItemRequest(
                    PRODUCT_ID, null, null,
                    "Hamburguesa", null,
                    2, new BigDecimal("89.00"),
                    List.of(), null);

            Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("5. Neither productId nor comboId fails cross-field validation")
        void neitherProductIdNorComboIdFails() {
            OrderItemRequest request = new OrderItemRequest(
                    null, null, null,
                    "Hamburguesa", null,
                    1, new BigDecimal("89.00"),
                    List.of(), null);

            Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("productOrComboPresent"));
        }

        @Test
        @DisplayName("6. Both productId and comboId present fails cross-field validation")
        void bothProductIdAndComboIdFail() {
            UUID comboId = UUID.randomUUID();
            OrderItemRequest request = new OrderItemRequest(
                    PRODUCT_ID, null, comboId,
                    "Combo Burger", null,
                    1, new BigDecimal("99.00"),
                    List.of(), null);

            Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("productOrComboPresent"));
        }

        @Test
        @DisplayName("7. Valid item with comboId passes validation")
        void validItemWithComboIdPassesValidation() {
            UUID comboId = UUID.randomUUID();
            OrderItemRequest request = new OrderItemRequest(
                    null, null, comboId,
                    "Combo 1", null,
                    1, new BigDecimal("99.00"),
                    List.of(), null);

            Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("8. Quantity less than 1 fails validation")
        void quantityLessThanOneFails() {
            OrderItemRequest request = new OrderItemRequest(
                    PRODUCT_ID, null, null,
                    "Hamburguesa", null,
                    0, new BigDecimal("89.00"),
                    List.of(), null);

            Set<ConstraintViolation<OrderItemRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("quantity"));
        }
    }

    // =========================================================================
    // OrderCreateRequest
    // =========================================================================

    @Nested
    @DisplayName("OrderCreateRequest")
    class OrderCreateRequestTests {

        private List<OrderItemRequest> oneItem() {
            return List.of(new OrderItemRequest(
                    PRODUCT_ID, null, null,
                    "Hamburguesa", null,
                    1, new BigDecimal("89.00"),
                    List.of(), null));
        }

        @Test
        @DisplayName("9. COUNTER order without tableId passes validation")
        void counterOrderWithoutTableIdPassesValidation() {
            OrderCreateRequest request = new OrderCreateRequest(
                    BRANCH_ID, ServiceType.COUNTER,
                    null, null,
                    oneItem(), null, null);

            Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("10. DINE_IN without tableId fails cross-field validation")
        void dineInWithoutTableIdFails() {
            OrderCreateRequest request = new OrderCreateRequest(
                    BRANCH_ID, ServiceType.DINE_IN,
                    null, null,
                    oneItem(), null, null);

            Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("tableIdValidForDineIn"));
        }

        @Test
        @DisplayName("11. DINE_IN with tableId passes validation")
        void dineInWithTableIdPassesValidation() {
            OrderCreateRequest request = new OrderCreateRequest(
                    BRANCH_ID, ServiceType.DINE_IN,
                    TABLE_ID, null,
                    oneItem(), null, null);

            Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("12. DELIVERY without customerId fails cross-field validation")
        void deliveryWithoutCustomerIdFails() {
            OrderCreateRequest request = new OrderCreateRequest(
                    BRANCH_ID, ServiceType.DELIVERY,
                    null, null,
                    oneItem(), null, null);

            Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v ->
                    v.getPropertyPath().toString().equals("customerIdValidForDelivery"));
        }

        @Test
        @DisplayName("13. DELIVERY with customerId passes validation")
        void deliveryWithCustomerIdPassesValidation() {
            OrderCreateRequest request = new OrderCreateRequest(
                    BRANCH_ID, ServiceType.DELIVERY,
                    null, CUSTOMER_ID,
                    oneItem(), null, null);

            Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("14. Empty items list fails validation")
        void emptyItemsListFailsValidation() {
            OrderCreateRequest request = new OrderCreateRequest(
                    BRANCH_ID, ServiceType.COUNTER,
                    null, null,
                    List.of(), null, null);

            Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("items"));
        }
    }

    // =========================================================================
    // Response factory methods
    // =========================================================================

    @Test
    @DisplayName("15. OrderItemModifierResponse.from maps all entity fields")
    void orderItemModifierResponseFromMapsAllFields() {
        OrderItemModifier modifier = buildModifier();

        OrderItemModifierResponse response = OrderItemModifierResponse.from(modifier);

        assertThat(response.id()).isEqualTo(modifier.getId());
        assertThat(response.modifierName()).isEqualTo("Extra Queso");
        assertThat(response.priceAdjustment()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(response.quantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("16. OrderResponse.from includes items list")
    void orderResponseFromIncludesItems() {
        Order order = buildOrder();
        OrderItem item = buildItem(order.getId(), order.getTenantId());
        order.addItem(item);

        OrderResponse response = OrderResponse.from(order);

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.orderNumber()).isEqualTo("ORD-20260225-001");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Hamburguesa");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private OrderItemModifier buildModifier() {
        OrderItemModifier modifier = new OrderItemModifier();
        modifier.setId(UUID.randomUUID());
        modifier.setTenantId(UUID.randomUUID());
        modifier.setOrderItemId(UUID.randomUUID());
        modifier.setModifierId(UUID.randomUUID());
        modifier.setModifierName("Extra Queso");
        modifier.setPriceAdjustment(new BigDecimal("10.00"));
        modifier.setQuantity(2);
        return modifier;
    }

    private Order buildOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setTenantId(UUID.randomUUID());
        order.setBranchId(UUID.randomUUID());
        order.setOrderNumber("ORD-20260225-001");
        order.setDailySequence(1);
        order.setServiceType(ServiceType.COUNTER);
        order.setStatusId(OrderStatusConstants.PENDING);
        order.setSubtotal(BigDecimal.ZERO);
        order.setTaxRate(new BigDecimal("0.16"));
        order.setTax(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);
        order.setSource(OrderSource.POS);
        order.setOpenedAt(Instant.now());
        return order;
    }

    private OrderItem buildItem(UUID orderId, UUID tenantId) {
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrderId(orderId);
        item.setTenantId(tenantId);
        item.setProductId(PRODUCT_ID);
        item.setProductName("Hamburguesa");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("89.00"));
        item.setModifiersTotal(BigDecimal.ZERO);
        item.setKdsStatus(KdsStatus.PENDING);
        return item;
    }
}
