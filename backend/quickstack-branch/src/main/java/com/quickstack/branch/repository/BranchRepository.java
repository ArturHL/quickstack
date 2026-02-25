package com.quickstack.branch.repository;

import com.quickstack.branch.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Branch entity with tenant-safe queries.
 * <p>
 * All queries automatically filter by tenant_id to enforce multi-tenancy.
 * Soft-deleted branches (deleted_at != null) are excluded from default queries.
 * <p>
 * ASVS V4.1: Access control - all queries enforce tenant isolation.
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    /**
     * Finds all non-deleted branches for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of active branches
     */
    @Query("SELECT b FROM Branch b WHERE b.tenantId = :tenantId AND b.deletedAt IS NULL ORDER BY b.name ASC")
    List<Branch> findAllByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Finds a branch by ID and tenant ID, excluding soft-deleted.
     * Returns empty if the branch belongs to another tenant (IDOR protection).
     *
     * @param id       the branch ID
     * @param tenantId the tenant ID
     * @return Optional containing the branch if found and belongs to tenant
     */
    @Query("SELECT b FROM Branch b WHERE b.id = :id AND b.tenantId = :tenantId AND b.deletedAt IS NULL")
    Optional<Branch> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a branch with the given name already exists for the tenant.
     * Excludes soft-deleted branches.
     *
     * @param name     the branch name
     * @param tenantId the tenant ID
     * @return true if a branch with this name already exists
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Branch b WHERE b.name = :name AND b.tenantId = :tenantId AND b.deletedAt IS NULL")
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a branch with the given name exists, excluding a specific branch ID.
     * Used during update validation.
     *
     * @param name      the branch name
     * @param tenantId  the tenant ID
     * @param excludeId the branch ID to exclude from the check
     * @return true if another branch with this name exists
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Branch b WHERE b.name = :name AND b.tenantId = :tenantId AND b.id != :excludeId AND b.deletedAt IS NULL")
    boolean existsByNameAndTenantIdAndIdNot(
            @Param("name") String name,
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);

    /**
     * Checks if a branch with the given code already exists for the tenant.
     * Excludes soft-deleted branches.
     *
     * @param code     the branch code
     * @param tenantId the tenant ID
     * @return true if a branch with this code already exists
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Branch b WHERE b.code = :code AND b.tenantId = :tenantId AND b.deletedAt IS NULL")
    boolean existsByCodeAndTenantId(@Param("code") String code, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a branch with the given code exists, excluding a specific branch ID.
     * Used during update validation.
     *
     * @param code      the branch code
     * @param tenantId  the tenant ID
     * @param excludeId the branch ID to exclude from the check
     * @return true if another branch with this code exists
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Branch b WHERE b.code = :code AND b.tenantId = :tenantId AND b.id != :excludeId AND b.deletedAt IS NULL")
    boolean existsByCodeAndTenantIdAndIdNot(
            @Param("code") String code,
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);
}
