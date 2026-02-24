package com.quickstack.product.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.response.MenuResponse;
import com.quickstack.product.service.MenuService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the public menu view consumed by the POS screen.
 * <p>
 * Endpoint:
 * - GET /api/v1/menu — returns the full active menu organized by category
 * <p>
 * Access: any authenticated user (OWNER, MANAGER, CASHIER).
 * Caching: Cache-Control max-age=30, private — safe to cache on client for 30 seconds.
 */
@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private static final String CACHE_CONTROL_VALUE = "max-age=30, private";

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * Returns the full active menu for the authenticated tenant.
     * <p>
     * Includes all active products grouped by category. Unavailable (agotado) products
     * are included but marked as such. Empty categories are excluded.
     *
     * @param principal the authenticated user principal (from JWT)
     * @return HTTP 200 with MenuResponse (empty categories list if no active products)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<MenuResponse>> getMenu(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal) {

        MenuResponse menu = menuService.getMenu(principal.tenantId());

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
            .body(ApiResponse.success(menu));
    }
}
