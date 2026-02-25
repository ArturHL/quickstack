package com.quickstack.pos.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.pos.dto.request.CustomerCreateRequest;
import com.quickstack.pos.dto.request.CustomerUpdateRequest;
import com.quickstack.pos.dto.response.CustomerResponse;
import com.quickstack.pos.service.CustomerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for customer management.
 * <p>
 * Endpoints:
 * - GET    /api/v1/customers        - List customers with optional search (any authenticated user)
 * - POST   /api/v1/customers        - Create customer (OWNER, MANAGER, CASHIER)
 * - GET    /api/v1/customers/{id}   - Get single customer (any authenticated user)
 * - PUT    /api/v1/customers/{id}   - Update customer (OWNER, MANAGER, CASHIER)
 * - DELETE /api/v1/customers/{id}   - Soft delete customer (OWNER, MANAGER)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request params
 * - V4.1: IDOR protection — cross-tenant access returns 404
 * - V4.2: @PreAuthorize for operation-level access control
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Lists customers for the authenticated user's tenant.
     * Supports optional search by name, phone, or email.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> listCustomers(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        log.debug("Listing customers for tenant={}, search={}", principal.tenantId(), search);
        Page<CustomerResponse> customers = customerService.listCustomers(
                principal.tenantId(), search, pageable);
        return ResponseEntity.ok(ApiResponse.success(customers));
    }

    /**
     * Creates a new customer. Requires OWNER, MANAGER, or CASHIER role.
     */
    @PostMapping
    @PreAuthorize("@posPermissionEvaluator.canCreateCustomer(authentication)")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody CustomerCreateRequest request
    ) {
        log.info("Creating customer for tenant={}", principal.tenantId());

        CustomerResponse response = customerService.createCustomer(
                principal.tenantId(), principal.userId(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single customer by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID customerId
    ) {
        log.debug("Getting customer id={}, tenant={}", customerId, principal.tenantId());
        CustomerResponse response = customerService.getCustomer(principal.tenantId(), customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing customer. Requires OWNER, MANAGER, or CASHIER role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@posPermissionEvaluator.canCreateCustomer(authentication)")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID customerId,
            @Valid @RequestBody CustomerUpdateRequest request
    ) {
        log.info("Updating customer id={}, tenant={}", customerId, principal.tenantId());

        CustomerResponse response = customerService.updateCustomer(
                principal.tenantId(), principal.userId(), customerId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Soft-deletes a customer. Requires OWNER or MANAGER role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@posPermissionEvaluator.canDeleteCustomer(authentication)")
    public ResponseEntity<Void> deleteCustomer(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID customerId
    ) {
        log.info("Deleting customer id={}, tenant={}, by={}", customerId, principal.tenantId(), principal.userId());

        customerService.deleteCustomer(principal.tenantId(), principal.userId(), customerId);

        return ResponseEntity.noContent().build();
    }
}
