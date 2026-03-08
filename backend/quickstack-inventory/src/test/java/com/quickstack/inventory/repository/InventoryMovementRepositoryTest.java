package com.quickstack.inventory.repository;

import com.quickstack.inventory.AbstractInventoryRepositoryTest;
import com.quickstack.inventory.entity.Ingredient;
import com.quickstack.inventory.entity.InventoryMovement;
import com.quickstack.inventory.entity.MovementType;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("InventoryMovementRepository")
class InventoryMovementRepositoryTest extends AbstractInventoryRepositoryTest {

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantId;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Plan', ?, 0, 10, 5)")
                .setParameter(1, planId).setParameter(2, "PLN3-" + planId).executeUpdate();

        tenantId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id) VALUES (?, 'Rest', ?, ?)")
                .setParameter(1, tenantId).setParameter(2, "r3-" + tenantId).setParameter(3, planId).executeUpdate();

        ingredient = new Ingredient();
        ingredient.setTenantId(tenantId);
        ingredient.setName("Pollo-" + UUID.randomUUID());
        ingredient.setUnit(UnitType.KILOGRAM);
        ingredient.setCostPerUnit(new BigDecimal("80.00"));
        entityManager.persist(ingredient);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. Save and retrieve SALE_DEDUCTION movement")
    void canSaveAndRetrieveSaleDeduction() {
        UUID orderId = UUID.randomUUID();
        InventoryMovement movement = createMovement(
                MovementType.SALE_DEDUCTION, new BigDecimal("-0.250"), orderId);

        entityManager.persist(movement);
        entityManager.flush();

        List<InventoryMovement> found = movementRepository.findByTenantIdAndIngredientId(
                tenantId, ingredient.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getMovementType()).isEqualTo(MovementType.SALE_DEDUCTION);
        assertThat(found.get(0).getQuantityDelta()).isEqualByComparingTo(new BigDecimal("-0.250"));
        assertThat(found.get(0).getReferenceId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("2. findByTenantIdAndMovementType filters by movement type")
    void findByMovementTypeFilters() {
        entityManager.persist(createMovement(MovementType.SALE_DEDUCTION, new BigDecimal("-1.0"), UUID.randomUUID()));
        entityManager.persist(createMovement(MovementType.PURCHASE, new BigDecimal("10.0"), null));
        entityManager.flush();

        List<InventoryMovement> deductions = movementRepository.findByTenantIdAndMovementType(
                tenantId, MovementType.SALE_DEDUCTION);
        List<InventoryMovement> purchases = movementRepository.findByTenantIdAndMovementType(
                tenantId, MovementType.PURCHASE);

        assertThat(deductions).hasSize(1);
        assertThat(purchases).hasSize(1);
    }

    @Test
    @DisplayName("3. findByTenantIdAndReferenceId finds movements by orderId")
    void findByReferenceIdLinksToOrder() {
        UUID orderId = UUID.randomUUID();
        entityManager.persist(createMovement(MovementType.SALE_DEDUCTION, new BigDecimal("-0.5"), orderId));
        entityManager.persist(createMovement(MovementType.MANUAL_ADJUSTMENT, new BigDecimal("2.0"), null));
        entityManager.flush();

        List<InventoryMovement> linked = movementRepository.findByTenantIdAndReferenceId(tenantId, orderId);

        assertThat(linked).hasSize(1);
        assertThat(linked.get(0).getReferenceId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("4. Movements are never deleted (delete operation should not be used)")
    void movementsArePersistedEvenAfterTime() {
        entityManager.persist(createMovement(MovementType.PURCHASE, new BigDecimal("5.0"), null));
        entityManager.flush();
        entityManager.clear();

        List<InventoryMovement> movements = movementRepository.findByTenantIdAndIngredientId(
                tenantId, ingredient.getId());

        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("5. unitCostAtTime captures historical cost correctly")
    void unitCostAtTimeCapturesHistoricalCost() {
        InventoryMovement movement = createMovement(MovementType.SALE_DEDUCTION, new BigDecimal("-1.0"), null);
        movement.setUnitCostAtTime(new BigDecimal("80.0000")); // cost at time of movement

        entityManager.persist(movement);
        entityManager.flush();
        entityManager.clear();

        List<InventoryMovement> found = movementRepository.findByTenantIdAndIngredientId(
                tenantId, ingredient.getId());

        assertThat(found.get(0).getUnitCostAtTime()).isEqualByComparingTo(new BigDecimal("80.0000"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private InventoryMovement createMovement(MovementType type, BigDecimal delta, UUID referenceId) {
        InventoryMovement movement = new InventoryMovement();
        movement.setTenantId(tenantId);
        movement.setIngredientId(ingredient.getId());
        movement.setMovementType(type);
        movement.setQuantityDelta(delta);
        movement.setUnitCostAtTime(ingredient.getCostPerUnit());
        movement.setReferenceId(referenceId);
        return movement;
    }
}
