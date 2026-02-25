package com.quickstack.app.pos;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.pos.dto.request.CustomerCreateRequest;
import com.quickstack.pos.dto.request.CustomerUpdateRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Customer management.
 * <p>
 * Tests cover:
 * - Validation: at least one contact method required
 * - RBAC: OWNER/CASHIER can create; only OWNER can delete
 * - IDOR: cross-tenant access returns 404
 * - Duplicate detection: 409 for duplicate email same tenant
 * - Full CRUD flow
 */
@DisplayName("Customer Integration Tests")
class CustomerIntegrationTest extends BaseE2ETest {

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
                userId, tenantId, OWNER_ROLE_ID, "owner@customer.test", "Owner User");

        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@customer.test");
        cashierToken = authHeader(UUID.randomUUID(), tenantId, CASHIER_ROLE_ID, "cashier@customer.test");
    }

    @Test
    @DisplayName("1. POST without any contact method returns 400")
    void postWithoutContactMethodReturns400() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "Sin Contacto", null, null, null, null, null, null, null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/customers")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("2. POST with phone creates customer and returns 201 with Location header")
    void postWithPhoneCreatesCustomer() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "Juan Perez", "5551234567", null, null, "Av. Principal 1", null, "CDMX", null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/customers")
                .then()
                .statusCode(201)
                .header("Location", containsString("/customers/"))
                .body("data.id", notNullValue())
                .body("data.name", is("Juan Perez"))
                .body("data.phone", is("5551234567"));
    }

    @Test
    @DisplayName("3. POST with duplicate email same tenant returns 409")
    void postWithDuplicateEmailSameTenantReturns409() {
        CustomerCreateRequest first = new CustomerCreateRequest(
                "Maria", null, "maria@test.com", null, null, null, null, null, null);
        CustomerCreateRequest duplicate = new CustomerCreateRequest(
                "Maria Dos", null, "maria@test.com", null, null, null, null, null, null);

        given().header("Authorization", ownerToken).contentType(ContentType.JSON).body(first)
                .when().post("/customers").then().statusCode(201);

        given().header("Authorization", ownerToken).contentType(ContentType.JSON).body(duplicate)
                .when().post("/customers").then().statusCode(409);
    }

    @Test
    @DisplayName("4. GET list returns 200 with customers page")
    void getListReturns200() {
        createCustomerDirect(tenantId, "Carlos", "5552345678", null);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/customers")
                .then()
                .statusCode(200)
                .body("data.content", not(empty()));
    }

    @Test
    @DisplayName("5. GET list with search filters results")
    void getListWithSearchFilters() {
        createCustomerDirect(tenantId, "Rosa Gomez", "5553456789", null);
        createCustomerDirect(tenantId, "Pedro Lopez", "5554567890", null);

        given()
                .header("Authorization", ownerToken)
                .queryParam("search", "Gomez")
                .when()
                .get("/customers")
                .then()
                .statusCode(200)
                .body("data.totalElements", is(1))
                .body("data.content[0].name", is("Rosa Gomez"));
    }

    @Test
    @DisplayName("6. GET by id returns 200 with customer data")
    void getByIdReturns200() {
        UUID customerId = createCustomerDirect(tenantId, "Elena", "5555678901", null);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/customers/{id}", customerId)
                .then()
                .statusCode(200)
                .body("data.id", is(customerId.toString()))
                .body("data.name", is("Elena"));
    }

    @Test
    @DisplayName("7. GET cross-tenant customer returns 404 (IDOR protection)")
    void getCrossTenantReturns404() {
        UUID customerId = createCustomerDirect(tenantId, "Privado", "5556789012", null);

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .get("/customers/{id}", customerId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("8. PUT update returns 200 with updated data")
    void putUpdateReturns200() {
        UUID customerId = createCustomerDirect(tenantId, "Original Name", "5557890123", null);

        CustomerUpdateRequest update = new CustomerUpdateRequest(
                "Updated Name", "5557890123", null, null, null, null, null, null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(update)
                .when()
                .put("/customers/{id}", customerId)
                .then()
                .statusCode(200)
                .body("data.name", is("Updated Name"));
    }

    @Test
    @DisplayName("9. DELETE by OWNER returns 204 and customer becomes inaccessible")
    void deleteByOwnerReturns204() {
        UUID customerId = createCustomerDirect(tenantId, "Para Borrar", "5558901234", null);

        given()
                .header("Authorization", ownerToken)
                .when()
                .delete("/customers/{id}", customerId)
                .then()
                .statusCode(204);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/customers/{id}", customerId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("10. DELETE by CASHIER returns 403 (insufficient permissions)")
    void deleteByCashierReturns403() {
        UUID customerId = createCustomerDirect(tenantId, "No Borrar", "5559012345", null);

        given()
                .header("Authorization", cashierToken)
                .when()
                .delete("/customers/{id}", customerId)
                .then()
                .statusCode(403);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        UUID planId = jdbcTemplate.queryForObject("SELECT id FROM subscription_plans LIMIT 1", UUID.class);
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
                id, "Customer Test Tenant " + id, "customer-" + id, planId);
        return id;
    }

    private UUID createCustomerDirect(UUID forTenantId, String name, String phone, String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO customers (id, tenant_id, name, phone, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                id, forTenantId, name, phone, email);
        return id;
    }
}
