package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.ModifierCreateRequest;
import com.quickstack.product.dto.request.ModifierUpdateRequest;
import com.quickstack.product.dto.response.ModifierResponse;
import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
import com.quickstack.product.repository.ModifierGroupRepository;
import com.quickstack.product.repository.ModifierRepository;
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
@DisplayName("ModifierService")
class ModifierServiceTest {

    @Mock private ModifierRepository modifierRepository;
    @Mock private ModifierGroupRepository modifierGroupRepository;

    private ModifierService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final UUID MODIFIER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ModifierService(modifierRepository, modifierGroupRepository);
    }

    // -------------------------------------------------------------------------
    // addModifier
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("addModifier")
    class AddModifierTests {

        @Test
        @DisplayName("Should create modifier when valid")
        void shouldCreateModifierWhenValid() {
            when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildGroup()));
            when(modifierRepository.existsByNameAndModifierGroupIdAndTenantId("Extra Queso", GROUP_ID, TENANT_ID))
                    .thenReturn(false);
            when(modifierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ModifierCreateRequest request = new ModifierCreateRequest(
                    GROUP_ID, "Extra Queso", new BigDecimal("15.00"), false, 0);

            ModifierResponse response = service.addModifier(TENANT_ID, USER_ID, request);

            assertThat(response.name()).isEqualTo("Extra Queso");
            assertThat(response.isDefault()).isFalse();
            verify(modifierRepository).save(any(Modifier.class));
        }

        @Test
        @DisplayName("Should reset other defaults when isDefault=true")
        void shouldResetOtherDefaultsWhenNewDefaultAdded() {
            Modifier existingDefault = buildModifier("Extra Jalapeño", true);

            when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildGroup()));
            when(modifierRepository.existsByNameAndModifierGroupIdAndTenantId("Extra Queso", GROUP_ID, TENANT_ID))
                    .thenReturn(false);
            when(modifierRepository.findAllByModifierGroupIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(List.of(existingDefault));
            when(modifierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ModifierCreateRequest request = new ModifierCreateRequest(
                    GROUP_ID, "Extra Queso", new BigDecimal("15.00"), true, 0);

            service.addModifier(TENANT_ID, USER_ID, request);

            assertThat(existingDefault.isDefault()).isFalse();
            verify(modifierRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when group not found")
        void shouldThrowWhenGroupNotFound() {
            when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            ModifierCreateRequest request = new ModifierCreateRequest(
                    GROUP_ID, "Extra Queso", new BigDecimal("15.00"), false, 0);

            assertThatThrownBy(() -> service.addModifier(TENANT_ID, USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when name already exists in group")
        void shouldThrowWhenNameDuplicate() {
            when(modifierGroupRepository.findByIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildGroup()));
            when(modifierRepository.existsByNameAndModifierGroupIdAndTenantId("Extra Queso", GROUP_ID, TENANT_ID))
                    .thenReturn(true);

            ModifierCreateRequest request = new ModifierCreateRequest(
                    GROUP_ID, "Extra Queso", new BigDecimal("15.00"), false, 0);

            assertThatThrownBy(() -> service.addModifier(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    // -------------------------------------------------------------------------
    // updateModifier
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateModifier")
    class UpdateModifierTests {

        @Test
        @DisplayName("Should update price adjustment")
        void shouldUpdatePriceAdjustment() {
            Modifier modifier = buildModifier("Extra Queso", false);
            when(modifierRepository.findByIdAndTenantId(MODIFIER_ID, TENANT_ID))
                    .thenReturn(Optional.of(modifier));
            when(modifierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ModifierUpdateRequest request = new ModifierUpdateRequest(
                    null, new BigDecimal("20.00"), null, null, null);

            ModifierResponse response = service.updateModifier(TENANT_ID, USER_ID, MODIFIER_ID, request);

            assertThat(response.priceAdjustment()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("Should reset other defaults when setting isDefault=true")
        void shouldResetOtherDefaultsOnUpdate() {
            Modifier modifier = buildModifier("Extra Queso", false);
            Modifier existingDefault = buildModifier("Extra Aguacate", true);

            when(modifierRepository.findByIdAndTenantId(MODIFIER_ID, TENANT_ID))
                    .thenReturn(Optional.of(modifier));
            when(modifierRepository.findAllByModifierGroupIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(List.of(existingDefault));
            when(modifierRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ModifierUpdateRequest request = new ModifierUpdateRequest(null, null, true, null, null);

            service.updateModifier(TENANT_ID, USER_ID, MODIFIER_ID, request);

            assertThat(existingDefault.isDefault()).isFalse();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when modifier not found")
        void shouldThrowWhenModifierNotFound() {
            when(modifierRepository.findByIdAndTenantId(MODIFIER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            ModifierUpdateRequest request = new ModifierUpdateRequest("Nuevo nombre", null, null, null, null);

            assertThatThrownBy(() -> service.updateModifier(TENANT_ID, USER_ID, MODIFIER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // deleteModifier
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteModifier")
    class DeleteModifierTests {

        @Test
        @DisplayName("Should soft-delete modifier when not the last active one")
        void shouldSoftDeleteWhenNotLastActive() {
            Modifier modifier = buildModifier("Extra Queso", false);
            when(modifierRepository.findByIdAndTenantId(MODIFIER_ID, TENANT_ID))
                    .thenReturn(Optional.of(modifier));
            when(modifierRepository.countActiveByModifierGroupId(GROUP_ID, TENANT_ID)).thenReturn(2L);

            service.deleteModifier(TENANT_ID, USER_ID, MODIFIER_ID);

            assertThat(modifier.getDeletedAt()).isNotNull();
            verify(modifierRepository).save(modifier);
        }

        @Test
        @DisplayName("Should throw BusinessRuleException when deleting last active modifier")
        void shouldThrowWhenDeletingLastActiveModifier() {
            Modifier modifier = buildModifier("Extra Queso", false);
            when(modifierRepository.findByIdAndTenantId(MODIFIER_ID, TENANT_ID))
                    .thenReturn(Optional.of(modifier));
            when(modifierRepository.countActiveByModifierGroupId(GROUP_ID, TENANT_ID)).thenReturn(1L);

            assertThatThrownBy(() -> service.deleteModifier(TENANT_ID, USER_ID, MODIFIER_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("last active modifier");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when modifier not found")
        void shouldThrowWhenModifierNotFound() {
            when(modifierRepository.findByIdAndTenantId(MODIFIER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteModifier(TENANT_ID, USER_ID, MODIFIER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // listModifiers
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listModifiers")
    class ListModifiersTests {

        @Test
        @DisplayName("Should return list of modifiers for group")
        void shouldReturnListOfModifiers() {
            Modifier m1 = buildModifier("Extra Queso", false);
            Modifier m2 = buildModifier("Extra Jalapeño", false);
            when(modifierRepository.findAllByModifierGroupIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(List.of(m1, m2));

            List<ModifierResponse> responses = service.listModifiers(TENANT_ID, GROUP_ID);

            assertThat(responses).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no modifiers")
        void shouldReturnEmptyListWhenNoModifiers() {
            when(modifierRepository.findAllByModifierGroupIdAndTenantId(GROUP_ID, TENANT_ID))
                    .thenReturn(List.of());

            List<ModifierResponse> responses = service.listModifiers(TENANT_ID, GROUP_ID);

            assertThat(responses).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ModifierGroup buildGroup() {
        ModifierGroup group = new ModifierGroup();
        group.setId(GROUP_ID);
        group.setTenantId(TENANT_ID);
        group.setProductId(UUID.randomUUID());
        group.setName("Extras");
        return group;
    }

    private Modifier buildModifier(String name, boolean isDefault) {
        Modifier modifier = new Modifier();
        modifier.setId(MODIFIER_ID);
        modifier.setTenantId(TENANT_ID);
        modifier.setModifierGroupId(GROUP_ID);
        modifier.setName(name);
        modifier.setPriceAdjustment(BigDecimal.ZERO);
        modifier.setDefault(isDefault);
        modifier.setActive(true);
        return modifier;
    }
}
