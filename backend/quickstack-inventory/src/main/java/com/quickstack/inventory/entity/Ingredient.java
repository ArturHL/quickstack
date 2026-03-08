package com.quickstack.inventory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Raw material or supply used in recipes.
 * <p>
 * An ingredient belongs to a tenant. branchId is nullable — an ingredient
 * can be global (shared across all branches of a tenant) or specific to
 * one branch. For MVP, most owners will use global ingredients.
 * <p>
 * Business Rules:
 * - currentStock and minimumStock are in the same unit as the unit field.
 * - costPerUnit is the current purchase cost (historical costs tracked in InventoryMovement).
 * - isBelowMinimum() is a derived state: currentStock < minimumStock.
 * - Soft delete: ingredient is never physically removed (recipes may reference it).
 */
@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnitType unit;

    @Column(name = "cost_per_unit", nullable = false, precision = 10, scale = 4)
    private BigDecimal costPerUnit;

    @Column(name = "current_stock", nullable = false, precision = 12, scale = 4)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "minimum_stock", nullable = false, precision = 12, scale = 4)
    private BigDecimal minimumStock = BigDecimal.ZERO;

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

    public boolean isBelowMinimum() {
        return currentStock.compareTo(minimumStock) < 0;
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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UnitType getUnit() { return unit; }
    public void setUnit(UnitType unit) { this.unit = unit; }

    public BigDecimal getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(BigDecimal costPerUnit) { this.costPerUnit = costPerUnit; }

    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal currentStock) { this.currentStock = currentStock; }

    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal minimumStock) { this.minimumStock = minimumStock; }

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
