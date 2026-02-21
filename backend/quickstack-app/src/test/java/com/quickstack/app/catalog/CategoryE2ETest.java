package com.quickstack.app.catalog;

import com.quickstack.app.BaseIntegrationTest;
import com.quickstack.product.dto.request.CategoryCreateRequest;
import com.quickstack.product.dto.request.CategoryUpdateRequest;
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
 * End-to-end integration tests for category management.
 * <p>
 * Each test that needs a fresh tenant creates its own tenant via {@link #createTenant()}.
 * Roles are the seeded system roles from V7 migration:
 * - OWNER:   aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
 * - CASHIER: bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb
 * <p>
 * ASVS V4.1: Tests verify multi-tenant IDOR protection.
 */
@DisplayName("Category E2E Tests")
class CategoryE2ETest extends BaseE2ETest {

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
        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@test.com");
        cashierToken = authHeader(userId, tenantId, CASHIER_ROLE_ID, "cashier@test.com");
    }

    // -------------------------------------------------------------------------
    // Test 1: POST with OWNER -> 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1. OWNER can create category and receives 201 with generated id")
    void ownerCanCreateCategory() {
        CategoryCreateRequest request = new CategoryCreateRequest(
            "Bebidas", "Todo tipo de bebidas", null, null, 1);

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/categories")
        .then()
            .statusCode(201)
            .header("Location", containsString("/categories/"))
            .body("data.id", notNullValue())
            .body("data.name", is("Bebidas"))
            .body("data.sortOrder", is(1));
    }

    // -------------------------------------------------------------------------
    // Test 2: POST with CASHIER -> 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("2. CASHIER cannot create category, receives 403")
    void cashierCannotCreateCategory() {
        CategoryCreateRequest request = new CategoryCreateRequest("Comidas", null, null, null, null);

        given()
            .header("Authorization", cashierToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/categories")
        .then()
            .statusCode(403);
    }

    // -------------------------------------------------------------------------
    // Test 3: POST duplicate name same tenant -> 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("3. POST duplicate category name within same tenant returns 409")
    void duplicateNameReturnConflict() {
        CategoryCreateRequest request = new CategoryCreateRequest("Postres", null, null, null, null);

        // Create first time
        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/categories")
        .then()
            .statusCode(201);

        // Attempt duplicate
        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/categories")
        .then()
            .statusCode(409)
            .body("error.code", is("DUPLICATE_NAME"));
    }

    // -------------------------------------------------------------------------
    // Test 4: GET categories with CASHIER -> no inactive
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("4. CASHIER sees only active categories, inactive ones are hidden")
    void cashierSeesOnlyActiveCategories() {
        // Create active category
        createCategoryDirect(tenantId, "Activa", true);
        // Create inactive category
        createCategoryDirect(tenantId, "Inactiva", false);

        List<Map<String, Object>> content = given()
            .header("Authorization", cashierToken)
            .param("includeInactive", "true")   // CASHIER param ignored
        .when()
            .get("/categories")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("data.content");

        // Only active should appear
        assertThat(content).allMatch(c -> (Boolean) c.get("isActive"));
    }

    // -------------------------------------------------------------------------
    // Test 5: GET categories with MANAGER + includeInactive=true -> includes inactive
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("5. OWNER with includeInactive=true sees both active and inactive categories")
    void ownerSeesInactiveCategoriesWhenRequested() {
        createCategoryDirect(tenantId, "Activa", true);
        createCategoryDirect(tenantId, "Inactiva", false);

        int totalCount = given()
            .header("Authorization", ownerToken)
            .param("includeInactive", "true")
        .when()
            .get("/categories")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getInt("data.totalElements");

        assertThat(totalCount).isGreaterThanOrEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Test 6: DELETE with active products -> 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("6. DELETE category with active products returns 409")
    void deleteCategoryWithProductsReturnsConflict() {
        UUID categoryId = createCategoryDirect(tenantId, "Con Productos", true);
        createProduct(tenantId, categoryId, "Agua", "AGUA-001");

        given()
            .header("Authorization", ownerToken)
        .when()
            .delete("/categories/{id}", categoryId)
        .then()
            .statusCode(409)
            .body("error.code", is("CATEGORY_HAS_PRODUCTS"));
    }

    // -------------------------------------------------------------------------
    // Test 7: DELETE without products -> 204, record still in DB with deleted_at set
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("7. DELETE category without products returns 204, record retained with deleted_at")
    void deleteCategoryWithoutProductsSetsDeletedAt() {
        UUID categoryId = createCategoryDirect(tenantId, "Sin Productos", true);

        given()
            .header("Authorization", ownerToken)
        .when()
            .delete("/categories/{id}", categoryId)
        .then()
            .statusCode(204);

        // Verify record still exists in DB with deleted_at
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, deleted_at FROM categories WHERE id = ?", categoryId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("deleted_at")).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Test 8: Cross-tenant IDOR protection -> 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("8. Category from tenant A is not visible to JWT from tenant B (IDOR protection)")
    void crossTenantAccessReturns404() {
        // Create category in tenant A
        UUID categoryInTenantA = createCategoryDirect(tenantId, "Privada", true);

        // Create a completely separate tenant B with its own user and token
        UUID tenantB = createTenant();
        String tenantBToken = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@test.com");

        // Tenant B should get 404, not 403 (prevents resource enumeration)
        given()
            .header("Authorization", tenantBToken)
        .when()
            .get("/categories/{id}", categoryInTenantA)
        .then()
            .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Test 9: POST with MANAGER -> 201
    // (Using OWNER role here as MANAGER is not in the seed data for MVP)
    // In future, when MANAGER role is seeded, this test will use a MANAGER token
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("9. Second OWNER user can create category (OWNER always has catalog write access)")
    void secondOwnerCanCreateCategory() {
        UUID anotherOwnerId = UUID.randomUUID();
        String anotherOwnerToken = authHeader(anotherOwnerId, tenantId, OWNER_ROLE_ID, "owner2@test.com");

        CategoryCreateRequest request = new CategoryCreateRequest("Snacks", null, null, null, null);

        given()
            .header("Authorization", anotherOwnerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/categories")
        .then()
            .statusCode(201)
            .body("data.id", notNullValue());
    }

    // -------------------------------------------------------------------------
    // Test 10: PUT update category name -> 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("10. PUT updates category name and returns 200")
    void ownerCanUpdateCategoryName() {
        UUID categoryId = createCategoryViaApi("Bebidas Original");

        CategoryUpdateRequest updateRequest = new CategoryUpdateRequest(
            "Bebidas Actualizado", null, null, null, null, null);

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .put("/categories/{id}", categoryId)
        .then()
            .statusCode(200)
            .body("data.name", is("Bebidas Actualizado"));
    }

    // -------------------------------------------------------------------------
    // Test 11: GET single category -> 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("11. GET single category returns 200 with full details")
    void getSingleCategoryReturns200() {
        UUID categoryId = createCategoryViaApi("Jugos");

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/categories/{id}", categoryId)
        .then()
            .statusCode(200)
            .body("data.id", is(categoryId.toString()))
            .body("data.name", is("Jugos"))
            .body("data.isActive", is(true));
    }

    // -------------------------------------------------------------------------
    // Test 12: Restore deleted category with OWNER -> 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("12. OWNER can restore a soft-deleted category")
    void ownerCanRestoreDeletedCategory() {
        UUID categoryId = createCategoryDirect(tenantId, "Temporada", true);

        // Delete it first
        given()
            .header("Authorization", ownerToken)
        .when()
            .delete("/categories/{id}", categoryId)
        .then()
            .statusCode(204);

        // Restore it
        given()
            .header("Authorization", ownerToken)
        .when()
            .post("/categories/{id}/restore", categoryId)
        .then()
            .statusCode(200)
            .body("data.isActive", is(true))
            .body("data.id", is(categoryId.toString()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
            id, "Test Tenant " + id
        );
        return id;
    }

    private UUID createCategoryDirect(UUID forTenantId, String name, boolean isActive) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO categories (id, tenant_id, name, is_active, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, 0, NOW(), NOW())
            """,
            id, forTenantId, name, isActive
        );
        return id;
    }

    private UUID createCategoryViaApi(String name) {
        CategoryCreateRequest request = new CategoryCreateRequest(name, null, null, null, null);

        String id = given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/categories")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("data.id");

        return UUID.fromString(id);
    }

    private void createProduct(UUID forTenantId, UUID categoryId, String name, String sku) {
        jdbcTemplate.update("""
            INSERT INTO products (id, tenant_id, category_id, name, sku, base_price,
                                  product_type, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 10.00, 'SIMPLE', NOW(), NOW())
            """,
            UUID.randomUUID(), forTenantId, categoryId, name, sku
        );
    }
}
