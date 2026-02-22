package com.quickstack.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickstack.product.security.CatalogPermissionEvaluator;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.controller.VariantController;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.dto.request.VariantUpdateRequest;
import com.quickstack.product.dto.response.VariantResponse;
import com.quickstack.product.service.VariantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = VariantController.class)
@ContextConfiguration(classes = {VariantController.class, CatalogPermissionEvaluator.class})
@EnableMethodSecurity
// SecurityConfig import might be needed if custom exception handling is active,
// but for WebMvcTest, we try to focus on the controller logic.
class VariantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VariantService variantService;
    
    @MockBean
    private CatalogPermissionEvaluator catalogPermissionEvaluator;

    private JwtAuthenticationPrincipal principal;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        principal = new JwtAuthenticationPrincipal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, "manager@test.com");
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
    }

    @Test
    void listVariants_Success() throws Exception {
        VariantResponse res1 = new VariantResponse(UUID.randomUUID(), "Chico", "SKU1", BigDecimal.ZERO, BigDecimal.TEN, true, true, 1);
        
        when(variantService.listVariants(principal.tenantId(), productId)).thenReturn(List.of(res1));

        mockMvc.perform(get("/api/v1/products/{productId}/variants", productId)
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Chico"));
    }

    @Test
    void addVariant_Success() throws Exception {
        VariantCreateRequest req = new VariantCreateRequest("Medio", "SKU2", BigDecimal.TEN, false, 2);
        VariantResponse res = new VariantResponse(variantId, "Medio", "SKU2", BigDecimal.TEN, new BigDecimal("20.00"), false, true, 2);
        
        when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(true);
        when(variantService.addVariant(eq(principal.tenantId()), eq(principal.userId()), eq(productId), any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/products/{productId}/variants", productId)
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(variantId.toString()))
                .andExpect(jsonPath("$.name").value("Medio"));
    }

    @Test
    void addVariant_ForbiddenWithoutPermission() throws Exception {
        VariantCreateRequest req = new VariantCreateRequest("Medio", "SKU2", BigDecimal.TEN, false, 2);
        JwtAuthenticationPrincipal cashierPrincipal = new JwtAuthenticationPrincipal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, "cashier@test.com");
        
        when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(false);

        mockMvc.perform(post("/api/v1/products/{productId}/variants", productId)
                .with(authentication(new UsernamePasswordAuthenticationToken(cashierPrincipal, null, List.of(new SimpleGrantedAuthority("ROLE_CASHIER")))))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateVariant_Success() throws Exception {
        VariantUpdateRequest req = new VariantUpdateRequest("Grande", null, null, null, null, null);
        VariantResponse res = new VariantResponse(variantId, "Grande", "SKU-GR", new BigDecimal("20.00"), new BigDecimal("30.00"), false, true, 3);
        
        when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(true);
        when(variantService.updateVariant(eq(principal.tenantId()), eq(principal.userId()), eq(productId), eq(variantId), any())).thenReturn(res);

        mockMvc.perform(put("/api/v1/products/{productId}/variants/{variantId}", productId, variantId)
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grande"));
    }

    @Test
    void deleteVariant_Success() throws Exception {
        when(catalogPermissionEvaluator.canManageCatalog(any())).thenReturn(true);
        
        mockMvc.perform(delete("/api/v1/products/{productId}/variants/{variantId}", productId, variantId)
                .with(authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")))))
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(variantService).deleteVariant(principal.tenantId(), principal.userId(), productId, variantId);
    }
}
