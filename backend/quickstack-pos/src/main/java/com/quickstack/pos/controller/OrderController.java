package com.quickstack.pos.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.pos.dto.request.OrderCreateRequest;
import com.quickstack.pos.dto.request.OrderItemRequest;
import com.quickstack.pos.dto.response.OrderResponse;
import com.quickstack.pos.security.PosPermissionEvaluator;
import com.quickstack.pos.service.OrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for order lifecycle management.
 * <p>
 * Endpoints:
 * - POST   /api/v1/orders              - Create order (OWNER, MANAGER, CASHIER)
 * - GET    /api/v1/orders/{id}         - Get single order (cashier sees only own orders)
 * - GET    /api/v1/orders              - List orders (manager sees all, cashier sees own)
 * - POST   /api/v1/orders/{id}/items   - Add item to PENDING order (OWNER, MANAGER, CASHIER)
 * - DELETE /api/v1/orders/{id}/items/{itemId} - Remove item from PENDING order
 * - POST   /api/v1/orders/{id}/submit  - Send order to kitchen (OWNER, MANAGER, CASHIER)
 * - POST   /api/v1/orders/{id}/cancel  - Cancel order (OWNER, MANAGER only)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request body or path
 * - V4.1: IDOR protection — cross-tenant access returns 404 (enforced in service layer)
 * - V4.2: @PreAuthorize for operation-level access control
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final PosPermissionEvaluator posPermissionEvaluator;

    public OrderController(OrderService orderService, PosPermissionEvaluator posPermissionEvaluator) {
        this.orderService = orderService;
        this.posPermissionEvaluator = posPermissionEvaluator;
    }

    /**
     * Creates a new order. Requires OWNER, MANAGER, or CASHIER role.
     */
    @PostMapping
    @PreAuthorize("@posPermissionEvaluator.canCreateOrder(authentication)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        log.info("Creating order for tenant={} by user={}", principal.tenantId(), principal.userId());

        OrderResponse response = orderService.createOrder(
                principal.tenantId(), principal.userId(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single order.
     * Managers see any order; cashiers only see their own.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID orderId
    ) {
        boolean isManager = posPermissionEvaluator.isManagerOrAbove(currentAuthentication());
        log.debug("Getting order id={} tenant={} isManager={}", orderId, principal.tenantId(), isManager);

        OrderResponse response = orderService.getOrder(
                principal.tenantId(), principal.userId(), isManager, orderId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Lists orders with optional filters.
     * Managers see all orders; cashiers only see their own.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listOrders(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID statusId,
            Pageable pageable
    ) {
        boolean isManager = posPermissionEvaluator.isManagerOrAbove(currentAuthentication());
        log.debug("Listing orders tenant={} isManager={} branchId={}", principal.tenantId(), isManager, branchId);

        Page<OrderResponse> orders = orderService.listOrders(
                principal.tenantId(), principal.userId(), isManager, branchId, statusId, pageable);

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Adds an item to a PENDING order. Requires OWNER, MANAGER, or CASHIER role.
     */
    @PostMapping("/{id}/items")
    @PreAuthorize("@posPermissionEvaluator.canCreateOrder(authentication)")
    public ResponseEntity<ApiResponse<OrderResponse>> addItem(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID orderId,
            @Valid @RequestBody OrderItemRequest request
    ) {
        log.info("Adding item to order id={} tenant={}", orderId, principal.tenantId());

        OrderResponse response = orderService.addItemToOrder(
                principal.tenantId(), principal.userId(), orderId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Removes an item from a PENDING order. Requires OWNER, MANAGER, or CASHIER role.
     */
    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("@posPermissionEvaluator.canCreateOrder(authentication)")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("orderId") UUID orderId,
            @PathVariable("itemId") UUID itemId
    ) {
        log.info("Removing item id={} from order id={} tenant={}", itemId, orderId, principal.tenantId());

        orderService.removeItemFromOrder(
                principal.tenantId(), principal.userId(), orderId, itemId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Submits a PENDING order to the kitchen. Requires OWNER, MANAGER, or CASHIER role.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("@posPermissionEvaluator.canCreateOrder(authentication)")
    public ResponseEntity<ApiResponse<OrderResponse>> submitOrder(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID orderId
    ) {
        log.info("Submitting order id={} tenant={}", orderId, principal.tenantId());

        OrderResponse response = orderService.submitOrder(
                principal.tenantId(), principal.userId(), orderId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Marks an IN_PROGRESS order as READY. Requires OWNER, MANAGER, or CASHIER role.
     */
    @PostMapping("/{id}/ready")
    @PreAuthorize("@posPermissionEvaluator.canCreateOrder(authentication)")
    public ResponseEntity<ApiResponse<OrderResponse>> markOrderReady(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID orderId
    ) {
        log.info("Marking order ready id={} tenant={}", orderId, principal.tenantId());

        OrderResponse response = orderService.markOrderReady(
                principal.tenantId(), principal.userId(), orderId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Cancels an order. Requires OWNER or MANAGER role.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("@posPermissionEvaluator.isManagerOrAbove(authentication)")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID orderId
    ) {
        log.info("Cancelling order id={} tenant={} by user={}", orderId, principal.tenantId(), principal.userId());

        orderService.cancelOrder(principal.tenantId(), principal.userId(), orderId);

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
