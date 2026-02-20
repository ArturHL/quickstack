package com.quickstack.app.controller;

import com.quickstack.app.security.CatalogPermissionEvaluator;
import com.quickstack.app.security.JwtAuthenticationFilter.JwtAuthenticationPrincipal;
import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.CategoryCreateRequest;
import com.quickstack.product.dto.request.CategoryUpdateRequest;
import com.quickstack.product.dto.response.CategoryResponse;
import com.quickstack.product.service.CategoryService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryController.
 * <p>
 * Tests controller logic in isolation: tenant extraction, parameter handling,
 * service delegation, and response mapping. Security enforcement (@PreAuthorize)
 * is tested indirectly via the permissionEvaluator mock.
 * <p>
 * Integration tests (CategoryIntegrationTest) verify the full security filter chain.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private CatalogPermissionEvaluator permissionEvaluator;

    private CategoryController categoryController;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal ownerPrincipal;
    private Authentication ownerAuth;
    private Authentication cashierAuth;

    @BeforeEach
    void setUp() {
        categoryController = new CategoryController(categoryService, permissionEvaluator);
        ownerPrincipal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "owner@test.com");
        ownerAuth = buildAuth(ownerPrincipal);
        cashierAuth = buildAuth(new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "cashier@test.com"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/categories
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listCategories")
    class ListCategoriesTests {

        @Test
        @DisplayName("should return 200 with page of categories")
        void shouldReturn200WithCategories() {
            CategoryResponse response = buildCategoryResponse("Bebidas", true);
            Page<CategoryResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
            // Use lenient to allow stubs that may not be consumed in all execution paths
            lenient().when(permissionEvaluator.canViewInactive(any())).thenReturn(false);
            lenient().when(categoryService.listCategories(any(), anyBoolean(), any())).thenReturn(page);

            ResponseEntity<?> result = categoryController.listCategories(
                ownerPrincipal, ownerAuth, false, PageRequest.of(0, 20));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should force includeInactive to false when user cannot view inactive")
        void shouldForceIncludeInactiveFalseWhenNotPermitted() {
            when(permissionEvaluator.canViewInactive(any())).thenReturn(false);
            when(categoryService.listCategories(eq(TENANT_ID), eq(false), any()))
                .thenReturn(Page.empty());

            categoryController.listCategories(
                ownerPrincipal, cashierAuth, true, PageRequest.of(0, 20));

            verify(categoryService).listCategories(eq(TENANT_ID), eq(false), any());
        }

        @Test
        @DisplayName("should pass includeInactive=true when user has view inactive permission")
        void shouldPassIncludeInactiveTrueWhenPermitted() {
            when(permissionEvaluator.canViewInactive(any())).thenReturn(true);
            when(categoryService.listCategories(eq(TENANT_ID), eq(true), any()))
                .thenReturn(Page.empty());

            categoryController.listCategories(
                ownerPrincipal, ownerAuth, true, PageRequest.of(0, 20));

            verify(categoryService).listCategories(eq(TENANT_ID), eq(true), any());
        }

        @Test
        @DisplayName("should use tenantId from JWT principal, not from request")
        void shouldUseTenantIdFromPrincipal() {
            Page<CategoryResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            lenient().when(permissionEvaluator.canViewInactive(any())).thenReturn(false);
            lenient().when(categoryService.listCategories(any(), anyBoolean(), any())).thenReturn(emptyPage);

            categoryController.listCategories(
                ownerPrincipal, ownerAuth, false, PageRequest.of(0, 20));

            verify(categoryService).listCategories(eq(TENANT_ID), anyBoolean(), any());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/categories
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("should delegate to service with tenantId and userId from JWT")
        void shouldDelegateWithCorrectIds() {
            CategoryCreateRequest request = new CategoryCreateRequest("Comidas", null, null, null, null);
            CategoryResponse response = buildCategoryResponse("Comidas", true);
            when(categoryService.createCategory(TENANT_ID, USER_ID, request)).thenReturn(response);
            mockServletRequestContext();

            categoryController.createCategory(ownerPrincipal, request);

            verify(categoryService).createCategory(TENANT_ID, USER_ID, request);
        }

        @Test
        @DisplayName("should return 201 with Location header after service call")
        void shouldReturn201WithCreatedCategory() {
            CategoryCreateRequest request = new CategoryCreateRequest("Bebidas", null, null, null, null);
            CategoryResponse response = buildCategoryResponse("Bebidas", true);
            when(categoryService.createCategory(TENANT_ID, USER_ID, request)).thenReturn(response);
            mockServletRequestContext();

            ResponseEntity<?> result = categoryController.createCategory(ownerPrincipal, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("should propagate DuplicateResourceException from service")
        void shouldPropagateDuplicateException() {
            CategoryCreateRequest request = new CategoryCreateRequest("Bebidas", null, null, null, null);
            when(categoryService.createCategory(any(), any(), any()))
                .thenThrow(new DuplicateResourceException("Category", "name", "Bebidas"));
            mockServletRequestContext();

            assertThatThrownBy(() -> categoryController.createCategory(ownerPrincipal, request))
                .isInstanceOf(DuplicateResourceException.class);
        }

        private void mockServletRequestContext() {
            // ServletUriComponentsBuilder.fromCurrentRequest() requires an active HTTP request.
            // We set a minimal mock request so the builder can construct a URI.
            org.springframework.mock.web.MockHttpServletRequest mockRequest =
                new org.springframework.mock.web.MockHttpServletRequest();
            mockRequest.setScheme("http");
            mockRequest.setServerName("localhost");
            mockRequest.setServerPort(8080);
            mockRequest.setRequestURI("/api/v1/categories");
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(mockRequest)
            );
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/categories/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getCategory")
    class GetCategoryTests {

        @Test
        @DisplayName("should return 200 with category details")
        void shouldReturn200WithCategoryDetails() {
            CategoryResponse response = buildCategoryResponse("Bebidas", true);
            when(categoryService.getCategory(TENANT_ID, CATEGORY_ID)).thenReturn(response);

            ResponseEntity<?> result = categoryController.getCategory(ownerPrincipal, CATEGORY_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException for cross-tenant access")
        void shouldPropagateNotFoundForCrossTenant() {
            when(categoryService.getCategory(TENANT_ID, CATEGORY_ID))
                .thenThrow(new ResourceNotFoundException("Category", CATEGORY_ID));

            assertThatThrownBy(() -> categoryController.getCategory(ownerPrincipal, CATEGORY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should use tenantId from JWT, not from path variable")
        void shouldUseTenantIdFromJwt() {
            when(categoryService.getCategory(TENANT_ID, CATEGORY_ID))
                .thenReturn(buildCategoryResponse("Bebidas", true));

            categoryController.getCategory(ownerPrincipal, CATEGORY_ID);

            verify(categoryService).getCategory(TENANT_ID, CATEGORY_ID);
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/categories/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("should return 200 with updated category")
        void shouldReturn200WithUpdatedCategory() {
            CategoryUpdateRequest request = new CategoryUpdateRequest("Drinks", null, null, null, null, null);
            CategoryResponse response = buildCategoryResponse("Drinks", true);
            when(categoryService.updateCategory(TENANT_ID, USER_ID, CATEGORY_ID, request))
                .thenReturn(response);

            ResponseEntity<?> result = categoryController.updateCategory(ownerPrincipal, CATEGORY_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("should delegate with correct tenantId and userId from JWT")
        void shouldDelegateWithCorrectIds() {
            CategoryUpdateRequest request = new CategoryUpdateRequest("Drinks", null, null, null, null, null);
            when(categoryService.updateCategory(any(), any(), any(), any()))
                .thenReturn(buildCategoryResponse("Drinks", true));

            categoryController.updateCategory(ownerPrincipal, CATEGORY_ID, request);

            verify(categoryService).updateCategory(TENANT_ID, USER_ID, CATEGORY_ID, request);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/categories/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("should return 204 when deletion succeeds")
        void shouldReturn204WhenDeleted() {
            doNothing().when(categoryService).deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID);

            ResponseEntity<Void> result = categoryController.deleteCategory(ownerPrincipal, CATEGORY_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("should propagate BusinessRuleException when category has products")
        void shouldPropagateBusinessRuleException() {
            doThrow(new BusinessRuleException("CATEGORY_HAS_PRODUCTS", "Has 3 active products"))
                .when(categoryService).deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID);

            assertThatThrownBy(() -> categoryController.deleteCategory(ownerPrincipal, CATEGORY_ID))
                .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("should delegate with correct tenantId and userId from JWT")
        void shouldDelegateWithCorrectIds() {
            doNothing().when(categoryService).deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID);

            categoryController.deleteCategory(ownerPrincipal, CATEGORY_ID);

            verify(categoryService).deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID);
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/categories/{id}/restore
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("restoreCategory")
    class RestoreCategoryTests {

        @Test
        @DisplayName("should return 200 with restored category")
        void shouldReturn200WhenRestored() {
            CategoryResponse response = buildCategoryResponse("Bebidas", true);
            when(categoryService.restoreCategory(TENANT_ID, USER_ID, CATEGORY_ID)).thenReturn(response);

            ResponseEntity<?> result = categoryController.restoreCategory(ownerPrincipal, CATEGORY_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("should propagate ResourceNotFoundException when category not found")
        void shouldPropagateNotFound() {
            when(categoryService.restoreCategory(TENANT_ID, USER_ID, CATEGORY_ID))
                .thenThrow(new ResourceNotFoundException("Category", CATEGORY_ID));

            assertThatThrownBy(() -> categoryController.restoreCategory(ownerPrincipal, CATEGORY_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should delegate with correct tenantId and userId from JWT")
        void shouldDelegateWithCorrectIds() {
            when(categoryService.restoreCategory(any(), any(), any()))
                .thenReturn(buildCategoryResponse("Bebidas", true));

            categoryController.restoreCategory(ownerPrincipal, CATEGORY_ID);

            verify(categoryService).restoreCategory(TENANT_ID, USER_ID, CATEGORY_ID);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Authentication buildAuth(JwtAuthenticationPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(
            principal, null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private CategoryResponse buildCategoryResponse(String name, boolean isActive) {
        return new CategoryResponse(
            CATEGORY_ID,
            TENANT_ID,
            null,
            name,
            null,
            null,
            0,
            isActive,
            0,
            Instant.now(),
            Instant.now(),
            USER_ID,
            null
        );
    }
}
