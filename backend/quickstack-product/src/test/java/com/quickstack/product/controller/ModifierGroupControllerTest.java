package com.quickstack.product.controller;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.ModifierCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupUpdateRequest;
import com.quickstack.product.dto.request.ModifierUpdateRequest;
import com.quickstack.product.dto.response.ModifierGroupResponse;
import com.quickstack.product.dto.response.ModifierResponse;
import com.quickstack.product.service.ModifierGroupService;
import com.quickstack.product.service.ModifierService;
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
 * Unit tests for ModifierGroupController.
 * Tests controller logic in isolation: tenant extraction, delegation, response
 * mapping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModifierGroupController")
class ModifierGroupControllerTest {

    @Mock
    private ModifierGroupService modifierGroupService;
    @Mock
    private ModifierService modifierService;
    @Mock

    private ModifierGroupController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID MODIFIER_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal ownerPrincipal;

    @BeforeEach
    void setUp() {
        controller = new ModifierGroupController(modifierGroupService, modifierService);
        ownerPrincipal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "owner@test.com");
        mockServletContext();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/products/{productId}/modifier-groups
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listModifierGroups")
    class ListModifierGroupsTests {

        @Test
        @DisplayName("Should return 200 with list of modifier groups")
        void shouldReturn200WithGroups() {
            when(modifierGroupService.listModifierGroupsByProduct(TENANT_ID, PRODUCT_ID))
                    .thenReturn(List.of(buildGroupResponse()));

            ResponseEntity<?> result = controller.listModifierGroups(ownerPrincipal, PRODUCT_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should use tenantId from JWT, not from path")
        void shouldUseTenantIdFromJwt() {
            when(modifierGroupService.listModifierGroupsByProduct(TENANT_ID, PRODUCT_ID))
                    .thenReturn(List.of());

            controller.listModifierGroups(ownerPrincipal, PRODUCT_ID);

            verify(modifierGroupService).listModifierGroupsByProduct(eq(TENANT_ID), eq(PRODUCT_ID));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/products/{productId}/modifier-groups
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createModifierGroup")
    class CreateModifierGroupTests {

        @Test
        @DisplayName("Should return 201 with Location header")
        void shouldReturn201WithLocation() {
            when(modifierGroupService.createModifierGroup(eq(TENANT_ID), eq(USER_ID), any()))
                    .thenReturn(buildGroupResponse());

            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    PRODUCT_ID, "Extras", null, 0, null, false, null);

            ResponseEntity<?> result = controller.createModifierGroup(ownerPrincipal, PRODUCT_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getLocation()).isNotNull();
        }

        @Test
        @DisplayName("Should override productId in request body with path variable")
        void shouldOverrideProductIdWithPathVariable() {
            when(modifierGroupService.createModifierGroup(any(), any(), any()))
                    .thenReturn(buildGroupResponse());

            UUID differentProductId = UUID.randomUUID();
            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    differentProductId, "Extras", null, 0, null, false, null);

            controller.createModifierGroup(ownerPrincipal, PRODUCT_ID, request);

            verify(modifierGroupService).createModifierGroup(
                    eq(TENANT_ID), eq(USER_ID),
                    argThat(r -> PRODUCT_ID.equals(r.productId())));
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when product not found")
        void shouldPropagateNotFoundWhenProductNotFound() {
            when(modifierGroupService.createModifierGroup(any(), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

            ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                    PRODUCT_ID, "Extras", null, 0, null, false, null);

            assertThatThrownBy(() -> controller.createModifierGroup(ownerPrincipal, PRODUCT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/modifier-groups/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getModifierGroup")
    class GetModifierGroupTests {

        @Test
        @DisplayName("Should return 200 with modifier group details")
        void shouldReturn200WithGroupDetails() {
            when(modifierGroupService.getModifierGroup(TENANT_ID, GROUP_ID))
                    .thenReturn(buildGroupResponse());

            ResponseEntity<?> result = controller.getModifierGroup(ownerPrincipal, GROUP_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException for cross-tenant access")
        void shouldPropagateNotFoundForCrossTenant() {
            when(modifierGroupService.getModifierGroup(TENANT_ID, GROUP_ID))
                    .thenThrow(new ResourceNotFoundException("ModifierGroup", GROUP_ID));

            assertThatThrownBy(() -> controller.getModifierGroup(ownerPrincipal, GROUP_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/modifier-groups/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateModifierGroup")
    class UpdateModifierGroupTests {

        @Test
        @DisplayName("Should return 200 with updated group")
        void shouldReturn200WithUpdatedGroup() {
            when(modifierGroupService.updateModifierGroup(eq(TENANT_ID), eq(USER_ID), eq(GROUP_ID), any()))
                    .thenReturn(buildGroupResponse());

            ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                    "Sin ingredientes", null, null, null, null, null);

            ResponseEntity<?> result = controller.updateModifierGroup(ownerPrincipal, GROUP_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/modifier-groups/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteModifierGroup")
    class DeleteModifierGroupTests {

        @Test
        @DisplayName("Should return 204 on successful deletion")
        void shouldReturn204OnDeletion() {
            doNothing().when(modifierGroupService)
                    .deleteModifierGroup(TENANT_ID, USER_ID, GROUP_ID);

            ResponseEntity<Void> result = controller.deleteModifierGroup(ownerPrincipal, GROUP_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when not found")
        void shouldPropagateNotFound() {
            doThrow(new ResourceNotFoundException("ModifierGroup", GROUP_ID))
                    .when(modifierGroupService).deleteModifierGroup(TENANT_ID, USER_ID, GROUP_ID);

            assertThatThrownBy(() -> controller.deleteModifierGroup(ownerPrincipal, GROUP_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/modifier-groups/{groupId}/modifiers
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addModifier")
    class AddModifierTests {

        @Test
        @DisplayName("Should return 201 with Location header")
        void shouldReturn201WithLocation() {
            when(modifierService.addModifier(eq(TENANT_ID), eq(USER_ID), any()))
                    .thenReturn(buildModifierResponse());

            ModifierCreateRequest request = new ModifierCreateRequest(
                    GROUP_ID, "Extra Queso", new BigDecimal("15.00"), false, 0);

            ResponseEntity<?> result = controller.addModifier(ownerPrincipal, GROUP_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getLocation()).isNotNull();
        }

        @Test
        @DisplayName("Should override groupId in request body with path variable")
        void shouldOverrideGroupIdWithPathVariable() {
            when(modifierService.addModifier(any(), any(), any()))
                    .thenReturn(buildModifierResponse());

            UUID differentGroupId = UUID.randomUUID();
            ModifierCreateRequest request = new ModifierCreateRequest(
                    differentGroupId, "Extra Queso", new BigDecimal("15.00"), false, 0);

            controller.addModifier(ownerPrincipal, GROUP_ID, request);

            verify(modifierService).addModifier(
                    eq(TENANT_ID), eq(USER_ID),
                    argThat(r -> GROUP_ID.equals(r.modifierGroupId())));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/modifiers/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateModifier")
    class UpdateModifierTests {

        @Test
        @DisplayName("Should return 200 with updated modifier")
        void shouldReturn200WithUpdatedModifier() {
            when(modifierService.updateModifier(eq(TENANT_ID), eq(USER_ID), eq(MODIFIER_ID), any()))
                    .thenReturn(buildModifierResponse());

            ModifierUpdateRequest request = new ModifierUpdateRequest(null, new BigDecimal("20.00"), null, null, null);

            ResponseEntity<?> result = controller.updateModifier(ownerPrincipal, MODIFIER_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/modifiers/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteModifier")
    class DeleteModifierTests {

        @Test
        @DisplayName("Should return 204 on successful deletion")
        void shouldReturn204OnDeletion() {
            doNothing().when(modifierService).deleteModifier(TENANT_ID, USER_ID, MODIFIER_ID);

            ResponseEntity<Void> result = controller.deleteModifier(ownerPrincipal, MODIFIER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should propagate BusinessRuleException when deleting last active modifier")
        void shouldPropagateBusinessRuleExceptionWhenLastActive() {
            doThrow(new BusinessRuleException("LAST_ACTIVE_MODIFIER", "Cannot delete last active modifier"))
                    .when(modifierService).deleteModifier(TENANT_ID, USER_ID, MODIFIER_ID);

            assertThatThrownBy(() -> controller.deleteModifier(ownerPrincipal, MODIFIER_ID))
                    .isInstanceOf(BusinessRuleException.class);
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
        mockRequest.setRequestURI("/api/v1/products/" + PRODUCT_ID + "/modifier-groups");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    private ModifierGroupResponse buildGroupResponse() {
        return new ModifierGroupResponse(
                GROUP_ID, TENANT_ID, PRODUCT_ID, "Extras", null,
                0, null, false, 0, Instant.now(), Instant.now(), List.of());
    }

    private ModifierResponse buildModifierResponse() {
        return new ModifierResponse(
                MODIFIER_ID, TENANT_ID, GROUP_ID, "Extra Queso",
                new BigDecimal("15.00"), false, true, 0, Instant.now(), Instant.now());
    }
}
