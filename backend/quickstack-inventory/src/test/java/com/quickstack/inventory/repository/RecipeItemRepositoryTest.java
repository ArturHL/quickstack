package com.quickstack.inventory.repository;

import com.quickstack.inventory.AbstractInventoryRepositoryTest;
import com.quickstack.inventory.entity.Ingredient;
import com.quickstack.inventory.entity.Recipe;
import com.quickstack.inventory.entity.RecipeItem;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("RecipeItemRepository")
class RecipeItemRepositoryTest extends AbstractInventoryRepositoryTest {

    @Autowired
    private RecipeItemRepository recipeItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantId;
    private Recipe recipe;
    private Ingredient ingredientA;
    private Ingredient ingredientB;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Plan', ?, 0, 10, 5)")
                .setParameter(1, planId).setParameter(2, "PLN2-" + planId).executeUpdate();

        tenantId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id) VALUES (?, 'Rest', ?, ?)")
                .setParameter(1, tenantId).setParameter(2, "r-" + tenantId).setParameter(3, planId).executeUpdate();

        recipe = new Recipe();
        recipe.setTenantId(tenantId);
        recipe.setProductId(UUID.randomUUID());
        entityManager.persist(recipe);

        ingredientA = new Ingredient();
        ingredientA.setTenantId(tenantId);
        ingredientA.setName("Ing-A-" + UUID.randomUUID());
        ingredientA.setUnit(UnitType.GRAM);
        ingredientA.setCostPerUnit(new BigDecimal("5.00"));
        entityManager.persist(ingredientA);

        ingredientB = new Ingredient();
        ingredientB.setTenantId(tenantId);
        ingredientB.setName("Ing-B-" + UUID.randomUUID());
        ingredientB.setUnit(UnitType.MILLILITER);
        ingredientB.setCostPerUnit(new BigDecimal("3.00"));
        entityManager.persist(ingredientB);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. findAllByRecipeId returns items for a recipe")
    void findAllByRecipeIdReturnsItems() {
        entityManager.persist(createItem(recipe.getId(), ingredientA.getId(), "100.0"));
        entityManager.persist(createItem(recipe.getId(), ingredientB.getId(), "50.0"));
        entityManager.flush();

        List<RecipeItem> items = recipeItemRepository.findAllByRecipeId(recipe.getId());

        assertThat(items).hasSize(2);
    }

    @Test
    @DisplayName("2. deleteAllByRecipeId removes all items for a recipe")
    void deleteAllByRecipeIdRemovesItems() {
        entityManager.persist(createItem(recipe.getId(), ingredientA.getId(), "200.0"));
        entityManager.persist(createItem(recipe.getId(), ingredientB.getId(), "100.0"));
        entityManager.flush();
        entityManager.clear();

        recipeItemRepository.deleteAllByRecipeId(recipe.getId());

        List<RecipeItem> items = recipeItemRepository.findAllByRecipeId(recipe.getId());
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("3. Duplicate ingredient in same recipe violates unique constraint")
    void duplicateIngredientInSameRecipeViolatesConstraint() {
        entityManager.persist(createItem(recipe.getId(), ingredientA.getId(), "100.0"));
        entityManager.flush();

        assertThrows(Exception.class, () -> {
            entityManager.persist(createItem(recipe.getId(), ingredientA.getId(), "200.0"));
            entityManager.flush();
        });
    }

    @Test
    @DisplayName("4. Same ingredient can appear in different recipes")
    void sameIngredientCanAppearInDifferentRecipes() {
        Recipe recipe2 = new Recipe();
        recipe2.setTenantId(tenantId);
        recipe2.setProductId(UUID.randomUUID());
        entityManager.persist(recipe2);
        entityManager.flush();

        entityManager.persist(createItem(recipe.getId(), ingredientA.getId(), "100.0"));
        entityManager.persist(createItem(recipe2.getId(), ingredientA.getId(), "200.0"));
        entityManager.flush();

        assertThat(recipeItemRepository.findAllByRecipeId(recipe.getId())).hasSize(1);
        assertThat(recipeItemRepository.findAllByRecipeId(recipe2.getId())).hasSize(1);
    }

    @Test
    @DisplayName("5. existsByRecipeIdAndIngredientId returns correct values")
    void existsByRecipeIdAndIngredientId() {
        entityManager.persist(createItem(recipe.getId(), ingredientA.getId(), "100.0"));
        entityManager.flush();

        assertThat(recipeItemRepository.existsByRecipeIdAndIngredientId(recipe.getId(), ingredientA.getId())).isTrue();
        assertThat(recipeItemRepository.existsByRecipeIdAndIngredientId(recipe.getId(), ingredientB.getId())).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RecipeItem createItem(UUID recipeId, UUID ingredientId, String quantity) {
        RecipeItem item = new RecipeItem();
        item.setTenantId(tenantId);
        item.setRecipeId(recipeId);
        item.setIngredientId(ingredientId);
        item.setQuantity(new BigDecimal(quantity));
        return item;
    }
}
