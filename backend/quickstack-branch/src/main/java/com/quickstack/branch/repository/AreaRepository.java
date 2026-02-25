package com.quickstack.branch.repository;

import com.quickstack.branch.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Area entity with tenant-safe queries.
 * <p>
 * All queries filter by tenant_id to enforce multi-tenancy.
 * Soft-deleted areas are excluded from default queries.
 * <p>
 * ASVS V4.1: Access control - all queries enforce tenant isolation.
 */
@Repository
public interface AreaRepository extends JpaRepository<Area, UUID> {

    /**
     * Finds all non-deleted areas belonging to a specific branch and tenant.
     * Results are ordered by sort_order for display consistency.
     *
     * @param branchId the branch ID
     * @param tenantId the tenant ID
     * @return list of areas ordered by sort_order
     */
    @Query("SELECT a FROM Area a WHERE a.branchId = :branchId AND a.tenantId = :tenantId AND a.deletedAt IS NULL ORDER BY a.sortOrder ASC")
    List<Area> findAllByBranchIdAndTenantId(
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId);

    /**
     * Finds an area by ID and tenant ID, excluding soft-deleted.
     * Returns empty if the area belongs to another tenant (IDOR protection).
     *
     * @param id       the area ID
     * @param tenantId the tenant ID
     * @return Optional containing the area if found and belongs to tenant
     */
    @Query("SELECT a FROM Area a WHERE a.id = :id AND a.tenantId = :tenantId AND a.deletedAt IS NULL")
    Optional<Area> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Checks if an area with the given name already exists in the specified branch.
     * Excludes soft-deleted areas.
     *
     * @param name     the area name
     * @param branchId the branch ID
     * @param tenantId the tenant ID
     * @return true if an area with this name already exists in the branch
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Area a WHERE a.name = :name AND a.branchId = :branchId AND a.tenantId = :tenantId AND a.deletedAt IS NULL")
    boolean existsByNameAndBranchIdAndTenantId(
            @Param("name") String name,
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId);

    /**
     * Checks if an area with the given name exists in the branch, excluding a specific area ID.
     * Used during update validation to allow keeping the same name.
     *
     * @param name      the area name
     * @param branchId  the branch ID
     * @param tenantId  the tenant ID
     * @param excludeId the area ID to exclude from the check
     * @return true if another area with this name exists in the branch
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Area a WHERE a.name = :name AND a.branchId = :branchId AND a.tenantId = :tenantId AND a.id != :excludeId AND a.deletedAt IS NULL")
    boolean existsByNameAndBranchIdAndTenantIdAndIdNot(
            @Param("name") String name,
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);
}
