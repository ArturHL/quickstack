package com.quickstack.inventory.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Recipe entity")
class RecipeEntityTest {

    @Test
    @DisplayName("1. New recipe is active by default")
    void newRecipeIsActiveByDefault() {
        Recipe recipe = new Recipe();

        assertThat(recipe.isActive()).isTrue();
    }

    @Test
    @DisplayName("2. Recipe can be deactivated")
    void recipeCanBeDeactivated() {
        Recipe recipe = new Recipe();
        recipe.setActive(false);

        assertThat(recipe.isActive()).isFalse();
    }

    @Test
    @DisplayName("3. variantId is null for base recipes")
    void variantIdIsNullForBaseRecipe() {
        Recipe recipe = new Recipe();
        recipe.setProductId(UUID.randomUUID());
        recipe.setTenantId(UUID.randomUUID());

        assertThat(recipe.getVariantId()).isNull();
    }

    @Test
    @DisplayName("4. Recipe stores productId and variantId correctly")
    void recipeStoresProductAndVariantId() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        Recipe recipe = new Recipe();
        recipe.setProductId(productId);
        recipe.setVariantId(variantId);

        assertThat(recipe.getProductId()).isEqualTo(productId);
        assertThat(recipe.getVariantId()).isEqualTo(variantId);
    }
}
