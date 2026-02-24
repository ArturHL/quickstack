package com.quickstack.product.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * ComboItem entity representing a single product entry within a combo.
 * <p>
 * Each row associates a product with a combo, specifying quantity and
 * optional substitution rules.
 * <p>
 * Features:
 * - Multi-tenant isolation via tenant_id
 * - Hard delete (no deleted_at) — follows combo lifecycle via DB CASCADE
 * - Substitution groups: items sharing the same substitute_group can be swapped
 * <p>
 * Business Rules:
 * - quantity must be >= 1
 * - A combo must have at least 2 items (enforced at service level)
 * - The same product cannot appear twice in the same combo (DB unique constraint)
 * <p>
 * ASVS Compliance:
 * - V4.1: Multi-tenant isolation enforced at database level
 */
@Entity
@Table(name = "combo_items")
public class ComboItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "combo_id", nullable = false)
    private UUID comboId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // Quantity
    @Column(nullable = false)
    private int quantity = 1;

    // Substitution
    @Column(name = "allow_substitutes", nullable = false)
    private boolean allowSubstitutes = false;

    @Column(name = "substitute_group", length = 50)
    private String substituteGroup;

    // Display
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getComboId() {
        return comboId;
    }

    public void setComboId(UUID comboId) {
        this.comboId = comboId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAllowSubstitutes() {
        return allowSubstitutes;
    }

    public void setAllowSubstitutes(boolean allowSubstitutes) {
        this.allowSubstitutes = allowSubstitutes;
    }

    public String getSubstituteGroup() {
        return substituteGroup;
    }

    public void setSubstituteGroup(String substituteGroup) {
        this.substituteGroup = substituteGroup;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
