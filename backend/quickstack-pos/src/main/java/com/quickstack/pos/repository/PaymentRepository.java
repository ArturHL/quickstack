package com.quickstack.pos.repository;

import com.quickstack.pos.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Returns all payments for an order scoped to a tenant.
     * Cross-tenant access returns empty list (IDOR protection — callers convert to 404 if needed).
     */
    List<Payment> findAllByOrderIdAndTenantId(UUID orderId, UUID tenantId);

    /**
     * Returns the sum of all payment amounts for an order within a tenant.
     * Returns 0 (via COALESCE) when no payments exist yet.
     * Used to determine if the order has been fully paid.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.orderId = :orderId AND p.tenantId = :tenantId")
    BigDecimal sumPaymentsByOrder(@Param("orderId") UUID orderId,
                                  @Param("tenantId") UUID tenantId);
}
