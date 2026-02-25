package com.quickstack.product.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.ComboCreateRequest;
import com.quickstack.product.dto.request.ComboUpdateRequest;
import com.quickstack.product.dto.response.ComboResponse;
import com.quickstack.product.service.ComboService;
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
 * REST controller for combo management.
 * <p>
 * Endpoints:
 * - GET /api/v1/combos - List all combos for the authenticated tenant
 * - POST /api/v1/combos - Create combo (OWNER/MANAGER)
 * - GET /api/v1/combos/{id} - Get combo by ID (with items)
 * - PUT /api/v1/combos/{id} - Update combo (OWNER/MANAGER)
 * - DELETE /api/v1/combos/{id} - Soft delete combo (OWNER/MANAGER)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from path params or body
 * - V4.1: Cross-tenant access returns 404 (IDOR protection)
 * - V4.2: @PreAuthorize for write operation access control
 */
@RestController
public class ComboController {

        private static final Logger log = LoggerFactory.getLogger(ComboController.class);

        private final ComboService comboService;

        public ComboController(ComboService comboService) {
                this.comboService = comboService;
        }

        /**
         * Lists all combos for the authenticated tenant, ordered by sort_order.
         */
        @GetMapping("/api/v1/combos")
        public ResponseEntity<ApiResponse<List<ComboResponse>>> listCombos(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {
                log.debug("Listing combos for tenant={}", principal.tenantId());

                List<ComboResponse> combos = comboService.listCombos(principal.tenantId());

                return ResponseEntity.ok(ApiResponse.success(combos));
        }

        /**
         * Creates a new combo with its items. Requires OWNER or MANAGER role.
         */
        @PostMapping("/api/v1/combos")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<ApiResponse<ComboResponse>> createCombo(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @Valid @RequestBody ComboCreateRequest request) {
                log.info("Creating combo name='{}', tenant={}", request.name(), principal.tenantId());

                ComboResponse response = comboService.createCombo(
                                principal.tenantId(), principal.userId(), request);

                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/v1/combos/{id}")
                                .buildAndExpand(response.id())
                                .toUri();

                return ResponseEntity.created(location).body(ApiResponse.success(response));
        }

        /**
         * Retrieves a combo by ID, including its items with product names.
         */
        @GetMapping("/api/v1/combos/{id}")
        public ResponseEntity<ApiResponse<ComboResponse>> getCombo(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id) {
                log.debug("Getting combo id={}, tenant={}", id, principal.tenantId());

                ComboResponse response = comboService.getCombo(principal.tenantId(), id);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Updates an existing combo. Requires OWNER or MANAGER role.
         * If items are provided, they fully replace the existing combo items.
         */
        @PutMapping("/api/v1/combos/{id}")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<ApiResponse<ComboResponse>> updateCombo(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id,
                        @Valid @RequestBody ComboUpdateRequest request) {
                log.info("Updating combo id={}, tenant={}", id, principal.tenantId());

                ComboResponse response = comboService.updateCombo(
                                principal.tenantId(), principal.userId(), id, request);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Soft-deletes a combo. Requires OWNER or MANAGER role.
         */
        @DeleteMapping("/api/v1/combos/{id}")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<Void> deleteCombo(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id) {
                log.info("Deleting combo id={}, tenant={}, by={}", id, principal.tenantId(), principal.userId());

                comboService.deleteCombo(principal.tenantId(), principal.userId(), id);

                return ResponseEntity.noContent().build();
        }
}
