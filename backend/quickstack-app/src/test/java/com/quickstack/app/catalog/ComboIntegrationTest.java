package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.product.dto.request.ComboCreateRequest;
import com.quickstack.product.dto.request.ComboItemRequest;
import com.quickstack.product.dto.request.ComboUpdateRequest;
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
 * End-to-end integration tests for combo management.
 * <p>
 * ASVS V4.1: Tests verify multi-tenant IDOR protection.
 * Checkpoint de Seguridad Post-Sprint 3 incluido.
 */
@DisplayName("Combos E2E Tests")
class ComboIntegrationTest extends BaseE2ETest {

    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID productAId;
    private UUID productBId;
    private String ownerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() {
        tenantId = createTenant();
        UUID userId = UUID.randomUUID();
        createUser(tenantId, userId, OWNER_ROLE_ID, "owner@test.com");
        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@test.com");
        cashierToken = authHeader(userId, tenantId, CASHIER_ROLE_ID, "cashier@test.com");
        productAId = createProduct(tenantId, "Hamburguesa Clásica", "HAM-" + UUID.randomUUID());
        productBId = createProduct(tenantId, "Refresco 500ml", "REF-" + UUID.randomUUID());
    }

    // -------------------------------------------------------------------------
    // Test 1: OWNER creates combo → 201
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("1. OWNER can create combo and receives 201 with Location header")
    void ownerCanCreateCombo() {
        ComboCreateRequest request = buildCreateRequest("Combo Clásico", productAId, productBId);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/combos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/combos/"))
                .body("data.id", notNullValue())
                .body("data.name", is("Combo Clásico"))
                .body("data.price", equalTo(99.0f))
                .body("data.items", hasSize(2));
    }

