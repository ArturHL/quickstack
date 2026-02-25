package com.quickstack.app.branch;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.branch.dto.request.AreaCreateRequest;
import com.quickstack.branch.dto.request.AreaUpdateRequest;
import com.quickstack.branch.dto.request.BranchCreateRequest;
import com.quickstack.branch.dto.request.BranchUpdateRequest;
import com.quickstack.branch.dto.request.TableCreateRequest;
import com.quickstack.branch.dto.request.TableStatusUpdateRequest;
import com.quickstack.branch.entity.TableStatus;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Branch, Area, and Table management.
 * <p>
 * Tests cover:
 * - RBAC: OWNER vs CASHIER access control
 * - IDOR: cross-tenant access returns 404
 * - Full CRUD flow for Branch, Area, and Table
 * - PATCH table status update
 * <p>
 * Since the seed data only includes OWNER and CASHIER roles,
 * CASHIER is used to verify that non-privileged users cannot manage branches.
 */
@DisplayName("Branch Integration Tests")
class BranchIntegrationTest extends BaseE2ETest {

    // Seeded role UUIDs from V7__seed_data.sql
    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID userId;
    private String ownerToken;
    private String cashierToken;

    @BeforeEach
    void setUpTenantAndUsers() {
        tenantId = createTenant();
        userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                userId, tenantId, OWNER_ROLE_ID, "owner@branch.test", "Owner User");

        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@branch.test");
        cashierToken = authHeader(UUID.randomUUID(), tenantId, CASHIER_ROLE_ID, "cashier@branch.test");
    }

    // =========================================================================
    // BRANCH TESTS
    // =========================================================================

    @Test
    @DisplayName("1. OWNER can create branch and receives 201 with Location header")
    void ownerCanCreateBranch() {
        BranchCreateRequest request = new BranchCreateRequest(
                "Sucursal Centro", "CENTRO", "Av. Reforma 1", "CDMX", "5551234567", "centro@test.com");

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/branches")
                .then()
                .statusCode(201)
                .header("Location", containsString("/branches/"))
                .body("data.id", notNullValue())
                .body("data.name", is("Sucursal Centro"))
                .body("data.code", is("CENTRO"))
                .body("data.isActive", is(true));
    }

    @Test
    @DisplayName("2. CASHIER cannot create branch, receives 403")
    void cashierCannotCreateBranch() {
        BranchCreateRequest request = new BranchCreateRequest(
                "Sucursal Norte", "NORTE", null, null, null, null);

        given()
                .header("Authorization", cashierToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/branches")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("3. POST duplicate branch name same tenant returns 409")
    void duplicateBranchNameReturnConflict() {
        BranchCreateRequest request = new BranchCreateRequest(
                "Sucursal Unica", "UNI01", null, null, null, null);

        // First creation
        given().header("Authorization", ownerToken).contentType(ContentType.JSON).body(request)
                .when().post("/branches").then().statusCode(201);

        // Duplicate
        BranchCreateRequest duplicate = new BranchCreateRequest(
                "Sucursal Unica", "UNI02", null, null, null, null);
        given().header("Authorization", ownerToken).contentType(ContentType.JSON).body(duplicate)
                .when().post("/branches").then().statusCode(409);
    }

    @Test
    @DisplayName("4. GET branch by cross-tenant IDOR returns 404")
    void crossTenantBranchAccessReturns404() {
        UUID branchId = createBranchDirect(tenantId, "Privada", "PRV");

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .get("/branches/{id}", branchId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("5. OWNER can update branch name and receives 200")
    void ownerCanUpdateBranch() {
        UUID branchId = createBranchDirect(tenantId, "Original", "ORI");

        BranchUpdateRequest update = new BranchUpdateRequest("Actualizada", null, null, null, null, null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .put("/branches/{id}", branchId)
                .then()
                .statusCode(200)
                .body("data.name", is("Actualizada"));
    }

    @Test
    @DisplayName("6. OWNER can soft delete branch, receives 204, record retained with deleted_at")
    void ownerCanSoftDeleteBranch() {
        UUID branchId = createBranchDirect(tenantId, "Para Borrar", "DEL");

        given()
                .header("Authorization", ownerToken)
                .when()
                .delete("/branches/{id}", branchId)
                .then()
                .statusCode(204);

        // Verify deleted_at set in DB
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT deleted_at FROM branches WHERE id = ?", branchId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("deleted_at")).isNotNull();

        // Branch no longer findable via API (returns 404)
        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/branches/{id}", branchId)
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // AREA TESTS
    // =========================================================================

    @Test
    @DisplayName("7. OWNER can create area within a branch and receives 201")
    void ownerCanCreateArea() {
        UUID branchId = createBranchDirect(tenantId, "Branch con Areas", "BCA");

        AreaCreateRequest request = new AreaCreateRequest(branchId, "Terraza", "Al aire libre", 0);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/branches/{branchId}/areas", branchId)
                .then()
                .statusCode(201)
                .body("data.name", is("Terraza"))
                .body("data.branchId", is(branchId.toString()));
    }

    @Test
    @DisplayName("8. CASHIER cannot create area, receives 403")
    void cashierCannotCreateArea() {
        UUID branchId = createBranchDirect(tenantId, "Branch", "BR");
        AreaCreateRequest request = new AreaCreateRequest(branchId, "Interior", null, null);

        given()
                .header("Authorization", cashierToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/branches/{branchId}/areas", branchId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("9. GET area by cross-tenant IDOR returns 404")
    void crossTenantAreaAccessReturns404() {
        UUID branchId = createBranchDirect(tenantId, "Branch", "BR1");
        UUID areaId = createAreaDirect(tenantId, branchId, "Interior");

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b2@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .get("/areas/{id}", areaId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("10. OWNER can update area name and receives 200")
    void ownerCanUpdateArea() {
        UUID branchId = createBranchDirect(tenantId, "Branch Update", "BU");
        UUID areaId = createAreaDirect(tenantId, branchId, "Area Original");

        AreaUpdateRequest update = new AreaUpdateRequest("Area Actualizada", null, null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .put("/areas/{id}", areaId)
                .then()
                .statusCode(200)
                .body("data.name", is("Area Actualizada"));
    }

    // =========================================================================
    // TABLE TESTS
    // =========================================================================

    @Test
    @DisplayName("11. OWNER can create table within an area and receives 201")
    void ownerCanCreateTable() {
        UUID branchId = createBranchDirect(tenantId, "Branch Tables", "BT");
        UUID areaId = createAreaDirect(tenantId, branchId, "Comedor");

        TableCreateRequest request = new TableCreateRequest(areaId, "1", "Mesa 1", 4, 0);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/areas/{areaId}/tables", areaId)
                .then()
                .statusCode(201)
                .body("data.number", is("1"))
                .body("data.status", is("AVAILABLE"))
                .body("data.areaId", is(areaId.toString()));
    }

    @Test
    @DisplayName("12. CASHIER cannot create table, receives 403")
    void cashierCannotCreateTable() {
        UUID branchId = createBranchDirect(tenantId, "Branch T", "BTT");
        UUID areaId = createAreaDirect(tenantId, branchId, "Sala");

        TableCreateRequest request = new TableCreateRequest(areaId, "2", null, null, null);

        given()
                .header("Authorization", cashierToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/areas/{areaId}/tables", areaId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("13. OWNER can update table status via PATCH and receives 200 with updated status")
    void ownerCanUpdateTableStatus() {
        UUID branchId = createBranchDirect(tenantId, "Branch Status", "BS");
        UUID areaId = createAreaDirect(tenantId, branchId, "Salon");
        UUID tableId = createTableDirect(tenantId, areaId, "5");

        TableStatusUpdateRequest statusUpdate = new TableStatusUpdateRequest(TableStatus.OCCUPIED);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(statusUpdate)
                .when()
                .patch("/tables/{id}/status", tableId)
                .then()
                .statusCode(200)
                .body("data.status", is("OCCUPIED"))
                .body("data.id", is(tableId.toString()));
    }

    @Test
    @DisplayName("14. GET table by cross-tenant IDOR returns 404")
    void crossTenantTableAccessReturns404() {
        UUID branchId = createBranchDirect(tenantId, "Branch IDOR", "BI");
        UUID areaId = createAreaDirect(tenantId, branchId, "Zona");
        UUID tableId = createTableDirect(tenantId, areaId, "7");

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b3@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .get("/tables/{id}", tableId)
                .then()
                .statusCode(404);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        UUID planId = jdbcTemplate.queryForObject("SELECT id FROM subscription_plans LIMIT 1", UUID.class);
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                id, "Branch Test Tenant " + id, "branch-" + id, planId);
        return id;
    }

    private UUID createBranchDirect(UUID forTenantId, String name, String code) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO branches (id, tenant_id, name, code, is_active, settings, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, true, '{}', NOW(), NOW())",
                id, forTenantId, name, code);
        return id;
    }

    private UUID createAreaDirect(UUID forTenantId, UUID forBranchId, String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO areas (id, tenant_id, branch_id, name, sort_order, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 0, true, NOW(), NOW())",
                id, forTenantId, forBranchId, name);
        return id;
    }

    private UUID createTableDirect(UUID forTenantId, UUID forAreaId, String number) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tables (id, tenant_id, area_id, number, status, sort_order, is_active, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 'AVAILABLE', 0, true, NOW(), NOW())",
                id, forTenantId, forAreaId, number);
        return id;
    }
}
