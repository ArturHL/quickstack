package com.quickstack.product.repository;

import com.quickstack.product.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Modifier entities.
 * <p>
 * All queries enforce multi-tenant isolation (tenant_id) and exclude soft-deleted records.
 * <p>
 * ASVS Compliance:
 * - V4.1: All queries include tenant_id to prevent cross-tenant data access (IDOR prevention)
 */
@Repository
public interface ModifierRepository extends JpaRepository<Modifier, UUID> {

    /**
     * Finds all active, non-deleted modifiers for a modifier group, ordered by sort_order.
     *
     * @param modifierGroupId the modifier group ID
     * @param tenantId        the tenant ID
     * @return list of active modifiers ordered by sort_order ascending
     */
    @Query("SELECT m FROM Modifier m WHERE m.modifierGroupId = :modifierGroupId AND m.tenantId = :tenantId AND m.isActive = true AND m.deletedAt IS NULL ORDER BY m.sortOrder ASC")
    List<Modifier> findAllByModifierGroupIdAndTenantId(
            @Param("modifierGroupId") UUID modifierGroupId,
            @Param("tenantId") UUID tenantId);

    /**
     * Finds a non-deleted modifier by ID with tenant isolation.
     *
     * @param id       the modifier ID
     * @param tenantId the tenant ID
     * @return Optional containing the modifier, or empty if not found or soft-deleted
     */
    @Query("SELECT m FROM Modifier m WHERE m.id = :id AND m.tenantId = :tenantId AND m.deletedAt IS NULL")
    Optional<Modifier> findByIdAndTenantId(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId);

    /**
     * Checks if a modifier with the given name exists within a group and tenant.
     * Used for uniqueness validation on creation.
     *
     * @param name            the modifier name
     * @param modifierGroupId the modifier group ID
     * @param tenantId        the tenant ID
     * @return true if a modifier with that name already exists in the group
     */
    boolean existsByNameAndModifierGroupIdAndTenantId(String name, UUID modifierGroupId, UUID tenantId);

    /**
     * Finds all non-deleted modifiers (active and inactive) for a modifier group.
     * Used for cascade soft-delete when a modifier group is deleted.
     *
     * @param modifierGroupId the modifier group ID
     * @param tenantId        the tenant ID
     * @return list of all non-deleted modifiers in the group
     */
    @Query("SELECT m FROM Modifier m WHERE m.modifierGroupId = :modifierGroupId AND m.tenantId = :tenantId AND m.deletedAt IS NULL")
    List<Modifier> findAllNonDeletedByModifierGroupIdAndTenantId(
            @Param("modifierGroupId") UUID modifierGroupId,
            @Param("tenantId") UUID tenantId);

    /**
     * Counts active (non-deleted, is_active = true) modifiers in a modifier group.
     * Used to prevent deletion of the last active modifier in a group.
     *
     * @param modifierGroupId the modifier group ID
     * @param tenantId        the tenant ID
     * @return count of active modifiers
     */
    @Query("SELECT COUNT(m) FROM Modifier m WHERE m.modifierGroupId = :modifierGroupId AND m.tenantId = :tenantId AND m.isActive = true AND m.deletedAt IS NULL")
    long countActiveByModifierGroupId(
            @Param("modifierGroupId") UUID modifierGroupId,
            @Param("tenantId") UUID tenantId);
}
