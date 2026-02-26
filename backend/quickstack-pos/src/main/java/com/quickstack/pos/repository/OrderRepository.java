package com.quickstack.pos.repository;

import com.quickstack.pos.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Finds an order by ID scoped to a tenant, eagerly loading items and their modifiers.
     * Cross-tenant access returns empty (IDOR protection — callers convert to 404).
     */
    @EntityGraph(attributePaths = {"items", "items.modifiers"})
    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Returns all orders for a branch within a tenant, paged.
     * Used by the branch order history view.
     */
    Page<Order> findAllByBranchIdAndTenantId(UUID branchId, UUID tenantId, Pageable pageable);

    /**
     * Returns orders for a branch whose opened_at date falls within [startDate, endDate].
     * Uses native SQL because JPQL DATE() function is not portable across databases.
     */
    @Query(value = "SELECT * FROM orders WHERE tenant_id = :tenantId AND branch_id = :branchId " +
                   "AND DATE(opened_at) BETWEEN :startDate AND :endDate ORDER BY opened_at DESC",
           countQuery = "SELECT COUNT(*) FROM orders WHERE tenant_id = :tenantId AND branch_id = :branchId " +
                        "AND DATE(opened_at) BETWEEN :startDate AND :endDate",
           nativeQuery = true)
    Page<Order> findOrdersByDateRange(@Param("tenantId") UUID tenantId,
                                      @Param("branchId") UUID branchId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      Pageable pageable);

    /**
     * Returns all non-terminal orders currently associated with a table.
     * Used to prevent double-assigning a table and to find the active order for a table.
     */
    @Query("SELECT o FROM Order o WHERE o.tableId = :tableId AND o.tenantId = :tenantId " +
           "AND o.statusId NOT IN :terminalStatusIds")
    List<Order> findOpenOrdersByTable(@Param("tableId") UUID tableId,
                                      @Param("tenantId") UUID tenantId,
                                      @Param("terminalStatusIds") List<UUID> terminalStatusIds);

    /**
     * Returns the next daily sequence number for a branch+date combination.
     * Result is MAX(daily_sequence) + 1, or 1 if no orders exist yet for that day.
     * This must be called within a transaction to avoid race conditions.
     */
    @Query(value = "SELECT COALESCE(MAX(daily_sequence), 0) + 1 FROM orders " +
                   "WHERE tenant_id = :tenantId AND branch_id = :branchId " +
                   "AND DATE(opened_at) = :date",
           nativeQuery = true)
    Integer getNextDailySequence(@Param("tenantId") UUID tenantId,
                                  @Param("branchId") UUID branchId,
                                  @Param("date") LocalDate date);

    /**
     * Checks whether an order number already exists within a tenant.
     * Used to detect collisions before persisting.
     */
    boolean existsByOrderNumberAndTenantId(String orderNumber, UUID tenantId);
}
