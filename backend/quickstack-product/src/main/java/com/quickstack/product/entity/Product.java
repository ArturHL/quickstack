package com.quickstack.product.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Product entity representing items that can be sold in the POS system.
 * <p>
 * Products can be of three types:
 * - SIMPLE: Single-price item (e.g., Taco)
 * - VARIANT: Item with size/option variations (e.g., Coffee: Small/Medium/Large)
 * - COMBO: Bundle of products at special price (Phase 1.2)
 * <p>
 * Features:
 * - Multi-tenant isolation via tenant_id
 * - Soft delete (deleted_at, deleted_by)
 * - Category association
 * - Availability management (is_active vs is_available)
 * - Optional inventory tracking
 * - Variants for size-based pricing
 * <p>
 * Business Rules:
 * - SKU must be unique within tenant
 * - VARIANT products must have at least one ProductVariant
 * - is_active: permanent removal from catalog
 * - is_available: temporary out-of-stock status
 * - Price must be non-negative
 * <p>
 * ASVS Compliance:
 * - V4.1: Multi-tenant isolation enforced at database level
 * - V7: Audit trail with created_by, updated_by, deleted_by
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "category_id")
    private UUID categoryId;

    // Information
    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String sku;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Pricing
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    // Type
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType = ProductType.SIMPLE;

    // Availability
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "available_from")
    private LocalTime availableFrom;

    @Column(name = "available_until")
    private LocalTime availableUntil;

    // Inventory Integration
    @Column(name = "track_inventory", nullable = false)
    private boolean trackInventory = false;

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

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    // Relationships
    @OneToMany(mappedBy = "productId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ProductVariant> variants = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Business methods
    // -------------------------------------------------------------------------

    /**
     * Gets the effective price for this product.
     * <p>
     * For SIMPLE products, returns the base price.
     * For VARIANT products, returns null (price depends on selected variant).
     * For COMBO products, returns null (handled separately in Phase 1.2).
     *
     * @return the effective price, or null if price depends on variant/combo selection
     */
    public BigDecimal getEffectivePrice() {
        if (productType == ProductType.SIMPLE) {
            return basePrice;
        }
        return null; // VARIANT and COMBO prices are calculated separately
    }

    /**
     * Checks if this product has been soft deleted.
     *
     * @return true if deletedAt is not null, false otherwise
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Performs a soft delete by setting deletedAt and deletedBy.
     *
     * @param deletedByUserId the ID of the user performing the deletion
     */
    public void softDelete(UUID deletedByUserId) {
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUserId;
        this.isActive = false;
    }

    /**
     * Restores a soft-deleted product.
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public LocalTime getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalTime availableFrom) {
        this.availableFrom = availableFrom;
    }

    public LocalTime getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(LocalTime availableUntil) {
        this.availableUntil = availableUntil;
    }

    public boolean isTrackInventory() {
        return trackInventory;
    }

    public void setTrackInventory(boolean trackInventory) {
        this.trackInventory = trackInventory;
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

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }
}
