package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.product.dto.request.ModifierCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupCreateRequest;
import com.quickstack.product.dto.request.ModifierGroupUpdateRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end integration tests for modifier groups and modifiers management.
 * <p>
 * ASVS V4.1: Tests verify multi-tenant IDOR protection.
 */
@DisplayName("Modifier Groups & Modifiers E2E Tests")
class ModifierIntegrationTest extends BaseE2ETest {

    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID productId;
    private String ownerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() {
        tenantId = createTenant();
        UUID userId = UUID.randomUUID();
        createUser(tenantId, userId, OWNER_ROLE_ID, "owner@test.com");
        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@test.com");
        cashierToken = authHeader(userId, tenantId, CASHIER_ROLE_ID, "cashier@test.com");
        productId = createProduct(tenantId, "Hamburguesa Clásica", "HAM-" + UUID.randomUUID());
    }

    // -------------------------------------------------------------------------
    // Test 1: OWNER creates modifier group → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1. OWNER can create modifier group for product and receives 201")
    void ownerCanCreateModifierGroup() {
        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                productId, "Tamaño", null, 1, 1, true, 0);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/products/{productId}/modifier-groups", productId)
                .then()
                .statusCode(201)
                .header("Location", containsString("/modifier-groups/"))
                .body("data.id", notNullValue())
                .body("data.name", is("Tamaño"))
                .body("data.isRequired", is(true))
                .body("data.minSelections", is(1));
    }

    // -------------------------------------------------------------------------
    // Test 2: CASHIER cannot create modifier group → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("2. CASHIER cannot create modifier group, receives 403")
    void cashierCannotCreateModifierGroup() {
        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                productId, "Extras", null, 0, null, false, 0);

        given()
                .header("Authorization", cashierToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/products/{productId}/modifier-groups", productId)
                .then()
                .statusCode(403);
    }

    // -------------------------------------------------------------------------
    // Test 3: Duplicate modifier group name within same product → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("3. Duplicate modifier group name for same product returns 409")
    void duplicateModifierGroupNameReturnsConflict() {
        ModifierGroupCreateRequest request = new ModifierGroupCreateRequest(
                productId, "Salsas", null, 0, null, false, 0);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/products/{productId}/modifier-groups", productId)
                .then()
                .statusCode(201);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/products/{productId}/modifier-groups", productId)
                .then()
                .statusCode(409)
                .body("error.code", is("DUPLICATE_NAME"));
    }

    // -------------------------------------------------------------------------
    // Test 4: GET modifier groups for product returns list
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("4. GET modifier groups for product returns all created groups")
    void listModifierGroupsForProduct() {
        createModifierGroupDirect(tenantId, productId, "Proteína", 1);
        createModifierGroupDirect(tenantId, productId, "Acompañamiento", 2);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/products/{productId}/modifier-groups", productId)
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThanOrEqualTo(2)));
    }

    // -------------------------------------------------------------------------
    // Test 5: OWNER adds modifier to group → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("5. OWNER can add modifier to group and receives 201")
    void ownerCanAddModifier() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Tamaño", 1);
        ModifierCreateRequest request = new ModifierCreateRequest(
                groupId, "Grande", new BigDecimal("15.00"), null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/modifier-groups/{groupId}/modifiers", groupId)
                .then()
                .statusCode(201)
                .body("data.id", notNullValue())
                .body("data.name", is("Grande"))
                .body("data.priceAdjustment", is(15.0f));
    }

    // -------------------------------------------------------------------------
    // Test 6: CASHIER cannot add modifier → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("6. CASHIER cannot add modifier to group, receives 403")
    void cashierCannotAddModifier() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Tamaño", 1);
        ModifierCreateRequest request = new ModifierCreateRequest(
                groupId, "Mediano", BigDecimal.ZERO, null, null);

        given()
                .header("Authorization", cashierToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/modifier-groups/{groupId}/modifiers", groupId)
                .then()
                .statusCode(403);
    }

    // -------------------------------------------------------------------------
    // Test 7: Cross-tenant IDOR for modifier group → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("7. Modifier group from tenant A is not visible to tenant B (IDOR protection)")
    void crossTenantModifierGroupReturns404() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Privado", 1);

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .get("/modifier-groups/{id}", groupId)
                .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Test 8: DELETE modifier group cascades soft-delete to its modifiers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("8. DELETE modifier group soft-deletes group and all its modifiers")
    void deleteModifierGroupCascadesToModifiers() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Bebida", 1);
        UUID modifierId = createModifierDirect(tenantId, groupId, "Refresco", new BigDecimal("10.00"));

        given()
                .header("Authorization", ownerToken)
                .when()
                .delete("/modifier-groups/{id}", groupId)
                .then()
                .statusCode(204);

        List<Map<String, Object>> groupRows = jdbcTemplate.queryForList(
                "SELECT deleted_at FROM modifier_groups WHERE id = ?", groupId);
        assertThat(groupRows.get(0).get("deleted_at")).isNotNull();

        List<Map<String, Object>> modifierRows = jdbcTemplate.queryForList(
                "SELECT deleted_at FROM modifiers WHERE id = ?", modifierId);
        assertThat(modifierRows.get(0).get("deleted_at")).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Test 9: Cannot delete last active modifier → 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("9. DELETE last active modifier in group returns 409")
    void cannotDeleteLastActiveModifier() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Salsa", 1);
        UUID modifierId = createModifierDirect(tenantId, groupId, "Roja", BigDecimal.ZERO);

        given()
                .header("Authorization", ownerToken)
                .when()
                .delete("/modifiers/{id}", modifierId)
                .then()
                .statusCode(409)
                .body("error.code", is("LAST_ACTIVE_MODIFIER"));
    }

    // -------------------------------------------------------------------------
    // Test 10: GET modifier group returns group with its modifiers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("10. GET modifier group returns group data with nested active modifiers")
    void getModifierGroupIncludesModifiers() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Proteína", 1);
        createModifierDirect(tenantId, groupId, "Pollo", BigDecimal.ZERO);
        createModifierDirect(tenantId, groupId, "Res", new BigDecimal("20.00"));

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/modifier-groups/{id}", groupId)
                .then()
                .statusCode(200)
                .body("data.name", is("Proteína"))
                .body("data.modifiers", hasSize(2));
    }

    // -------------------------------------------------------------------------
    // Test 11: PUT modifier group updates name → 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("11. PUT modifier group updates name and returns 200")
    void ownerCanUpdateModifierGroup() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Viejo Nombre", 1);
        ModifierGroupUpdateRequest updateRequest = new ModifierGroupUpdateRequest(
                "Nuevo Nombre", null, null, null, null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/modifier-groups/{id}", groupId)
                .then()
                .statusCode(200)
                .body("data.name", is("Nuevo Nombre"));
    }

    // -------------------------------------------------------------------------
    // Test 12: Cross-tenant IDOR for modifier delete → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("12. Modifier from tenant A is not deletable by tenant B (IDOR protection)")
    void crossTenantModifierDeleteReturns404() {
        UUID groupId = createModifierGroupDirect(tenantId, productId, "Extra", 1);
        UUID modifierId = createModifierDirect(tenantId, groupId, "Opción A", BigDecimal.ZERO);
        createModifierDirect(tenantId, groupId, "Opción B", BigDecimal.ZERO);

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .delete("/modifiers/{id}", modifierId)
                .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) VALUES (?, ?, ?, '11111111-1111-1111-1111-111111111111', NOW(), NOW())",
                id, "Test Tenant " + id, "test-tenant-" + id);
        return id;
    }

    private void createUser(UUID tenantId, UUID userId, UUID roleId, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                userId, tenantId, roleId, email, "Test User");
    }

    private UUID createProduct(UUID forTenantId, String name, String sku) {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO categories (id, tenant_id, name, is_active, sort_order, created_at, updated_at)
                VALUES (?, ?, 'Cat', true, 0, NOW(), NOW())
                """, categoryId, forTenantId);
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO products (id, tenant_id, category_id, name, sku, base_price,
                                      product_type, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 10.00, 'SIMPLE', NOW(), NOW())
                """, id, forTenantId, categoryId, name, sku);
        return id;
    }

    private UUID createModifierGroupDirect(UUID forTenantId, UUID forProductId, String name, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO modifier_groups (id, tenant_id, product_id, name, min_selections, max_selections,
                                             is_required, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, 0, 1, false, ?, NOW(), NOW())
                """, id, forTenantId, forProductId, name, sortOrder);
        return id;
    }

    private UUID createModifierDirect(UUID forTenantId, UUID groupId, String name, BigDecimal price) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO modifiers (id, tenant_id, group_id, name, price_adjustment,
                                       is_default, is_active, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, false, true, 0, NOW(), NOW())
                """, id, forTenantId, groupId, name, price);
        return id;
    }
}
