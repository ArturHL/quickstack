package com.quickstack.pos.repository;

import com.quickstack.pos.AbstractRepositoryTest;
import com.quickstack.pos.entity.Order;
import com.quickstack.pos.entity.OrderItem;
import com.quickstack.pos.entity.OrderStatusConstants;
import com.quickstack.pos.entity.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for OrderRepository using Testcontainers + Flyway.
 * <p>
 * Verifies:
 * - Multi-tenant isolation (IDOR protection)
 * - Entity graph eager loading
 * - Pagination for branch order history
 * - Date-range filtering
 * - Open orders by table
 * - Daily sequence calculation
 * - Order number uniqueness check
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Order Repository")
class OrderRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;
    private UUID branchA;
    private UUID branchB;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Test Plan', ?, 0, 10, 5)")
                .setParameter(1, planId)
                .setParameter(2, "TEST-" + planId)
                .executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant A', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantA)
                .setParameter(2, "ta-" + tenantA)
                .setParameter(3, planId)
                .executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant B', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantB)
                .setParameter(2, "tb-" + tenantB)
                .setParameter(3, planId)
                .executeUpdate();

        branchA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO branches (id, tenant_id, name, is_active) VALUES (?, ?, 'Branch A', true)")
                .setParameter(1, branchA)
                .setParameter(2, tenantA)
                .executeUpdate();

        branchB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO branches (id, tenant_id, name, is_active) VALUES (?, ?, 'Branch B', true)")
                .setParameter(1, branchB)
                .setParameter(2, tenantA)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    // -------------------------------------------------------------------------
    // findByIdAndTenantId
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findByIdAndTenantId()")
    class FindByIdAndTenantId {

        @Test
        @DisplayName("1. Returns order when found for correct tenant")
        void returnsOrderWhenFound() {
            Order order = persistOrder(tenantA, branchA, "ORD-001");

            Optional<Order> result = orderRepository.findByIdAndTenantId(order.getId(), tenantA);

            assertThat(result).isPresent();
            assertThat(result.get().getOrderNumber()).isEqualTo("ORD-001");
        }

        @Test
        @DisplayName("2. Returns empty for cross-tenant access (IDOR protection)")
        void returnsEmptyForCrossTenantAccess() {
            Order order = persistOrder(tenantA, branchA, "ORD-002");

            Optional<Order> result = orderRepository.findByIdAndTenantId(order.getId(), tenantB);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("3. Eagerly loads items via EntityGraph")
        void eagerlyLoadsItemsViaEntityGraph() {
            Order order = persistOrder(tenantA, branchA, "ORD-003");
            persistOrderItem(order);
            entityManager.clear();

            Optional<Order> result = orderRepository.findByIdAndTenantId(order.getId(), tenantA);

            assertThat(result).isPresent();
            // Items must be loaded without triggering LazyInitializationException
            assertThat(result.get().getItems()).hasSize(1);
        }
    }

    // -------------------------------------------------------------------------
    // findAllByBranchIdAndTenantId
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAllByBranchIdAndTenantId()")
    class FindAllByBranchIdAndTenantId {

        @Test
        @DisplayName("4. Returns paged orders for the correct branch")
        void returnsPagedOrdersForBranch() {
            persistOrder(tenantA, branchA, "ORD-004-1");
            persistOrder(tenantA, branchA, "ORD-004-2");
            entityManager.clear();

            Page<Order> result = orderRepository.findAllByBranchIdAndTenantId(
                    branchA, tenantA, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("5. Excludes orders from a different branch")
        void excludesOrdersFromOtherBranch() {
            persistOrder(tenantA, branchA, "ORD-005-A");
            persistOrder(tenantA, branchB, "ORD-005-B");
            entityManager.clear();

            Page<Order> result = orderRepository.findAllByBranchIdAndTenantId(
                    branchA, tenantA, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-005-A");
        }

        @Test
        @DisplayName("6. Excludes orders from a different tenant")
        void excludesOrdersFromOtherTenant() {
            // branchA belongs to tenantA; tenantB has its own branch
            UUID branchForTenantB = UUID.randomUUID();
            entityManager.getEntityManager().createNativeQuery(
                    "INSERT INTO branches (id, tenant_id, name, is_active) VALUES (?, ?, 'Branch TB', true)")
                    .setParameter(1, branchForTenantB)
                    .setParameter(2, tenantB)
                    .executeUpdate();
            entityManager.flush();

            persistOrder(tenantA, branchA, "ORD-006-A");
            persistOrder(tenantB, branchForTenantB, "ORD-006-B");
            entityManager.clear();

            Page<Order> resultA = orderRepository.findAllByBranchIdAndTenantId(
                    branchA, tenantA, PageRequest.of(0, 10));

            assertThat(resultA.getTotalElements()).isEqualTo(1);
            assertThat(resultA.getContent().get(0).getOrderNumber()).isEqualTo("ORD-006-A");
        }
    }

    // -------------------------------------------------------------------------
    // findOrdersByDateRange
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findOrdersByDateRange()")
    class FindOrdersByDateRange {

        @Test
        @DisplayName("7. Returns orders within the date range")
        void returnsOrdersWithinDateRange() {
            LocalDate today = LocalDate.now();
            persistOrder(tenantA, branchA, "ORD-007-IN");
            entityManager.clear();

            Page<Order> result = orderRepository.findOrdersByDateRange(
                    tenantA, branchA, today, today, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("8. Excludes orders outside the date range")
        void excludesOrdersOutsideDateRange() {
            // Use a past date range that should contain no orders from this test run
            LocalDate past = LocalDate.of(2000, 1, 1);
            persistOrder(tenantA, branchA, "ORD-008-NOW");
            entityManager.clear();

            Page<Order> result = orderRepository.findOrdersByDateRange(
                    tenantA, branchA, past, past, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // -------------------------------------------------------------------------
    // findOpenOrdersByTable
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findOpenOrdersByTable()")
    class FindOpenOrdersByTable {

        @Test
        @DisplayName("9. Returns non-terminal orders for the table")
        void returnsOpenOrdersForTable() {
            UUID tableId = createTable(tenantA, branchA);
            Order pending = persistOrderForTable(tenantA, branchA, "ORD-009-PENDING", tableId,
                    OrderStatusConstants.PENDING);
            entityManager.clear();

            List<Order> result = orderRepository.findOpenOrdersByTable(
                    tableId, tenantA, OrderStatusConstants.TERMINAL_STATUS_IDS);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(pending.getId());
        }

        @Test
        @DisplayName("10. Excludes COMPLETED orders (terminal)")
        void excludesCompletedOrders() {
            UUID tableId = createTable(tenantA, branchA);
            persistOrderForTable(tenantA, branchA, "ORD-010-COMP", tableId,
                    OrderStatusConstants.COMPLETED);
            entityManager.clear();

            List<Order> result = orderRepository.findOpenOrdersByTable(
                    tableId, tenantA, OrderStatusConstants.TERMINAL_STATUS_IDS);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("11. Excludes CANCELLED orders (terminal)")
        void excludesCancelledOrders() {
            UUID tableId = createTable(tenantA, branchA);
            persistOrderForTable(tenantA, branchA, "ORD-011-CANCEL", tableId,
                    OrderStatusConstants.CANCELLED);
            entityManager.clear();

            List<Order> result = orderRepository.findOpenOrdersByTable(
                    tableId, tenantA, OrderStatusConstants.TERMINAL_STATUS_IDS);

            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getNextDailySequence
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getNextDailySequence()")
    class GetNextDailySequence {

        @Test
        @DisplayName("12. Returns 1 when no orders exist for branch and date")
        void returnsOneWhenNoOrdersExist() {
            LocalDate futureDate = LocalDate.of(2099, 12, 31);

            Integer result = orderRepository.getNextDailySequence(tenantA, branchA, futureDate);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("13. Returns MAX+1 when orders already exist for branch and date")
        void returnsMaxPlusOneWhenOrdersExist() {
            LocalDate today = LocalDate.now();

            Order order1 = buildOrder(tenantA, branchA, "ORD-013-1");
            order1.setDailySequence(1);
            Order order2 = buildOrder(tenantA, branchA, "ORD-013-2");
            order2.setDailySequence(2);
            entityManager.persist(order1);
            entityManager.persist(order2);
            entityManager.flush();
            entityManager.clear();

            Integer result = orderRepository.getNextDailySequence(tenantA, branchA, today);

            assertThat(result).isEqualTo(3);
        }
    }

    // -------------------------------------------------------------------------
    // existsByOrderNumberAndTenantId
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("existsByOrderNumberAndTenantId()")
    class ExistsByOrderNumberAndTenantId {

        @Test
        @DisplayName("14. Returns true/false correctly for order number existence")
        void returnsTrueWhenExistsFalseWhenNot() {
            persistOrder(tenantA, branchA, "ORD-014-EXISTS");
            entityManager.clear();

            assertThat(orderRepository.existsByOrderNumberAndTenantId("ORD-014-EXISTS", tenantA)).isTrue();
            assertThat(orderRepository.existsByOrderNumberAndTenantId("ORD-014-NOPE", tenantA)).isFalse();
            // Same number in different tenant should not conflict
            assertThat(orderRepository.existsByOrderNumberAndTenantId("ORD-014-EXISTS", tenantB)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Order buildOrder(UUID tenantId, UUID branchId, String orderNumber) {
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setBranchId(branchId);
        order.setOrderNumber(orderNumber);
        order.setDailySequence(1);
        order.setServiceType(ServiceType.COUNTER);
        order.setStatusId(OrderStatusConstants.PENDING);
        order.setTaxRate(new BigDecimal("0.1600"));
        return order;
    }

    private Order persistOrder(UUID tenantId, UUID branchId, String orderNumber) {
        Order order = buildOrder(tenantId, branchId, orderNumber);
        entityManager.persist(order);
        entityManager.flush();
        return order;
    }

    private Order persistOrderForTable(UUID tenantId, UUID branchId, String orderNumber,
                                       UUID tableId, UUID statusId) {
        Order order = buildOrder(tenantId, branchId, orderNumber);
        order.setTableId(tableId);
        order.setStatusId(statusId);
        entityManager.persist(order);
        entityManager.flush();
        return order;
    }

    /**
     * Inserts a minimal category + product row so that order_items FK (tenant_id, product_id)
     * is satisfied. Returns the product UUID.
     */
    private UUID createProduct(UUID tenantId) {
        UUID categoryId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO categories (id, tenant_id, name, is_active) " +
                        "VALUES (?, ?, 'Test Category', true)")
                .setParameter(1, categoryId)
                .setParameter(2, tenantId)
                .executeUpdate();

        UUID productId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO products (id, tenant_id, category_id, name, base_price, is_active) " +
                        "VALUES (?, ?, ?, 'Test Product', 50.00, true)")
                .setParameter(1, productId)
                .setParameter(2, tenantId)
                .setParameter(3, categoryId)
                .executeUpdate();

        entityManager.flush();
        return productId;
    }

    /**
     * Inserts a minimal area + table so that orders.table_id FK (tenant_id, table_id) is satisfied.
     * Returns the table UUID.
     */
    private UUID createTable(UUID tenantId, UUID branchId) {
        UUID areaId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO areas (id, tenant_id, branch_id, name, is_active) " +
                        "VALUES (?, ?, ?, 'Test Area', true)")
                .setParameter(1, areaId)
                .setParameter(2, tenantId)
                .setParameter(3, branchId)
                .executeUpdate();

        UUID tableId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tables (id, tenant_id, area_id, number, is_active) " +
                        "VALUES (?, ?, ?, ?, true)")
                .setParameter(1, tableId)
                .setParameter(2, tenantId)
                .setParameter(3, areaId)
                .setParameter(4, "T-" + tableId.toString().substring(0, 4))
                .executeUpdate();

        entityManager.flush();
        return tableId;
    }

    /**
     * Persists an order item. The product must already exist to satisfy the composite FK
     * (tenant_id, product_id) -> products(tenant_id, id).
     */
    private void persistOrderItem(Order order) {
        UUID productId = createProduct(order.getTenantId());
        UUID itemId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO order_items (id, tenant_id, order_id, product_id, product_name, unit_price) " +
                        "VALUES (?, ?, ?, ?, 'Test Product', 50.00)")
                .setParameter(1, itemId)
                .setParameter(2, order.getTenantId())
                .setParameter(3, order.getId())
                .setParameter(4, productId)
                .executeUpdate();
        entityManager.flush();
    }
}
