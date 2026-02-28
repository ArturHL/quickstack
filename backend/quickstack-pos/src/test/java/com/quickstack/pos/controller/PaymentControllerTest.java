package com.quickstack.pos.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.pos.dto.request.PaymentRequest;
import com.quickstack.pos.dto.response.PaymentResponse;
import com.quickstack.pos.entity.PaymentMethod;
import com.quickstack.pos.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentController using Mockito (no Spring context).
 * Verifies HTTP status codes, delegation to PaymentService, and JWT principal extraction.
 * @PreAuthorize annotations are NOT executed here — verified in PaymentIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private PaymentController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("89.00");

    private JwtAuthenticationPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentService);
        principal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "cashier@test.com");

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("http");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8080);
        mockRequest.setRequestURI("/api/v1/payments");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    // =========================================================================
    // POST /api/v1/payments
    // =========================================================================

    @Nested
    @DisplayName("registerPayment")
    class RegisterPaymentTests {

        @Test
        @DisplayName("1. Returns 201 with Location header on success")
        void returnsCreatedWithLocation() {
            PaymentRequest request = cashPaymentRequest();
            when(paymentService.registerPayment(any(), any(), any())).thenReturn(buildPaymentResponse());

            ResponseEntity<ApiResponse<PaymentResponse>> result = controller.registerPayment(principal, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getLocation()).isNotNull();
            assertThat(result.getHeaders().getLocation().toString()).contains(PAYMENT_ID.toString());
        }

        @Test
        @DisplayName("2. Delegates to service with tenantId and userId from JWT")
        void delegatesWithCorrectTenantAndUser() {
            PaymentRequest request = cashPaymentRequest();
            when(paymentService.registerPayment(any(), any(), any())).thenReturn(buildPaymentResponse());

            controller.registerPayment(principal, request);

            verify(paymentService).registerPayment(eq(TENANT_ID), eq(USER_ID), eq(request));
        }

        @Test
        @DisplayName("3. ResourceNotFoundException propagates from service (order not found)")
        void notFoundPropagates() {
            when(paymentService.registerPayment(any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Order", ORDER_ID));

            assertThatThrownBy(() -> controller.registerPayment(principal, cashPaymentRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("4. BusinessRuleException propagates (order not READY)")
        void businessRuleExceptionPropagates() {
            when(paymentService.registerPayment(any(), any(), any()))
                    .thenThrow(new BusinessRuleException("ORDER_NOT_READY", "Order is not ready"));

            assertThatThrownBy(() -> controller.registerPayment(principal, cashPaymentRequest()))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // =========================================================================
    // GET /api/v1/orders/{orderId}/payments
    // =========================================================================

    @Nested
    @DisplayName("getPaymentsForOrder")
    class GetPaymentsTests {

        @Test
        @DisplayName("5. Returns 200 with payment list")
        void returnsOkWithPayments() {
            when(paymentService.listPaymentsForOrder(TENANT_ID, ORDER_ID))
                    .thenReturn(List.of(buildPaymentResponse()));

            ResponseEntity<ApiResponse<List<PaymentResponse>>> result =
                    controller.getPaymentsForOrder(principal, ORDER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data()).hasSize(1);
        }

        @Test
        @DisplayName("6. Delegates with correct tenantId from JWT")
        void delegatesWithCorrectTenantId() {
            when(paymentService.listPaymentsForOrder(any(), any())).thenReturn(List.of());

            controller.getPaymentsForOrder(principal, ORDER_ID);

            verify(paymentService).listPaymentsForOrder(eq(TENANT_ID), eq(ORDER_ID));
        }

        @Test
        @DisplayName("7. Returns empty list when no payments exist")
        void returnsEmptyList() {
            when(paymentService.listPaymentsForOrder(TENANT_ID, ORDER_ID)).thenReturn(List.of());

            ResponseEntity<ApiResponse<List<PaymentResponse>>> result =
                    controller.getPaymentsForOrder(principal, ORDER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().data()).isEmpty();
        }

        @Test
        @DisplayName("8. ResourceNotFoundException propagates (cross-tenant / order not found)")
        void notFoundPropagates() {
            when(paymentService.listPaymentsForOrder(TENANT_ID, ORDER_ID))
                    .thenThrow(new ResourceNotFoundException("Order", ORDER_ID));

            assertThatThrownBy(() -> controller.getPaymentsForOrder(principal, ORDER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PaymentRequest cashPaymentRequest() {
        return new PaymentRequest(ORDER_ID, PaymentMethod.CASH, AMOUNT, null);
    }

    private PaymentResponse buildPaymentResponse() {
        return new PaymentResponse(
                PAYMENT_ID, TENANT_ID, ORDER_ID,
                AMOUNT, PaymentMethod.CASH,
                AMOUNT, BigDecimal.ZERO,
                "COMPLETED", null, null,
                Instant.now(), USER_ID);
    }
}
