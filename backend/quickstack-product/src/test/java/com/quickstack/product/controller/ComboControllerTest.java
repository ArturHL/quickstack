package com.quickstack.product.controller;

import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.ComboCreateRequest;
import com.quickstack.product.dto.request.ComboItemRequest;
import com.quickstack.product.dto.request.ComboUpdateRequest;
import com.quickstack.product.dto.response.ComboItemResponse;
import com.quickstack.product.dto.response.ComboResponse;
import com.quickstack.product.service.ComboService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ComboController.
 * Tests controller logic in isolation: tenant extraction, delegation, response
 * mapping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComboController")
class ComboControllerTest {

    @Mock
    private ComboService comboService;

    private ComboController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID COMBO_ID = UUID.randomUUID();
    private static final UUID PRODUCT_A_ID = UUID.randomUUID();
    private static final UUID PRODUCT_B_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal ownerPrincipal;

    @BeforeEach
    void setUp() {
        controller = new ComboController(comboService);
        ownerPrincipal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "owner@test.com");
        mockServletContext();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/combos
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listCombos")
    class ListCombosTests {

        @Test
        @DisplayName("Should return 200 with list of combos")
        void shouldReturn200WithCombos() {
            when(comboService.listCombos(TENANT_ID)).thenReturn(List.of(buildComboResponse()));

            ResponseEntity<?> result = controller.listCombos(ownerPrincipal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should use tenantId from JWT")
        void shouldUseTenantIdFromJwt() {
            when(comboService.listCombos(TENANT_ID)).thenReturn(List.of());

            controller.listCombos(ownerPrincipal);

            verify(comboService).listCombos(eq(TENANT_ID));
        }

        @Test
        @DisplayName("Should return empty list when no combos exist")
        void shouldReturnEmptyList() {
            when(comboService.listCombos(TENANT_ID)).thenReturn(List.of());

            ResponseEntity<?> result = controller.listCombos(ownerPrincipal);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/combos
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createCombo")
    class CreateComboTests {

        @Test
        @DisplayName("Should return 201 with Location header")
        void shouldReturn201WithLocation() {
            when(comboService.createCombo(eq(TENANT_ID), eq(USER_ID), any()))
                    .thenReturn(buildComboResponse());

            ComboCreateRequest request = buildCreateRequest();

            ResponseEntity<?> result = controller.createCombo(ownerPrincipal, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getLocation()).isNotNull();
            assertThat(result.getHeaders().getLocation().toString()).contains("/api/v1/combos/");
        }

        @Test
        @DisplayName("Should delegate to service with tenantId and userId from JWT")
        void shouldDelegateWithJwtIdentity() {
            when(comboService.createCombo(any(), any(), any())).thenReturn(buildComboResponse());

            controller.createCombo(ownerPrincipal, buildCreateRequest());

            verify(comboService).createCombo(eq(TENANT_ID), eq(USER_ID), any());
        }

        @Test
        @DisplayName("Should propagate DuplicateResourceException when name exists")
        void shouldPropagateDuplicateException() {
            when(comboService.createCombo(any(), any(), any()))
                    .thenThrow(new DuplicateResourceException("Combo", "name", "Combo 1"));

            assertThatThrownBy(() -> controller.createCombo(ownerPrincipal, buildCreateRequest()))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when product not found")
        void shouldPropagateNotFoundWhenProductNotFound() {
            when(comboService.createCombo(any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Product", PRODUCT_A_ID));

            assertThatThrownBy(() -> controller.createCombo(ownerPrincipal, buildCreateRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/combos/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getCombo")
    class GetComboTests {

        @Test
        @DisplayName("Should return 200 with combo details and items")
        void shouldReturn200WithComboDetails() {
            when(comboService.getCombo(TENANT_ID, COMBO_ID)).thenReturn(buildComboResponse());

            ResponseEntity<?> result = controller.getCombo(ownerPrincipal, COMBO_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException for cross-tenant access")
        void shouldPropagateNotFoundForCrossTenant() {
            when(comboService.getCombo(TENANT_ID, COMBO_ID))
                    .thenThrow(new ResourceNotFoundException("Combo", COMBO_ID));

            assertThatThrownBy(() -> controller.getCombo(ownerPrincipal, COMBO_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/combos/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateCombo")
    class UpdateComboTests {

        @Test
        @DisplayName("Should return 200 with updated combo")
        void shouldReturn200WithUpdatedCombo() {
            when(comboService.updateCombo(eq(TENANT_ID), eq(USER_ID), eq(COMBO_ID), any()))
                    .thenReturn(buildComboResponse());

            ComboUpdateRequest request = new ComboUpdateRequest("Nuevo Nombre", null, null, null, null, null, null);

            ResponseEntity<?> result = controller.updateCombo(ownerPrincipal, COMBO_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should delegate comboId and JWT identity to service")
        void shouldDelegateCorrectParams() {
            when(comboService.updateCombo(any(), any(), any(), any())).thenReturn(buildComboResponse());

            controller.updateCombo(ownerPrincipal, COMBO_ID,
                    new ComboUpdateRequest(null, null, null, null, null, null, null));

            verify(comboService).updateCombo(eq(TENANT_ID), eq(USER_ID), eq(COMBO_ID), any());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/combos/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteCombo")
    class DeleteComboTests {

        @Test
        @DisplayName("Should return 204 on successful deletion")
        void shouldReturn204OnDeletion() {
            doNothing().when(comboService).deleteCombo(TENANT_ID, USER_ID, COMBO_ID);

            ResponseEntity<Void> result = controller.deleteCombo(ownerPrincipal, COMBO_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when combo not found")
        void shouldPropagateNotFound() {
            doThrow(new ResourceNotFoundException("Combo", COMBO_ID))
                    .when(comboService).deleteCombo(TENANT_ID, USER_ID, COMBO_ID);

            assertThatThrownBy(() -> controller.deleteCombo(ownerPrincipal, COMBO_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void mockServletContext() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("http");
        mockRequest.setServerName("localhost");
        mockRequest.setServerPort(8080);
        mockRequest.setRequestURI("/api/v1/combos");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    private ComboCreateRequest buildCreateRequest() {
        return new ComboCreateRequest(
                "Combo 1", null, null, new BigDecimal("99.00"),
                List.of(
                        new ComboItemRequest(PRODUCT_A_ID, 1, null, null, null),
                        new ComboItemRequest(PRODUCT_B_ID, 1, null, null, null)),
                null);
    }

    private ComboResponse buildComboResponse() {
        List<ComboItemResponse> items = List.of(
                new ComboItemResponse(UUID.randomUUID(), PRODUCT_A_ID, "Hamburguesa", 1, false, null, 0),
                new ComboItemResponse(UUID.randomUUID(), PRODUCT_B_ID, "Refresco", 1, false, null, 1));
        return new ComboResponse(
                COMBO_ID, TENANT_ID, "Combo 1", null, null,
                new BigDecimal("99.00"), true, 0, Instant.now(), Instant.now(), items);
    }
}
