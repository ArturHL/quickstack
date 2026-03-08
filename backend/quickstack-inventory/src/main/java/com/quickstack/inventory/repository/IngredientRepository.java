package com.quickstack.inventory.repository;

import com.quickstack.inventory.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Ingredient entity with tenant-safe queries.
 * All queries filter by tenantId to enforce multi-tenancy.
 * Soft-deleted ingredients are excluded from default queries.
 */
@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    @Query("SELECT i FROM Ingredient i WHERE i.tenantId = :tenantId AND i.deletedAt IS NULL ORDER BY i.name ASC")
    List<Ingredient> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT i FROM Ingredient i WHERE i.id = :id AND i.tenantId = :tenantId AND i.deletedAt IS NULL")
    Optional<Ingredient> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT i FROM Ingredient i WHERE i.tenantId = :tenantId AND i.currentStock < i.minimumStock AND i.deletedAt IS NULL ORDER BY i.name ASC")
    List<Ingredient> findLowStockByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Ingredient i WHERE i.name = :name AND i.tenantId = :tenantId AND i.deletedAt IS NULL")
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Ingredient i WHERE i.name = :name AND i.tenantId = :tenantId AND i.id != :excludeId AND i.deletedAt IS NULL")
    boolean existsByNameAndTenantIdAndIdNot(
            @Param("name") String name,
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);
}
