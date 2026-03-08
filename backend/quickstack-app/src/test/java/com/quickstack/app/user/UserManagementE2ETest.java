package com.quickstack.app.user;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.user.dto.request.UserCreateAdminRequest;
import com.quickstack.user.dto.request.UserUpdateAdminRequest;
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
 * End-to-end integration tests for user management by OWNER.
 * <p>
 * Seeded role UUIDs from V7__seed_data.sql:
 * - OWNER:   aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
 * - CASHIER: bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb
 * <p>
 * ASVS V4.1: Tests verify OWNER-only access and multi-tenant IDOR protection.
 */
@DisplayName("User Management E2E Tests")
class UserManagementE2ETest extends BaseE2ETest {

    private static final UUID OWNER_ROLE_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID ownerId;
    private String ownerToken;
    private String cashierToken;

    @BeforeEach
    void setUpTenantAndUsers() {
        tenantId = createTenant();
        ownerId = UUID.randomUUID();
        insertUser(tenantId, ownerId, OWNER_ROLE_ID, "owner@test.com");
        ownerToken  = authHeader(ownerId,          tenantId, OWNER_ROLE_ID,   "owner@test.com");
        cashierToken = authHeader(UUID.randomUUID(), tenantId, CASHIER_ROLE_ID, "cashier@test.com");
    }

    // -------------------------------------------------------------------------
    // 1. OWNER lista usuarios → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1. OWNER can list users and receives paginated response")
    void ownerCanListUsers() {
        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("data.content", notNullValue())
                .body("data.totalElements", greaterThanOrEqualTo(1));
    }

    // -------------------------------------------------------------------------
    // 2. CASHIER intenta listar usuarios → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("2. CASHIER cannot list users, receives 403")
    void cashierCannotListUsers() {
        given()
                .header("Authorization", cashierToken)
                .when()
                .get("/users")
                .then()
                .statusCode(403);
    }

    // -------------------------------------------------------------------------
    // 3. OWNER crea usuario cajero → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("3. OWNER can create a cashier user and receives 201 with Location header")
    void ownerCanCreateCashierUser() {
        UserCreateAdminRequest request = new UserCreateAdminRequest(
                "cajero.nuevo@test.com",
                "Juan Cajero",
                "Password123!safe",
                CASHIER_ROLE_ID,
                null,
                "5512345678"
        );

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .header("Location", containsString("/users/"))
                .body("data.id", notNullValue())
                .body("data.email", is("cajero.nuevo@test.com"))
                .body("data.fullName", is("Juan Cajero"))
                .body("data.roleCode", is("CASHIER"))
                .body("data.isActive", is(true));
    }

    // -------------------------------------------------------------------------
    // 4. Email duplicado dentro del mismo tenant → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("4. Creating user with duplicate email in same tenant returns 409")
    void duplicateEmailReturnConflict() {
        UserCreateAdminRequest request = new UserCreateAdminRequest(
                "duplicado@test.com",
                "Primer Usuario",
                "Password123!safe",
                CASHIER_ROLE_ID,
                null,
                null
        );

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/users")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/users")
                .then()
                .statusCode(409);
    }

    // -------------------------------------------------------------------------
    // 5. OWNER actualiza nombre y rol → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("5. OWNER can update user fullName and role, receives 200")
    void ownerCanUpdateUser() {
        UUID targetUserId = createUserViaApi("update.me@test.com", "Original Name", CASHIER_ROLE_ID);

        UserUpdateAdminRequest update = new UserUpdateAdminRequest(
                "Nombre Actualizado",
                "5599999999",
                OWNER_ROLE_ID
        );

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .put("/users/{id}", targetUserId)
                .then()
                .statusCode(200)
                .body("data.fullName", is("Nombre Actualizado"))
                .body("data.phone", is("5599999999"))
                .body("data.roleCode", is("OWNER"));
    }

    // -------------------------------------------------------------------------
    // 6. OWNER desactiva usuario → 204, is_active=false en DB
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("6. OWNER can deactivate a user: receives 204 and user is_active=false in DB")
    void ownerCanDeactivateUser() {
        UUID targetUserId = createUserViaApi("to.deactivate@test.com", "Para Desactivar", CASHIER_ROLE_ID);

        given()
                .header("Authorization", ownerToken)
                .when()
                .delete("/users/{id}", targetUserId)
                .then()
                .statusCode(204);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT is_active FROM users WHERE id = ?", targetUserId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("is_active")).isEqualTo(false);
    }

    // -------------------------------------------------------------------------
    // 7. OWNER intenta desactivarse a sí mismo → 400
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("7. OWNER cannot deactivate own account, receives 400")
    void ownerCannotDeactivateOwnAccount() {
        given()
                .header("Authorization", ownerToken)
                .when()
                .delete("/users/{id}", ownerId)
                .then()
                .statusCode(400);
    }

    // -------------------------------------------------------------------------
    // 8. Cross-tenant IDOR → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("8. User from tenant A is not visible to JWT from tenant B (IDOR protection)")
    void crossTenantAccessReturns404() {
        UUID targetUserId = createUserViaApi("victim@test.com", "Victima", CASHIER_ROLE_ID);

        UUID tenantB = createTenant();
        UUID ownerB  = UUID.randomUUID();
        insertUser(tenantB, ownerB, OWNER_ROLE_ID, "ownerB@test.com");
        String tokenB = authHeader(ownerB, tenantB, OWNER_ROLE_ID, "ownerB@test.com");

        // Tenant B should get 404, not 403 — prevents resource enumeration
        given()
                .header("Authorization", tokenB)
                .when()
                .delete("/users/{id}", targetUserId)
                .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, '11111111-1111-1111-1111-111111111111', NOW(), NOW())",
                id, "Test Tenant " + id, "slug-" + id);
        return id;
    }

    private void insertUser(UUID tenantId, UUID userId, UUID roleId, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, 'Test User', 'hash', NOW(), NOW())",
                userId, tenantId, roleId, email);
    }

    private UUID createUserViaApi(String email, String fullName, UUID roleId) {
        UserCreateAdminRequest request = new UserCreateAdminRequest(
                email, fullName, "Password123!safe", roleId, null, null);

        String id = given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("data.id");

        return UUID.fromString(id);
    }
}
