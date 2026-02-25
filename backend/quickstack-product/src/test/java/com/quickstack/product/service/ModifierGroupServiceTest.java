package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.ModifierGroupCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupUpdateRequest;
import com.quickstack.product.dto.response.ModifierGroupResponse;
import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.repository.ModifierGroupRepository;
import com.quickstack.product.repository.ModifierRepository;
import com.quickstack.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModifierGroupService")
class ModifierGroupServiceTest {

        @Mock
        private ModifierGroupRepository modifierGroupRepository;
        @Mock
        private ModifierRepository modifierRepository;
        @Mock
        private ProductRepository productRepository;

        private ModifierGroupService service;

        private static final UUID TENANT_ID = UUID.randomUUID();
        private static final UUID USER_ID = UUID.randomUUID();
        private static final UUID PRODUCT_ID = UUID.randomUUID();
        private static final UUID GROUP_ID = UUID.randomUUID();

        @BeforeEach
        void setUp() {
                service = new ModifierGroupService(modifierGroupRepository, modifierRepository, productRepository);
        }

        // -------------------------------------------------------------------------
        // createModifierGroup
        // -------------------------------------------------------------------------

        @Nested
        @DisplayName("createModifierGroup")
        class CreateTests {

                @Test
                @DisplayName("Should create modifier group when valid")
                void shouldCreateWhenValid() {
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.of(buildProduct()));
                        when(modifierGroupRepository.existsByNameAndProductIdAndTenantId("Extras", PRODUCT_ID,
                                        TENANT_ID))
                                        .thenReturn(false);
                        when(modifierGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                                        PRODUCT_ID, "Extras", null, 0, null, false, null);

                        ModifierGroupResponse response = service.createModifierGroup(TENANT_ID, USER_ID, request);

                        assertThat(response.name()).isEqualTo("Extras");
                        assertThat(response.isRequired()).isFalse();
                        verify(modifierGroupRepository).save(any(ModifierGroup.class));
                }

                @Test
                @DisplayName("Should throw ResourceNotFoundException when product not found")
                void shouldThrowWhenProductNotFound() {
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                                        PRODUCT_ID, "Extras", null, 0, null, false, null);

