package com.quickstack.app.pos;

import com.quickstack.app.BaseE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Reporting endpoints (Sprint 6 — Phase 1.3).
 * <p>
 * Tests cover:
 * - Daily summary: correct metrics for completed orders
 * - Only COMPLETED orders are counted (not PENDING, CANCELLED)
 * - Empty day returns zeros
 * - RBAC: CASHIER gets 403 (MANAGER+ required)
 * - Missing required param returns 400
 * - IDOR: cross-tenant branchId returns 404
 */
@DisplayName("Report Integration Tests")
class ReportIntegrationTest extends BaseE2ETest {

    // Seeded role UUIDs from V7__seed_data.sql
    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    // Seeded order status UUIDs from V7__seed_data.sql
    private static final String COMPLETED_STATUS_ID = "d5555555-5555-5555-5555-555555555555";
    private static final String PENDING_STATUS_ID = "d1111111-1111-1111-1111-111111111111";
    private static final String CANCELLED_STATUS_ID = "d6666666-6666-6666-6666-666666666666";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID userId;
    private UUID branchId;
    private String ownerToken;
    private String cashierToken;
    private UUID cashierUserId;

    @BeforeEach
    void setUpTenantAndUsers() {
        tenantId = createTenant();
        userId = UUID.randomUUID();
        cashierUserId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                userId, tenantId, OWNER_ROLE_ID, "owner@report.test", "Owner User");

        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                cashierUserId, tenantId, CASHIER_ROLE_ID, "cashier@report.test", "Cashier User");

        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@report.test");
        cashierToken = authHeader(cashierUserId, tenantId, CASHIER_ROLE_ID, "cashier@report.test");

        branchId = createBranch(tenantId);
    }

    // =========================================================================
    // GET /reports/daily-summary
    // =========================================================================

    @Test
    @DisplayName("1. Returns correct metrics for completed orders")
    void returnsCorrectMetrics() {
        createCompletedOrder(tenantId, branchId, userId, "COUNTER", new BigDecimal("100.00"));
        createCompletedOrder(tenantId, branchId, userId, "COUNTER", new BigDecimal("200.00"));
        createCompletedOrder(tenantId, branchId, userId, "DINE_IN", new BigDecimal("150.00"));

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/reports/daily-summary?branchId=" + branchId)
                .then()
                .statusCode(200)
                .body("data.totalOrders", is(3))
                .body("data.totalSales", is(450.0f))
                .body("data.averageTicket", is(150.0f))
                .body("data.ordersByServiceType.COUNTER", is(2))
                .body("data.ordersByServiceType.DINE_IN", is(1))
                .body("data.topProducts", empty());
    }

    @Test
    @DisplayName("2. Returns zero stats when no completed orders for the day")
    void returnsZerosForEmptyDay() {
        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/reports/daily-summary?branchId=" + branchId)
                .then()
                .statusCode(200)
                .body("data.totalOrders", is(0))
                .body("data.totalSales", is(0.0f))
                .body("data.averageTicket", is(0.0f))
                .body("data.topProducts", empty());
    }

    @Test
    @DisplayName("3. Only counts COMPLETED orders — PENDING and CANCELLED are excluded")
    void onlyCountsCompletedOrders() {
        // 1 completed, 1 pending, 1 cancelled
        createCompletedOrder(tenantId, branchId, userId, "COUNTER", new BigDecimal("100.00"));
        createOrderWithStatus(tenantId, branchId, userId, "COUNTER", new BigDecimal("100.00"), PENDING_STATUS_ID);
        createOrderWithStatus(tenantId, branchId, userId, "COUNTER", new BigDecimal("100.00"), CANCELLED_STATUS_ID);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/reports/daily-summary?branchId=" + branchId)
                .then()
                .statusCode(200)
                .body("data.totalOrders", is(1))
                .body("data.totalSales", is(100.0f));
    }

    @Test
    @DisplayName("4. CASHIER role returns 403")
    void cashierReturns403() {
        given()
                .header("Authorization", cashierToken)
                .when()
                .get("/reports/daily-summary?branchId=" + branchId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("5. Missing branchId param returns 400")
    void missingBranchIdReturns400() {
        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/reports/daily-summary")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("6. Cross-tenant branchId returns 404")
    void crossTenantBranchReturns404() {
        UUID otherTenant = createTenant();
        UUID otherBranch = createBranch(otherTenant);

        given()
                .header("Authorization", ownerToken) // token for tenantId, not otherTenant
                .when()
                .get("/reports/daily-summary?branchId=" + otherBranch)
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        UUID planId = jdbcTemplate.queryForObject(
                "SELECT id FROM subscription_plans LIMIT 1", UUID.class);
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, NOW(), NOW())",
                id, "Report Test Tenant " + id, "rpt-" + id, planId);
        return id;
    }

    private UUID createBranch(UUID forTenantId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO branches (id, tenant_id, name, created_at, updated_at) " +
                        "VALUES (?, ?, ?, NOW(), NOW())",
                id, forTenantId, "Branch " + id);
        return id;
    }

    /**
     * Inserts a completed order for today's date.
     */
    private UUID createCompletedOrder(UUID forTenantId, UUID forBranchId, UUID createdByUserId,
                                       String serviceType, BigDecimal total) {
        return createOrderWithStatus(forTenantId, forBranchId, createdByUserId, serviceType, total, COMPLETED_STATUS_ID);
    }

    private UUID createOrderWithStatus(UUID forTenantId, UUID forBranchId, UUID createdByUserId,
                                        String serviceType, BigDecimal total, String statusId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO orders (id, tenant_id, branch_id, order_number, daily_sequence, " +
                        "service_type, status_id, subtotal, tax_rate, tax, discount, total, source, " +
                        "opened_at, closed_at, created_by, updated_by, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 1, ?, '" + statusId + "', " +
                        "?, 0.16, 0, 0, ?, 'POS', NOW(), NOW(), ?, ?, NOW(), NOW())",
                id, forTenantId, forBranchId,
                "ORD-RPT-" + id.toString().substring(0, 8),
                serviceType, total, total, createdByUserId, createdByUserId);
        return id;
    }

}
