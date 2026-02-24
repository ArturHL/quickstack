package com.quickstack.product.repository;

import com.quickstack.product.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Combo entities.
 * <p>
 * All queries enforce multi-tenant isolation (tenant_id) and exclude soft-deleted records.
 * <p>
 * ASVS Compliance:
 * - V4.1: All queries include tenant_id to prevent cross-tenant data access (IDOR prevention)
 */
@Repository
public interface ComboRepository extends JpaRepository<Combo, UUID> {

    /**
     * Finds all non-deleted active combos for a tenant, ordered by sort_order.
     *
     * @param tenantId the tenant ID
     * @return list of combos ordered by sort_order ascending
     */
    @Query("SELECT c FROM Combo c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL ORDER BY c.sortOrder ASC")
    List<Combo> findAllByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds a non-deleted combo by ID with tenant isolation.
     *
     * @param id       the combo ID
     * @param tenantId the tenant ID
     * @return Optional containing the combo, or empty if not found or soft-deleted
     */
    @Query("SELECT c FROM Combo c WHERE c.id = :id AND c.tenantId = :tenantId AND c.deletedAt IS NULL")
    Optional<Combo> findByIdAndTenantId(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId);

    /**
     * Checks if a combo with the given name exists within a tenant.
     * Used for uniqueness validation on creation.
     *
     * @param name     the combo name
     * @param tenantId the tenant ID
     * @return true if a combo with that name already exists
     */
    boolean existsByNameAndTenantId(String name, UUID tenantId);

    /**
     * Checks if a combo with the given name exists within a tenant, excluding a specific ID.
     * Used for uniqueness validation on update.
     *
     * @param name      the combo name
     * @param tenantId  the tenant ID
     * @param excludeId the ID to exclude from the check (the combo being updated)
     * @return true if another combo with that name already exists
     */
    boolean existsByNameAndTenantIdAndIdNot(String name, UUID tenantId, UUID excludeId);
}
