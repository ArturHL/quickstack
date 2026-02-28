package com.quickstack.pos.service;

import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;
import com.quickstack.branch.repository.TableRepository;
import com.quickstack.common.exception.ApiException;
import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.pos.dto.request.PaymentRequest;
import com.quickstack.pos.dto.response.PaymentResponse;
import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.entity.Payment;
import com.quickstack.pos.entity.PaymentMethod;
import com.quickstack.pos.entity.ServiceType;
import com.quickstack.pos.repository.OrderRepository;
import com.quickstack.pos.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService using Mockito.
 * All external dependencies are mocked — no Spring context, no database.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TableRepository tableRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query nativeQuery;

    private PaymentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID TABLE_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final BigDecimal ORDER_TOTAL = new BigDecimal("89.00");

    @BeforeEach
    void setUp() {
        service = new PaymentService(
                paymentRepository, orderRepository, tableRepository, customerService, entityManager);

        // Default stub for native query used in insertStatusHistory
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);
    }

    // -------------------------------------------------------------------------
    // registerPayment — happy path
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("registerPayment() — happy path")
    class RegisterPaymentHappyPath {

        @Test
        @DisplayName("1. Exact payment closes COUNTER order immediately")
        void exactPaymentClosesOrder() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(ORDER_TOTAL);

            PaymentRequest request = new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null);
            PaymentResponse response = service.registerPayment(TENANT_ID, USER_ID, request);

            assertThat(response.amount()).isEqualByComparingTo(ORDER_TOTAL);
            assertThat(response.changeGiven()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(orderRepository).save(order);
            assertThat(order.getStatusId()).isEqualTo(OrderStatusConstants.COMPLETED);
            assertThat(order.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("2. Overpayment calculates correct change")
        void overpaymentGivesCorrectChange() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(new BigDecimal("100.00"));

            BigDecimal amountGiven = new BigDecimal("100.00");
            PaymentRequest request = new PaymentRequest(ORDER_ID, PaymentMethod.CASH, amountGiven, null);
            PaymentResponse response = service.registerPayment(TENANT_ID, USER_ID, request);

            assertThat(response.changeGiven()).isEqualByComparingTo("11.00");
        }

        @Test
        @DisplayName("3. Payment is saved with correct fields")
        void paymentSavedWithCorrectFields() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(ORDER_TOTAL);

            PaymentRequest request = new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, "Test note");
            service.registerPayment(TENANT_ID, USER_ID, request);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment saved = captor.getValue();

            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
            assertThat(saved.getAmount()).isEqualByComparingTo(ORDER_TOTAL);
            assertThat(saved.getAmountReceived()).isEqualByComparingTo(ORDER_TOTAL);
            assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
            assertThat(saved.getNotes()).isEqualTo("Test note");
        }

        @Test
        @DisplayName("4. DINE_IN order: table released to AVAILABLE after payment")
        void dineInOrderReleasesTable() {
            Order order = readyOrder(ServiceType.DINE_IN, TABLE_ID, null);
            RestaurantTable table = new RestaurantTable();
            table.setStatus(TableStatus.OCCUPIED);

            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(ORDER_TOTAL);
            when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID)).thenReturn(Optional.of(table));

            service.registerPayment(TENANT_ID, USER_ID, new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null));

            assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
            verify(tableRepository).save(table);
        }

        @Test
        @DisplayName("5. COUNTER order: tableRepository never called when no table")
        void counterOrderDoesNotReleaseTable() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(ORDER_TOTAL);

            service.registerPayment(TENANT_ID, USER_ID, new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null));

            verifyNoInteractions(tableRepository);
        }

        @Test
        @DisplayName("6. Customer stats incremented when order has customerId")
        void customerStatsIncrementedWhenCustomerPresent() {
            Order order = readyOrder(ServiceType.DELIVERY, null, CUSTOMER_ID);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(ORDER_TOTAL);

            service.registerPayment(TENANT_ID, USER_ID, new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null));

            verify(customerService).incrementOrderStats(TENANT_ID, CUSTOMER_ID, ORDER_TOTAL);
        }

        @Test
        @DisplayName("7. Customer stats NOT called when order has no customerId")
        void customerStatsNotCalledWithoutCustomer() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(ORDER_TOTAL);

            service.registerPayment(TENANT_ID, USER_ID, new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null));

            verifyNoInteractions(customerService);
        }

        @Test
        @DisplayName("8. Status history inserted when order is closed")
        void statusHistoryInsertedOnClose() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.sumPaymentsByOrder(ORDER_ID, TENANT_ID)).thenReturn(ORDER_TOTAL);

            service.registerPayment(TENANT_ID, USER_ID, new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null));

            verify(entityManager).createNativeQuery(contains("order_status_history"));
        }
    }

    // -------------------------------------------------------------------------
    // registerPayment — validation errors
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("registerPayment() — validation errors")
    class RegisterPaymentValidation {

        @Test
        @DisplayName("9. Throws ResourceNotFoundException when order not found")
        void throwsWhenOrderNotFound() {
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registerPayment(TENANT_ID, USER_ID,
                    new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("10. Throws BusinessRuleException when order status is PENDING (not READY)")
        void throwsWhenOrderNotReady() {
            Order order = new Order();
            order.setId(ORDER_ID);
            order.setTenantId(TENANT_ID);
            order.setStatusId(OrderStatusConstants.PENDING);
            order.setTotal(ORDER_TOTAL);

            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.registerPayment(TENANT_ID, USER_ID,
                    new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                            .isEqualTo("ORDER_NOT_READY"));
        }

        @Test
        @DisplayName("11. Throws BusinessRuleException when order status is IN_PROGRESS (not READY)")
        void throwsWhenOrderInProgress() {
            Order order = new Order();
            order.setId(ORDER_ID);
            order.setTenantId(TENANT_ID);
            order.setStatusId(OrderStatusConstants.IN_PROGRESS);
            order.setTotal(ORDER_TOTAL);

            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.registerPayment(TENANT_ID, USER_ID,
                    new PaymentRequest(ORDER_ID, PaymentMethod.CASH, ORDER_TOTAL, null)))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("12. Throws BusinessRuleException for non-CASH payment method")
        void throwsForNonCashPayment() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.registerPayment(TENANT_ID, USER_ID,
                    new PaymentRequest(ORDER_ID, PaymentMethod.CARD, ORDER_TOTAL, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                            .isEqualTo("UNSUPPORTED_PAYMENT_METHOD"));
        }

        @Test
        @DisplayName("13. Throws BusinessRuleException when amount < order.total")
        void throwsWhenAmountInsufficient() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));

            BigDecimal insufficientAmount = new BigDecimal("50.00");
            assertThatThrownBy(() -> service.registerPayment(TENANT_ID, USER_ID,
                    new PaymentRequest(ORDER_ID, PaymentMethod.CASH, insufficientAmount, null)))
                    .isInstanceOf(ApiException.class)
                    .satisfies(ex -> assertThat(((ApiException) ex).getCode())
                            .isEqualTo("INSUFFICIENT_PAYMENT"));
        }
    }

    // -------------------------------------------------------------------------
    // listPaymentsForOrder
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listPaymentsForOrder()")
    class ListPaymentsForOrder {

        @Test
        @DisplayName("14. Returns all payments for a valid order")
        void returnsPaymentsForOrder() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));

            Payment p1 = buildPayment(ORDER_TOTAL);
            Payment p2 = buildPayment(new BigDecimal("10.00"));
            when(paymentRepository.findAllByOrderIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(List.of(p1, p2));

            List<PaymentResponse> result = service.listPaymentsForOrder(TENANT_ID, ORDER_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("15. Returns empty list when no payments exist")
        void returnsEmptyWhenNoPayments() {
            Order order = readyOrder(ServiceType.COUNTER, null, null);
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.of(order));
            when(paymentRepository.findAllByOrderIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(List.of());

            List<PaymentResponse> result = service.listPaymentsForOrder(TENANT_ID, ORDER_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("16. Throws ResourceNotFoundException when order not found (IDOR protection)")
        void throwsWhenOrderNotFoundForList() {
            when(orderRepository.findByIdAndTenantId(ORDER_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listPaymentsForOrder(TENANT_ID, ORDER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Order readyOrder(ServiceType serviceType, UUID tableId, UUID customerId) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setTenantId(TENANT_ID);
        order.setStatusId(OrderStatusConstants.READY);
        order.setTotal(ORDER_TOTAL);
        order.setServiceType(serviceType);
        order.setTableId(tableId);
        order.setCustomerId(customerId);
        return order;
    }

    private Payment buildPayment(BigDecimal amount) {
        Payment p = new Payment();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT_ID);
        p.setOrderId(ORDER_ID);
        p.setPaymentMethod(PaymentMethod.CASH);
        p.setAmount(amount);
        p.setAmountReceived(amount);
        p.setChangeGiven(BigDecimal.ZERO);
        return p;
    }
}