                        assertThatThrownBy(() -> service.createModifierGroup(TENANT_ID, USER_ID, request))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }

                @Test
                @DisplayName("Should throw DuplicateResourceException when name already exists")
                void shouldThrowWhenNameDuplicate() {
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.of(buildProduct()));
                        when(modifierGroupRepository.existsByNameAndProductIdAndTenantId("Extras", PRODUCT_ID,
                                        TENANT_ID))
                                        .thenReturn(true);

                        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                                        PRODUCT_ID, "Extras", null, 0, null, false, null);

                        assertThatThrownBy(() -> service.createModifierGroup(TENANT_ID, USER_ID, request))
                                        .isInstanceOf(DuplicateResourceException.class);
                }

                @Test
                @DisplayName("Should throw BusinessRuleException when isRequired=true and minSelections=0")
                void shouldThrowWhenRequiredConfigInvalid() {
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.of(buildProduct()));

                        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                                        PRODUCT_ID, "Tamaño", null, 0, null, true, null);

                        assertThatThrownBy(() -> service.createModifierGroup(TENANT_ID, USER_ID, request))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .hasMessageContaining("minSelections");
                }

                @Test
                @DisplayName("Should throw BusinessRuleException when maxSelections < minSelections")
                void shouldThrowWhenSelectionRangeInvalid() {
                        when(productRepository.findByIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(Optional.of(buildProduct()));

                        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                                        PRODUCT_ID, "Extras", null, 5, 2, false, null);

                        assertThatThrownBy(() -> service.createModifierGroup(TENANT_ID, USER_ID, request))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .hasMessageContaining("maxSelections");
                }
        }

        // -------------------------------------------------------------------------
        // updateModifierGroup
        // -------------------------------------------------------------------------

        @Nested
        @DisplayName("updateModifierGroup")
        class UpdateTests {

                @Test
                @DisplayName("Should update name when valid")
                void shouldUpdateNameWhenValid() {
                        ModifierGroup group = buildGroup("Extras", 0, null, false);
                        when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(Optional.of(group));
                        when(modifierGroupRepository.existsByNameAndProductIdAndTenantIdAndIdNot(
                                        "Sin ingredientes", PRODUCT_ID, TENANT_ID, GROUP_ID)).thenReturn(false);
                        when(modifierGroupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                        when(modifierRepository.findAllByModifierGroupIdAndTenantId(any(), any()))
                                        .thenReturn(List.of());

                        ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                                        "Sin ingredientes", null, null, null, null, null);

                        ModifierGroupResponse response = service.updateModifierGroup(TENANT_ID, USER_ID, GROUP_ID,
                                        request);

                        assertThat(response.name()).isEqualTo("Sin ingredientes");
                }

                @Test
                @DisplayName("Should throw ResourceNotFoundException when group not found")
                void shouldThrowWhenGroupNotFound() {
                        when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                                        "Nuevo nombre", null, null, null, null, null);

                        assertThatThrownBy(() -> service.updateModifierGroup(TENANT_ID, USER_ID, GROUP_ID, request))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }

                @Test
                @DisplayName("Should throw BusinessRuleException when update results in invalid required config")
                void shouldThrowWhenUpdateResultsInInvalidConfig() {
                        ModifierGroup group = buildGroup("Tamaño", 0, null, false);
                        when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(Optional.of(group));

                        // Update: set isRequired=true without changing minSelections (still 0)
                        ModifierGroupUpdateRequest request = new ModifierGroupUpdateRequest(
                                        null, null, null, null, true, null);

                        assertThatThrownBy(() -> service.updateModifierGroup(TENANT_ID, USER_ID, GROUP_ID, request))
                                        .isInstanceOf(BusinessRuleException.class)
                                        .hasMessageContaining("minSelections");
                }
        }

        // -------------------------------------------------------------------------
        // deleteModifierGroup
        // -------------------------------------------------------------------------

        @Nested
        @DisplayName("deleteModifierGroup")
        class DeleteTests {

                @Test
                @DisplayName("Should soft-delete group and cascade to modifiers")
                void shouldSoftDeleteGroupAndCascade() {
                        ModifierGroup group = buildGroup("Extras", 0, null, false);
                        Modifier modifier = buildModifier(GROUP_ID);

                        when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(Optional.of(group));
                        when(modifierRepository.findAllNonDeletedByModifierGroupIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(List.of(modifier));

                        service.deleteModifierGroup(TENANT_ID, USER_ID, GROUP_ID);

                        assertThat(group.getDeletedAt()).isNotNull();
                        assertThat(modifier.getDeletedAt()).isNotNull();
                        verify(modifierGroupRepository).save(group);
                        verify(modifierRepository).saveAll(anyList());
                }

                @Test
                @DisplayName("Should throw ResourceNotFoundException when group not found")
                void shouldThrowWhenGroupNotFound() {
                        when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> service.deleteModifierGroup(TENANT_ID, USER_ID, GROUP_ID))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        // -------------------------------------------------------------------------
        // getModifierGroup and listModifierGroupsByProduct
        // -------------------------------------------------------------------------

        @Nested
        @DisplayName("getModifierGroup")
        class GetTests {

                @Test
                @DisplayName("Should return group with its modifiers")
                void shouldReturnGroupWithModifiers() {
                        ModifierGroup group = buildGroup("Extras", 0, null, false);
                        group.setId(GROUP_ID);

                        when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(Optional.of(group));
                        when(modifierRepository.findAllByModifierGroupIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(List.of(buildModifier(GROUP_ID)));

                        ModifierGroupResponse response = service.getModifierGroup(TENANT_ID, GROUP_ID);

                        assertThat(response.name()).isEqualTo("Extras");
                        assertThat(response.modifiers()).hasSize(1);
                }

                @Test
                @DisplayName("Should throw ResourceNotFoundException when group not found")
                void shouldThrowWhenNotFound() {
                        when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                                        .thenReturn(Optional.empty());

                        assertThatThrownBy(() -> service.getModifierGroup(TENANT_ID, GROUP_ID))
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        @Nested
        @DisplayName("listModifierGroupsByProduct")
        class ListTests {

                @Test
                @DisplayName("Should return all groups for product")
                void shouldReturnAllGroupsForProduct() {
                        ModifierGroup g1 = buildGroup("Extras", 0, null, false);
                        g1.setId(UUID.randomUUID());
                        ModifierGroup g2 = buildGroup("Tamaño", 1, 1, true);
                        g2.setId(UUID.randomUUID());

                        when(modifierGroupRepository.findAllByProductIdAndTenantId(PRODUCT_ID, TENANT_ID))
                                        .thenReturn(List.of(g1, g2));
                        when(modifierRepository.findAllByModifierGroupIdAndTenantId(any(), any()))
                                        .thenReturn(List.of());

                        List<ModifierGroupResponse> responses = service.listModifierGroupsByProduct(TENANT_ID,
                                        PRODUCT_ID);

                        assertThat(responses).hasSize(2);
                }
        }

        // -------------------------------------------------------------------------
        // Helpers
        // -------------------------------------------------------------------------

        private Product buildProduct() {
                Product product = new Product();
                product.setId(PRODUCT_ID);
                product.setTenantId(TENANT_ID);
                product.setName("Hamburguesa");
                product.setBasePrice(new BigDecimal("50.00"));
                product.setProductType(ProductType.SIMPLE);
                return product;
        }

        private ModifierGroup buildGroup(String name, int minSelections, Integer maxSelections, boolean isRequired) {
                ModifierGroup group = new ModifierGroup();
                group.setId(GROUP_ID);
                group.setTenantId(TENANT_ID);
                group.setProductId(PRODUCT_ID);
                group.setName(name);
                group.setMinSelections(minSelections);
                group.setMaxSelections(maxSelections);
                group.setRequired(isRequired);
                return group;
        }

        private Modifier buildModifier(UUID groupId) {
                Modifier modifier = new Modifier();
                modifier.setId(UUID.randomUUID());
                modifier.setTenantId(TENANT_ID);
                modifier.setModifierGroupId(groupId);
                modifier.setName("Extra Queso");
                modifier.setPriceAdjustment(new BigDecimal("15.00"));
                modifier.setActive(true);
                return modifier;
        }
}
