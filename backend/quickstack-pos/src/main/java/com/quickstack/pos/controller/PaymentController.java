package com.quickstack.pos.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.pos.dto.request.PaymentRequest;
import com.quickstack.pos.dto.response.PaymentResponse;
import com.quickstack.pos.security.PosPermissionEvaluator;
import com.quickstack.pos.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment operations.
 * <p>
 * Endpoints:
 * - POST /api/v1/payments                     - Register payment (OWNER, MANAGER, CASHIER)
 * - GET  /api/v1/orders/{orderId}/payments     - List payments for an order (CASHIER+)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request body or path
 * - V4.1: IDOR protection — cross-tenant access returns 404 (enforced in service layer)
 * - V4.2: @PreAuthorize for operation-level access control
 */
@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Registers a payment for an order. Requires OWNER, MANAGER, or CASHIER role.
     * <p>
     * Only CASH payments supported in MVP. Amount must be >= order.total.
     * If total payments cover the order, the order is automatically closed.
     */
    @PostMapping("/api/v1/payments")
    @PreAuthorize("@posPermissionEvaluator.canCreateOrder(authentication)")
    public ResponseEntity<ApiResponse<PaymentResponse>> registerPayment(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody PaymentRequest request
    ) {
        log.info("Registering payment for order={} tenant={} by user={}",
                request.orderId(), principal.tenantId(), principal.userId());

        PaymentResponse response = paymentService.registerPayment(
                principal.tenantId(), principal.userId(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/payments/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Lists all payments registered for an order. Requires OWNER, MANAGER, or CASHIER role.
     * Returns 404 if the order does not belong to the authenticated tenant.
     */
    @GetMapping("/api/v1/orders/{orderId}/payments")
    @PreAuthorize("@posPermissionEvaluator.canCreateOrder(authentication)")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsForOrder(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable UUID orderId
    ) {
        log.debug("Listing payments for order={} tenant={}", orderId, principal.tenantId());

        List<PaymentResponse> payments = paymentService.listPaymentsForOrder(
                principal.tenantId(), orderId);

        return ResponseEntity.ok(ApiResponse.success(payments));
    }
}
