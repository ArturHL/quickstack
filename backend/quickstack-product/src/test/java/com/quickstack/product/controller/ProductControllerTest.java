package com.quickstack.product.controller;

import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.ProductAvailabilityRequest;
import com.quickstack.product.dto.request.ProductCreateRequest;
import com.quickstack.product.dto.request.ProductUpdateRequest;
import com.quickstack.product.dto.response.CategorySummaryResponse;
import com.quickstack.product.dto.response.ProductResponse;
import com.quickstack.product.dto.response.ProductSummaryResponse;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.security.CatalogPermissionEvaluator;
import com.quickstack.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController Unit Tests")
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private CatalogPermissionEvaluator permissionEvaluator;

    private ProductController productController;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal principal;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        productController = new ProductController(productService, permissionEvaluator);
        principal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, UUID.randomUUID(), null, "owner@test.com");
        auth = new UsernamePasswordAuthenticationToken(principal, null, 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Nested
    @DisplayName("listProducts")
    class ListProductsTests {
        @Test
        @DisplayName("should return 200 with page of products")
        void shouldReturn200WithProducts() {
            ProductSummaryResponse summary = new ProductSummaryResponse(
                PRODUCT_ID, "Coca Cola", new BigDecimal("15.00"),
                ProductType.SIMPLE, true, true, CATEGORY_ID, null, 0
            );
            Page<ProductSummaryResponse> page = new PageImpl<>(List.of(summary));
            
            org.mockito.Mockito.lenient().when(permissionEvaluator.canViewInactive(any())).thenReturn(false);
            when(productService.listProducts(any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(page);

            ResponseEntity<?> result = productController.listProducts(
                principal, auth, CATEGORY_ID, true, "coca", false, PageRequest.of(0, 10));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(productService).listProducts(eq(TENANT_ID), eq(CATEGORY_ID), eq(true), eq("coca"), eq(false), any());
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProductTests {
        @Test
        @DisplayName("should return 201 when creation succeeds")
        void shouldReturn201WhenCreated() {
            ProductCreateRequest request = new ProductCreateRequest(
                "Coca Cola", null, CATEGORY_ID, "COCA-001",
                new BigDecimal("15.00"), null, null, ProductType.SIMPLE, null, null
            );
            ProductResponse response = buildProductResponse("Coca Cola");
            
            when(productService.createProduct(eq(TENANT_ID), eq(USER_ID), eq(request))).thenReturn(response);
            mockServletRequestContext();

            ResponseEntity<?> result = productController.createProduct(principal, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(productService).createProduct(TENANT_ID, USER_ID, request);
        }

        private void mockServletRequestContext() {
            org.springframework.mock.web.MockHttpServletRequest mockRequest =
                new org.springframework.mock.web.MockHttpServletRequest();
            mockRequest.setScheme("http");
            mockRequest.setServerName("localhost");
            mockRequest.setServerPort(8080);
            mockRequest.setRequestURI("/api/v1/products");
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(mockRequest)
            );
        }
    }

    @Nested
    @DisplayName("getProduct")
    class GetProductTests {
        @Test
        @DisplayName("should return 200 with product details")
        void shouldReturn200WithProduct() {
            ProductResponse response = buildProductResponse("Coca Cola");
            when(productService.getProduct(TENANT_ID, PRODUCT_ID)).thenReturn(response);

            ResponseEntity<?> result = productController.getProduct(principal, PRODUCT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("setAvailability")
    class SetAvailabilityTests {
        @Test
        @DisplayName("should return 200 with updated product")
        void shouldReturn200WhenUpdated() {
            ProductAvailabilityRequest request = new ProductAvailabilityRequest(false);
            ProductResponse response = buildProductResponse("Coca Cola");
            
            when(productService.setAvailability(TENANT_ID, USER_ID, PRODUCT_ID, false)).thenReturn(response);

            ResponseEntity<?> result = productController.setAvailability(principal, PRODUCT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(productService).setAvailability(TENANT_ID, USER_ID, PRODUCT_ID, false);
        }
    }

    private ProductResponse buildProductResponse(String name) {
        return new ProductResponse(
            PRODUCT_ID, name, null, null, new BigDecimal("15.00"),
            null, null, ProductType.SIMPLE, true, true, 0,
            new CategorySummaryResponse(CATEGORY_ID, "Bebidas", 0, true, null),
            List.of()
        );
    }
}
