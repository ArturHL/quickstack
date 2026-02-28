package com.quickstack.pos.repository;

import com.quickstack.pos.AbstractRepositoryTest;
import com.quickstack.pos.entity.Payment;
import com.quickstack.pos.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for PaymentRepository using Testcontainers + Flyway.
 * Verifies:
 * - findAllByOrderIdAndTenantId (with IDOR protection)
 * - sumPaymentsByOrder (including zero case and multiple payments)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Payment Repository")
class PaymentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;
    private UUID orderId;
    private UUID orderIdB;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Pay Plan', ?, 0, 5, 5)")
                .setParameter(1, planId)
                .setParameter(2, "PAY-" + planId)
                .executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant A', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantA)
                .setParameter(2, "pay-ta-" + tenantA)
                .setParameter(3, planId)
                .executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant B', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantB)
                .setParameter(2, "pay-tb-" + tenantB)
                .setParameter(3, planId)
                .executeUpdate();

        UUID branchA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO branches (id, tenant_id, name, is_active) VALUES (?, ?, 'Branch A', true)")
                .setParameter(1, branchA)
                .setParameter(2, tenantA)
                .executeUpdate();

        UUID branchB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO branches (id, tenant_id, name, is_active) VALUES (?, ?, 'Branch B', true)")
                .setParameter(1, branchB)
                .setParameter(2, tenantB)
                .executeUpdate();

        orderId = persistOrderNative(tenantA, branchA, "ORD-PAY-001");
        orderIdB = persistOrderNative(tenantB, branchB, "ORD-PAY-002");

        entityManager.flush();
        entityManager.clear();
    }

    // -------------------------------------------------------------------------
    // findAllByOrderIdAndTenantId
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAllByOrderIdAndTenantId()")
    class FindAllByOrderIdAndTenantId {

        @Test
        @DisplayName("1. Returns payments for the correct order and tenant")
        void returnsPaymentsForOrder() {
            persistPayment(tenantA, orderId, new BigDecimal("100.00"));

            List<Payment> result = paymentRepository.findAllByOrderIdAndTenantId(orderId, tenantA);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("2. Returns empty for wrong tenant (IDOR protection)")
        void returnsEmptyForWrongTenant() {
            persistPayment(tenantA, orderId, new BigDecimal("100.00"));

            List<Payment> result = paymentRepository.findAllByOrderIdAndTenantId(orderId, tenantB);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("3. Returns empty when no payments exist for order")
        void returnsEmptyWhenNoPayments() {
            List<Payment> result = paymentRepository.findAllByOrderIdAndTenantId(orderId, tenantA);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("4. Returns multiple payments when several exist")
        void returnsMultiplePayments() {
            persistPayment(tenantA, orderId, new BigDecimal("50.00"));
            persistPayment(tenantA, orderId, new BigDecimal("60.00"));

            List<Payment> result = paymentRepository.findAllByOrderIdAndTenantId(orderId, tenantA);

            assertThat(result).hasSize(2);
        }
    }

    // -------------------------------------------------------------------------
    // sumPaymentsByOrder
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("sumPaymentsByOrder()")
    class SumPaymentsByOrder {

        @Test
        @DisplayName("5. Returns 0 when no payments exist")
        void returnsZeroWhenNoPayments() {
            BigDecimal sum = paymentRepository.sumPaymentsByOrder(orderId, tenantA);

            assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("6. Returns correct sum for a single payment")
        void returnsCorrectSumSingle() {
            persistPayment(tenantA, orderId, new BigDecimal("89.00"));

            BigDecimal sum = paymentRepository.sumPaymentsByOrder(orderId, tenantA);

            assertThat(sum).isEqualByComparingTo("89.00");
        }

        @Test
        @DisplayName("7. Returns correct sum for multiple payments")
        void returnsCorrectSumMultiple() {
            persistPayment(tenantA, orderId, new BigDecimal("50.00"));
            persistPayment(tenantA, orderId, new BigDecimal("39.50"));

            BigDecimal sum = paymentRepository.sumPaymentsByOrder(orderId, tenantA);

            assertThat(sum).isEqualByComparingTo("89.50");
        }

        @Test
        @DisplayName("8. Cross-tenant: returns 0 for payments belonging to another tenant")
        void returnZeroForWrongTenant() {
            persistPayment(tenantA, orderId, new BigDecimal("100.00"));

            BigDecimal sum = paymentRepository.sumPaymentsByOrder(orderId, tenantB);

            assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID persistOrderNative(UUID tenantId, UUID branchId, String orderNumber) {
        UUID id = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO orders (id, tenant_id, branch_id, order_number, daily_sequence, " +
                        "service_type, status_id, subtotal, tax_rate, tax, discount, total, source, " +
                        "opened_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 1, 'COUNTER', 'd3333333-3333-3333-3333-333333333333', " +
                        "89.00, 0.16, 14.24, 0, 89.00, 'POS', NOW(), NOW(), NOW())")
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .setParameter(3, branchId)
                .setParameter(4, orderNumber)
                .executeUpdate();
        return id;
    }

    private void persistPayment(UUID tenantId, UUID forOrderId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setOrderId(forOrderId);
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setAmount(amount);
        payment.setAmountReceived(amount);
        payment.setChangeGiven(BigDecimal.ZERO);
        entityManager.persist(payment);
        entityManager.flush();
        entityManager.clear();
    }
}
