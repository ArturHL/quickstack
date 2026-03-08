package com.quickstack.inventory.repository;

import com.quickstack.inventory.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Recipe entity.
 * Finds recipes by product (and optionally variant) within a tenant.
 */
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    @Query("SELECT r FROM Recipe r WHERE r.productId = :productId AND r.tenantId = :tenantId AND r.variantId IS NULL")
    Optional<Recipe> findBaseRecipe(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);

    @Query("SELECT r FROM Recipe r WHERE r.productId = :productId AND r.variantId = :variantId AND r.tenantId = :tenantId")
    Optional<Recipe> findVariantRecipe(
            @Param("productId") UUID productId,
            @Param("variantId") UUID variantId,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Recipe r WHERE r.productId = :productId AND r.tenantId = :tenantId AND r.variantId IS NULL")
    boolean existsBaseRecipe(@Param("productId") UUID productId, @Param("tenantId") UUID tenantId);
}
