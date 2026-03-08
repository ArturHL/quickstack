package com.quickstack.inventory.repository;

import com.quickstack.inventory.entity.Expense;
import com.quickstack.inventory.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Expense entity with tenant-safe queries.
 * Soft-deleted expenses are excluded from default queries.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("SELECT e FROM Expense e WHERE e.tenantId = :tenantId AND e.deletedAt IS NULL ORDER BY e.expenseDate DESC")
    List<Expense> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.tenantId = :tenantId AND e.deletedAt IS NULL")
    Optional<Expense> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT e FROM Expense e WHERE e.tenantId = :tenantId AND e.expenseDate BETWEEN :from AND :to AND e.deletedAt IS NULL ORDER BY e.expenseDate DESC")
    List<Expense> findByTenantIdAndDateRange(
            @Param("tenantId") UUID tenantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT e FROM Expense e WHERE e.tenantId = :tenantId AND e.expenseCategory = :category AND e.deletedAt IS NULL ORDER BY e.expenseDate DESC")
    List<Expense> findByTenantIdAndCategory(
            @Param("tenantId") UUID tenantId,
            @Param("category") ExpenseCategory category);
}
