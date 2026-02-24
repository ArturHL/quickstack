package com.quickstack.product.repository;

import com.quickstack.product.entity.ComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ComboItem entities.
 * <p>
 * ComboItems are hard-deleted (no soft delete). They follow their parent combo's lifecycle.
 * <p>
 * ASVS Compliance:
 * - V4.1: All queries include tenant_id to prevent cross-tenant data access (IDOR prevention)
 */
@Repository
public interface ComboItemRepository extends JpaRepository<ComboItem, UUID> {

    /**
     * Finds all items for a specific combo, ordered by sort_order.
     *
     * @param comboId  the combo ID
     * @param tenantId the tenant ID
     * @return list of combo items ordered by sort_order ascending
     */
    @Query("SELECT ci FROM ComboItem ci WHERE ci.comboId = :comboId AND ci.tenantId = :tenantId ORDER BY ci.sortOrder ASC")
    List<ComboItem> findAllByComboIdAndTenantId(
            @Param("comboId") UUID comboId,
            @Param("tenantId") UUID tenantId);

    /**
     * Finds all items for multiple combos in a single query (batch load to avoid N+1).
     *
     * @param comboIds the set of combo IDs
     * @param tenantId the tenant ID
     * @return list of combo items for all specified combos, ordered by sort_order
     */
    @Query("SELECT ci FROM ComboItem ci WHERE ci.comboId IN :comboIds AND ci.tenantId = :tenantId ORDER BY ci.sortOrder ASC")
    List<ComboItem> findAllByTenantIdAndComboIdIn(
            @Param("tenantId") UUID tenantId,
            @Param("comboIds") Collection<UUID> comboIds);

    /**
     * Deletes all items for a specific combo. Used when replacing a combo's items on update.
     *
     * @param comboId  the combo ID whose items to delete
     * @param tenantId the tenant ID
     */
    @Modifying
    @Query("DELETE FROM ComboItem ci WHERE ci.comboId = :comboId AND ci.tenantId = :tenantId")
    void deleteAllByComboIdAndTenantId(
            @Param("comboId") UUID comboId,
            @Param("tenantId") UUID tenantId);
}
