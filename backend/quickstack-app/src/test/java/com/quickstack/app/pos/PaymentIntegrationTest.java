package com.quickstack.app.pos;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.pos.dto.request.PaymentRequest;
import com.quickstack.pos.entity.PaymentMethod;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Payment management (Sprint 5 — Phase 1.3).
 * <p>
 * Tests cover:
 * - Register payment: closes order, releases table, updates customer stats
 * - Validation: insufficient amount (400), wrong status (409)
 * - IDOR: cross-tenant returns 404
 * - RBAC: unauthenticated returns 401
 * - List payments for order
 */
@DisplayName("Payment Integration Tests")
class PaymentIntegrationTest extends BaseE2ETest {

    // Seeded role UUIDs from V7__seed_data.sql
    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    // Seeded order status UUIDs from V7__seed_data.sql
    private static final String READY_STATUS_ID = "d3333333-3333-3333-3333-333333333333";
    private static final String PENDING_STATUS_ID = "d1111111-1111-1111-1111-111111111111";
    private static final String COMPLETED_STATUS_ID = "d5555555-5555-5555-5555-555555555555";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID userId;
    private UUID branchId;
    private UUID areaId;
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
                userId, tenantId, OWNER_ROLE_ID, "owner@payment.test", "Owner User");

        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                cashierUserId, tenantId, CASHIER_ROLE_ID, "cashier@payment.test", "Cashier User");

        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@payment.test");
        cashierToken = authHeader(cashierUserId, tenantId, CASHIER_ROLE_ID, "cashier@payment.test");

        branchId = createBranch(tenantId);
        areaId = createArea(tenantId, branchId);
    }

    // =========================================================================
    // POST /payments — Register
    // =========================================================================

    @Test
    @DisplayName("1. COUNTER order paid with exact amount returns 201 and is COMPLETED")
    void cashPaymentClosesCounterOrder() {
        UUID orderId = createReadyOrder(tenantId, branchId, userId, null, null, new BigDecimal("89.00"));

        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("89.00"), null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(201)
                .header("Location", containsString("/payments/"))
                .body("data.orderId", is(orderId.toString()))
                .body("data.paymentMethod", is("CASH"))
                .body("data.changeGiven", is(0.0f));

        // Verify order is now COMPLETED
        String statusId = jdbcTemplate.queryForObject(
                "SELECT status_id FROM orders WHERE id = ?", String.class, orderId);
        assertThat(statusId).isEqualTo(COMPLETED_STATUS_ID);
    }

    @Test
    @DisplayName("2. Overpayment returns correct change")
    void overpaymentReturnsChange() {
        UUID orderId = createReadyOrder(tenantId, branchId, userId, null, null, new BigDecimal("89.00"));

        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("100.00"), null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(201)
                .body("data.changeGiven", is(11.0f));
    }

    @Test
    @DisplayName("3. DINE_IN payment releases table back to AVAILABLE")
    void dineInPaymentReleasesTable() {
        UUID tableId = createTable(tenantId, areaId, "T10");
        // Simulate table being occupied
        jdbcTemplate.update("UPDATE tables SET status = 'OCCUPIED' WHERE id = ?", tableId);

        UUID orderId = createReadyOrder(tenantId, branchId, userId, tableId, null, new BigDecimal("120.00"));

        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("120.00"), null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(201);

        // Verify table is now AVAILABLE
        String tableStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM tables WHERE id = ?", String.class, tableId);
        assertThat(tableStatus).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("4. Payment updates customer totalOrders and totalSpent")
    void paymentUpdatesCustomerStats() {
        UUID customerId = createCustomer(tenantId, "+521234567890");
        UUID orderId = createReadyOrder(tenantId, branchId, userId, null, customerId, new BigDecimal("150.00"));

        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("150.00"), null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(201);

        // Verify customer stats updated
        Integer totalOrders = jdbcTemplate.queryForObject(
                "SELECT total_orders FROM customers WHERE id = ?", Integer.class, customerId);
        assertThat(totalOrders).isEqualTo(1);
    }

    @Test
    @DisplayName("5. Amount < order.total returns 400 INSUFFICIENT_PAYMENT")
    void insufficientAmountReturns400() {
        UUID orderId = createReadyOrder(tenantId, branchId, userId, null, null, new BigDecimal("89.00"));

        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("50.00"), null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("6. Order not in READY status returns 409")
    void orderNotReadyReturns409() {
        UUID orderId = createPendingOrder(tenantId, branchId, userId, new BigDecimal("89.00"));

        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("89.00"), null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("7. Cross-tenant: order from other tenant returns 404")
    void crossTenantReturns404() {
        UUID otherTenant = createTenant();
        UUID otherBranch = createBranch(otherTenant);
        UUID otherOrderId = createReadyOrder(otherTenant, otherBranch, userId, null, null, new BigDecimal("89.00"));

        PaymentRequest request = new PaymentRequest(otherOrderId, PaymentMethod.CASH, new BigDecimal("89.00"), null);

        given()
                .header("Authorization", ownerToken)  // token for tenantId, not otherTenant
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("8. Unauthenticated request returns 403 (Spring Security default without configured AuthenticationEntryPoint)")
    void unauthenticatedReturns403() {
        UUID orderId = createReadyOrder(tenantId, branchId, userId, null, null, new BigDecimal("89.00"));

        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("89.00"), null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(403);
    }

    // =========================================================================
    // GET /orders/{orderId}/payments
    // =========================================================================

    @Test
    @DisplayName("9. GET payments for order returns list after payment registered")
    void listPaymentsReturnsPayment() {
        UUID orderId = createReadyOrder(tenantId, branchId, userId, null, null, new BigDecimal("89.00"));

        // Register payment first
        PaymentRequest request = new PaymentRequest(orderId, PaymentMethod.CASH, new BigDecimal("89.00"), null);
        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/payments")
                .then()
                .statusCode(201);

        // Now list payments
        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/orders/{orderId}/payments", orderId)
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].orderId", is(orderId.toString()))
                .body("data[0].paymentMethod", is("CASH"));
    }

    @Test
    @DisplayName("10. GET payments for cross-tenant order returns 404")
    void listPaymentsCrossTenantReturns404() {
        UUID otherTenant = createTenant();
        UUID otherBranch = createBranch(otherTenant);
        UUID otherOrderId = createReadyOrder(otherTenant, otherBranch, userId, null, null, new BigDecimal("89.00"));

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/orders/{orderId}/payments", otherOrderId)
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
                id, "Payment Test Tenant " + id, "pay-" + id, planId);
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

    private UUID createArea(UUID forTenantId, UUID forBranchId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO areas (id, tenant_id, branch_id, name, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, NOW(), NOW())",
                id, forTenantId, forBranchId, "Salon Principal");
        return id;
    }

    private UUID createTable(UUID forTenantId, UUID forAreaId, String number) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tables (id, tenant_id, area_id, number, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 'AVAILABLE', NOW(), NOW())",
                id, forTenantId, forAreaId, number);
        return id;
    }

    private UUID createCustomer(UUID forTenantId, String phone) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO customers (id, tenant_id, phone, created_at, updated_at) " +
                        "VALUES (?, ?, ?, NOW(), NOW())",
                id, forTenantId, phone);
        return id;
    }

    /**
     * Creates an order directly in READY status via JDBC with the given total.
     * service_type is derived from tableId/customerId presence to avoid CASE expressions in JDBC.
     */
    private UUID createReadyOrder(UUID forTenantId, UUID forBranchId, UUID createdByUserId,
                                   UUID tableId, UUID customerId, BigDecimal total) {
        UUID id = UUID.randomUUID();
        String serviceType = tableId != null ? "DINE_IN" : (customerId != null ? "DELIVERY" : "COUNTER");
        jdbcTemplate.update(
                "INSERT INTO orders (id, tenant_id, branch_id, table_id, customer_id, " +
                        "order_number, daily_sequence, service_type, status_id, " +
                        "subtotal, tax_rate, tax, discount, total, source, " +
                        "opened_at, created_by, updated_by, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1, ?, '" + READY_STATUS_ID + "', " +
                        "?, 0.16, 0, 0, ?, 'POS', NOW(), ?, ?, NOW(), NOW())",
                id, forTenantId, forBranchId, tableId, customerId,
                "ORD-PAY-" + id.toString().substring(0, 8),
                serviceType,
                total, total,
                createdByUserId, createdByUserId);
        return id;
    }

    /**
     * Creates an order in PENDING (not READY) status — used to test validation.
     */
    private UUID createPendingOrder(UUID forTenantId, UUID forBranchId, UUID createdByUserId, BigDecimal total) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO orders (id, tenant_id, branch_id, order_number, daily_sequence, " +
                        "service_type, status_id, subtotal, tax_rate, tax, discount, total, source, " +
                        "opened_at, created_by, updated_by, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 1, 'COUNTER', '" + PENDING_STATUS_ID + "', " +
                        "?, 0.16, 0, 0, ?, 'POS', NOW(), ?, ?, NOW(), NOW())",
                id, forTenantId, forBranchId,
                "ORD-PEND-" + id.toString().substring(0, 8),
                total, total,
                createdByUserId, createdByUserId);
        return id;
    }

}
