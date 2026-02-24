package com.quickstack.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.ReorderItem;
import com.quickstack.product.dto.request.ReorderRequest;
import com.quickstack.product.security.CatalogPermissionEvaluator;
import com.quickstack.product.service.CategoryService;
import com.quickstack.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { CategoryController.class, ProductController.class })
@ContextConfiguration(classes = { CategoryController.class, ProductController.class, CatalogPermissionEvaluator.class })
@EnableMethodSecurity
class ReorderControllerWebMvcTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CategoryService categoryService;

        @MockitoBean
        private ProductService productService;

        @MockitoBean
        private CatalogPermissionEvaluator catalogPermissionEvaluator;

        private JwtAuthenticationPrincipal managerPrincipal;
        private JwtAuthenticationPrincipal cashierPrincipal;
        private ReorderRequest validRequest;

        @BeforeEach
        void setUp() {
                UUID tenantId = UUID.randomUUID();
                managerPrincipal = new JwtAuthenticationPrincipal(UUID.randomUUID(), tenantId, UUID.randomUUID(), null,
                                "manager@test.com");
                cashierPrincipal = new JwtAuthenticationPrincipal(UUID.randomUUID(), tenantId, UUID.randomUUID(), null,
                                "cashier@test.com");

                validRequest = new ReorderRequest(List.of(
                                new ReorderItem(UUID.randomUUID(), 1)));
        }

        // --- Category Reorder Tests ---

        @Test
        void reorderCategories_Success() throws Exception {
                when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(true);
                doNothing().when(categoryService).reorderCategories(eq(managerPrincipal.tenantId()),
                                eq(managerPrincipal.userId()), anyList());

                mockMvc.perform(patch("/api/v1/categories/reorder")
                                .with(authentication(new UsernamePasswordAuthenticationToken(managerPrincipal, null,
                                                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isNoContent());

                verify(categoryService).reorderCategories(eq(managerPrincipal.tenantId()),
                                eq(managerPrincipal.userId()),
                                anyList());
        }

        @Test
        void reorderCategories_ForbiddenWithoutPermission() throws Exception {
                when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(false);

                mockMvc.perform(patch("/api/v1/categories/reorder")
                                .with(authentication(new UsernamePasswordAuthenticationToken(cashierPrincipal, null,
                                                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")))))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isForbidden());

                verify(categoryService, never()).reorderCategories(any(), any(), any());
        }

        @Test
        void reorderCategories_BadRequestWhenEmptyList() throws Exception {
                when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(true);
                ReorderRequest invalidRequest = new ReorderRequest(List.of());

                mockMvc.perform(patch("/api/v1/categories/reorder")
                                .with(authentication(new UsernamePasswordAuthenticationToken(managerPrincipal, null,
                                                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        // --- Product Reorder Tests ---

        @Test
        void reorderProducts_Success() throws Exception {
                when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(true);
                doNothing().when(productService).reorderProducts(eq(managerPrincipal.tenantId()),
                                eq(managerPrincipal.userId()),
                                anyList());

                mockMvc.perform(patch("/api/v1/products/reorder")
                                .with(authentication(new UsernamePasswordAuthenticationToken(managerPrincipal, null,
                                                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isNoContent());

                verify(productService).reorderProducts(eq(managerPrincipal.tenantId()), eq(managerPrincipal.userId()),
                                anyList());
        }

        @Test
        void reorderProducts_ForbiddenWithoutPermission() throws Exception {
                when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(false);

                mockMvc.perform(patch("/api/v1/products/reorder")
                                .with(authentication(new UsernamePasswordAuthenticationToken(cashierPrincipal, null,
                                                List.of(new SimpleGrantedAuthority("ROLE_CASHIER")))))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest)))
                                .andExpect(status().isForbidden());

                verify(productService, never()).reorderProducts(any(), any(), any());
        }

        @Test
        void reorderProducts_BadRequestWhenEmptyList() throws Exception {
                when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(true);
                ReorderRequest invalidRequest = new ReorderRequest(List.of());

                mockMvc.perform(patch("/api/v1/products/reorder")
                                .with(authentication(new UsernamePasswordAuthenticationToken(managerPrincipal, null,
                                                List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }
}
