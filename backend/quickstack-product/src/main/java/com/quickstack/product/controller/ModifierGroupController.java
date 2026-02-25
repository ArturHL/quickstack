package com.quickstack.product.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.ModifierCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupUpdateRequest;
import com.quickstack.product.dto.request.ModifierUpdateRequest;
import com.quickstack.product.dto.response.ModifierGroupResponse;
import com.quickstack.product.dto.response.ModifierResponse;
import com.quickstack.product.service.ModifierGroupService;
import com.quickstack.product.service.ModifierService;
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
 * REST controller for modifier group and modifier management.
 * <p>
 * Endpoints:
 * - GET /api/v1/products/{productId}/modifier-groups - List modifier groups for
 * a product
 * - POST /api/v1/products/{productId}/modifier-groups - Create modifier group
 * (OWNER/MANAGER)
 * - GET /api/v1/modifier-groups/{id} - Get modifier group by ID
 * - PUT /api/v1/modifier-groups/{id} - Update modifier group (OWNER/MANAGER)
 * - DELETE /api/v1/modifier-groups/{id} - Soft delete modifier group
 * (OWNER/MANAGER)
 * - GET /api/v1/modifier-groups/{groupId}/modifiers - List modifiers in a group
 * - POST /api/v1/modifier-groups/{groupId}/modifiers - Add modifier
 * (OWNER/MANAGER)
 * - PUT /api/v1/modifiers/{id} - Update modifier (OWNER/MANAGER)
 * - DELETE /api/v1/modifiers/{id} - Soft delete modifier (OWNER/MANAGER)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from path params or body
 * - V4.1: Cross-tenant access returns 404 (IDOR protection)
 * - V4.2: @PreAuthorize for write operation access control
 */
@RestController
public class ModifierGroupController {

        private static final Logger log = LoggerFactory.getLogger(ModifierGroupController.class);

        private final ModifierGroupService modifierGroupService;
        private final ModifierService modifierService;

        public ModifierGroupController(
                        ModifierGroupService modifierGroupService,
                        ModifierService modifierService) {
                this.modifierGroupService = modifierGroupService;
                this.modifierService = modifierService;
        }

        // -------------------------------------------------------------------------
        // Modifier Group endpoints
        // -------------------------------------------------------------------------

        /**
         * Lists all modifier groups for a product, including their modifiers.
         */
        @GetMapping("/api/v1/products/{productId}/modifier-groups")
        public ResponseEntity<ApiResponse<List<ModifierGroupResponse>>> listModifierGroups(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID productId) {
                log.debug("Listing modifier groups for product={}, tenant={}", productId, principal.tenantId());

                List<ModifierGroupResponse> groups = modifierGroupService.listModifierGroupsByProduct(
                                principal.tenantId(), productId);

                return ResponseEntity.ok(ApiResponse.success(groups));
        }

        /**
         * Creates a new modifier group for a product. Requires OWNER or MANAGER role.
         */
        @PostMapping("/api/v1/products/{productId}/modifier-groups")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<ApiResponse<ModifierGroupResponse>> createModifierGroup(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID productId,
                        @Valid @RequestBody ModifierGroupCreateRequest request) {
                log.info("Creating modifier group for product={}, tenant={}", productId, principal.tenantId());

                // Build request with productId from path (overrides any body value for safety)
                ModifierGroupCreateRequest effectiveRequest = new ModifierGroupCreateRequest(
                                productId,
                                request.name(),
                                request.description(),
                                request.minSelections(),
                                request.maxSelections(),
                                request.isRequired(),
                                request.sortOrder());

                ModifierGroupResponse response = modifierGroupService.createModifierGroup(
                                principal.tenantId(), principal.userId(), effectiveRequest);

                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/v1/modifier-groups/{id}")
                                .buildAndExpand(response.id())
                                .toUri();

                return ResponseEntity.created(location).body(ApiResponse.success(response));
        }

