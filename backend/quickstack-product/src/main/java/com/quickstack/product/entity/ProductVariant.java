package com.quickstack.product.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product variant entity for size/type variations of a product.
 * <p>
 * Variants represent different sizes or options of a VARIANT-type product.
 * For example: "Café" product with variants "Chico" (+$0), "Mediano" (+$10), "Grande" (+$20).
 * <p>
 * Features:
 * - Multi-tenant isolation via tenant_id
 * - Soft delete (deleted_at only, no deleted_by for variants)
 * - Price adjustment relative to product base_price
 * - Default variant selection
 * - Sortable display order
 * <p>
 * Business Rules:
 * - Name must be unique within product
 * - SKU must be unique within tenant (if provided)
 * - Exactly one variant per product should have isDefault = true
 * - VARIANT products must have at least one active variant
 * - Effective price = product.basePrice + variant.priceAdjustment
 * - Price adjustment can be negative but effective price must be >= 0
 * <p>
 * ASVS Compliance:
 * - V4.1: Multi-tenant isolation enforced at database level
 * - V7: Audit trail with created_at, updated_at
 */
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    // Information
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String sku;

    // Pricing
    @Column(name = "price_adjustment", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    // Status
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    // Audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Calculates the effective price for this variant.
     * <p>
     * Effective price = product base price + this variant's price adjustment.
     *
     * @param productBasePrice the base price of the parent product
     * @return the effective price for this variant
     */
    public BigDecimal getEffectivePrice(BigDecimal productBasePrice) {
        return productBasePrice.add(priceAdjustment);
    }

    /**
     * Checks if this variant has been soft deleted.
     *
     * @return true if deletedAt is not null, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Performs a soft delete by setting deletedAt.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
        this.isActive = false;
    }

    /**
     * Restores a soft-deleted variant.
     */
    public void restore() {
        this.deletedAt = null;
        this.isActive = true;
    }

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

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }

    public void setPriceAdjustment(BigDecimal priceAdjustment) {
        this.priceAdjustment = priceAdjustment;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
