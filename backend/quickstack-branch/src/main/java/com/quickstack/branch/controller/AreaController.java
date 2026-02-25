package com.quickstack.branch.controller;

import com.quickstack.branch.dto.request.AreaCreateRequest;
import com.quickstack.branch.dto.request.AreaUpdateRequest;
import com.quickstack.branch.dto.response.AreaResponse;
import com.quickstack.branch.service.AreaService;
import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for area management.
 * <p>
 * Endpoints:
 * - GET    /api/v1/branches/{branchId}/areas  - List areas by branch
 * - POST   /api/v1/branches/{branchId}/areas  - Create area (OWNER/MANAGER)
 * - GET    /api/v1/areas/{id}                 - Get single area
 * - PUT    /api/v1/areas/{id}                 - Update area (OWNER/MANAGER)
 * - DELETE /api/v1/areas/{id}                 - Soft delete area (OWNER/MANAGER)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request params
 * - V4.1: IDOR protection — cross-tenant access returns 404
 * - V4.2: @PreAuthorize for operation-level access control
 */
@RestController
public class AreaController {

    private static final Logger log = LoggerFactory.getLogger(AreaController.class);

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    /**
     * Lists all areas for a given branch.
     */
    @GetMapping("/api/v1/branches/{branchId}/areas")
    public ResponseEntity<ApiResponse<List<AreaResponse>>> listAreasByBranch(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("branchId") UUID branchId
    ) {
        log.debug("Listing areas for branch={}, tenant={}", branchId, principal.tenantId());
        List<AreaResponse> areas = areaService.listAreasByBranch(principal.tenantId(), branchId);
        return ResponseEntity.ok(ApiResponse.success(areas));
    }

    /**
     * Creates a new area within the specified branch. Requires OWNER or MANAGER role.
     */
    @PostMapping("/api/v1/branches/{branchId}/areas")
    @PreAuthorize("@branchPermissionEvaluator.canManageArea(authentication)")
    public ResponseEntity<ApiResponse<AreaResponse>> createArea(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("branchId") UUID branchId,
            @Valid @RequestBody AreaCreateRequest request
    ) {
        log.info("Creating area for branch={}, tenant={}, name={}", branchId, principal.tenantId(), request.name());

        // Override branchId from path to prevent body/path mismatch
        AreaCreateRequest resolvedRequest = new AreaCreateRequest(
                branchId,
                request.name(),
                request.description(),
                request.sortOrder()
        );

        AreaResponse response = areaService.createArea(
                principal.tenantId(), principal.userId(), resolvedRequest);

        URI location = URI.create("/api/v1/areas/" + response.id());

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single area by ID.
     */
    @GetMapping("/api/v1/areas/{id}")
    public ResponseEntity<ApiResponse<AreaResponse>> getArea(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID areaId
    ) {
        log.debug("Getting area id={}, tenant={}", areaId, principal.tenantId());
        AreaResponse response = areaService.getArea(principal.tenantId(), areaId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing area. Requires OWNER or MANAGER role.
     */
    @PutMapping("/api/v1/areas/{id}")
    @PreAuthorize("@branchPermissionEvaluator.canManageArea(authentication)")
    public ResponseEntity<ApiResponse<AreaResponse>> updateArea(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID areaId,
            @Valid @RequestBody AreaUpdateRequest request
    ) {
        log.info("Updating area id={}, tenant={}", areaId, principal.tenantId());

        AreaResponse response = areaService.updateArea(
                principal.tenantId(), principal.userId(), areaId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Soft-deletes an area. Requires OWNER or MANAGER role.
     */
    @DeleteMapping("/api/v1/areas/{id}")
    @PreAuthorize("@branchPermissionEvaluator.canManageArea(authentication)")
    public ResponseEntity<Void> deleteArea(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID areaId
    ) {
        log.info("Deleting area id={}, tenant={}, by={}", areaId, principal.tenantId(), principal.userId());

        areaService.deleteArea(principal.tenantId(), principal.userId(), areaId);

        return ResponseEntity.noContent().build();
    }
}
