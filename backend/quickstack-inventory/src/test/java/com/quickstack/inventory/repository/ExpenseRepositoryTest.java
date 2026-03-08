package com.quickstack.inventory.repository;

import com.quickstack.inventory.AbstractInventoryRepositoryTest;
import com.quickstack.inventory.entity.Expense;
import com.quickstack.inventory.entity.ExpenseCategory;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("ExpenseRepository")
class ExpenseRepositoryTest extends AbstractInventoryRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Plan', ?, 0, 10, 5)")
                .setParameter(1, planId).setParameter(2, "PLN4-" + planId).executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id) VALUES (?, 'Rest A', ?, ?)")
                .setParameter(1, tenantA).setParameter(2, "ra-" + tenantA).setParameter(3, planId).executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id) VALUES (?, 'Rest B', ?, ?)")
                .setParameter(1, tenantB).setParameter(2, "rb-" + tenantB).setParameter(3, planId).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. Save and find expense by ID and tenant")
    void canSaveAndFindExpense() {
        Expense expense = createExpense(tenantA, "500.00", ExpenseCategory.FOOD_COST, LocalDate.now());
        entityManager.persist(expense);
        entityManager.flush();

        Optional<Expense> found = expenseRepository.findByIdAndTenantId(expense.getId(), tenantA);

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(found.get().getExpenseCategory()).isEqualTo(ExpenseCategory.FOOD_COST);
    }

    @Test
    @DisplayName("2. findAllByTenantId excludes soft-deleted expenses")
    void findAllExcludesSoftDeleted() {
        Expense active = createExpense(tenantA, "300.00", ExpenseCategory.LABOR, LocalDate.now());
        Expense deleted = createExpense(tenantA, "100.00", ExpenseCategory.OTHER, LocalDate.now());
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<Expense> result = expenseRepository.findAllByTenantId(tenantA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExpenseCategory()).isEqualTo(ExpenseCategory.LABOR);
    }

    @Test
    @DisplayName("3. findAllByTenantId enforces tenant isolation")
    void findAllEnforcesTenantIsolation() {
        entityManager.persist(createExpense(tenantA, "1000.00", ExpenseCategory.RENT, LocalDate.now()));
        entityManager.persist(createExpense(tenantB, "200.00", ExpenseCategory.UTILITIES, LocalDate.now()));
        entityManager.flush();

        assertThat(expenseRepository.findAllByTenantId(tenantA)).hasSize(1);
        assertThat(expenseRepository.findAllByTenantId(tenantB)).hasSize(1);
    }

    @Test
    @DisplayName("4. findByIdAndTenantId returns empty for cross-tenant (IDOR protection)")
    void findByIdAndTenantIdProtectsAgainstIDOR() {
        Expense expense = createExpense(tenantA, "250.00", ExpenseCategory.SUPPLIES, LocalDate.now());
        entityManager.persist(expense);
        entityManager.flush();

        Optional<Expense> result = expenseRepository.findByIdAndTenantId(expense.getId(), tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("5. findByTenantIdAndDateRange filters by date range")
    void findByDateRangeFiltersCorrectly() {
        LocalDate today = LocalDate.now();
        entityManager.persist(createExpense(tenantA, "100.00", ExpenseCategory.FOOD_COST, today.minusDays(5)));
        entityManager.persist(createExpense(tenantA, "200.00", ExpenseCategory.LABOR, today.minusDays(1)));
        entityManager.persist(createExpense(tenantA, "300.00", ExpenseCategory.RENT, today.plusDays(5)));
        entityManager.flush();

        List<Expense> result = expenseRepository.findByTenantIdAndDateRange(
                tenantA, today.minusDays(3), today);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("6. findByTenantIdAndCategory filters by expense category")
    void findByCategoryFiltersCorrectly() {
        entityManager.persist(createExpense(tenantA, "500.00", ExpenseCategory.FOOD_COST, LocalDate.now()));
        entityManager.persist(createExpense(tenantA, "800.00", ExpenseCategory.RENT, LocalDate.now()));
        entityManager.persist(createExpense(tenantA, "150.00", ExpenseCategory.FOOD_COST, LocalDate.now()));
        entityManager.flush();

        List<Expense> foodCosts = expenseRepository.findByTenantIdAndCategory(tenantA, ExpenseCategory.FOOD_COST);
        List<Expense> rents = expenseRepository.findByTenantIdAndCategory(tenantA, ExpenseCategory.RENT);

        assertThat(foodCosts).hasSize(2);
        assertThat(rents).hasSize(1);
    }

    @Test
    @DisplayName("7. softDelete sets deletedAt and expense disappears from findAllByTenantId")
    void softDeletedExpenseDisappearsFromList() {
        Expense expense = createExpense(tenantA, "400.00", ExpenseCategory.UTILITIES, LocalDate.now());
        entityManager.persist(expense);
        entityManager.flush();

        assertThat(expenseRepository.findAllByTenantId(tenantA)).hasSize(1);

        expense.softDelete(UUID.randomUUID());
        entityManager.merge(expense);
        entityManager.flush();
        entityManager.clear();

        assertThat(expenseRepository.findAllByTenantId(tenantA)).isEmpty();
    }

    @Test
    @DisplayName("8. branchId is nullable for tenant-wide expenses")
    void branchIdIsNullableForTenantWideExpenses() {
        Expense expense = createExpense(tenantA, "5000.00", ExpenseCategory.RENT, LocalDate.now());
        // branchId intentionally not set (null)
        entityManager.persist(expense);
        entityManager.flush();

        Optional<Expense> found = expenseRepository.findByIdAndTenantId(expense.getId(), tenantA);

        assertThat(found).isPresent();
        assertThat(found.get().getBranchId()).isNull();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Expense createExpense(UUID tenantId, String amount, ExpenseCategory category, LocalDate date) {
        Expense expense = new Expense();
        expense.setTenantId(tenantId);
        expense.setAmount(new BigDecimal(amount));
        expense.setExpenseCategory(category);
        expense.setExpenseDate(date);
        return expense;
    }
}
