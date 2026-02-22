package com.quickstack.product.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.ProductAvailabilityRequest;
import com.quickstack.product.dto.request.ProductCreateRequest;
import com.quickstack.product.dto.request.ProductUpdateRequest;
import com.quickstack.product.dto.request.ReorderRequest;
import com.quickstack.product.dto.response.ProductResponse;
import com.quickstack.product.dto.response.ProductSummaryResponse;
import com.quickstack.product.security.CatalogPermissionEvaluator;
import com.quickstack.product.service.ProductService;
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
 * REST controller for product management.
 * <p>
 * Endpoints:
 * - GET    /api/v1/products              - List products (paginated, with filters)
 * - POST   /api/v1/products              - Create product (OWNER/MANAGER)
 * - GET    /api/v1/products/{id}         - Get single product (with variants)
 * - PUT    /api/v1/products/{id}         - Update product (OWNER/MANAGER)
 * - DELETE /api/v1/products/{id}         - Soft delete product (OWNER/MANAGER)
 * - PATCH  /api/v1/products/{id}/availability - Toggle availability (OWNER/MANAGER)
 * - POST   /api/v1/products/{id}/restore - Restore deleted product (OWNER only)
 * <p>
 * ASVS Compliance:
 * - V4.1: Multi-tenant isolation — tenantId always extracted from JWT
 * - V4.1: IDOR protection — cross-tenant access returns 404
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final CatalogPermissionEvaluator permissionEvaluator;

    public ProductController(ProductService productService,
                              CatalogPermissionEvaluator permissionEvaluator) {
        this.productService = productService;
        this.permissionEvaluator = permissionEvaluator;
    }

    /**
     * Lists products for the authenticated user's tenant with optional filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> listProducts(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            Authentication authentication,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        boolean effectiveIncludeInactive = includeInactive
            && permissionEvaluator.canViewInactive(authentication);

        log.debug("Listing products for tenant={}, category={}, search={}",
            principal.tenantId(), categoryId, search);

        Page<ProductSummaryResponse> page = productService.listProducts(
            principal.tenantId(), categoryId, available, search, effectiveIncludeInactive, pageable);

        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Creates a new product. Requires OWNER or MANAGER role.
     */
    @PostMapping
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody ProductCreateRequest request
    ) {
        log.info("Creating product for tenant={}, name={}", principal.tenantId(), request.name());

        ProductResponse response = productService.createProduct(
            principal.tenantId(), principal.userId(), request);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Retrieves a single product by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID productId
    ) {
        log.debug("Getting product id={}, tenant={}", productId, principal.tenantId());

        ProductResponse response = productService.getProduct(principal.tenantId(), productId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates an existing product. Requires OWNER or MANAGER role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        log.info("Updating product id={}, tenant={}", productId, principal.tenantId());

        ProductResponse response = productService.updateProduct(
            principal.tenantId(), principal.userId(), productId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Reorders products. Requires OWNER or MANAGER role.
     * Validates that all product IDs provided belong to the current tenant.
     */
    @PatchMapping("/reorder")
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<Void> reorderProducts(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody ReorderRequest request
    ) {
        log.info("Reordering products for tenant={}, by={}", principal.tenantId(), principal.userId());

        productService.reorderProducts(principal.tenantId(), principal.userId(), request.items());

        return ResponseEntity.noContent().build();
    }

    /**
     * Soft-deletes a product. Requires OWNER or MANAGER role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@catalogPermissionEvaluator.canDeleteProduct(authentication)")
    public ResponseEntity<Void> deleteProduct(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID productId
    ) {
        log.info("Deleting product id={}, tenant={}, by={}", productId, principal.tenantId(), principal.userId());

        productService.deleteProduct(principal.tenantId(), principal.userId(), productId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Toggles availability of a product. Requires OWNER or MANAGER role.
     */
    @PatchMapping("/{id}/availability")
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<ApiResponse<ProductResponse>> setAvailability(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID productId,
            @Valid @RequestBody ProductAvailabilityRequest request
    ) {
        log.info("Setting availability for product id={}, status={}", productId, request.isAvailable());

        ProductResponse response = productService.setAvailability(
            principal.tenantId(), principal.userId(), productId, request.isAvailable());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Restores a soft-deleted product. Requires OWNER role only.
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("@catalogPermissionEvaluator.canRestoreProduct(authentication)")
    public ResponseEntity<ApiResponse<ProductResponse>> restoreProduct(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID productId
    ) {
        log.info("Restoring product id={}, tenant={}, by={}", productId, principal.tenantId(), principal.userId());

        ProductResponse response = productService.restoreProduct(
            principal.tenantId(), principal.userId(), productId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
