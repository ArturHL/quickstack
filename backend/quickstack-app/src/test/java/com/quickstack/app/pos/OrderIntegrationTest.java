package com.quickstack.app.pos;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.pos.dto.request.OrderCreateRequest;
import com.quickstack.pos.dto.request.OrderItemRequest;
import com.quickstack.pos.entity.ServiceType;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Order management (Sprint 4 — Phase 1.3).
 * <p>
 * Tests cover:
 * - Create order: COUNTER, DINE_IN (table state change), DELIVERY
 * - IDOR: cross-tenant returns 404
 * - RBAC: cashier sees only own orders; cancel requires OWNER
 * - State machine: submit, cancel, add/remove items
 * - Business rules: table occupancy, product availability
 */
@DisplayName("Order Integration Tests")
class OrderIntegrationTest extends BaseE2ETest {

        // Seeded role UUIDs from V7__seed_data.sql
        private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

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
                                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) "
                                                +
                                                "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                                userId, tenantId, OWNER_ROLE_ID, "owner@order.test", "Owner User");

                jdbcTemplate.update(
                                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) "
                                                +
                                                "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                                cashierUserId, tenantId, CASHIER_ROLE_ID, "cashier@order.test", "Cashier User");

                ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@order.test");
                cashierToken = authHeader(cashierUserId, tenantId, CASHIER_ROLE_ID, "cashier@order.test");

                branchId = createBranch(tenantId);
                areaId = createArea(tenantId, branchId);
        }

        // =========================================================================
        // POST /orders — Create
        // =========================================================================

        @Test
        @DisplayName("1. COUNTER order with valid product returns 201 with ORD-YYYYMMDD-NNN format")
        void counterOrderReturns201() {
                UUID productId = createProduct(tenantId, "Hamburguesa", new BigDecimal("89.00"));

                OrderCreateRequest request = new OrderCreateRequest(
                                branchId, ServiceType.COUNTER, null, null,
                                List.of(itemRequest(productId, "Hamburguesa", new BigDecimal("89.00"))),
                                null, null);

                given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/orders")
                                .then()
                                .statusCode(201)
                                .header("Location", containsString("/orders/"))
                                .body("data.id", notNullValue())
                                .body("data.orderNumber", matchesPattern("ORD-\\d{8}-\\d+"))
                                .body("data.serviceType", is("COUNTER"))
                                .body("data.items", hasSize(1));
        }

        @Test
        @DisplayName("2. DINE_IN order with AVAILABLE table returns 201 and marks table OCCUPIED")
        void dineInOrderMarksTableOccupied() {
                UUID productId = createProduct(tenantId, "Pizza", new BigDecimal("120.00"));
                UUID tableId = createTable(tenantId, areaId, "T1");

                OrderCreateRequest request = new OrderCreateRequest(
                                branchId, ServiceType.DINE_IN, tableId, null,
                                List.of(itemRequest(productId, "Pizza", new BigDecimal("120.00"))),
                                null, null);

                given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/orders")
                                .then()
                                .statusCode(201)
                                .body("data.tableId", is(tableId.toString()));

                // Verify table is now OCCUPIED
                String tableStatus = jdbcTemplate.queryForObject(
                                "SELECT status FROM tables WHERE id = ?", String.class, tableId);
                assertThat(tableStatus).isEqualTo("OCCUPIED");
        }

        @Test
        @DisplayName("3. DINE_IN with OCCUPIED table returns 409")
        void dineInWithOccupiedTableReturns409() {
                UUID productId = createProduct(tenantId, "Taco", new BigDecimal("35.00"));
                UUID tableId = createTable(tenantId, areaId, "T2");

                // Mark table as OCCUPIED
                jdbcTemplate.update("UPDATE tables SET status = 'OCCUPIED' WHERE id = ?", tableId);

                OrderCreateRequest request = new OrderCreateRequest(
                                branchId, ServiceType.DINE_IN, tableId, null,
                                List.of(itemRequest(productId, "Taco", new BigDecimal("35.00"))),
                                null, null);

                given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/orders")
                                .then()
                                .statusCode(409);
        }

        @Test
        @DisplayName("4. Order with unavailable product (is_available=false) returns 409")
        void unavailableProductReturns409() {
                UUID productId = createProduct(tenantId, "Producto Agotado", new BigDecimal("50.00"));
                jdbcTemplate.update("UPDATE products SET is_available = false WHERE id = ?", productId);

                OrderCreateRequest request = new OrderCreateRequest(
                                branchId, ServiceType.COUNTER, null, null,
                                List.of(itemRequest(productId, "Producto Agotado", new BigDecimal("50.00"))),
                                null, null);

                given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/orders")
                                .then()
                                .statusCode(409);
        }

        @Test
        @DisplayName("5. DELIVERY order with valid customerId returns 201")
        void deliveryOrderReturns201() {
                UUID productId = createProduct(tenantId, "Burger Delivery", new BigDecimal("99.00"));
                UUID customerId = createCustomer(tenantId, "5551234567");

                OrderCreateRequest request = new OrderCreateRequest(
                                branchId, ServiceType.DELIVERY, null, customerId,
                                List.of(itemRequest(productId, "Burger Delivery", new BigDecimal("99.00"))),
                                null, null);

                given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/orders")
                                .then()
                                .statusCode(201)
                                .body("data.customerId", is(customerId.toString()));
        }

        // =========================================================================
        // GET /orders/{id}
        // =========================================================================

        @Test
        @DisplayName("6. GET order by OWNER returns 200")
        void getOrderByOwnerReturns200() {
                UUID orderId = createOrderDirect(tenantId, branchId, userId);

                given()
                                .header("Authorization", ownerToken)
                                .when()
                                .get("/orders/{id}", orderId)
                                .then()
                                .statusCode(200)
                                .body("data.id", is(orderId.toString()));
        }

        @Test
        @DisplayName("7. GET order cross-tenant returns 404 (IDOR protection)")
        void getOrderCrossTenantReturns404() {
                UUID orderId = createOrderDirect(tenantId, branchId, userId);

                UUID tenantB = createTenant();
                String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@order.test");

                given()
                                .header("Authorization", tokenB)
                                .when()
                                .get("/orders/{id}", orderId)
                                .then()
                                .statusCode(404);
        }

        @Test
        @DisplayName("8. GET order by CASHIER who created it returns 200")
        void cashierCanGetOwnOrder() {
                UUID orderId = createOrderDirect(tenantId, branchId, cashierUserId);

                given()
                                .header("Authorization", cashierToken)
                                .when()
                                .get("/orders/{id}", orderId)
                                .then()
                                .statusCode(200)
                                .body("data.id", is(orderId.toString()));
        }

        @Test
        @DisplayName("9. GET order by CASHIER for another cashier's order returns 404 (IDOR)")
        void cashierCannotGetAnotherCashierOrder() {
                // Order created by owner (userId), not cashierUserId
                UUID orderId = createOrderDirect(tenantId, branchId, userId);

                given()
                                .header("Authorization", cashierToken)
                                .when()
                                .get("/orders/{id}", orderId)
                                .then()
                                .statusCode(404);
        }

        // =========================================================================
        // POST /orders/{id}/items
        // =========================================================================

        @Test
        @DisplayName("10. POST item to PENDING order returns 200")
        void addItemToPendingOrderReturns200() {
                UUID productId = createProduct(tenantId, "Papas Fritas", new BigDecimal("35.00"));
                UUID orderId = createOrderDirect(tenantId, branchId, userId);

                given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(itemRequest(productId, "Papas Fritas", new BigDecimal("35.00")))
                                .when()
                                .post("/orders/{id}/items", orderId)
                                .then()
                                .statusCode(200)
                                .body("data.items", hasSize(greaterThan(0)));
        }

        @Test
        @DisplayName("11. POST item to IN_PROGRESS order returns 409")
        void addItemToInProgressOrderReturns409() {
                UUID productId = createProduct(tenantId, "Refresco", new BigDecimal("25.00"));
                UUID orderId = createOrderDirect(tenantId, branchId, userId);

                // Move order to IN_PROGRESS
                jdbcTemplate.update("UPDATE orders SET status_id = 'd2222222-2222-2222-2222-222222222222' WHERE id = ?",
                                orderId);

                given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(itemRequest(productId, "Refresco", new BigDecimal("25.00")))
                                .when()
                                .post("/orders/{id}/items", orderId)
                                .then()
                                .statusCode(409);
        }

        // =========================================================================
        // DELETE /orders/{orderId}/items/{itemId}
        // =========================================================================

        @Test
        @DisplayName("12. DELETE item from PENDING order returns 204")
        void deleteItemFromPendingOrderReturns204() {
                UUID productId = createProduct(tenantId, "Para Borrar", new BigDecimal("50.00"));
                UUID orderId = createOrderDirect(tenantId, branchId, userId);
                UUID itemId = createOrderItemDirect(tenantId, orderId, productId);

                given()
                                .header("Authorization", ownerToken)
                                .when()
                                .delete("/orders/{orderId}/items/{itemId}", orderId, itemId)
                                .then()
                                .statusCode(204);
        }

        // =========================================================================
        // POST /orders/{id}/submit
        // =========================================================================

        @Test
        @DisplayName("13. POST submit transitions order to IN_PROGRESS")
        void submitOrderTransitionsToInProgress() {
                UUID productId = createProduct(tenantId, "Ensalada", new BigDecimal("65.00"));
                UUID orderId = createOrderDirect(tenantId, branchId, userId);
                createOrderItemDirect(tenantId, orderId, productId);

                given()
                                .header("Authorization", ownerToken)
                                .when()
                                .post("/orders/{id}/submit", orderId)
                                .then()
                                .statusCode(200);

                String statusId = jdbcTemplate.queryForObject(
                                "SELECT status_id::text FROM orders WHERE id = ?", String.class, orderId);
                assertThat(statusId).isEqualTo("d2222222-2222-2222-2222-222222222222");
        }

        // =========================================================================
        // POST /orders/{id}/ready
        // =========================================================================

        @Test
        @DisplayName("14. POST ready transitions IN_PROGRESS order to READY")
        void markOrderReadyTransitionsToReady() {
                UUID productId = createProduct(tenantId, "Torta", new BigDecimal("75.00"));
                UUID orderId = createOrderDirect(tenantId, branchId, userId);
                createOrderItemDirect(tenantId, orderId, productId);

                // First submit to IN_PROGRESS
                given()
                                .header("Authorization", ownerToken)
                                .when()
                                .post("/orders/{id}/submit", orderId)
                                .then()
                                .statusCode(200);

                // Then mark as READY
                given()
                                .header("Authorization", ownerToken)
                                .when()
                                .post("/orders/{id}/ready", orderId)
                                .then()
                                .statusCode(200);

                String statusId = jdbcTemplate.queryForObject(
                                "SELECT status_id::text FROM orders WHERE id = ?", String.class, orderId);
                assertThat(statusId).isEqualTo("d3333333-3333-3333-3333-333333333333");
        }

        @Test
        @DisplayName("15. POST ready on PENDING order returns 409 (wrong status)")
        void markReadyOnPendingOrderReturns409() {
                UUID orderId = createOrderDirect(tenantId, branchId, userId);

                given()
                                .header("Authorization", ownerToken)
                                .when()
                                .post("/orders/{id}/ready", orderId)
                                .then()
                                .statusCode(409);
        }

        // =========================================================================
        // POST /orders/{id}/cancel
        // =========================================================================

        @Test
        @DisplayName("17. POST cancel by OWNER returns 204, status CANCELLED, table released")
        void cancelOrderByOwnerReturns204() {
                UUID productId = createProduct(tenantId, "Comida Cancelada", new BigDecimal("80.00"));
                UUID tableId = createTable(tenantId, areaId, "TC1");

                // Create DINE_IN order that occupies table
                OrderCreateRequest request = new OrderCreateRequest(
                                branchId, ServiceType.DINE_IN, tableId, null,
                                List.of(itemRequest(productId, "Comida Cancelada", new BigDecimal("80.00"))),
                                null, null);

                String orderId = given()
                                .header("Authorization", ownerToken)
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/orders")
                                .then()
                                .statusCode(201)
                                .extract().path("data.id");

                given()
                                .header("Authorization", ownerToken)
                                .when()
                                .post("/orders/{id}/cancel", orderId)
                                .then()
                                .statusCode(204);

                // Verify status is CANCELLED
                String statusId = jdbcTemplate.queryForObject(
                                "SELECT status_id::text FROM orders WHERE id = ?", String.class,
                                UUID.fromString(orderId));
                assertThat(statusId).isEqualTo("d6666666-6666-6666-6666-666666666666");

                // Verify table is AVAILABLE again
                String tableStatus = jdbcTemplate.queryForObject(
                                "SELECT status FROM tables WHERE id = ?", String.class, tableId);
                assertThat(tableStatus).isEqualTo("AVAILABLE");
        }

        @Test
        @DisplayName("15. POST cancel by CASHIER returns 403 (insufficient permissions)")
        void cancelOrderByCashierReturns403() {
                UUID orderId = createOrderDirect(tenantId, branchId, cashierUserId);

                given()
                                .header("Authorization", cashierToken)
                                .when()
                                .post("/orders/{id}/cancel", orderId)
                                .then()
                                .statusCode(403);
        }

        // =========================================================================
        // GET /orders — List
        // =========================================================================

        @Test
        @DisplayName("16. GET /orders by CASHIER returns only their own orders")
        void cashierSeesOnlyOwnOrders() {
                // Create order by owner
                UUID ownerOrderId = createOrderDirect(tenantId, branchId, userId);
                // Create order by cashier
                UUID cashierOrderId = createOrderDirect(tenantId, branchId, cashierUserId);

                given()
                                .header("Authorization", cashierToken)
                                .when()
                                .get("/orders")
                                .then()
                                .statusCode(200)
                                .body("data.content", hasSize(1))
                                .body("data.content[0].id", is(cashierOrderId.toString()));
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
                                id, "Order Test Tenant " + id, "order-" + id, planId);
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

        private UUID createProduct(UUID forTenantId, String name, BigDecimal price) {
                // Try to find an existing category for this tenant
                UUID categoryId = jdbcTemplate.query(
                                "SELECT id FROM categories WHERE tenant_id = ? LIMIT 1",
                                (rs, rowNum) -> UUID.fromString(rs.getString("id")),
                                forTenantId).stream().findFirst().orElse(null);

                if (categoryId == null) {
                        categoryId = UUID.randomUUID();
                        jdbcTemplate.update(
                                        "INSERT INTO categories (id, tenant_id, name, created_at, updated_at) " +
                                                        "VALUES (?, ?, ?, NOW(), NOW())",
                                        categoryId, forTenantId, "General");
                }

                UUID id = UUID.randomUUID();
                jdbcTemplate.update(
                                "INSERT INTO products (id, tenant_id, category_id, name, base_price, " +
                                                "is_active, is_available, created_at, updated_at) " +
                                                "VALUES (?, ?, ?, ?, ?, true, true, NOW(), NOW())",
                                id, forTenantId, categoryId, name, price);
                return id;
        }

        /**
         * Creates a minimal PENDING order directly via JDBC, bypassing the API.
         * Used for tests that need an existing order to act upon.
         */
        private UUID createOrderDirect(UUID forTenantId, UUID forBranchId, UUID createdByUserId) {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update(
                                "INSERT INTO orders (id, tenant_id, branch_id, order_number, daily_sequence, " +
                                                "service_type, status_id, subtotal, tax_rate, tax, discount, total, source, "
                                                +
                                                "opened_at, created_by, updated_by, created_at, updated_at) " +
                                                "VALUES (?, ?, ?, ?, 1, 'COUNTER', 'd1111111-1111-1111-1111-111111111111', "
                                                +
                                                "0, 0.16, 0, 0, 0, 'POS', NOW(), ?, ?, NOW(), NOW())",
                                id, forTenantId, forBranchId,
                                "ORD-TEST-" + id.toString().substring(0, 8),
                                createdByUserId, createdByUserId);
                return id;
        }

        /**
         * Creates an order item directly via JDBC for an existing order.
         * line_total is a generated column — computed automatically by the DB.
         */
        private UUID createOrderItemDirect(UUID forTenantId, UUID forOrderId, UUID forProductId) {
                UUID id = UUID.randomUUID();
                jdbcTemplate.update(
                                "INSERT INTO order_items (id, tenant_id, order_id, product_id, product_name, " +
                                                "quantity, unit_price, modifiers_total, kds_status, created_at, updated_at) "
                                                +
                                                "VALUES (?, ?, ?, ?, 'Producto Test', 1, 50.00, 0, 'PENDING', NOW(), NOW())",
                                id, forTenantId, forOrderId, forProductId);
                return id;
        }

        private OrderItemRequest itemRequest(UUID productId, String name, BigDecimal price) {
                return new OrderItemRequest(
                                productId, null, null, name, null,
                                1, price, List.of(), null);
        }

}
