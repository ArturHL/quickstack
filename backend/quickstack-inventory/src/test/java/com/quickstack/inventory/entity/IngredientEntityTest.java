package com.quickstack.inventory.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Ingredient entity")
class IngredientEntityTest {

    @Test
    @DisplayName("1. New ingredient has correct default stock values")
    void newIngredientHasDefaultValues() {
        Ingredient ingredient = new Ingredient();

        assertThat(ingredient.getCurrentStock()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ingredient.getMinimumStock()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ingredient.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("2. isDeleted returns false when deletedAt is null")
    void isDeletedReturnsFalseWhenNotDeleted() {
        Ingredient ingredient = new Ingredient();
        ingredient.setName("Aguacate");

        assertThat(ingredient.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("3. softDelete sets deletedAt and deletedBy")
    void softDeleteSetsFields() {
        Ingredient ingredient = new Ingredient();
        UUID deletedBy = UUID.randomUUID();

        ingredient.softDelete(deletedBy);

        assertThat(ingredient.isDeleted()).isTrue();
        assertThat(ingredient.getDeletedAt()).isNotNull();
        assertThat(ingredient.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("4. isBelowMinimum returns true when currentStock < minimumStock")
    void isBelowMinimumReturnsTrueWhenStockIsLow() {
        Ingredient ingredient = new Ingredient();
        ingredient.setCurrentStock(new BigDecimal("0.5"));
        ingredient.setMinimumStock(new BigDecimal("1.0"));

        assertThat(ingredient.isBelowMinimum()).isTrue();
    }

    @Test
    @DisplayName("5. isBelowMinimum returns false when currentStock >= minimumStock")
    void isBelowMinimumReturnsFalseWhenStockIsAdequate() {
        Ingredient ingredient = new Ingredient();
        ingredient.setCurrentStock(new BigDecimal("2.0"));
        ingredient.setMinimumStock(new BigDecimal("1.0"));

        assertThat(ingredient.isBelowMinimum()).isFalse();
    }

    @Test
    @DisplayName("6. isBelowMinimum returns false when currentStock equals minimumStock")
    void isBelowMinimumReturnsFalseAtExactMinimum() {
        Ingredient ingredient = new Ingredient();
        ingredient.setCurrentStock(new BigDecimal("1.000"));
        ingredient.setMinimumStock(new BigDecimal("1.000"));

        assertThat(ingredient.isBelowMinimum()).isFalse();
    }

    @Test
    @DisplayName("7. softDelete does not affect stock values")
    void softDeleteDoesNotAffectStock() {
        Ingredient ingredient = new Ingredient();
        ingredient.setCurrentStock(new BigDecimal("5.0"));
        ingredient.setMinimumStock(new BigDecimal("2.0"));
        ingredient.softDelete(UUID.randomUUID());

        assertThat(ingredient.getCurrentStock()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(ingredient.getMinimumStock()).isEqualByComparingTo(new BigDecimal("2.0"));
    }
}
