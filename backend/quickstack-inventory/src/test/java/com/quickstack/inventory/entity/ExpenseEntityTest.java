package com.quickstack.inventory.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Expense entity")
class ExpenseEntityTest {

    @Test
    @DisplayName("1. New expense is not deleted")
    void newExpenseIsNotDeleted() {
        Expense expense = new Expense();

        assertThat(expense.isDeleted()).isFalse();
        assertThat(expense.getDeletedAt()).isNull();
        assertThat(expense.getDeletedBy()).isNull();
    }

    @Test
    @DisplayName("2. softDelete sets deletedAt and deletedBy")
    void softDeleteSetsFields() {
        Expense expense = new Expense();
        UUID deletedBy = UUID.randomUUID();

        expense.softDelete(deletedBy);

        assertThat(expense.isDeleted()).isTrue();
        assertThat(expense.getDeletedAt()).isNotNull();
        assertThat(expense.getDeletedBy()).isEqualTo(deletedBy);
    }

    @Test
    @DisplayName("3. isDeleted returns false before softDelete is called")
    void isDeletedReturnsFalseInitially() {
        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("150.00"));
        expense.setExpenseCategory(ExpenseCategory.FOOD_COST);
        expense.setExpenseDate(LocalDate.now());

        assertThat(expense.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("4. branchId is nullable for tenant-wide expenses")
    void branchIdIsNullableForTenantWideExpenses() {
        Expense expense = new Expense();
        expense.setAmount(new BigDecimal("5000.00"));
        expense.setExpenseCategory(ExpenseCategory.RENT);
        expense.setExpenseDate(LocalDate.now());

        assertThat(expense.getBranchId()).isNull();
    }

    @Test
    @DisplayName("5. Expense stores all categories correctly")
    void expenseStoresAllCategories() {
        for (ExpenseCategory category : ExpenseCategory.values()) {
            Expense expense = new Expense();
            expense.setExpenseCategory(category);
            assertThat(expense.getExpenseCategory()).isEqualTo(category);
        }
    }
}
