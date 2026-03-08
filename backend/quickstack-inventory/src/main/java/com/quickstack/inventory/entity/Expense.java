package com.quickstack.inventory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Business expense recorded by the OWNER.
 * <p>
 * Expenses live in quickstack-inventory because the actor (OWNER) and the
 * cycle of change (manual, infrequent) are the same as for ingredients
 * and recipes (ADR-006). COGS calculation for P&L crosses InventoryMovement
 * and Expense in the same module.
 * <p>
 * Business Rules:
 * - Soft delete: OWNER may correct mistakes.
 * - amount must be strictly positive.
 * - expenseDate is a date (not timestamp) — expenses are recorded by day.
 * - branchId is nullable (some expenses are tenant-wide, e.g., rent).
 * - receiptUrl is optional; a future phase may add photo upload via WhatsApp.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 30)
    private ExpenseCategory expenseCategory;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(length = 500)
    private String description;

    @Column(name = "receipt_url", length = 500)
    private String receiptUrl;

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(UUID deletedByUserId) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUserId;
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public ExpenseCategory getExpenseCategory() { return expenseCategory; }
    public void setExpenseCategory(ExpenseCategory expenseCategory) { this.expenseCategory = expenseCategory; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public UUID getDeletedBy() { return deletedBy; }
    public void setDeletedBy(UUID deletedBy) { this.deletedBy = deletedBy; }
}
