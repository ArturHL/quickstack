package com.quickstack.app.controller;

import com.quickstack.app.security.CatalogPermissionEvaluator;
import com.quickstack.app.security.JwtAuthenticationFilter.JwtAuthenticationPrincipal;
import com.quickstack.common.dto.ApiResponse;
import com.quickstack.product.dto.request.CategoryCreateRequest;
import com.quickstack.product.dto.request.CategoryUpdateRequest;
import com.quickstack.product.dto.response.CategoryResponse;
import com.quickstack.product.service.CategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for category management.
 * <p>
 * Endpoints:
 * - GET    /api/v1/categories             - List categories (paginated)
 * - POST   /api/v1/categories             - Create category (OWNER/MANAGER)
 * - GET    /api/v1/categories/{id}        - Get single category
 * - PUT    /api/v1/categories/{id}        - Update category (OWNER/MANAGER)
 * - DELETE /api/v1/categories/{id}        - Soft delete category (OWNER/MANAGER)
 * - POST   /api/v1/categories/{id}/restore - Restore deleted category (OWNER only)
 * <p>
 * Security:
 * - All endpoints require JWT authentication
 * - Write operations require OWNER or MANAGER role
 * - CASHIER role: read-only, active categories only (includeInactive silently ignored)
 * - Tenant context is always extracted from the JWT principal, never from request params
 * <p>
 * ASVS Compliance:
 * - V4.1: Multi-tenant isolation enforced via tenantId from JWT
 * - V4.1: IDOR protection — cross-tenant access returns 404
 * - V4.2: @PreAuthorize for operation-level access control
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;
    private final CatalogPermissionEvaluator permissionEvaluator;

    public CategoryController(CategoryService categoryService,
                               CatalogPermissionEvaluator permissionEvaluator) {
        this.categoryService = categoryService;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * Lists categories for the authenticated user's tenant.
     * <p>
     * CASHIER users always receive only active categories, even if {@code includeInactive=true}
     * is passed — the parameter is silently forced to false for security.
     * OWNER and MANAGER users can request inactive categories via {@code ?includeInactive=true}.
     *
     * @param principal       the authenticated user's JWT principal
     * @param authentication  used to check role-based visibility permissions
     * @param includeInactive when true (and allowed by role), includes inactive categories
     * @param pageable        pagination and sorting parameters
     * @return paginated list of categories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> listCategories(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        // CASHIER: force includeInactive to false regardless of the param value
        boolean effectiveIncludeInactive = includeInactive
            && permissionEvaluator.canViewInactive(authentication);

        log.debug("Listing categories for tenant={}, includeInactive={}",
            principal.tenantId(), effectiveIncludeInactive);

        Page<CategoryResponse> page = categoryService.listCategories(
            principal.tenantId(), effectiveIncludeInactive, pageable);

        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Creates a new category.
     * Requires OWNER or MANAGER role.
     *
     * @param principal the authenticated user's JWT principal
     * @param request   the category creation request
     * @return 201 Created with Location header pointing to the new resource
     */
    @PostMapping
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        log.info("Creating category for tenant={}, name={}", principal.tenantId(), request.name());

        CategoryResponse response = categoryService.createCategory(
            principal.tenantId(), principal.userId(), request);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single category by ID.
     * All authenticated users can access this endpoint.
     *
     * @param principal  the authenticated user's JWT principal
     * @param categoryId the category UUID
     * @return the category details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID categoryId
    ) {
        log.debug("Getting category id={}, tenant={}", categoryId, principal.tenantId());

        CategoryResponse response = categoryService.getCategory(principal.tenantId(), categoryId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing category.
     * Requires OWNER or MANAGER role.
     *
     * @param principal  the authenticated user's JWT principal
     * @param categoryId the category UUID to update
     * @param request    the update request with fields to change
     * @return the updated category
     */
    @PutMapping("/{id}")
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        log.info("Updating category id={}, tenant={}", categoryId, principal.tenantId());

        CategoryResponse response = categoryService.updateCategory(
            principal.tenantId(), principal.userId(), categoryId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Soft-deletes a category.
     * Requires OWNER or MANAGER role.
     * Returns 409 if the category has active products.
     *
     * @param principal  the authenticated user's JWT principal
     * @param categoryId the category UUID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@catalogPermissionEvaluator.canDeleteCategory(authentication)")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID categoryId
    ) {
        log.info("Deleting category id={}, tenant={}, by={}", categoryId, principal.tenantId(), principal.userId());

        categoryService.deleteCategory(principal.tenantId(), principal.userId(), categoryId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted category.
     * Requires OWNER role only.
     *
     * @param principal  the authenticated user's JWT principal
     * @param categoryId the category UUID to restore
     * @return the restored category
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("@catalogPermissionEvaluator.canRestoreCategory(authentication)")
    public ResponseEntity<ApiResponse<CategoryResponse>> restoreCategory(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID categoryId
    ) {
        log.info("Restoring category id={}, tenant={}, by={}", categoryId, principal.tenantId(), principal.userId());

        CategoryResponse response = categoryService.restoreCategory(
            principal.tenantId(), principal.userId(), categoryId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
