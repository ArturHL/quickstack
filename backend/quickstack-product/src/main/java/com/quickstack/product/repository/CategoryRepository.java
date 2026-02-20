package com.quickstack.product.repository;

import com.quickstack.product.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Category entity with tenant-safe queries.
 * <p>
 * All queries automatically filter by tenant_id to enforce multi-tenancy.
 * Soft-deleted categories (deleted_at != null) are excluded from default queries.
 * <p>
 * ASVS V4.1: Access control - all queries enforce tenant isolation.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /**
     * Finds all active (non-deleted) categories for a tenant.
     * <p>
     * Excludes soft-deleted categories (deleted_at IS NULL).
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of active categories
     */
    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.isActive = true AND c.deletedAt IS NULL")
    Page<Category> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Finds all categories (active and inactive) for a tenant, excluding soft-deleted.
     * <p>
     * Used by OWNER/MANAGER roles with includeInactive=true parameter.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of all non-deleted categories (regardless of is_active status)
     */
    @Query("SELECT c FROM Category c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL")
    Page<Category> findAllByTenantIdIncludingInactive(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Finds a category by ID and tenant ID.
     * <p>
     * Includes inactive categories but excludes soft-deleted.
     * Returns 404 if category doesn't exist or belongs to another tenant (IDOR protection).
     *
     * @param id the category ID
     * @param tenantId the tenant ID
     * @return Optional containing the category if found and belongs to tenant
     */
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.tenantId = :tenantId AND c.deletedAt IS NULL")
    Optional<Category> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a category with the given name exists in the same parent level and tenant.
     * <p>
     * Used for validation before creating a new category.
     * Name must be unique within the same parent level (parent_id).
     *
     * @param name the category name
     * @param tenantId the tenant ID
     * @param parentId the parent category ID (null for top-level categories)
     * @return true if a category with this name already exists
     */
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Category c
        WHERE c.name = :name
        AND c.tenantId = :tenantId
        AND ((:parentId IS NULL AND c.parentId IS NULL) OR c.parentId = :parentId)
        AND c.deletedAt IS NULL
        """)
    boolean existsByNameAndTenantIdAndParentId(
        @Param("name") String name,
        @Param("tenantId") UUID tenantId,
        @Param("parentId") UUID parentId
    );

    /**
     * Checks if a category with the given name exists, excluding a specific category ID.
     * <p>
     * Used for validation during category update to allow keeping the same name.
     *
     * @param name the category name
     * @param tenantId the tenant ID
     * @param parentId the parent category ID (null for top-level categories)
     * @param excludeId the category ID to exclude from the check
     * @return true if another category with this name exists
     */
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Category c
        WHERE c.name = :name
        AND c.tenantId = :tenantId
        AND ((:parentId IS NULL AND c.parentId IS NULL) OR c.parentId = :parentId)
        AND c.id != :excludeId
        AND c.deletedAt IS NULL
        """)
    boolean existsByNameAndTenantIdAndParentIdAndIdNot(
        @Param("name") String name,
        @Param("tenantId") UUID tenantId,
        @Param("parentId") UUID parentId,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Finds a category by ID and tenant ID, including soft-deleted records.
     * <p>
     * Used exclusively for restore operations where we need to find records
     * that have been soft-deleted.
     *
     * @param id the category ID
     * @param tenantId the tenant ID
     * @return Optional containing the category if found (deleted or not)
     */
    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.tenantId = :tenantId")
    Optional<Category> findByIdAndTenantIdIncludingDeleted(
        @Param("id") UUID id,
        @Param("tenantId") UUID tenantId
    );

    /**
     * Counts active products in a category.
     * <p>
     * Used to prevent deletion of categories that still contain active products.
     * Only counts products that are active and not soft-deleted.
     *
     * @param categoryId the category ID
     * @param tenantId the tenant ID
     * @return count of active products in the category
     */
    @Query("""
        SELECT COUNT(p)
        FROM Product p
        WHERE p.categoryId = :categoryId
        AND p.tenantId = :tenantId
        AND p.isActive = true
        AND p.deletedAt IS NULL
        """)
    long countActiveProductsByCategory(
        @Param("categoryId") UUID categoryId,
        @Param("tenantId") UUID tenantId
    );
}
