package com.quickstack.product.repository;

import com.quickstack.product.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ModifierGroup entities.
 * <p>
 * All queries enforce multi-tenant isolation (tenant_id) and exclude soft-deleted records.
 * <p>
 * ASVS Compliance:
 * - V4.1: All queries include tenant_id to prevent cross-tenant data access (IDOR prevention)
 */
@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {

    /**
     * Finds all non-deleted modifier groups for a product, ordered by sort_order.
     *
     * @param productId the product ID
     * @param tenantId  the tenant ID
     * @return list of modifier groups ordered by sort_order ascending
     */
    @Query("SELECT mg FROM ModifierGroup mg WHERE mg.productId = :productId AND mg.tenantId = :tenantId AND mg.deletedAt IS NULL ORDER BY mg.sortOrder ASC")
    List<ModifierGroup> findAllByProductIdAndTenantId(
            @Param("productId") UUID productId,
            @Param("tenantId") UUID tenantId);

    /**
     * Finds a non-deleted modifier group by ID with tenant isolation.
     *
     * @param id       the modifier group ID
     * @param tenantId the tenant ID
     * @return Optional containing the modifier group, or empty if not found or soft-deleted
     */
    @Query("SELECT mg FROM ModifierGroup mg WHERE mg.id = :id AND mg.tenantId = :tenantId AND mg.deletedAt IS NULL")
    Optional<ModifierGroup> findByIdAndTenantId(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId);

    /**
     * Checks if a modifier group with the given name exists for a product within a tenant.
     * Used for uniqueness validation on creation.
     *
     * @param name      the modifier group name
     * @param productId the product ID
     * @param tenantId  the tenant ID
     * @return true if a modifier group with that name already exists
     */
    boolean existsByNameAndProductIdAndTenantId(String name, UUID productId, UUID tenantId);

    /**
     * Checks if a modifier group with the given name exists for a product within a tenant,
     * excluding a specific ID. Used for uniqueness validation on update.
     *
     * @param name      the modifier group name
     * @param productId the product ID
     * @param tenantId  the tenant ID
     * @param excludeId the ID to exclude from the check (the group being updated)
     * @return true if another modifier group with that name already exists
     */
    boolean existsByNameAndProductIdAndTenantIdAndIdNot(String name, UUID productId, UUID tenantId, UUID excludeId);
}
