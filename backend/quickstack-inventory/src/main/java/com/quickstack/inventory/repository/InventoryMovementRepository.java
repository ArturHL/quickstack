package com.quickstack.inventory.repository;

import com.quickstack.inventory.entity.InventoryMovement;
import com.quickstack.inventory.entity.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for InventoryMovement entity.
 * Records are NEVER deleted — this is an immutable audit log.
 */
@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    @Query("SELECT m FROM InventoryMovement m WHERE m.tenantId = :tenantId AND m.ingredientId = :ingredientId ORDER BY m.createdAt DESC")
    List<InventoryMovement> findByTenantIdAndIngredientId(
            @Param("tenantId") UUID tenantId,
            @Param("ingredientId") UUID ingredientId);

    @Query("SELECT m FROM InventoryMovement m WHERE m.tenantId = :tenantId AND m.movementType = :type ORDER BY m.createdAt DESC")
    List<InventoryMovement> findByTenantIdAndMovementType(
            @Param("tenantId") UUID tenantId,
            @Param("type") MovementType type);

    @Query("SELECT m FROM InventoryMovement m WHERE m.tenantId = :tenantId AND m.referenceId = :referenceId ORDER BY m.createdAt ASC")
    List<InventoryMovement> findByTenantIdAndReferenceId(
            @Param("tenantId") UUID tenantId,
            @Param("referenceId") UUID referenceId);
}
