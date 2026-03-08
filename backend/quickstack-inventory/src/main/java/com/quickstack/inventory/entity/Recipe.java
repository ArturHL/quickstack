package com.quickstack.inventory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Recipe for a product or variant — defines what ingredients are consumed
 * when one unit of that product is sold.
 * <p>
 * productId is a logical FK to products (no DB-level FK — cross-module boundary).
 * variantId is nullable: if null, the recipe applies to the base product and
 * all its variants. A variant-specific recipe takes priority over the base recipe
 * (implemented in RecipeService.findRecipeForOrderItem).
 * <p>
 * Business Rules:
 * - One recipe per (productId, variantId) pair within a tenant.
 * - When OWNER updates a recipe, the entire item list is replaced (upsert semantics).
 * - isActive allows disabling auto-deduction without deleting the recipe.
 */
/**
 * NOTE: Uniqueness is enforced by two partial indexes at the DB level (see V9 migration):
 * - uq_recipes_base_per_product: unique (tenant_id, product_id) WHERE variant_id IS NULL
 * - uq_recipes_variant_per_product: unique (tenant_id, product_id, variant_id) WHERE variant_id IS NOT NULL
 * JPA @UniqueConstraint cannot express partial indexes, so the constraint lives in SQL only.
 */
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public UUID getVariantId() { return variantId; }
    public void setVariantId(UUID variantId) { this.variantId = variantId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