        /**
         * Retrieves a modifier group by ID, including its modifiers.
         */
        @GetMapping("/api/v1/modifier-groups/{id}")
        public ResponseEntity<ApiResponse<ModifierGroupResponse>> getModifierGroup(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id) {
                log.debug("Getting modifier group id={}, tenant={}", id, principal.tenantId());

                ModifierGroupResponse response = modifierGroupService.getModifierGroup(principal.tenantId(), id);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Updates an existing modifier group. Requires OWNER or MANAGER role.
         */
        @PutMapping("/api/v1/modifier-groups/{id}")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<ApiResponse<ModifierGroupResponse>> updateModifierGroup(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id,
                        @Valid @RequestBody ModifierGroupUpdateRequest request) {
                log.info("Updating modifier group id={}, tenant={}", id, principal.tenantId());

                ModifierGroupResponse response = modifierGroupService.updateModifierGroup(
                                principal.tenantId(), principal.userId(), id, request);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Soft-deletes a modifier group and cascades to its modifiers. Requires OWNER
         * or MANAGER role.
         */
        @DeleteMapping("/api/v1/modifier-groups/{id}")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<Void> deleteModifierGroup(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id) {
                log.info("Deleting modifier group id={}, tenant={}, by={}", id, principal.tenantId(),
                                principal.userId());

                modifierGroupService.deleteModifierGroup(principal.tenantId(), principal.userId(), id);

                return ResponseEntity.noContent().build();
        }

        // -------------------------------------------------------------------------
        // Modifier endpoints
        // -------------------------------------------------------------------------

        /**
         * Lists all active modifiers in a modifier group.
         */
        @GetMapping("/api/v1/modifier-groups/{groupId}/modifiers")
        public ResponseEntity<ApiResponse<List<ModifierResponse>>> listModifiers(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID groupId) {
                log.debug("Listing modifiers for group={}, tenant={}", groupId, principal.tenantId());

                List<ModifierResponse> modifiers = modifierService.listModifiers(principal.tenantId(), groupId);

                return ResponseEntity.ok(ApiResponse.success(modifiers));
        }

        /**
         * Adds a new modifier to a modifier group. Requires OWNER or MANAGER role.
         */
        @PostMapping("/api/v1/modifier-groups/{groupId}/modifiers")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<ApiResponse<ModifierResponse>> addModifier(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID groupId,
                        @Valid @RequestBody ModifierCreateRequest request) {
                log.info("Adding modifier to group={}, tenant={}", groupId, principal.tenantId());

                // Build request with groupId from path for safety
                ModifierCreateRequest effectiveRequest = new ModifierCreateRequest(
                                groupId,
                                request.name(),
                                request.priceAdjustment(),
                                request.isDefault(),
                                request.sortOrder());

                ModifierResponse response = modifierService.addModifier(
                                principal.tenantId(), principal.userId(), effectiveRequest);

                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/api/v1/modifiers/{id}")
                                .buildAndExpand(response.id())
                                .toUri();

                return ResponseEntity.created(location).body(ApiResponse.success(response));
        }

        /**
         * Updates an existing modifier. Requires OWNER or MANAGER role.
         */
        @PutMapping("/api/v1/modifiers/{id}")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<ApiResponse<ModifierResponse>> updateModifier(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id,
                        @Valid @RequestBody ModifierUpdateRequest request) {
                log.info("Updating modifier id={}, tenant={}", id, principal.tenantId());

                ModifierResponse response = modifierService.updateModifier(
                                principal.tenantId(), principal.userId(), id, request);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        /**
         * Soft-deletes a modifier. Requires OWNER or MANAGER role.
         * Returns 409 if it would be the last active modifier in the group.
         */
        @DeleteMapping("/api/v1/modifiers/{id}")
        @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
        public ResponseEntity<Void> deleteModifier(
                        @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
                        @PathVariable UUID id) {
                log.info("Deleting modifier id={}, tenant={}, by={}", id, principal.tenantId(), principal.userId());

                modifierService.deleteModifier(principal.tenantId(), principal.userId(), id);

                return ResponseEntity.noContent().build();
        }
}
