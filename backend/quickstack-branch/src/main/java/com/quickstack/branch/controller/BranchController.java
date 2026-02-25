package com.quickstack.branch.controller;

import com.quickstack.branch.dto.request.BranchCreateRequest;
import com.quickstack.branch.dto.request.BranchUpdateRequest;
import com.quickstack.branch.dto.response.BranchResponse;
import com.quickstack.branch.service.BranchService;
import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
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
 * REST controller for branch management.
 * <p>
 * Endpoints:
 * - GET    /api/v1/branches        - List all branches (any authenticated user)
 * - POST   /api/v1/branches        - Create branch (OWNER only)
 * - GET    /api/v1/branches/{id}   - Get single branch (any authenticated user)
 * - PUT    /api/v1/branches/{id}   - Update branch (OWNER only)
 * - DELETE /api/v1/branches/{id}   - Soft delete branch (OWNER only)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request params
 * - V4.1: IDOR protection — cross-tenant access returns 404
 * - V4.2: @PreAuthorize for operation-level access control
 */
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {

    private static final Logger log = LoggerFactory.getLogger(BranchController.class);

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    /**
     * Lists all branches for the authenticated user's tenant.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BranchResponse>>> listBranches(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal
    ) {
        log.debug("Listing branches for tenant={}", principal.tenantId());
        List<BranchResponse> branches = branchService.listBranches(principal.tenantId());
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    /**
     * Creates a new branch. Requires OWNER role.
     */
    @PostMapping
    @PreAuthorize("@branchPermissionEvaluator.canManageBranch(authentication)")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody BranchCreateRequest request
    ) {
        log.info("Creating branch for tenant={}, name={}", principal.tenantId(), request.name());

        BranchResponse response = branchService.createBranch(
                principal.tenantId(), principal.userId(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single branch by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranch(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID branchId
    ) {
        log.debug("Getting branch id={}, tenant={}", branchId, principal.tenantId());
        BranchResponse response = branchService.getBranch(principal.tenantId(), branchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing branch. Requires OWNER role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@branchPermissionEvaluator.canManageBranch(authentication)")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID branchId,
            @Valid @RequestBody BranchUpdateRequest request
    ) {
        log.info("Updating branch id={}, tenant={}", branchId, principal.tenantId());

        BranchResponse response = branchService.updateBranch(
                principal.tenantId(), principal.userId(), branchId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Soft-deletes a branch. Requires OWNER role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@branchPermissionEvaluator.canManageBranch(authentication)")
    public ResponseEntity<Void> deleteBranch(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID branchId
    ) {
        log.info("Deleting branch id={}, tenant={}, by={}", branchId, principal.tenantId(), principal.userId());

        branchService.deleteBranch(principal.tenantId(), principal.userId(), branchId);

        return ResponseEntity.noContent().build();
    }
}
