package com.quickstack.inventory.repository;

import com.quickstack.inventory.AbstractInventoryRepositoryTest;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("IngredientRepository")
class IngredientRepositoryTest extends AbstractInventoryRepositoryTest {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Test Plan', ?, 0, 10, 5)")
                .setParameter(1, planId).setParameter(2, "TEST-" + planId).executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id) VALUES (?, 'Tenant A', ?, ?)")
                .setParameter(1, tenantA).setParameter(2, "ta-" + tenantA).setParameter(3, planId).executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id) VALUES (?, 'Tenant B', ?, ?)")
                .setParameter(1, tenantB).setParameter(2, "tb-" + tenantB).setParameter(3, planId).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. Save and find ingredient by ID")
    void canSaveAndFindIngredient() {
        Ingredient ingredient = createIngredient(tenantA, "Aguacate", UnitType.UNIT, "25.00");
        entityManager.persist(ingredient);
        entityManager.flush();

        Optional<Ingredient> found = ingredientRepository.findById(ingredient.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Aguacate");
        assertThat(found.get().getTenantId()).isEqualTo(tenantA);
        assertThat(found.get().getUnit()).isEqualTo(UnitType.UNIT);
    }

    @Test
    @DisplayName("2. findAllByTenantId excludes soft-deleted ingredients")
    void findAllExcludesSoftDeleted() {
        Ingredient active = createIngredient(tenantA, "Tomate", UnitType.KILOGRAM, "15.00");
        Ingredient deleted = createIngredient(tenantA, "Eliminado", UnitType.GRAM, "5.00");
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<Ingredient> result = ingredientRepository.findAllByTenantId(tenantA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Tomate");
    }

    @Test
    @DisplayName("3. findAllByTenantId enforces tenant isolation")
    void findAllEnforcesTenantIsolation() {
        entityManager.persist(createIngredient(tenantA, "Sal", UnitType.GRAM, "2.00"));
        entityManager.persist(createIngredient(tenantB, "Azúcar", UnitType.KILOGRAM, "18.00"));
        entityManager.flush();

        List<Ingredient> tenantAResults = ingredientRepository.findAllByTenantId(tenantA);
        List<Ingredient> tenantBResults = ingredientRepository.findAllByTenantId(tenantB);

        assertThat(tenantAResults).hasSize(1);
        assertThat(tenantAResults.get(0).getName()).isEqualTo("Sal");
        assertThat(tenantBResults).hasSize(1);
        assertThat(tenantBResults.get(0).getName()).isEqualTo("Azúcar");
    }

    @Test
    @DisplayName("4. findByIdAndTenantId returns empty for cross-tenant access (IDOR protection)")
    void findByIdAndTenantIdProtectsAgainstIDOR() {
        Ingredient ingredient = createIngredient(tenantA, "Cebolla", UnitType.UNIT, "3.00");
        entityManager.persist(ingredient);
        entityManager.flush();

        Optional<Ingredient> result = ingredientRepository.findByIdAndTenantId(ingredient.getId(), tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("5. findByIdAndTenantId excludes soft-deleted ingredients")
    void findByIdAndTenantIdExcludesSoftDeleted() {
        Ingredient ingredient = createIngredient(tenantA, "Eliminado", UnitType.LITER, "10.00");
        ingredient.setDeletedAt(Instant.now());
        entityManager.persist(ingredient);
        entityManager.flush();

        Optional<Ingredient> result = ingredientRepository.findByIdAndTenantId(ingredient.getId(), tenantA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("6. findLowStockByTenantId returns only ingredients below minimum")
    void findLowStockReturnsOnlyLowIngredients() {
        Ingredient low = createIngredient(tenantA, "Stock Bajo", UnitType.KILOGRAM, "20.00");
        low.setCurrentStock(new BigDecimal("0.500"));
        low.setMinimumStock(new BigDecimal("1.000"));

        Ingredient adequate = createIngredient(tenantA, "Stock OK", UnitType.KILOGRAM, "20.00");
        adequate.setCurrentStock(new BigDecimal("5.000"));
        adequate.setMinimumStock(new BigDecimal("1.000"));

        entityManager.persist(low);
        entityManager.persist(adequate);
        entityManager.flush();

        List<Ingredient> lowStock = ingredientRepository.findLowStockByTenantId(tenantA);

        assertThat(lowStock).hasSize(1);
        assertThat(lowStock.get(0).getName()).isEqualTo("Stock Bajo");
    }

    @Test
    @DisplayName("7. findLowStockByTenantId excludes soft-deleted ingredients")
    void findLowStockExcludesSoftDeleted() {
        Ingredient deleted = createIngredient(tenantA, "Eliminado Bajo", UnitType.UNIT, "5.00");
        deleted.setCurrentStock(new BigDecimal("0.0"));
        deleted.setMinimumStock(new BigDecimal("10.0"));
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(deleted);
        entityManager.flush();

        List<Ingredient> result = ingredientRepository.findLowStockByTenantId(tenantA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("8. findLowStockByTenantId returns empty when all stocks are adequate")
    void findLowStockReturnsEmptyWhenAllAdequate() {
        Ingredient ok = createIngredient(tenantA, "Bien Surtido", UnitType.LITER, "8.00");
        ok.setCurrentStock(new BigDecimal("10.000"));
        ok.setMinimumStock(new BigDecimal("2.000"));

        entityManager.persist(ok);
        entityManager.flush();

        List<Ingredient> result = ingredientRepository.findLowStockByTenantId(tenantA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("9. existsByNameAndTenantId detects duplicate names")
    void existsByNameDetectsDuplicates() {
        entityManager.persist(createIngredient(tenantA, "Pollo", UnitType.KILOGRAM, "80.00"));
        entityManager.flush();

        assertThat(ingredientRepository.existsByNameAndTenantId("Pollo", tenantA)).isTrue();
        assertThat(ingredientRepository.existsByNameAndTenantId("Pollo", tenantB)).isFalse();
        assertThat(ingredientRepository.existsByNameAndTenantId("Res", tenantA)).isFalse();
    }

    @Test
    @DisplayName("10. existsByNameAndTenantIdAndIdNot allows same name for same ingredient (update)")
    void existsByNameAndIdNotAllowsSameIngredientUpdate() {
        Ingredient ingredient = createIngredient(tenantA, "Frijol", UnitType.KILOGRAM, "35.00");
        entityManager.persist(ingredient);
        entityManager.flush();

        assertThat(ingredientRepository.existsByNameAndTenantIdAndIdNot("Frijol", tenantA, ingredient.getId())).isFalse();

        Ingredient other = createIngredient(tenantA, "Arroz", UnitType.KILOGRAM, "25.00");
        entityManager.persist(other);
        entityManager.flush();
        assertThat(ingredientRepository.existsByNameAndTenantIdAndIdNot("Frijol", tenantA, other.getId())).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Ingredient createIngredient(UUID tenantId, String name, UnitType unit, String costPerUnit) {
        Ingredient ingredient = new Ingredient();
        ingredient.setTenantId(tenantId);
        ingredient.setName(name);
        ingredient.setUnit(unit);
        ingredient.setCostPerUnit(new BigDecimal(costPerUnit));
        return ingredient;
    }
}
