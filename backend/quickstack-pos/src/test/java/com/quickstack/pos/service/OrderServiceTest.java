package com.quickstack.pos.service;

import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;
import com.quickstack.branch.entity.Branch;
import com.quickstack.branch.repository.BranchRepository;
import com.quickstack.branch.repository.TableRepository;
import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.pos.dto.request.OrderCreateRequest;
import com.quickstack.pos.dto.request.OrderItemModifierRequest;
import com.quickstack.pos.dto.request.OrderItemRequest;
import com.quickstack.pos.dto.response.OrderResponse;
import com.quickstack.pos.entity.Customer;
import com.quickstack.pos.entity.KdsStatus;
import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderItem;
import com.quickstack.pos.entity.OrderSource;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.entity.ServiceType;
import com.quickstack.pos.repository.CustomerRepository;
import com.quickstack.pos.repository.OrderRepository;
import com.quickstack.product.entity.Combo;
import com.quickstack.product.entity.Product;
import com.quickstack.product.repository.ComboRepository;
import com.quickstack.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService using Mockito.
 * All external dependencies are mocked — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

        @Mock
        private OrderRepository orderRepository;
        @Mock
        private OrderCalculationService orderCalculationService;
        @Mock
        private BranchRepository branchRepository;
        @Mock
        private TableRepository tableRepository;
        @Mock
        private CustomerRepository customerRepository;
        @Mock
        private ProductRepository productRepository;
        @Mock
        private ComboRepository comboRepository;
        @Mock
        private JdbcTemplate jdbcTemplate;
        @Mock
        private EntityManager entityManager;

        private OrderService orderService;

        private static final UUID TENANT_ID = UUID.randomUUID();
        private static final UUID USER_ID = UUID.randomUUID();
        private static final UUID BRANCH_ID = UUID.randomUUID();
        private static final UUID TABLE_ID = UUID.randomUUID();
        private static final UUID CUSTOMER_ID = UUID.randomUUID();
        private static final UUID PRODUCT_ID = UUID.randomUUID();
        private static final UUID ORDER_ID = UUID.randomUUID();
        private static final UUID ITEM_ID = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                orderService = new OrderService(
                                orderRepository, orderCalculationService,
                                branchRepository, tableRepository, customerRepository,
                                productRepository, comboRepository, jdbcTemplate, entityManager);
        }

        // =========================================================================
        // createOrder
        // =========================================================================

        @Nested
        @DisplayName("createOrder")
        class CreateOrderTests {

                @Test
                @DisplayName("1. Happy path COUNTER order — creates order with correct number format")
                void happyPathCounter() {
                        setUpCounterRequest();
                        Order saved = buildPersistedOrder(ServiceType.COUNTER, null);
                        when(orderRepository.save(any())).thenReturn(saved);

                        OrderResponse response = orderService.createOrder(TENANT_ID, USER_ID, counterRequest());

                        assertThat(response.orderNumber()).matches("ORD-\\d{8}-\\d{3}");
                        assertThat(response.serviceType()).isEqualTo(ServiceType.COUNTER);
                        verify(orderRepository, times(2)).save(any());
                }

                @Test
                @DisplayName("2. Happy path DINE_IN — marks table as OCCUPIED")
                void happyPathDineIn() {
                        setUpDineInRequest();
                        Order saved = buildPersistedOrder(ServiceType.DINE_IN, TABLE_ID);
                        when(orderRepository.save(any())).thenReturn(saved);

                        OrderResponse response = orderService.createOrder(TENANT_ID, USER_ID, dineInRequest());

                        ArgumentCaptor<RestaurantTable> tableCaptor = ArgumentCaptor.forClass(RestaurantTable.class);
                        verify(tableRepository).save(tableCaptor.capture());
                        assertThat(tableCaptor.getValue().getStatus()).isEqualTo(TableStatus.OCCUPIED);
                }

                @Test
                @DisplayName("3. Happy path DELIVERY — validates customer")
                void happyPathDelivery() {
                        setUpDeliveryRequest();
                        Order saved = buildPersistedOrder(ServiceType.DELIVERY, null);
                        when(orderRepository.save(any())).thenReturn(saved);

                        OrderResponse response = orderService.createOrder(TENANT_ID, USER_ID, deliveryRequest());

                        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
                        verify(customerRepository).findByIdAndTenantId(CUSTOMER_ID, TENANT_ID);
                }

                @Test
                @DisplayName("4. Branch not found throws ResourceNotFoundException")
                void branchNotFoundThrows() {
                        when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, USER_ID, counterRequest()))
                                        .isInstanceOf(ResourceNotFoundException.class);

                        verify(orderRepository, never()).save(any());
                }

                @Test
                @DisplayName("5. Table not available throws BusinessRuleException")
                void tableNotAvailableThrows() {
                        branchFound();
                        RestaurantTable occupiedTable = buildTable(TableStatus.OCCUPIED);
                        when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID))
                                        .thenReturn(Optional.of(occupiedTable));
                        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class),
                                        eq(TABLE_ID), eq(BRANCH_ID), eq(TENANT_ID)))
                                        .thenReturn(1);

                        assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, USER_ID, dineInRequest()))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("TABLE_NOT_AVAILABLE"));
                }

                @Test
                @DisplayName("6. Product not active throws BusinessRuleException")
                void productNotActiveThrows() {
                        setUpCounterRequestWithUnavailableProduct();

                        assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, USER_ID, counterRequest()))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("PRODUCT_NOT_AVAILABLE"));
                }

                @Test
                @DisplayName("7. Order number matches ORD-YYYYMMDD-NNN format")
                void orderNumberFormat() {
                        setUpCounterRequest();
                        Order saved = buildPersistedOrder(ServiceType.COUNTER, null);
                        when(orderRepository.save(any())).thenReturn(saved);

                        OrderResponse response = orderService.createOrder(TENANT_ID, USER_ID, counterRequest());

                        assertThat(response.orderNumber()).matches("ORD-\\d{8}-\\d+");
                }

                @Test
                @DisplayName("8. Status history inserted for new order")
                void statusHistoryInserted() {
                        setUpCounterRequest();
                        Order saved = buildPersistedOrder(ServiceType.COUNTER, null);
                        when(orderRepository.save(any())).thenReturn(saved);

                        orderService.createOrder(TENANT_ID, USER_ID, counterRequest());

                        verify(jdbcTemplate).update(
                                        contains("order_status_history"),
                                        eq(TENANT_ID), eq(saved.getId()),
                                        eq(OrderStatusConstants.PENDING), eq(USER_ID));
                }

                @Test
                @DisplayName("9. Modifiers total is computed from price adjustment x quantity")
                void modifiersTotalComputed() {
                        // Setup with a product that has a modifier
                        branchFound();
                        Product product = buildActiveProduct();
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.of(product));
                        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(TENANT_ID)))
                                        .thenReturn(new BigDecimal("0.16"));
                        when(orderRepository.getNextDailySequence(any(), any(), any())).thenReturn(1);

                        Order captured = new Order();
                        captured.setId(ORDER_ID);
                        captured.setTenantId(TENANT_ID);
                        captured.setBranchId(BRANCH_ID);
                        captured.setOrderNumber("ORD-20260225-001");
                        captured.setDailySequence(1);
                        captured.setServiceType(ServiceType.COUNTER);
                        captured.setStatusId(OrderStatusConstants.PENDING);
                        captured.setSubtotal(BigDecimal.ZERO);
                        captured.setTaxRate(new BigDecimal("0.16"));
                        captured.setTax(BigDecimal.ZERO);
                        captured.setDiscount(BigDecimal.ZERO);
                        captured.setTotal(BigDecimal.ZERO);
                        captured.setSource(OrderSource.POS);
                        captured.setOpenedAt(Instant.now());

                        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                                Order o = inv.getArgument(0);
                                if (o.getId() == null) {
                                        o.setId(ORDER_ID);
                                }
                                return o;
                        });

                        OrderItemModifierRequest modReq = new OrderItemModifierRequest(
                                        null, "Extra Queso", new BigDecimal("10.00"), 2);
                        OrderItemRequest itemReq = new OrderItemRequest(
                                        PRODUCT_ID, null, null, "Hamburguesa", null,
                                        1, new BigDecimal("89.00"), List.of(modReq), null);
                        OrderCreateRequest request = new OrderCreateRequest(
                                        BRANCH_ID, ServiceType.COUNTER, null, null, List.of(itemReq), null, null);

                        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
                        orderService.createOrder(TENANT_ID, USER_ID, request);

                        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());
                        Order savedOrder = orderCaptor.getAllValues().get(0);
                        assertThat(savedOrder.getItems()).hasSize(1);
                        // modifiersTotal = priceAdjustment(10.00) * qty(2) = 20.00
                        assertThat(savedOrder.getItems().get(0).getModifiersTotal())
                                        .isEqualByComparingTo(new BigDecimal("20.00"));
                }

                // ---- Helper setups ----

                private void setUpCounterRequest() {
                        branchFound();
                        productFound();
                        taxRateFound();
                        sequenceFound(1);
                }

                private void setUpDineInRequest() {
                        branchFound();
                        RestaurantTable table = buildTable(TableStatus.AVAILABLE);
                        when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID))
                                        .thenReturn(Optional.of(table));
                        when(jdbcTemplate.queryForObject(
                                        contains("FROM tables t JOIN areas"),
                                        eq(Integer.class), eq(TABLE_ID), eq(BRANCH_ID), eq(TENANT_ID)))
                                        .thenReturn(1);
                        productFound();
                        taxRateFound();
                        sequenceFound(1);
                }

                private void setUpDeliveryRequest() {
                        branchFound();
                        Customer customer = new Customer();
                        customer.setId(CUSTOMER_ID);
                        when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(customer));
                        productFound();
                        taxRateFound();
                        sequenceFound(1);
                }

                private void setUpCounterRequestWithUnavailableProduct() {
                        branchFound();
                        Product product = buildActiveProduct();
                        product.setAvailable(false);
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.of(product));
                }
        }

        // =========================================================================
        // getOrder
        // =========================================================================

        @Nested
        @DisplayName("getOrder")
        class GetOrderTests {

                @Test
                @DisplayName("10. Manager can retrieve any order in the tenant")
                void managerCanGetAnyOrder() {
                        UUID anotherUser = UUID.randomUUID();
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setCreatedBy(anotherUser);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        // isManager=true — should not throw
                        OrderResponse response = orderService.getOrder(TENANT_ID, USER_ID, true, ORDER_ID);

                        assertThat(response.id()).isEqualTo(ORDER_ID);
                }

                @Test
                @DisplayName("11. Cashier can retrieve own order")
                void cashierCanGetOwnOrder() {
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setCreatedBy(USER_ID);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        OrderResponse response = orderService.getOrder(TENANT_ID, USER_ID, false, ORDER_ID);

                        assertThat(response.id()).isEqualTo(ORDER_ID);
                }

                @Test
                @DisplayName("12. Cashier cannot get order created by another user — 404 (IDOR)")
                void cashierCannotGetOtherUserOrder() {
                        UUID anotherUser = UUID.randomUUID();
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setCreatedBy(anotherUser);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        assertThatThrownBy(() -> orderService.getOrder(TENANT_ID, USER_ID, false, ORDER_ID))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }

                @Test
                @DisplayName("13. Order not found throws ResourceNotFoundException")
                void orderNotFoundThrows() {
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> orderService.getOrder(TENANT_ID, USER_ID, true, ORDER_ID))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        // =========================================================================
        // listOrders
        // =========================================================================

        @Nested
        @DisplayName("listOrders")
        class ListOrdersTests {

                @Test
                @DisplayName("14. Manager sees all orders — createdBy filter is null")
                void managerSeesAllOrders() {
                        Page<Order> page = new PageImpl<>(List.of(buildOrder(ServiceType.COUNTER, null)));
                        when(orderRepository.findAllWithFilters(eq(TENANT_ID), any(), any(), eq(null), any()))
                                        .thenReturn(page);

                        Page<OrderResponse> result = orderService.listOrders(
                                        TENANT_ID, USER_ID, true, null, null, PageRequest.of(0, 10));

                        assertThat(result.getTotalElements()).isEqualTo(1);
                        verify(orderRepository).findAllWithFilters(eq(TENANT_ID), isNull(), isNull(), isNull(), any());
                }

                @Test
                @DisplayName("15. Cashier only sees own orders — createdBy filter is userId")
                void cashierSeesOwnOrders() {
                        Page<Order> page = new PageImpl<>(List.of());
                        when(orderRepository.findAllWithFilters(eq(TENANT_ID), any(), any(), eq(USER_ID), any()))
                                        .thenReturn(page);

                        orderService.listOrders(TENANT_ID, USER_ID, false, null, null, PageRequest.of(0, 10));

                        verify(orderRepository).findAllWithFilters(eq(TENANT_ID), isNull(), isNull(), eq(USER_ID),
                                        any());
                }
        }

        // =========================================================================
        // addItemToOrder
        // =========================================================================

        @Nested
        @DisplayName("addItemToOrder")
        class AddItemTests {

                @Test
                @DisplayName("16. Adds item to PENDING order successfully")
                void addItemSuccessfully() {
                        Order order = buildPendingOrder();
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));
                        productFound();
                        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        OrderResponse response = orderService.addItemToOrder(
                                        TENANT_ID, USER_ID, ORDER_ID, oneItemRequest());

                        assertThat(response.items()).hasSize(1);
                        verify(orderCalculationService).recalculateOrder(order);
                }

                @Test
                @DisplayName("17. Adding item to non-PENDING order throws BusinessRuleException")
                void addItemToNonPendingThrows() {
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setStatusId(OrderStatusConstants.IN_PROGRESS);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        assertThatThrownBy(() -> orderService.addItemToOrder(
                                        TENANT_ID, USER_ID, ORDER_ID, oneItemRequest()))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("ORDER_NOT_MODIFIABLE"));
                }

                @Test
                @DisplayName("18. Adding unavailable product throws BusinessRuleException")
                void addUnavailableProductThrows() {
                        Order order = buildPendingOrder();
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));
                        Product product = buildActiveProduct();
                        product.setAvailable(false);
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.of(product));

                        assertThatThrownBy(() -> orderService.addItemToOrder(
                                        TENANT_ID, USER_ID, ORDER_ID, oneItemRequest()))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("PRODUCT_NOT_AVAILABLE"));
                }
        }

        // =========================================================================
        // removeItemFromOrder
        // =========================================================================

        @Nested
        @DisplayName("removeItemFromOrder")
        class RemoveItemTests {

                @Test
                @DisplayName("19. Removes item from PENDING order successfully")
                void removeItemSuccessfully() {
                        Order order = buildPendingOrder();
                        OrderItem item = buildItem(order);
                        item.setId(ITEM_ID);
                        order.addItem(item);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));
                        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        OrderResponse response = orderService.removeItemFromOrder(
                                        TENANT_ID, USER_ID, ORDER_ID, ITEM_ID);

                        assertThat(response.items()).isEmpty();
                        verify(orderCalculationService).recalculateOrder(order);
                }

                @Test
                @DisplayName("20. Removing item from non-PENDING order throws BusinessRuleException")
                void removeFromNonPendingThrows() {
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setStatusId(OrderStatusConstants.IN_PROGRESS);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        assertThatThrownBy(() -> orderService.removeItemFromOrder(
                                        TENANT_ID, USER_ID, ORDER_ID, ITEM_ID))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("ORDER_NOT_MODIFIABLE"));
                }

                @Test
                @DisplayName("21. Removing non-existent item throws ResourceNotFoundException")
                void removeNonExistentItemThrows() {
                        Order order = buildPendingOrder();
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        UUID nonExistentId = UUID.randomUUID();
                        assertThatThrownBy(() -> orderService.removeItemFromOrder(
                                        TENANT_ID, USER_ID, ORDER_ID, nonExistentId))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        // =========================================================================
        // submitOrder
        // =========================================================================

        @Nested
        @DisplayName("submitOrder")
        class SubmitOrderTests {

                @Test
                @DisplayName("22. Submitting PENDING order transitions to IN_PROGRESS and sets kdsSentAt")
                void submitSetsInProgressAndKdsSentAt() {
                        Order order = buildPendingOrder();
                        OrderItem item = buildItem(order);
                        order.addItem(item);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));
                        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        orderService.submitOrder(TENANT_ID, USER_ID, ORDER_ID);

                        assertThat(order.getStatusId()).isEqualTo(OrderStatusConstants.IN_PROGRESS);
                        assertThat(order.getItems().get(0).getKdsSentAt()).isNotNull();
                        verify(jdbcTemplate).update(contains("order_status_history"),
                                        eq(TENANT_ID), eq(ORDER_ID),
                                        eq(OrderStatusConstants.IN_PROGRESS), eq(USER_ID));
                }

                @Test
                @DisplayName("23. Submitting non-PENDING order throws BusinessRuleException")
                void submitNonPendingThrows() {
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setStatusId(OrderStatusConstants.IN_PROGRESS);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        assertThatThrownBy(() -> orderService.submitOrder(TENANT_ID, USER_ID, ORDER_ID))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("ORDER_NOT_MODIFIABLE"));
                }

                @Test
                @DisplayName("24. Submitting empty order throws BusinessRuleException")
                void submitEmptyOrderThrows() {
                        Order order = buildPendingOrder(); // no items
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        assertThatThrownBy(() -> orderService.submitOrder(TENANT_ID, USER_ID, ORDER_ID))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("ORDER_HAS_NO_ITEMS"));
                }
        }

        // =========================================================================
        // cancelOrder
        // =========================================================================

        @Nested
        @DisplayName("cancelOrder")
        class CancelOrderTests {

                @Test
                @DisplayName("25. Cancelling PENDING order sets CANCELLED status and closedAt")
                void cancelPendingOrder() {
                        Order order = buildPendingOrder();
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));
                        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        orderService.cancelOrder(TENANT_ID, USER_ID, ORDER_ID);

                        assertThat(order.getStatusId()).isEqualTo(OrderStatusConstants.CANCELLED);
                        assertThat(order.getClosedAt()).isNotNull();
                        verify(jdbcTemplate).update(contains("order_status_history"),
                                        eq(TENANT_ID), eq(ORDER_ID),
                                        eq(OrderStatusConstants.CANCELLED), eq(USER_ID));
                }

                @Test
                @DisplayName("26. Cancelling DINE_IN order releases table to AVAILABLE")
                void cancelDineInReleasesTable() {
                        Order order = buildOrder(ServiceType.DINE_IN, TABLE_ID);
                        order.setStatusId(OrderStatusConstants.PENDING);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));
                        RestaurantTable table = buildTable(TableStatus.OCCUPIED);
                        when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID))
                                        .thenReturn(Optional.of(table));
                        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        orderService.cancelOrder(TENANT_ID, USER_ID, ORDER_ID);

                        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
                        verify(tableRepository).save(table);
                }

                @Test
                @DisplayName("27. Cancelling already terminal order throws BusinessRuleException")
                void cancelTerminalOrderThrows() {
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setStatusId(OrderStatusConstants.COMPLETED);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        assertThatThrownBy(() -> orderService.cancelOrder(TENANT_ID, USER_ID, ORDER_ID))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("ORDER_ALREADY_TERMINAL"));
                }

                @Test
                @DisplayName("28. Cancelling already CANCELLED order throws BusinessRuleException")
                void cancelAlreadyCancelledThrows() {
                        Order order = buildOrder(ServiceType.COUNTER, null);
                        order.setStatusId(OrderStatusConstants.CANCELLED);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));

                        assertThatThrownBy(() -> orderService.cancelOrder(TENANT_ID, USER_ID, ORDER_ID))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("ORDER_ALREADY_TERMINAL"));
                }
        }

        // =========================================================================
        // Edge cases and additional coverage
        // =========================================================================

        @Nested
        @DisplayName("Edge cases")
        class EdgeCaseTests {

                @Test
                @DisplayName("29. Combo item validation — inactive combo throws BusinessRuleException")
                void inactiveComboThrows() {
                        branchFound();
                        UUID comboId = UUID.randomUUID();
                        Combo combo = new Combo();
                        combo.setId(comboId);
                        combo.setActive(false);
                        when(comboRepository.findByIdAndTenantId(comboId, TENANT_ID))
                                        .thenReturn(Optional.of(combo));
                        // no taxRate/sequence stubs needed — exception thrown before those calls

                        OrderItemRequest comboItemReq = new OrderItemRequest(
                                        null, null, comboId, "Combo 1", null,
                                        1, new BigDecimal("99.00"), List.of(), null);
                        OrderCreateRequest request = new OrderCreateRequest(
                                        BRANCH_ID, ServiceType.COUNTER, null, null,
                                        List.of(comboItemReq), null, null);

                        assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, USER_ID, request))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                                                        .isEqualTo("PRODUCT_NOT_AVAILABLE"));
                }

                @Test
                @DisplayName("30. Table not belonging to branch throws ResourceNotFoundException")
                void tableNotInBranchThrows() {
                        branchFound();
                        RestaurantTable table = buildTable(TableStatus.AVAILABLE);
                        when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID))
                                        .thenReturn(Optional.of(table));
                        // count = 0 means table doesn't belong to branch
                        when(jdbcTemplate.queryForObject(
                                        contains("FROM tables t JOIN areas"),
                                        eq(Integer.class), eq(TABLE_ID), eq(BRANCH_ID), eq(TENANT_ID)))
                                        .thenReturn(0);

                        assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, USER_ID, dineInRequest()))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }

                @Test
                @DisplayName("31. Modifier quantity 0 is normalized to 1")
                void modifierQuantityZeroNormalizedToOne() {
                        branchFound();
                        productFound();
                        taxRateFound();
                        sequenceFound(1);

                        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                                Order o = inv.getArgument(0);
                                if (o.getId() == null) {
                                        o.setId(ORDER_ID);
                                }
                                return o;
                        });

                        OrderItemModifierRequest modReq = new OrderItemModifierRequest(
                                        null, "Sin Sal", new BigDecimal("0.00"), 0); // quantity=0
                        OrderItemRequest itemReq = new OrderItemRequest(
                                        PRODUCT_ID, null, null, "Hamburguesa", null,
                                        1, new BigDecimal("89.00"), List.of(modReq), null);
                        OrderCreateRequest request = new OrderCreateRequest(
                                        BRANCH_ID, ServiceType.COUNTER, null, null, List.of(itemReq), null, null);

                        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
                        orderService.createOrder(TENANT_ID, USER_ID, request);

                        verify(orderRepository, atLeastOnce()).save(captor.capture());
                        OrderItem capturedItem = captor.getAllValues().get(0).getItems().get(0);
                        // quantity=0 should be normalized to 1
                        assertThat(capturedItem.getModifiers()).hasSize(1);
                        assertThat(capturedItem.getModifiers().iterator().next().getQuantity()).isEqualTo(1);
                }

                @Test
                @DisplayName("32. Tax rate fallback to 0.16 when tenant has null rate")
                void taxRateFallbackToDefault() {
                        branchFound();
                        productFound();
                        // Return null for tax rate — should default to 0.16
                        when(jdbcTemplate.queryForObject(
                                        contains("tax_rate"), eq(BigDecimal.class), eq(TENANT_ID)))
                                        .thenReturn(null);
                        sequenceFound(1);

                        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
                                Order o = inv.getArgument(0);
                                if (o.getId() == null)
                                        o.setId(ORDER_ID);
                                return o;
                        });

                        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
                        orderService.createOrder(TENANT_ID, USER_ID, counterRequest());

                        verify(orderRepository, atLeastOnce()).save(captor.capture());
                        assertThat(captor.getAllValues().get(0).getTaxRate())
                                        .isEqualByComparingTo(new BigDecimal("0.16"));
                }

                @Test
                @DisplayName("33. Cancel COUNTER order does not try to release table")
                void cancelCounterOrderDoesNotTouchTable() {
                        Order order = buildOrder(ServiceType.COUNTER, null); // no tableId
                        order.setStatusId(OrderStatusConstants.PENDING);
                        when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID))
                                        .thenReturn(Optional.of(order));
                        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        orderService.cancelOrder(TENANT_ID, USER_ID, ORDER_ID);

                        verify(tableRepository, never()).findByIdAndTenantId(any(), any());
                }

                @Test
                @DisplayName("34. Product not found in item throws ResourceNotFoundException")
                void productNotFoundThrows() {
                        branchFound();
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, USER_ID, counterRequest()))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }

                @Test
                @DisplayName("35. Customer not found for DELIVERY throws ResourceNotFoundException")
                void customerNotFoundThrows() {
                        branchFound();
                        when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, USER_ID, deliveryRequest()))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        // =========================================================================
        // Helpers
        // =========================================================================

        private OrderCreateRequest counterRequest() {
                return new OrderCreateRequest(
                                BRANCH_ID, ServiceType.COUNTER, null, null,
                                List.of(oneItemRequest()), null, null);
        }

        private OrderCreateRequest dineInRequest() {
                return new OrderCreateRequest(
                                BRANCH_ID, ServiceType.DINE_IN, TABLE_ID, null,
                                List.of(oneItemRequest()), null, null);
        }

        private OrderCreateRequest deliveryRequest() {
                return new OrderCreateRequest(
                                BRANCH_ID, ServiceType.DELIVERY, null, CUSTOMER_ID,
                                List.of(oneItemRequest()), null, null);
        }

        private OrderItemRequest oneItemRequest() {
                return new OrderItemRequest(
                                PRODUCT_ID, null, null, "Hamburguesa", null,
                                1, new BigDecimal("89.00"), List.of(), null);
        }

        private void branchFound() {
                Branch branch = new Branch();
                branch.setId(BRANCH_ID);
                branch.setTenantId(TENANT_ID);
                when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID))
                                .thenReturn(Optional.of(branch));
        }

        private void productFound() {
                when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                .thenReturn(Optional.of(buildActiveProduct()));
        }

        private void taxRateFound() {
                when(jdbcTemplate.queryForObject(
                                contains("tax_rate"), eq(BigDecimal.class), eq(TENANT_ID)))
                                .thenReturn(new BigDecimal("0.16"));
        }

        private void sequenceFound(int seq) {
                when(orderRepository.getNextDailySequence(any(), any(), any())).thenReturn(seq);
        }

        private Product buildActiveProduct() {
                Product product = new Product();
                product.setId(PRODUCT_ID);
                product.setActive(true);
                product.setAvailable(true);
                return product;
        }

        private RestaurantTable buildTable(TableStatus status) {
                RestaurantTable table = new RestaurantTable();
                table.setId(TABLE_ID);
                table.setTenantId(TENANT_ID);
                table.setAreaId(UUID.randomUUID());
                table.setStatus(status);
                return table;
        }

        private Order buildOrder(ServiceType serviceType, UUID tableId) {
                Order order = new Order();
                order.setId(ORDER_ID);
                order.setTenantId(TENANT_ID);
                order.setBranchId(BRANCH_ID);
                order.setTableId(tableId);
                order.setCustomerId(serviceType == ServiceType.DELIVERY ? CUSTOMER_ID : null);
                order.setOrderNumber("ORD-20260225-001");
                order.setDailySequence(1);
                order.setServiceType(serviceType);
                order.setStatusId(OrderStatusConstants.PENDING);
                order.setSubtotal(BigDecimal.ZERO);
                order.setTaxRate(new BigDecimal("0.16"));
                order.setTax(BigDecimal.ZERO);
                order.setDiscount(BigDecimal.ZERO);
                order.setTotal(BigDecimal.ZERO);
                order.setSource(OrderSource.POS);
                order.setCreatedBy(USER_ID);
                order.setOpenedAt(Instant.now());
                return order;
        }

        private Order buildPendingOrder() {
                return buildOrder(ServiceType.COUNTER, null);
        }

        private Order buildPersistedOrder(ServiceType serviceType, UUID tableId) {
                Order order = buildOrder(serviceType, tableId);
                order.setCustomerId(serviceType == ServiceType.DELIVERY ? CUSTOMER_ID : null);
                return order;
        }

        private OrderItem buildItem(Order order) {
                OrderItem item = new OrderItem();
                item.setTenantId(TENANT_ID);
                item.setOrderId(ORDER_ID);
                item.setProductId(PRODUCT_ID);
                item.setProductName("Hamburguesa");
                item.setQuantity(1);
                item.setUnitPrice(new BigDecimal("89.00"));
                item.setModifiersTotal(BigDecimal.ZERO);
                item.setKdsStatus(KdsStatus.PENDING);
                return item;
        }
}
