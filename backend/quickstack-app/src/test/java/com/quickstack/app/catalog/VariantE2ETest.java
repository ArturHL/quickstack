package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.dto.request.VariantUpdateRequest;
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

@DisplayName("Variant E2E Tests")
class VariantE2ETest extends BaseE2ETest {

    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID userId;
    private UUID categoryId;
    private UUID productId;
    private String ownerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() {
        tenantId = createTenant();
        userId = UUID.randomUUID();
        categoryId = createCategoryDirect(tenantId, "Pizzas", true);
        productId = createProductDirect(tenantId, categoryId, "Pizza Peperoni", "PIZ-PEP");
        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@test.com");
        cashierToken = authHeader(userId, tenantId, CASHIER_ROLE_ID, "cashier@test.com");
    }

    @Test
    @DisplayName("1. List variants of a product")
    void listVariants() {
        createVariantDirect(tenantId, productId, "Chica", "PIZ-PEP-CH", BigDecimal.ZERO, true, 1);
        createVariantDirect(tenantId, productId, "Mediana", "PIZ-PEP-MD", new BigDecimal("50.00"), false, 2);

        given()
            .header("Authorization", cashierToken) // Any user can read
        .when().log().all()
            .get("/products/{id}/variants", productId)
        .then().log().all()
            .statusCode(200)
            .body("data", hasSize(2))
            .body("data[0].name", is("Chica"))
            .body("data[1].name", is("Mediana"));
    }

    @Test
    @DisplayName("2. Add variant to a product as MANAGER+")
    void addVariantSuccess() {
        VariantCreateRequest request = new VariantCreateRequest("Familiar", "PIZ-PEP-FM", new BigDecimal("100.00"), false, 3);

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when().log().all()
            .post("/products/{id}/variants", productId)
        .then().log().all()
            .statusCode(201)
            .body("data.name", is("Familiar"))
            .body("data.effectivePrice", is(110.0f)); // Product base = 10.00, variant adjustment = +100.00
    }

    @Test
    @DisplayName("3. Cannot add variant as CASHIER")
    void addVariantForbiddenCashier() {
        VariantCreateRequest request = new VariantCreateRequest("Extra Grande", "PIZ-PEP-XG", new BigDecimal("150.00"), false, 4);

        given()
            .header("Authorization", cashierToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when().log().all()
            .post("/products/{id}/variants", productId)
        .then().log().all()
            .statusCode(403);
    }

    @Test
    @DisplayName("4. Update an existing variant")
    void updateVariantSuccess() {
        UUID variantId = createVariantDirect(tenantId, productId, "Chica", "PIZ-PEP-CH", BigDecimal.ZERO, true, 1);
        
        VariantUpdateRequest request = new VariantUpdateRequest("Super Chica", null, new BigDecimal("-2.00"), null, null, null);

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when().log().all()
            .put("/products/{productId}/variants/{variantId}", productId, variantId)
        .then().log().all()
            .statusCode(200)
            .body("data.name", is("Super Chica"))
            .body("data.effectivePrice", is(8.0f)); // 10.00 - 2.00
    }

    @Test
    @DisplayName("5. Delete an existing variant (Soft Delete)")
    void deleteVariantSuccess() {
        // Need at least 2 variants so we don't hit the "cannot delete last variant" rule.
        createVariantDirect(tenantId, productId, "Chica", "PIZ-PEP-CH", BigDecimal.ZERO, true, 1);
        UUID variantToDelete = createVariantDirect(tenantId, productId, "Mediana", "PIZ-PEP-MD", new BigDecimal("50.00"), false, 2);

        given()
            .header("Authorization", ownerToken)
        .when().log().all()
            .delete("/products/{productId}/variants/{variantId}", productId, variantToDelete)
        .then().log().all()
            .statusCode(204);

        // Verify it was softly deleted (should not return in list)
        given()
            .header("Authorization", ownerToken)
        .when().log().all()
            .get("/products/{id}/variants", productId)
        .then().log().all()
            .statusCode(200)
            .body("data", hasSize(1))
            .body("data[0].name", is("Chica"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO subscription_plans (id, name, code, description, price_monthly_mxn, max_branches, max_users_per_branch, features, is_active, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-000000000001', 'Test Plan', 'TEST', 'Test Plan', 0.00, 1, 5, '{}', true, NOW(), NOW())
            ON CONFLICT (id) DO NOTHING
            """
        );
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) VALUES (?, ?, ?, '00000000-0000-0000-0000-000000000001', NOW(), NOW())",
            id, "Test Tenant " + id, "test-tenant-" + id
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

    private UUID createProductDirect(UUID forTenantId, UUID catId, String name, String sku) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO products (id, tenant_id, category_id, name, sku, base_price,
                                  product_type, is_active, is_available, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 10.00, 'VARIANT', true, true, 0, NOW(), NOW())
            """,
            id, forTenantId, catId, name, sku
        );
        return id;
    }

    private UUID createVariantDirect(UUID forTenantId, UUID prodId, String name, String sku, BigDecimal priceAdj, boolean isDefault, int sortOrder) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO product_variants (id, tenant_id, product_id, name, sku, price_adjustment,
                                          is_default, is_active, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, true, ?, NOW(), NOW())
            """,
            id, forTenantId, prodId, name, sku, priceAdj, isDefault, sortOrder
        );
        return id;
    }
}
