package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end integration tests for the GET /api/v1/menu endpoint.
 * <p>
 * Verifies:
 * - Menu assembles correctly from DB state (categories + products)
 * - Modifier groups and modifiers appear in products
 * - Combos appear as a top-level list in the menu response
 * - Filtering rules (inactive excluded, agotados included)
 * - Category exclusion when empty
 * - sort_order ordering
 * - Authentication required
 * - Multi-tenant isolation
 */
@DisplayName("Menu E2E Tests")
class MenuE2ETest extends BaseE2ETest {

    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID userId;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        tenantId = createTenant();
        userId = UUID.randomUUID();
        createUser(tenantId, userId, OWNER_ROLE_ID, "owner@menutest.com");
        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@menutest.com");
    }

    @Test
    @DisplayName("1. GET /api/v1/menu returns only categories that have active products")
    void returnsOnlyCategoriesWithActiveProducts() {
        UUID catWithProducts = createCategory(tenantId, "Tacos", 0);
        createCategory(tenantId, "Postres", 1);
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
    @DisplayName("7. Request without JWT returns 403")
    void requestWithoutJwtReturns403() {
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

    @Test
    @DisplayName("9. Products with modifier groups include them in the response")
    void productModifierGroupsIncludedInMenu() {
        UUID cat = createCategory(tenantId, "Hamburguesas", 0);
        UUID productId = createSimpleProduct(tenantId, cat, "Hamburguesa Clásica", true, true, 0);
        UUID groupId = createModifierGroup(tenantId, productId, "Extras", 0, 3, false);
        createModifier(tenantId, groupId, "Extra Queso", new java.math.BigDecimal("15.00"), 0);
        createModifier(tenantId, groupId, "Extra Jalapeño", new java.math.BigDecimal("5.00"), 1);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/menu")
                .then()
                .statusCode(200)
                .body("data.categories[0].products[0].name", is("Hamburguesa Clásica"))
                .body("data.categories[0].products[0].modifierGroups", hasSize(1))
                .body("data.categories[0].products[0].modifierGroups[0].name", is("Extras"))
                .body("data.categories[0].products[0].modifierGroups[0].minSelections", is(0))
                .body("data.categories[0].products[0].modifierGroups[0].maxSelections", is(3))
                .body("data.categories[0].products[0].modifierGroups[0].isRequired", is(false))
                .body("data.categories[0].products[0].modifierGroups[0].modifiers", hasSize(2))
                .body("data.categories[0].products[0].modifierGroups[0].modifiers[0].name", is("Extra Queso"))
                .body("data.categories[0].products[0].modifierGroups[0].modifiers[1].name", is("Extra Jalapeño"));
    }

    @Test
    @DisplayName("10. Products without modifier groups return empty modifierGroups array")
    void productWithoutModifierGroupsReturnsEmptyArray() {
        UUID cat = createCategory(tenantId, "Tacos", 0);
        createSimpleProduct(tenantId, cat, "Taco Simple", true, true, 0);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/menu")
                .then()
                .statusCode(200)
                .body("data.categories[0].products[0].modifierGroups", hasSize(0));
    }

    @Test
    @DisplayName("11. Active combos appear as a top-level list in the menu")
    void activeCombosAppearInMenu() {
        UUID cat = createCategory(tenantId, "Tacos", 0);
        UUID productId1 = createSimpleProduct(tenantId, cat, "Taco Pastor", true, true, 0);
        UUID productId2 = createSimpleProduct(tenantId, cat, "Agua", true, true, 1);
        UUID comboId = createCombo(tenantId, "Combo Taquero", new java.math.BigDecimal("75.00"), true, 0);
        createComboItem(tenantId, comboId, productId1, 3);
        createComboItem(tenantId, comboId, productId2, 1);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/menu")
                .then()
                .statusCode(200)
                .body("data.combos", hasSize(1))
                .body("data.combos[0].name", is("Combo Taquero"))
                .body("data.combos[0].price", is(75.0f))
                .body("data.combos[0].items", hasSize(2));
    }

    @Test
    @DisplayName("12. Inactive combos (is_active=false) do NOT appear in menu")
    void inactiveCombosExcluded() {
        UUID cat = createCategory(tenantId, "Tacos", 0);
        UUID productId = createSimpleProduct(tenantId, cat, "Taco", true, true, 0);
        UUID comboId = createCombo(tenantId, "Combo Inactivo", new java.math.BigDecimal("60.00"), false, 0);
        createComboItem(tenantId, comboId, productId, 2);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/menu")
                .then()
                .statusCode(200)
                .body("data.combos", hasSize(0));
    }

    @Test
    @DisplayName("13. Combo with an inactive product does NOT appear in menu")
    void comboWithInactiveProductExcluded() {
        UUID cat = createCategory(tenantId, "Tacos", 0);
        UUID activeProductId = createSimpleProduct(tenantId, cat, "Taco Activo", true, true, 0);
        UUID inactiveProductId = createSimpleProduct(tenantId, cat, "Taco Inactivo", false, true, 1);

        UUID comboId = createCombo(tenantId, "Combo Mixto", new java.math.BigDecimal("80.00"), true, 0);
        createComboItem(tenantId, comboId, activeProductId, 1);
        createComboItem(tenantId, comboId, inactiveProductId, 1);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/menu")
                .then()
                .statusCode(200)
                .body("data.combos", hasSize(0));
    }

    @Test
    @DisplayName("14. Combos respect multi-tenant isolation")
    void combosRespectMultiTenantIsolation() {
        // Tenant A setup
        UUID catA = createCategory(tenantId, "Tacos", 0);
        UUID productA = createSimpleProduct(tenantId, catA, "Taco A", true, true, 0);
        UUID comboA = createCombo(tenantId, "Combo Tenant A", new java.math.BigDecimal("50.00"), true, 0);
        createComboItem(tenantId, comboA, productA, 2);

        // Tenant B setup
        UUID tenantB = createTenant();
        UUID catB = createCategory(tenantB, "Burgers", 0);
        UUID productB = createSimpleProduct(tenantB, catB, "Burger B", true, true, 0);
        UUID comboB = createCombo(tenantB, "Combo Tenant B", new java.math.BigDecimal("70.00"), true, 0);
        createComboItem(tenantB, comboB, productB, 1);

        // Tenant A token should only see Tenant A combos
        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/menu")
                .then()
                .statusCode(200)
                .body("data.combos", hasSize(1))
                .body("data.combos[0].name", is("Combo Tenant A"));
    }

    @Test
    @DisplayName("15. Menu returns both categories and combos in same response")
    void menuReturnsCategoriesAndCombos() {
        UUID cat = createCategory(tenantId, "Tacos", 0);
        UUID productId = createSimpleProduct(tenantId, cat, "Taco Pastor", true, true, 0);
        UUID comboId = createCombo(tenantId, "Combo Taquero", new java.math.BigDecimal("65.00"), true, 0);
        createComboItem(tenantId, comboId, productId, 3);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/menu")
                .then()
                .statusCode(200)
                .body("data.categories", hasSize(1))
                .body("data.categories[0].name", is("Tacos"))
                .body("data.combos", hasSize(1))
                .body("data.combos[0].name", is("Combo Taquero"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID createTenant() {
        jdbcTemplate.update("""
                INSERT INTO subscription_plans (id, name, code, description, price_monthly_mxn, max_branches, max_users_per_branch, features, is_active, created_at, updated_at)
                VALUES ('00000000-0000-0000-0000-000000000001', 'Test Plan', 'TEST', 'Test Plan', 0.00, 1, 5, '{}', true, NOW(), NOW())
                ON CONFLICT (id) DO NOTHING
                """);
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) VALUES (?, ?, ?, '00000000-0000-0000-0000-000000000001', NOW(), NOW())",
                id, "Test Tenant " + id, "test-tenant-" + id);
        return id;
    }

    private void createUser(UUID forTenantId, UUID forUserId, UUID roleId, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                forUserId, forTenantId, roleId, email, "Test User");
    }

    private UUID createCategory(UUID forTenantId, String name, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO categories (id, tenant_id, name, is_active, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, true, ?, NOW(), NOW())
                """,
                id, forTenantId, name, sortOrder);
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
                id, forTenantId, catId, name, isActive, isAvailable, sortOrder);
        return id;
    }

    private UUID createModifierGroup(UUID forTenantId, UUID productId, String name,
            int minSelections, Integer maxSelections, boolean isRequired) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO modifier_groups (id, tenant_id, product_id, name, min_selections, max_selections,
                                             is_required, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, NOW(), NOW())
                """,
                id, forTenantId, productId, name, minSelections, maxSelections, isRequired);
        return id;
    }

    private UUID createModifier(UUID forTenantId, UUID groupId, String name,
            java.math.BigDecimal priceAdjustment, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO modifiers (id, tenant_id, group_id, name, price_adjustment,
                                       is_default, is_active, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, false, true, ?, NOW(), NOW())
                """,
                id, forTenantId, groupId, name, priceAdjustment, sortOrder);
        return id;
    }

    private UUID createCombo(UUID forTenantId, String name, java.math.BigDecimal price,
            boolean isActive, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO combos (id, tenant_id, name, price, is_active, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                """,
                id, forTenantId, name, price, isActive, sortOrder);
        return id;
    }

    private void createComboItem(UUID forTenantId, UUID comboId, UUID productId, int quantity) {
        jdbcTemplate.update("""
                INSERT INTO combo_items (id, tenant_id, combo_id, product_id, quantity,
                                         allow_substitutes, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, false, 0, NOW(), NOW())
                """,
                UUID.randomUUID(), forTenantId, comboId, productId, quantity);
    }
}