    // -------------------------------------------------------------------------
    // Test 2: CASHIER cannot create combo → 403
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("2. CASHIER cannot create combo, receives 403")
    void cashierCannotCreateCombo() {
        ComboCreateRequest request = buildCreateRequest("Combo Prohibido", productAId, productBId);

        given()
                .header("Authorization", cashierToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/combos")
                .then()
                .statusCode(403);
    }

    // -------------------------------------------------------------------------
    // Test 3: Duplicate name returns 409
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("3. Duplicate combo name within same tenant returns 409")
    void duplicateComboNameReturnsConflict() {
        ComboCreateRequest request = buildCreateRequest("Combo Único", productAId, productBId);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/combos")
                .then()
                .statusCode(201);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/combos")
                .then()
                .statusCode(409)
                .body("error.code", is("DUPLICATE_NAME"));
    }

    // -------------------------------------------------------------------------
    // Test 4: GET list returns combos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("4. GET /combos returns all active combos for tenant")
    void listCombosReturnsCreatedCombos() {
        createComboDirect(tenantId, "Combo A", productAId, productBId);
        createComboDirect(tenantId, "Combo B", productAId, productBId);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/combos")
                .then()
                .statusCode(200)
                .body("data", hasSize(greaterThanOrEqualTo(2)));
    }

    // -------------------------------------------------------------------------
    // Test 5: GET combo by ID returns combo with items
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("5. GET /combos/{id} returns combo with items and product names")
    void getComboReturnsItemsWithProductNames() {
        UUID comboId = createComboDirect(tenantId, "Combo Con Items", productAId, productBId);

        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/combos/{id}", comboId)
                .then()
                .statusCode(200)
                .body("data.name", is("Combo Con Items"))
                .body("data.items", hasSize(2))
                .body("data.items[0].productName", notNullValue());
    }

    // -------------------------------------------------------------------------
    // Test 6: PUT updates combo name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("6. PUT /combos/{id} updates combo name and returns 200")
    void ownerCanUpdateComboName() {
        UUID comboId = createComboDirect(tenantId, "Nombre Viejo", productAId, productBId);
        ComboUpdateRequest updateRequest = new ComboUpdateRequest(
                "Nombre Nuevo", null, null, null, null, null, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/combos/{id}", comboId)
                .then()
                .statusCode(200)
                .body("data.name", is("Nombre Nuevo"));
    }

    // -------------------------------------------------------------------------
    // Test 7: PUT replaces items (orphan removal)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("7. PUT /combos/{id} with items replaces existing items (orphan removal)")
    void updateComboReplacesItems() {
        UUID productCId = createProduct(tenantId, "Papa Frita", "PAP-" + UUID.randomUUID());
        UUID comboId = createComboDirect(tenantId, "Combo Para Actualizar", productAId, productBId);

        List<ComboItemRequest> newItems = List.of(
                new ComboItemRequest(productAId, 2, null, null, null),
                new ComboItemRequest(productCId, 1, null, null, null)
        );
        ComboUpdateRequest updateRequest = new ComboUpdateRequest(null, null, null, null, null, newItems, null);

        given()
                .header("Authorization", ownerToken)
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .when()
                .put("/combos/{id}", comboId)
                .then()
                .statusCode(200)
                .body("data.items", hasSize(2));

        // Verify old items are removed from DB
        int itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM combo_items WHERE combo_id = ?",
                Integer.class, comboId);
        assertThat(itemCount).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Test 8: Cross-tenant IDOR → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("8. Combo from tenant A is not visible to tenant B (IDOR protection)")
    void crossTenantComboReturns404() {
        UUID comboId = createComboDirect(tenantId, "Combo Privado", productAId, productBId);

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .get("/combos/{id}", comboId)
                .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Test 9: Cross-tenant IDOR on DELETE → 404
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("9. Combo from tenant A cannot be deleted by tenant B (IDOR protection)")
    void crossTenantDeleteReturns404() {
        UUID comboId = createComboDirect(tenantId, "Combo Protegido", productAId, productBId);

        UUID tenantB = createTenant();
        String tokenB = authHeader(UUID.randomUUID(), tenantB, OWNER_ROLE_ID, "b@test.com");

        given()
                .header("Authorization", tokenB)
                .when()
                .delete("/combos/{id}", comboId)
                .then()
                .statusCode(404);
    }

    // -------------------------------------------------------------------------
    // Test 10: DELETE soft-deletes combo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("10. DELETE /combos/{id} soft-deletes the combo")
    void deleteComboSoftDeletes() {
        UUID comboId = createComboDirect(tenantId, "Combo Para Borrar", productAId, productBId);

        given()
                .header("Authorization", ownerToken)
                .when()
                .delete("/combos/{id}", comboId)
                .then()
                .statusCode(204);

        // Verify soft-delete in DB
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT deleted_at, deleted_by FROM combos WHERE id = ?", comboId);
        assertThat(rows.get(0).get("deleted_at")).isNotNull();

        // Verify combo no longer accessible via API
        given()
                .header("Authorization", ownerToken)
                .when()
                .get("/combos/{id}", comboId)
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

    private void createUser(UUID forTenantId, UUID userId, UUID roleId, String email) {
        jdbcTemplate.update(
                "INSERT INTO users (id, tenant_id, role_id, email, full_name, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'hash', NOW(), NOW())",
                userId, forTenantId, roleId, email, "Test User");
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

    private UUID createComboDirect(UUID forTenantId, String name, UUID prodA, UUID prodB) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO combos (id, tenant_id, name, price, is_active, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, 99.00, true, 0, NOW(), NOW())
                """, id, forTenantId, name);
        jdbcTemplate.update("""
                INSERT INTO combo_items (id, tenant_id, combo_id, product_id, quantity, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 0, NOW(), NOW())
                """, UUID.randomUUID(), forTenantId, id, prodA);
        jdbcTemplate.update("""
                INSERT INTO combo_items (id, tenant_id, combo_id, product_id, quantity, sort_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 1, NOW(), NOW())
                """, UUID.randomUUID(), forTenantId, id, prodB);
        return id;
    }

    private ComboCreateRequest buildCreateRequest(String name, UUID prodA, UUID prodB) {
        return new ComboCreateRequest(
                name, null, null, new BigDecimal("99.00"),
                List.of(
                        new ComboItemRequest(prodA, 1, null, null, null),
                        new ComboItemRequest(prodB, 1, null, null, null)
                ), null);
    }
}
