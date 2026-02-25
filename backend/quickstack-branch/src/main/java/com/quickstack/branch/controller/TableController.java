package com.quickstack.branch.controller;

import com.quickstack.branch.dto.request.TableCreateRequest;
import com.quickstack.branch.dto.request.TableStatusUpdateRequest;
import com.quickstack.branch.dto.request.TableUpdateRequest;
import com.quickstack.branch.dto.response.TableResponse;
import com.quickstack.branch.service.TableService;
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
 * REST controller for table management.
 * <p>
 * Endpoints:
 * - GET    /api/v1/areas/{areaId}/tables      - List tables by area
 * - POST   /api/v1/areas/{areaId}/tables      - Create table (OWNER/MANAGER)
 * - GET    /api/v1/tables/{id}                - Get single table
 * - PUT    /api/v1/tables/{id}                - Update table (OWNER/MANAGER)
 * - DELETE /api/v1/tables/{id}                - Soft delete table (OWNER/MANAGER)
 * - PATCH  /api/v1/tables/{id}/status         - Update table status (OWNER/MANAGER)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request params
 * - V4.1: IDOR protection — cross-tenant access returns 404
 * - V4.2: @PreAuthorize for operation-level access control
 */
@RestController
public class TableController {

    private static final Logger log = LoggerFactory.getLogger(TableController.class);

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    /**
     * Lists all tables for a given area.
     */
    @GetMapping("/api/v1/areas/{areaId}/tables")
    public ResponseEntity<ApiResponse<List<TableResponse>>> listTablesByArea(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("areaId") UUID areaId
    ) {
        log.debug("Listing tables for area={}, tenant={}", areaId, principal.tenantId());
        List<TableResponse> tables = tableService.listTablesByArea(principal.tenantId(), areaId);
        return ResponseEntity.ok(ApiResponse.success(tables));
    }

    /**
     * Creates a new table within the specified area. Requires OWNER or MANAGER role.
     */
    @PostMapping("/api/v1/areas/{areaId}/tables")
    @PreAuthorize("@branchPermissionEvaluator.canManageTable(authentication)")
    public ResponseEntity<ApiResponse<TableResponse>> createTable(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("areaId") UUID areaId,
            @Valid @RequestBody TableCreateRequest request
    ) {
        log.info("Creating table for area={}, tenant={}, number={}", areaId, principal.tenantId(), request.number());

        // Override areaId from path to prevent body/path mismatch
        TableCreateRequest resolvedRequest = new TableCreateRequest(
                areaId,
                request.number(),
                request.name(),
                request.capacity(),
                request.sortOrder()
        );

        TableResponse response = tableService.createTable(
                principal.tenantId(), principal.userId(), resolvedRequest);

        URI location = URI.create("/api/v1/tables/" + response.id());

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single table by ID.
     */
    @GetMapping("/api/v1/tables/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID tableId
    ) {
        log.debug("Getting table id={}, tenant={}", tableId, principal.tenantId());
        TableResponse response = tableService.getTable(principal.tenantId(), tableId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing table. Requires OWNER or MANAGER role.
     */
    @PutMapping("/api/v1/tables/{id}")
    @PreAuthorize("@branchPermissionEvaluator.canManageTable(authentication)")
    public ResponseEntity<ApiResponse<TableResponse>> updateTable(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID tableId,
            @Valid @RequestBody TableUpdateRequest request
    ) {
        log.info("Updating table id={}, tenant={}", tableId, principal.tenantId());

        TableResponse response = tableService.updateTable(
                principal.tenantId(), principal.userId(), tableId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Soft-deletes a table. Requires OWNER or MANAGER role.
     */
    @DeleteMapping("/api/v1/tables/{id}")
    @PreAuthorize("@branchPermissionEvaluator.canManageTable(authentication)")
    public ResponseEntity<Void> deleteTable(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID tableId
    ) {
        log.info("Deleting table id={}, tenant={}, by={}", tableId, principal.tenantId(), principal.userId());

        tableService.deleteTable(principal.tenantId(), principal.userId(), tableId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the status of a table. Requires OWNER or MANAGER role.
     * Returns 200 with the updated TableResponse.
     */
    @PatchMapping("/api/v1/tables/{id}/status")
    @PreAuthorize("@branchPermissionEvaluator.canManageTable(authentication)")
    public ResponseEntity<ApiResponse<TableResponse>> updateTableStatus(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID tableId,
            @Valid @RequestBody TableStatusUpdateRequest request
    ) {
        log.info("Updating table status id={}, tenant={}, status={}", tableId, principal.tenantId(), request.status());

        TableResponse response = tableService.updateTableStatus(
                principal.tenantId(), tableId, request.status(), principal.userId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
