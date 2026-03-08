package com.quickstack.inventory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit trail of every inventory change.
 * <p>
 * This record is NEVER deleted — it is the financial audit log for COGS.
 * quantityDelta is negative for SALE_DEDUCTION, positive for PURCHASE
 * and MANUAL_ADJUSTMENT that adds stock.
 * <p>
 * unitCostAtTime records the ingredient's cost per unit AT THE MOMENT of the
 * movement (ADR-007: historical cost pricing). This ensures P&L reports are
 * historically accurate even if the OWNER later changes the ingredient cost.
 * <p>
 * referenceId links the movement to its source document (e.g., the orderId
 * for SALE_DEDUCTION movements).
 */
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    @Column(name = "quantity_delta", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityDelta;

    @Column(name = "unit_cost_at_time", precision = 10, scale = 4)
    private BigDecimal unitCostAtTime;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getIngredientId() { return ingredientId; }
    public void setIngredientId(UUID ingredientId) { this.ingredientId = ingredientId; }

    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }

    public BigDecimal getQuantityDelta() { return quantityDelta; }
    public void setQuantityDelta(BigDecimal quantityDelta) { this.quantityDelta = quantityDelta; }

    public BigDecimal getUnitCostAtTime() { return unitCostAtTime; }
    public void setUnitCostAtTime(BigDecimal unitCostAtTime) { this.unitCostAtTime = unitCostAtTime; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
