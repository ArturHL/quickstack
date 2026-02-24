package com.quickstack.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ModifierGroup entity.
 */
@DisplayName("ModifierGroup Entity")
class ModifierGroupTest {

    @Test
    @DisplayName("Should create modifier group with required fields")
    void shouldCreateModifierGroupWithRequiredFields() {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        // When
        ModifierGroup group = new ModifierGroup();
        group.setTenantId(tenantId);
        group.setProductId(productId);
        group.setName("Extras");

        // Then
        assertThat(group.getTenantId()).isEqualTo(tenantId);
        assertThat(group.getProductId()).isEqualTo(productId);
        assertThat(group.getName()).isEqualTo("Extras");
    }

    @Test
    @DisplayName("Should have correct default values")
    void shouldHaveCorrectDefaultValues() {
        // When
        ModifierGroup group = new ModifierGroup();

        // Then
        assertThat(group.getMinSelections()).isEqualTo(0);
        assertThat(group.getMaxSelections()).isNull();
        assertThat(group.isRequired()).isFalse();
        assertThat(group.getSortOrder()).isEqualTo(0);
        assertThat(group.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Should not be deleted when deletedAt is null")
    void shouldNotBeDeletedWhenDeletedAtIsNull() {
        // Given
        ModifierGroup group = new ModifierGroup();

        // When / Then
        assertThat(group.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should be deleted when deletedAt is set")
    void shouldBeDeletedWhenDeletedAtIsSet() {
        // Given
        ModifierGroup group = new ModifierGroup();
        group.setDeletedAt(Instant.now());

        // When / Then
        assertThat(group.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should initialize modifiers list as empty")
    void shouldInitializeModifiersListAsEmpty() {
        // When
        ModifierGroup group = new ModifierGroup();

        // Then
        assertThat(group.getModifiers()).isNotNull();
        assertThat(group.getModifiers()).isEmpty();
    }

    @Test
    @DisplayName("Should allow null max selections for unlimited choices")
    void shouldAllowNullMaxSelectionsForUnlimitedChoices() {
        // Given
        ModifierGroup group = new ModifierGroup();
        group.setName("Extras");
        group.setMinSelections(0);
        group.setMaxSelections(null);

        // When / Then
        assertThat(group.getMaxSelections()).isNull();
        assertThat(group.getMinSelections()).isEqualTo(0);
    }
}
