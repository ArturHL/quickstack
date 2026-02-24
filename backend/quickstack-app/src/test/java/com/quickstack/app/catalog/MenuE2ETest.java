package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import io.restassured.http.ContentType;
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
 * End-to-end integration tests for the GET /api/v1/menu endpoint.
 * <p>
 * Verifies:
 * - Menu assembles correctly from DB state
 * - Filtering rules (inactive excluded, agotados included)
 * - Category exclusion when empty
 * - sort_order ordering
 * - Authentication required
 * - Multi-tenant isolation
 */
@DisplayName("Menu E2E Tests")
class MenuE2ETest extends BaseE2ETest {

    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID userId;
    private String ownerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() {
        tenantId = createTenant();
        userId = UUID.randomUUID();
        createUser(tenantId, userId, OWNER_ROLE_ID, "owner@menutest.com");
        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@menutest.com");
        cashierToken = authHeader(userId, tenantId, CASHIER_ROLE_ID, "cashier@menutest.com");
    }

    @Test
    @DisplayName("1. GET /api/v1/menu returns only categories that have active products")
    void returnsOnlyCategoriesWithActiveProducts() {
        UUID catWithProducts = createCategory(tenantId, "Tacos", 0);
        UUID catEmpty = createCategory(tenantId, "Postres", 1);
        createSimpleProduct(tenantId, catWithProducts, "Taco Pastor", true, true, 0);

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/menu")
        .then()
            .statusCode(200)
            .body("data.categories", hasSize(1))
            .body("data.categories[0].name", is("Tacos"));
    }

    @Test
    @DisplayName("2. Unavailable products (agotado) appear in menu marked as is_available=false")
    void unavailableProductsIncludedButMarked() {
        UUID cat = createCategory(tenantId, "Bebidas", 0);
        createSimpleProduct(tenantId, cat, "Agua", true, true, 0);
        createSimpleProduct(tenantId, cat, "Jugo", true, false, 1); // agotado

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/menu")
        .then()
            .statusCode(200)
            .body("data.categories[0].products", hasSize(2))
            .body("data.categories[0].products.find { it.name == 'Jugo' }.isAvailable", is(false))
            .body("data.categories[0].products.find { it.name == 'Agua' }.isAvailable", is(true));
    }

    @Test
    @DisplayName("3. Inactive products (is_active=false) do NOT appear in menu")
    void inactiveProductsExcluded() {
        UUID cat = createCategory(tenantId, "Tacos", 0);
        createSimpleProduct(tenantId, cat, "Taco Activo", true, true, 0);
        createSimpleProduct(tenantId, cat, "Taco Inactivo", false, true, 1);

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/menu")
        .then()
            .statusCode(200)
            .body("data.categories[0].products", hasSize(1))
            .body("data.categories[0].products[0].name", is("Taco Activo"));
    }

    @Test
    @DisplayName("4. Category with only inactive products is excluded from menu")
    void categoryWithOnlyInactiveProductsExcluded() {
        UUID catVisible = createCategory(tenantId, "Tacos", 0);
        UUID catHidden = createCategory(tenantId, "Invisible", 1);
        createSimpleProduct(tenantId, catVisible, "Taco", true, true, 0);
        createSimpleProduct(tenantId, catHidden, "Producto Inactivo", false, true, 0);

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/menu")
        .then()
            .statusCode(200)
            .body("data.categories", hasSize(1))
            .body("data.categories[0].name", is("Tacos"));
    }

    @Test
    @DisplayName("5. Products within a category are ordered by sort_order")
    void productsOrderedBySortOrder() {
        UUID cat = createCategory(tenantId, "Menu", 0);
        createSimpleProduct(tenantId, cat, "Primero", true, true, 0);
        createSimpleProduct(tenantId, cat, "Segundo", true, true, 1);
        createSimpleProduct(tenantId, cat, "Tercero", true, true, 2);

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/menu")
        .then()
            .statusCode(200)
            .body("data.categories[0].products[0].name", is("Primero"))
            .body("data.categories[0].products[1].name", is("Segundo"))
            .body("data.categories[0].products[2].name", is("Tercero"));
    }

    @Test
    @DisplayName("6. Categories are ordered by sort_order")
    void categoriesOrderedBySortOrder() {
        UUID cat1 = createCategory(tenantId, "A - Primera", 0);
        UUID cat2 = createCategory(tenantId, "B - Segunda", 1);
        UUID cat3 = createCategory(tenantId, "C - Tercera", 2);
        createSimpleProduct(tenantId, cat1, "P1", true, true, 0);
        createSimpleProduct(tenantId, cat2, "P2", true, true, 0);
        createSimpleProduct(tenantId, cat3, "P3", true, true, 0);

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/menu")
        .then()
            .statusCode(200)
            .body("data.categories[0].name", is("A - Primera"))
            .body("data.categories[1].name", is("B - Segunda"))
            .body("data.categories[2].name", is("C - Tercera"));
    }

    @Test
    @DisplayName("7. Request without JWT returns 403 (no AuthenticationEntryPoint configured)")
    void requestWithoutJwtReturns403() {
        // Spring Security defaults to 403 when no AuthenticationEntryPoint is configured.
        // The important thing is that unauthenticated access is rejected.
        given()
        .when()
            .get("/menu")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("8. Multi-tenant isolation: tenant A does not see tenant B products")
    void multiTenantIsolation() {
        UUID tenantB = createTenant();
        UUID catB = createCategory(tenantB, "Cat Tenant B", 0);
        createSimpleProduct(tenantB, catB, "Producto de Tenant B", true, true, 0);

        UUID catA = createCategory(tenantId, "Cat Tenant A", 0);
        createSimpleProduct(tenantId, catA, "Producto de Tenant A", true, true, 0);

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/menu")
        .then()
            .statusCode(200)
            .body("data.categories", hasSize(1))
            .body("data.categories[0].name", is("Cat Tenant A"))
            .body("data.categories[0].products[0].name", is("Producto de Tenant A"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID createTenant() {
        jdbcTemplate.update("""
            INSERT INTO subscription_plans (id, name, code, description, price_monthly_mxn, max_branches, max_users_per_branch, features, is_active, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-000000000001', 'Test Plan', 'TEST', 'Test Plan', 0.00, 1, 5, '{}', true, NOW(), NOW())
            ON CONFLICT (id) DO NOTHING
            """
        );
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) VALUES (?, ?, ?, '00000000-0000-0000-0000-000000000001', NOW(), NOW())",
            id, "Test Tenant " + id, "test-tenant-" + id
        );
        return id;
    }

    private void createUser(UUID forTenantId, UUID forUserId, UUID roleId, String email) {
        jdbcTemplate.update(
            "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
            forUserId, forTenantId, roleId, email, "Test User"
        );
    }

    private UUID createCategory(UUID forTenantId, String name, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO categories (id, tenant_id, name, is_active, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, true, ?, NOW(), NOW())
            """,
            id, forTenantId, name, sortOrder
        );
        return id;
    }

    private UUID createSimpleProduct(UUID forTenantId, UUID catId, String name,
                                     boolean isActive, boolean isAvailable, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO products (id, tenant_id, category_id, name, base_price,
                                  product_type, is_active, is_available, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, 25.00, 'SIMPLE', ?, ?, ?, NOW(), NOW())
            """,
            id, forTenantId, catId, name, isActive, isAvailable, sortOrder
        );
        return id;
    }
}
