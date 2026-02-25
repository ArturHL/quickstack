package com.quickstack.product.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.response.MenuCategoryItem;
import com.quickstack.product.dto.response.MenuProductItem;
import com.quickstack.product.dto.response.MenuResponse;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuController Unit Tests")
class MenuControllerTest {

    @Mock
    private MenuService menuService;

    private MenuController menuController;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal principal;

    @BeforeEach
    void setUp() {
        menuController = new MenuController(menuService);
        principal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, UUID.randomUUID(), null, "user@test.com");
    }

    @Test
    @DisplayName("GET /api/v1/menu returns 200 with full menu")
    void getMenuReturns200WithFullMenu() {
        MenuProductItem product = new MenuProductItem(
            UUID.randomUUID(), "Taco", new BigDecimal("25.00"),
            null, true, ProductType.SIMPLE, List.of(), List.of()
        );
        MenuCategoryItem category = new MenuCategoryItem(
            UUID.randomUUID(), "Tacos", 0, null, List.of(product)
        );
        MenuResponse expectedMenu = MenuResponse.of(List.of(category), List.of());
        when(menuService.getMenu(TENANT_ID)).thenReturn(expectedMenu);

        ResponseEntity<ApiResponse<MenuResponse>> response = menuController.getMenu(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().categories()).hasSize(1);
        assertThat(response.getBody().data().categories().get(0).name()).isEqualTo("Tacos");
        verify(menuService).getMenu(TENANT_ID);
    }

    @Test
    @DisplayName("GET /api/v1/menu extracts tenantId from principal and delegates to service")
    void getMenuExtractsTenantIdFromPrincipal() {
        UUID otherTenantId = UUID.randomUUID();
        JwtAuthenticationPrincipal otherPrincipal = new JwtAuthenticationPrincipal(
            UUID.randomUUID(), otherTenantId, UUID.randomUUID(), null, "other@test.com"
        );
        when(menuService.getMenu(otherTenantId)).thenReturn(MenuResponse.empty());

        menuController.getMenu(otherPrincipal);

        verify(menuService).getMenu(otherTenantId);
    }

    @Test
    @DisplayName("GET /api/v1/menu returns Cache-Control header")
    void getMenuReturnsCacheControlHeader() {
        when(menuService.getMenu(TENANT_ID)).thenReturn(MenuResponse.empty());

        ResponseEntity<ApiResponse<MenuResponse>> response = menuController.getMenu(principal);

        String cacheControl = response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL);
        assertThat(cacheControl).isEqualTo("max-age=30, private");
    }

    @Test
    @DisplayName("GET /api/v1/menu returns 200 with empty categories when no active products")
    void getMenuReturns200WithEmptyMenuWhenNoProducts() {
        when(menuService.getMenu(TENANT_ID)).thenReturn(MenuResponse.empty());

        ResponseEntity<ApiResponse<MenuResponse>> response = menuController.getMenu(principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().categories()).isEmpty();
        assertThat(response.getBody().data().combos()).isEmpty();
    }

    @Test
    @DisplayName("GET /api/v1/menu wraps response in ApiResponse.success wrapper")
    void getMenuWrapsResponseInApiResponseSuccess() {
        MenuResponse menu = MenuResponse.empty();
        when(menuService.getMenu(TENANT_ID)).thenReturn(menu);

        ResponseEntity<ApiResponse<MenuResponse>> response = menuController.getMenu(principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(menu);
        assertThat(response.getBody().error()).isNull();
    }

    @Test
    @DisplayName("GET /api/v1/menu returns menu with VARIANT products including variants list")
    void getMenuReturnsVariantProductsWithVariants() {
        com.quickstack.product.dto.response.MenuVariantItem variant =
            new com.quickstack.product.dto.response.MenuVariantItem(
                UUID.randomUUID(), "Grande", new BigDecimal("10.00"), new BigDecimal("60.00"), true, 0
            );
        MenuProductItem variantProduct = new MenuProductItem(
            UUID.randomUUID(), "Café", new BigDecimal("50.00"),
            null, true, ProductType.VARIANT, List.of(variant), List.of()
        );
        MenuCategoryItem category = new MenuCategoryItem(
            UUID.randomUUID(), "Bebidas", 0, null, List.of(variantProduct)
        );
        when(menuService.getMenu(TENANT_ID)).thenReturn(MenuResponse.of(List.of(category), List.of()));

        ResponseEntity<ApiResponse<MenuResponse>> response = menuController.getMenu(principal);

        MenuProductItem product = response.getBody().data().categories().get(0).products().get(0);
        assertThat(product.productType()).isEqualTo(ProductType.VARIANT);
        assertThat(product.variants()).hasSize(1);
        assertThat(product.variants().get(0).name()).isEqualTo("Grande");
    }
}
