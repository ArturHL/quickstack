package com.quickstack.product.repository;

import com.quickstack.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findAllByProductIdAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(UUID productId, UUID tenantId);

    /**
     * Finds all active variants for a tenant, ordered by sort order.
     * <p>
     * Non-paginated bulk query used by MenuService to load all variants in one database
     * round-trip, avoiding N+1 queries when building the full menu.
     *
     * @param tenantId the tenant ID
     * @return list of non-deleted variants ordered by sort_order ascending
     */
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.tenantId = :tenantId AND pv.deletedAt IS NULL ORDER BY pv.sortOrder ASC")
    List<ProductVariant> findAllActiveByTenantId(@Param("tenantId") UUID tenantId);

    Optional<ProductVariant> findByIdAndProductIdAndTenantId(UUID id, UUID productId, UUID tenantId);

    long countByProductIdAndTenantIdAndDeletedAtIsNull(UUID productId, UUID tenantId);

    boolean existsBySkuAndTenantId(String sku, UUID tenantId);

    boolean existsByNameAndProductIdAndTenantIdAndIdNot(String name, UUID productId, UUID tenantId, UUID excludeId);
    
    boolean existsByNameAndProductIdAndTenantId(String name, UUID productId, UUID tenantId);
    
    boolean existsBySkuAndTenantIdAndIdNot(String sku, UUID tenantId, UUID excludeId);

}
