package com.quickstack.product.repository;

import com.quickstack.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity with tenant-safe queries and filtering support.
 * <p>
 * All queries automatically filter by tenant_id to enforce multi-tenancy.
 * Soft-deleted products (deleted_at != null) are excluded from default queries.
 * <p>
 * ASVS V4.1: Access control - all queries enforce tenant isolation.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * Finds all active and available products for a tenant.
     * <p>
     * Excludes soft-deleted products and inactive products.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of active and available products
     */
    @Query("""
        SELECT p FROM Product p
        WHERE p.tenantId = :tenantId
        AND p.isActive = true
        AND p.isAvailable = true
        AND p.deletedAt IS NULL
        """)
    Page<Product> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Finds products by tenant with optional filters.
     * <p>
     * All filters are optional (null = no filter):
     * - categoryId: filter by specific category
     * - isActive: filter by active status (true = active only, false = inactive only, null = both)
     * - isAvailable: filter by availability status
     * - nameSearch: case-insensitive LIKE search on product name
     * <p>
     * Always excludes soft-deleted products.
     *
     * @param tenantId the tenant ID
     * @param categoryId optional category filter
     * @param isActive optional active filter
     * @param isAvailable optional availability filter
     * @param nameSearch optional name search (case-insensitive partial match)
     * @param pageable pagination parameters
     * @return page of filtered products
     */
    @Query("""
        SELECT p FROM Product p
        WHERE p.tenantId = :tenantId
        AND p.deletedAt IS NULL
        AND (:categoryId IS NULL OR p.categoryId = :categoryId)
        AND (:isActive IS NULL OR p.isActive = :isActive)
        AND (:isAvailable IS NULL OR p.isAvailable = :isAvailable)
        AND (CAST(:nameSearch AS string) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:nameSearch AS string), '%')))
        """)
    Page<Product> findAllByTenantIdWithFilters(
        @Param("tenantId") UUID tenantId,
        @Param("categoryId") UUID categoryId,
        @Param("isActive") Boolean isActive,
        @Param("isAvailable") Boolean isAvailable,
        @Param("nameSearch") String nameSearch,
        Pageable pageable
    );

    /**
     * Finds a product by ID and tenant ID.
     * <p>
     * Includes inactive products but excludes soft-deleted.
     * Returns empty if product doesn't exist or belongs to another tenant (IDOR protection).
     *
     * @param id the product ID
     * @param tenantId the tenant ID
     * @return Optional containing the product if found and belongs to tenant
     */
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.tenantId = :tenantId AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a product with the given SKU exists in the tenant.
     * <p>
     * Used for validation before creating a new product.
     * SKU must be unique within tenant.
     *
     * @param sku the product SKU
     * @param tenantId the tenant ID
     * @return true if a product with this SKU already exists
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Product p
        WHERE p.sku = :sku
        AND p.tenantId = :tenantId
        AND p.deletedAt IS NULL
        """)
    boolean existsBySkuAndTenantId(@Param("sku") String sku, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a product with the given SKU exists, excluding a specific product ID.
     * <p>
     * Used for validation during product update to allow keeping the same SKU.
     *
     * @param sku the product SKU
     * @param tenantId the tenant ID
     * @param excludeId the product ID to exclude from the check
     * @return true if another product with this SKU exists
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Product p
        WHERE p.sku = :sku
        AND p.tenantId = :tenantId
        AND p.id != :excludeId
        AND p.deletedAt IS NULL
        """)
    boolean existsBySkuAndTenantIdAndIdNot(
        @Param("sku") String sku,
        @Param("tenantId") UUID tenantId,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Checks if a product with the given name exists in the same category and tenant.
     * <p>
     * Used for validation to ensure unique product names within a category.
     *
     * @param name the product name
     * @param tenantId the tenant ID
     * @param categoryId the category ID
     * @return true if a product with this name exists in the category
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Product p
        WHERE p.name = :name
        AND p.tenantId = :tenantId
        AND p.categoryId = :categoryId
        AND p.deletedAt IS NULL
        """)
    boolean existsByNameAndTenantIdAndCategoryId(
        @Param("name") String name,
        @Param("tenantId") UUID tenantId,
        @Param("categoryId") UUID categoryId
    );

    /**
     * Checks if a product with the given name exists, excluding a specific product ID.
     * <p>
     * Used for validation during product update to allow keeping the same name.
     *
     * @param name the product name
     * @param tenantId the tenant ID
     * @param categoryId the category ID
     * @param excludeId the product ID to exclude from the check
     * @return true if another product with this name exists in the category
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Product p
        WHERE p.name = :name
        AND p.tenantId = :tenantId
        AND p.categoryId = :categoryId
        AND p.id != :excludeId
        AND p.deletedAt IS NULL
        """)
    boolean existsByNameAndTenantIdAndCategoryIdAndIdNot(
        @Param("name") String name,
        @Param("tenantId") UUID tenantId,
        @Param("categoryId") UUID categoryId,
        @Param("excludeId") UUID excludeId
    );

    /**
     * Finds a product by ID and tenant ID, including soft-deleted records.
     * <p>
     * Used exclusively for restore operations.
     *
     * @param id the product ID
     * @param tenantId the tenant ID
     * @return Optional containing the product if found (deleted or not)
     */
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.tenantId = :tenantId")
    Optional<Product> findByIdAndTenantIdIncludingDeleted(
        @Param("id") UUID id,
        @Param("tenantId") UUID tenantId
    );
}
