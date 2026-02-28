package com.quickstack.pos.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.pos.dto.request.OrderCreateRequest;
import com.quickstack.pos.dto.request.OrderItemRequest;
import com.quickstack.pos.dto.response.OrderResponse;
import com.quickstack.pos.entity.OrderSource;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.entity.ServiceType;
import com.quickstack.pos.security.PosPermissionEvaluator;
import com.quickstack.pos.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderController using Mockito (no Spring context).
 * <p>
 * Controller is instantiated directly — tests verify:
 * - Correct HTTP status codes
 * - Correct delegation to OrderService with proper tenantId/userId from JWT
 * - isManager flag derived from PosPermissionEvaluator
 * - Location header on creation
 * <p>
 * 
 * @PreAuthorize annotations are NOT executed here (Spring Security interceptors
 *               not running).
 *               That behavior is verified in OrderIntegrationTest (E2E).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderController")
class OrderControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private PosPermissionEvaluator posPermissionEvaluator;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    private OrderController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal ownerPrincipal;
    private JwtAuthenticationPrincipal cashierPrincipal;

    @BeforeEach
    void setUp() {
        controller = new OrderController(orderService, posPermissionEvaluator);
        ownerPrincipal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "owner@test.com");
        cashierPrincipal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "cashier@test.com");

        // Wire SecurityContextHolder so controller can call posPermissionEvaluator
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        mockServletContext();
    }

    // =========================================================================
    // POST /orders
    // =========================================================================

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("1. Returns 201 with Location header on success")
        void returnsCreatedWithLocation() {
            OrderCreateRequest request = counterRequest();
            OrderResponse response = buildOrderResponse();
            when(orderService.createOrder(TENANT_ID, USER_ID, request)).thenReturn(response);

            ResponseEntity<ApiResponse<OrderResponse>> result = controller.createOrder(ownerPrincipal, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getLocation()).isNotNull();
            assertThat(result.getHeaders().getLocation().toString()).contains(ORDER_ID.toString());
        }

        @Test
        @DisplayName("2. Delegates to service with tenantId and userId from JWT principal")
        void delegatesWithCorrectTenantAndUser() {
            OrderCreateRequest request = counterRequest();
            when(orderService.createOrder(any(), any(), any())).thenReturn(buildOrderResponse());

            controller.createOrder(ownerPrincipal, request);

            verify(orderService).createOrder(eq(TENANT_ID), eq(USER_ID), eq(request));
        }

        @Test
        @DisplayName("3. Service exception propagates from createOrder")
        void serviceExceptionPropagates() {
            OrderCreateRequest request = counterRequest();
            when(orderService.createOrder(any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Branch", BRANCH_ID));

            assertThatThrownBy(() -> controller.createOrder(ownerPrincipal, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // GET /orders/{id}
    // =========================================================================

    @Nested
    @DisplayName("getOrder")
    class GetOrderTests {

        @Test
        @DisplayName("4. Returns 200 with order data")
        void returnsOkWithOrder() {
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(true);
            when(orderService.getOrder(any(), any(), anyBoolean(), any()))
                    .thenReturn(buildOrderResponse());

            ResponseEntity<?> result = controller.getOrder(ownerPrincipal, ORDER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("5. Passes isManager=true to service when evaluator returns true")
        void passesIsManagerTrueWhenManagerRole() {
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(true);
            when(orderService.getOrder(any(), any(), anyBoolean(), any()))
                    .thenReturn(buildOrderResponse());

            controller.getOrder(ownerPrincipal, ORDER_ID);

            verify(orderService).getOrder(eq(TENANT_ID), eq(USER_ID), eq(true), eq(ORDER_ID));
        }

        @Test
        @DisplayName("6. Passes isManager=false to service when cashier role")
        void passesIsManagerFalseWhenCashierRole() {
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(false);
            when(orderService.getOrder(any(), any(), anyBoolean(), any()))
                    .thenReturn(buildOrderResponse());

            controller.getOrder(cashierPrincipal, ORDER_ID);

            verify(orderService).getOrder(eq(TENANT_ID), eq(USER_ID), eq(false), eq(ORDER_ID));
        }
    }

    // =========================================================================
    // GET /orders
    // =========================================================================

    @Nested
    @DisplayName("listOrders")
    class ListOrdersTests {

        @Test
        @DisplayName("7. Manager call passes isManager=true to service")
        void managerPassesIsManagerTrue() {
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(true);
            when(orderService.listOrders(any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn(Page.empty());

            controller.listOrders(ownerPrincipal, null, null, PageRequest.of(0, 10));

            verify(orderService).listOrders(eq(TENANT_ID), eq(USER_ID),
                    eq(true), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("8. Cashier call passes isManager=false to service")
        void cashierPassesIsManagerFalse() {
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(false);
            when(orderService.listOrders(any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn(Page.empty());

            controller.listOrders(cashierPrincipal, null, null, PageRequest.of(0, 10));

            verify(orderService).listOrders(eq(TENANT_ID), eq(USER_ID),
                    eq(false), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("9. Returns 200 with paged orders")
        void returnsOkWithPage() {
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(true);
            Page<OrderResponse> page = new PageImpl<>(List.of(buildOrderResponse()));
            when(orderService.listOrders(any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn(page);

            ResponseEntity<?> result = controller.listOrders(ownerPrincipal, null, null, PageRequest.of(0, 10));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // =========================================================================
    // POST /orders/{id}/items
    // =========================================================================

    @Nested
    @DisplayName("addItem")
    class AddItemTests {

        @Test
        @DisplayName("10. Returns 200 with updated order on success")
        void returnsOkOnSuccess() {
            OrderItemRequest itemRequest = oneItemRequest();
            when(orderService.addItemToOrder(any(), any(), any(), any()))
                    .thenReturn(buildOrderResponse());

            ResponseEntity<?> result = controller.addItem(ownerPrincipal, ORDER_ID, itemRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(orderService).addItemToOrder(eq(TENANT_ID), eq(USER_ID), eq(ORDER_ID), eq(itemRequest));
        }

        @Test
        @DisplayName("11. BusinessRuleException from service propagates")
        void businessRuleExceptionPropagates() {
            OrderItemRequest itemRequest = oneItemRequest();
            when(orderService.addItemToOrder(any(), any(), any(), any()))
                    .thenThrow(new BusinessRuleException("ORDER_NOT_MODIFIABLE", "Order not modifiable"));

            assertThatThrownBy(() -> controller.addItem(ownerPrincipal, ORDER_ID, itemRequest))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // =========================================================================
    // DELETE /orders/{orderId}/items/{itemId}
    // =========================================================================

    @Nested
    @DisplayName("removeItem")
    class RemoveItemTests {

        @Test
        @DisplayName("12. Returns 204 on successful removal")
        void returns204OnSuccess() {
            when(orderService.removeItemFromOrder(TENANT_ID, USER_ID, ORDER_ID, ITEM_ID))
                    .thenReturn(buildOrderResponse());

            ResponseEntity<Void> result = controller.removeItem(ownerPrincipal, ORDER_ID, ITEM_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(orderService).removeItemFromOrder(eq(TENANT_ID), eq(USER_ID), eq(ORDER_ID), eq(ITEM_ID));
        }

        @Test
        @DisplayName("13. ResourceNotFoundException propagates when item not found")
        void notFoundPropagates() {
            doThrow(new ResourceNotFoundException("OrderItem", ITEM_ID))
                    .when(orderService).removeItemFromOrder(any(), any(), any(), any());

            assertThatThrownBy(() -> controller.removeItem(ownerPrincipal, ORDER_ID, ITEM_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // POST /orders/{id}/submit
    // =========================================================================

    @Nested
    @DisplayName("submitOrder")
    class SubmitOrderTests {

        @Test
        @DisplayName("14. Returns 200 with updated order on success")
        void returnsOkOnSuccess() {
            when(orderService.submitOrder(TENANT_ID, USER_ID, ORDER_ID))
                    .thenReturn(buildOrderResponse());

            ResponseEntity<?> result = controller.submitOrder(ownerPrincipal, ORDER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(orderService).submitOrder(TENANT_ID, USER_ID, ORDER_ID);
        }

        @Test
        @DisplayName("15. BusinessRuleException propagates when order has no items")
        void businessRuleExceptionPropagates() {
            when(orderService.submitOrder(any(), any(), any()))
                    .thenThrow(new BusinessRuleException("ORDER_HAS_NO_ITEMS", "Empty order"));

            assertThatThrownBy(() -> controller.submitOrder(ownerPrincipal, ORDER_ID))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // =========================================================================
    // POST /orders/{id}/ready
    // =========================================================================

    @Nested
    @DisplayName("markOrderReady")
    class MarkOrderReadyTests {

        @Test
        @DisplayName("16. Returns 200 with updated order on success")
        void returnsOkOnSuccess() {
            when(orderService.markOrderReady(TENANT_ID, USER_ID, ORDER_ID))
                    .thenReturn(buildOrderResponse());

            ResponseEntity<?> result = controller.markOrderReady(ownerPrincipal, ORDER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(orderService).markOrderReady(TENANT_ID, USER_ID, ORDER_ID);
        }

        @Test
        @DisplayName("17. BusinessRuleException propagates when order is not IN_PROGRESS")
        void businessRuleExceptionPropagates() {
            when(orderService.markOrderReady(any(), any(), any()))
                    .thenThrow(new BusinessRuleException("ORDER_NOT_IN_PROGRESS", "Not in progress"));

            assertThatThrownBy(() -> controller.markOrderReady(ownerPrincipal, ORDER_ID))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // =========================================================================
    // POST /orders/{id}/cancel
    // =========================================================================

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("18. Returns 204 on successful cancellation")
        void returns204OnSuccess() {
            doNothing().when(orderService).cancelOrder(TENANT_ID, USER_ID, ORDER_ID);

            ResponseEntity<Void> result = controller.cancelOrder(ownerPrincipal, ORDER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(orderService).cancelOrder(eq(TENANT_ID), eq(USER_ID), eq(ORDER_ID));
        }

        @Test
        @DisplayName("17. BusinessRuleException propagates when order is terminal")
        void terminalOrderExceptionPropagates() {
            doThrow(new BusinessRuleException("ORDER_ALREADY_TERMINAL", "Already terminal"))
                    .when(orderService).cancelOrder(any(), any(), any());

            assertThatThrownBy(() -> controller.cancelOrder(ownerPrincipal, ORDER_ID))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // =========================================================================
    // Principal extraction
    // =========================================================================

    @Nested
    @DisplayName("Principal extraction")
    class PrincipalExtractionTests {

        @Test
        @DisplayName("18. tenantId is always extracted from JWT principal (never from request)")
        void tenantIdFromJwt() {
            OrderCreateRequest request = counterRequest();
            when(orderService.createOrder(any(), any(), any())).thenReturn(buildOrderResponse());

            controller.createOrder(ownerPrincipal, request);

            // Verify tenantId comes from principal, not anywhere else
            verify(orderService).createOrder(eq(TENANT_ID), any(), any());
        }

        @Test
        @DisplayName("19. userId is extracted from JWT principal for audit trail")
        void userIdFromJwt() {
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(true);
            when(orderService.getOrder(any(), any(), anyBoolean(), any()))
                    .thenReturn(buildOrderResponse());

            controller.getOrder(ownerPrincipal, ORDER_ID);

            verify(orderService).getOrder(any(), eq(USER_ID), anyBoolean(), any());
        }

        @Test
        @DisplayName("20. listOrders forwards branchId and statusId filters to service")
        void listOrdersForwardsFilters() {
            UUID branchId = UUID.randomUUID();
            UUID statusId = UUID.randomUUID();
            when(posPermissionEvaluator.isManagerOrAbove(any())).thenReturn(true);
            when(orderService.listOrders(any(), any(), anyBoolean(), any(), any(), any()))
                    .thenReturn(Page.empty());

            controller.listOrders(ownerPrincipal, branchId, statusId, PageRequest.of(0, 10));

            verify(orderService).listOrders(eq(TENANT_ID), eq(USER_ID),
                    eq(true), eq(branchId), eq(statusId), any());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void mockServletContext() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("http");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8080);
        mockRequest.setRequestURI("/api/v1/orders");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    private OrderCreateRequest counterRequest() {
        return new OrderCreateRequest(
                BRANCH_ID, ServiceType.COUNTER, null, null,
                List.of(oneItemRequest()), null, null);
    }

    private OrderItemRequest oneItemRequest() {
        return new OrderItemRequest(
                PRODUCT_ID, null, null, "Hamburguesa", null,
                1, new BigDecimal("89.00"), List.of(), null);
    }

    private OrderResponse buildOrderResponse() {
        return new OrderResponse(
                ORDER_ID, TENANT_ID, BRANCH_ID,
                null, null,
                "ORD-20260225-001", 1,
                ServiceType.COUNTER,
                OrderStatusConstants.PENDING,
                BigDecimal.ZERO, new BigDecimal("0.16"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                OrderSource.POS,
                null, null,
                Instant.now(), null,
                USER_ID, USER_ID,
                Instant.now(), Instant.now(),
                List.of());
    }
}
