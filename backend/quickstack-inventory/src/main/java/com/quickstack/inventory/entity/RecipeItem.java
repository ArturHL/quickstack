package com.quickstack.inventory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single ingredient line in a recipe.
 * <p>
 * quantity is expressed in the same unit as Ingredient.unit.
 * For example: if the ingredient is "Aguacate" with unit=UNIT,
 * a quantity of 2.0 means 2 whole avocados per dish.
 * <p>
 * Business Rules:
 * - One recipe cannot contain the same ingredient twice.
 *   Enforced by unique constraint uq_recipe_items_recipe_ingredient.
 * - quantity must be strictly positive.
 * - ingredientId is a logical FK (no DB-level FK — managed in service layer for IDOR checks).
 */
@Entity
@Table(name = "recipe_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recipe_items_recipe_ingredient",
                columnNames = {"recipe_id", "ingredient_id"}))
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(name = "ingredient_id", nullable = false)
    private UUID ingredientId;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;

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

    public UUID getRecipeId() { return recipeId; }
    public void setRecipeId(UUID recipeId) { this.recipeId = recipeId; }

    public UUID getIngredientId() { return ingredientId; }
    public void setIngredientId(UUID ingredientId) { this.ingredientId = ingredientId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
