package com.quickstack.branch.repository;

import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RestaurantTable entity with tenant-safe queries.
 * <p>
 * All queries filter by tenant_id to enforce multi-tenancy.
 * Soft-deleted tables are excluded from default queries.
 * <p>
 * Note: The entity maps to the {@code tables} SQL table. JPQL uses the entity
 * class name {@code RestaurantTable} to avoid confusion with reserved words.
 * <p>
 * ASVS V4.1: Access control - all queries enforce tenant isolation.
 */
@Repository
public interface TableRepository extends JpaRepository<RestaurantTable, UUID> {

    /**
     * Finds all non-deleted tables belonging to a specific area and tenant.
     * Results are ordered by sort_order for display consistency.
     *
     * @param areaId   the area ID
     * @param tenantId the tenant ID
     * @return list of tables ordered by sort_order
     */
    @Query("SELECT t FROM RestaurantTable t WHERE t.areaId = :areaId AND t.tenantId = :tenantId AND t.deletedAt IS NULL ORDER BY t.sortOrder ASC")
    List<RestaurantTable> findAllByAreaIdAndTenantId(
            @Param("areaId") UUID areaId,
            @Param("tenantId") UUID tenantId);

    /**
     * Finds a table by ID and tenant ID, excluding soft-deleted.
     * Returns empty if the table belongs to another tenant (IDOR protection).
     *
     * @param id       the table ID
     * @param tenantId the tenant ID
     * @return Optional containing the table if found and belongs to tenant
     */
    @Query("SELECT t FROM RestaurantTable t WHERE t.id = :id AND t.tenantId = :tenantId AND t.deletedAt IS NULL")
    Optional<RestaurantTable> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a table with the given number already exists in the specified area.
     * Excludes soft-deleted tables.
     *
     * @param number   the table number
     * @param areaId   the area ID
     * @param tenantId the tenant ID
     * @return true if a table with this number already exists in the area
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM RestaurantTable t WHERE t.number = :number AND t.areaId = :areaId AND t.tenantId = :tenantId AND t.deletedAt IS NULL")
    boolean existsByNumberAndAreaIdAndTenantId(
            @Param("number") String number,
            @Param("areaId") UUID areaId,
            @Param("tenantId") UUID tenantId);

    /**
     * Checks if a table with the given number exists in the area, excluding a specific table ID.
     * Used during update validation.
     *
     * @param number    the table number
     * @param areaId    the area ID
     * @param tenantId  the tenant ID
     * @param excludeId the table ID to exclude
     * @return true if another table with this number exists in the area
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM RestaurantTable t WHERE t.number = :number AND t.areaId = :areaId AND t.tenantId = :tenantId AND t.id != :excludeId AND t.deletedAt IS NULL")
    boolean existsByNumberAndAreaIdAndTenantIdAndIdNot(
            @Param("number") String number,
            @Param("areaId") UUID areaId,
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);

    /**
     * Finds all available tables for all areas within a branch.
     * <p>
     * Used by the POS to display which tables can accept new orders.
     * A table is available when: status=AVAILABLE, not soft-deleted, and is_active=true.
     *
     * @param branchId the branch ID (tables are looked up through their areas)
     * @param tenantId the tenant ID
     * @return list of available tables across all areas in the branch
     */
    @Query("""
            SELECT t FROM RestaurantTable t
            JOIN Area a ON t.areaId = a.id
            WHERE a.branchId = :branchId
            AND t.tenantId = :tenantId
            AND t.status = :status
            AND t.deletedAt IS NULL
            AND t.isActive = true
            ORDER BY a.sortOrder ASC, t.sortOrder ASC
            """)
    List<RestaurantTable> findAvailableByBranchIdAndTenantId(
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId,
            @Param("status") TableStatus status);
}
