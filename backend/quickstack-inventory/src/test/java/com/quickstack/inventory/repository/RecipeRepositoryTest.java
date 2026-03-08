package com.quickstack.inventory.repository;

import com.quickstack.inventory.AbstractInventoryRepositoryTest;
import com.quickstack.inventory.entity.Recipe;
import com.quickstack.inventory.entity.RecipeItem;
import com.quickstack.inventory.entity.Ingredient;
import com.quickstack.inventory.entity.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("RecipeRepository")
class RecipeRepositoryTest extends AbstractInventoryRepositoryTest {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private RecipeItemRepository recipeItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Plan', ?, 0, 10, 5)")
                .setParameter(1, planId).setParameter(2, "PLN-" + planId).executeUpdate();

        tenantId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id) VALUES (?, 'Restaurante', ?, ?)")
                .setParameter(1, tenantId).setParameter(2, "rest-" + tenantId).setParameter(3, planId).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. Save and find base recipe (no variant)")
    void canSaveAndFindBaseRecipe() {
        UUID productId = UUID.randomUUID();
        Recipe recipe = createBaseRecipe(tenantId, productId);
        entityManager.persist(recipe);
        entityManager.flush();

        Optional<Recipe> found = recipeRepository.findBaseRecipe(productId, tenantId);

        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo(productId);
        assertThat(found.get().getVariantId()).isNull();
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("2. findVariantRecipe finds recipe for specific variant")
    void canFindVariantRecipe() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        Recipe baseRecipe = createBaseRecipe(tenantId, productId);
        Recipe variantRecipe = new Recipe();
        variantRecipe.setTenantId(tenantId);
        variantRecipe.setProductId(productId);
        variantRecipe.setVariantId(variantId);

        entityManager.persist(baseRecipe);
        entityManager.persist(variantRecipe);
        entityManager.flush();

        Optional<Recipe> found = recipeRepository.findVariantRecipe(productId, variantId, tenantId);

        assertThat(found).isPresent();
        assertThat(found.get().getVariantId()).isEqualTo(variantId);
    }

    @Test
    @DisplayName("3. findBaseRecipe returns empty for unknown product")
    void findBaseRecipeReturnsEmptyForUnknownProduct() {
        Optional<Recipe> found = recipeRepository.findBaseRecipe(UUID.randomUUID(), tenantId);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("4. existsBaseRecipe returns true when base recipe exists")
    void existsBaseRecipeReturnsTrueWhenExists() {
        UUID productId = UUID.randomUUID();
        entityManager.persist(createBaseRecipe(tenantId, productId));
        entityManager.flush();

        assertThat(recipeRepository.existsBaseRecipe(productId, tenantId)).isTrue();
    }

    @Test
    @DisplayName("5. existsBaseRecipe returns false when no base recipe exists")
    void existsBaseRecipeReturnsFalseWhenNotExists() {
        assertThat(recipeRepository.existsBaseRecipe(UUID.randomUUID(), tenantId)).isFalse();
    }

    @Test
    @DisplayName("6. RecipeItems cascade delete when recipe is deleted")
    void recipeItemsCascadeDeleteWithRecipe() {
        UUID productId = UUID.randomUUID();
        Recipe recipe = createBaseRecipe(tenantId, productId);
        entityManager.persist(recipe);
        entityManager.flush();

        // Add a recipe item
        Ingredient ingredient = new Ingredient();
        ingredient.setTenantId(tenantId);
        ingredient.setName("Ingredient-" + UUID.randomUUID());
        ingredient.setUnit(UnitType.KILOGRAM);
        ingredient.setCostPerUnit(new BigDecimal("10.00"));
        entityManager.persist(ingredient);

        RecipeItem item = new RecipeItem();
        item.setTenantId(tenantId);
        item.setRecipeId(recipe.getId());
        item.setIngredientId(ingredient.getId());
        item.setQuantity(new BigDecimal("0.500"));
        entityManager.persist(item);
        entityManager.flush();

        // Delete the recipe
        recipeRepository.deleteById(recipe.getId());
        entityManager.flush();
        entityManager.clear();

        // RecipeItem should also be deleted (ON DELETE CASCADE)
        List<RecipeItem> items = recipeItemRepository.findAllByRecipeId(recipe.getId());
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("7. Duplicate base recipe for same product+tenant violates unique partial index")
    void duplicateBaseRecipeViolatesUniqueConstraint() {
        UUID productId = UUID.randomUUID();
        Recipe recipe1 = createBaseRecipe(tenantId, productId);
        entityManager.persist(recipe1);
        entityManager.flush();

        // Inserting a second base recipe (variant_id IS NULL) for the same product+tenant
        // violates the partial unique index uq_recipes_base_per_product.
        assertThrows(Exception.class, () -> {
            Recipe recipe2 = createBaseRecipe(tenantId, productId);
            entityManager.persist(recipe2);
            entityManager.flush();
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Recipe createBaseRecipe(UUID tenantId, UUID productId) {
        Recipe recipe = new Recipe();
        recipe.setTenantId(tenantId);
        recipe.setProductId(productId);
        // variantId = null (base recipe)
        return recipe;
    }
}
