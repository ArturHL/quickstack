package com.quickstack.product.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Modifier entity.
 */
@DisplayName("Modifier Entity")
class ModifierTest {

    @Test
    @DisplayName("Should create modifier with required fields")
    void shouldCreateModifierWithRequiredFields() {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        // When
        Modifier modifier = new Modifier();
        modifier.setTenantId(tenantId);
        modifier.setModifierGroupId(groupId);
        modifier.setName("Extra Queso");
        modifier.setPriceAdjustment(new BigDecimal("15.00"));

        // Then
        assertThat(modifier.getTenantId()).isEqualTo(tenantId);
        assertThat(modifier.getModifierGroupId()).isEqualTo(groupId);
        assertThat(modifier.getName()).isEqualTo("Extra Queso");
        assertThat(modifier.getPriceAdjustment()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("Should have correct default values")
    void shouldHaveCorrectDefaultValues() {
        // When
        Modifier modifier = new Modifier();

        // Then
        assertThat(modifier.getPriceAdjustment()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(modifier.isDefault()).isFalse();
        assertThat(modifier.isActive()).isTrue();
        assertThat(modifier.getSortOrder()).isEqualTo(0);
        assertThat(modifier.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Should not be deleted when deletedAt is null")
    void shouldNotBeDeletedWhenDeletedAtIsNull() {
        // Given
        Modifier modifier = new Modifier();

        // When / Then
        assertThat(modifier.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Should be deleted when deletedAt is set")
    void shouldBeDeletedWhenDeletedAtIsSet() {
        // Given
        Modifier modifier = new Modifier();
        modifier.setDeletedAt(Instant.now());

        // When / Then
        assertThat(modifier.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Should support positive, negative, and zero price adjustments")
    void shouldSupportAllPriceAdjustmentTypes() {
        // Given
        Modifier extra = new Modifier();
        Modifier discount = new Modifier();
        Modifier noCharge = new Modifier();

        // When
        extra.setPriceAdjustment(new BigDecimal("15.00"));   // Extra charge
        discount.setPriceAdjustment(new BigDecimal("-10.00")); // Discount
        noCharge.setPriceAdjustment(BigDecimal.ZERO);           // No change

        // Then
        assertThat(extra.getPriceAdjustment()).isEqualByComparingTo("15.00");
        assertThat(discount.getPriceAdjustment()).isEqualByComparingTo("-10.00");
        assertThat(noCharge.getPriceAdjustment()).isEqualByComparingTo("0.00");
    }
}
