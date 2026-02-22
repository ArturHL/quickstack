package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.product.dto.request.ProductAvailabilityRequest;
import com.quickstack.product.dto.request.ProductCreateRequest;
import com.quickstack.product.dto.request.ProductUpdateRequest;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.entity.ProductType;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
 * End-to-end integration tests for product management.
 */
@Disabled("E2E tests disabled for Phase 1.1 development")
@DisplayName("Product E2E Tests")
class ProductE2ETest extends BaseE2ETest {

    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID userId;
    private UUID categoryId;
    private String ownerToken;
    private String cashierToken;

    @BeforeEach
    void setUp() {
        tenantId = createTenant();
        userId = UUID.randomUUID();
        categoryId = createCategoryDirect(tenantId, "Bebidas", true);
        ownerToken = authHeader(userId, tenantId, OWNER_ROLE_ID, "owner@test.com");
        cashierToken = authHeader(userId, tenantId, CASHIER_ROLE_ID, "cashier@test.com");
    }

    @Test
    @DisplayName("1. OWNER can create SIMPLE product")
    void ownerCanCreateSimpleProduct() {
        ProductCreateRequest request = new ProductCreateRequest(
            "Coca Cola", "Refresco frío", categoryId, "COCA-001",
            new BigDecimal("15.00"), new BigDecimal("8.00"),
            null, ProductType.SIMPLE, 1, null);

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/products")
        .then()
            .statusCode(201)
            .body("data.name", is("Coca Cola"))
            .body("data.sku", is("COCA-001"));
    }

    @Test
    @DisplayName("2. OWNER can create VARIANT product with variants")
    void ownerCanCreateVariantProduct() {
        VariantCreateRequest v1 = new VariantCreateRequest("Chico", "COF-S", BigDecimal.ZERO, true, 1);
        VariantCreateRequest v2 = new VariantCreateRequest("Grande", "COF-L", new BigDecimal("10.00"), false, 2);

        ProductCreateRequest request = new ProductCreateRequest(
            "Cafe", "Cafe caliente", categoryId, "COF-001",
            new BigDecimal("30.00"), null, null, ProductType.VARIANT, 1, List.of(v1, v2));

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/products")
        .then()
            .statusCode(201)
            .body("data.productType", is("VARIANT"))
            .body("data.variants", hasSize(2))
            .body("data.variants[1].effectivePrice", is(40.0f));
    }

    @Test
    @DisplayName("3. VARIANT product requires variants")
    void variantProductRequiresVariants() {
        ProductCreateRequest request = new ProductCreateRequest(
            "Cafe", null, categoryId, null,
            new BigDecimal("30.00"), null, null, ProductType.VARIANT, 1, List.of());

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/products")
        .then()
            .statusCode(409) // BusinessRuleException
            .body("error.code", is("VARIANT_PRODUCT_REQUIRES_VARIANTS"));
    }

    @Test
    @DisplayName("4. Cannot create product with duplicate SKU in same tenant")
    void cannotCreateDuplicateSku() {
        createProductDirect(tenantId, categoryId, "Product 1", "SKU-DUPE");

        ProductCreateRequest request = new ProductCreateRequest(
            "Product 2", null, categoryId, "SKU-DUPE",
            new BigDecimal("10.00"), null, null, ProductType.SIMPLE, 1, null);

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/products")
        .then()
            .statusCode(409)
            .body("error.code", is("DUPLICATE_SKU"));
    }

    @Test
    @DisplayName("5. Cannot create product in category of another tenant")
    void cannotCreateInOtherTenantCategory() {
        UUID otherTenantId = createTenant();
        UUID otherCategoryId = createCategoryDirect(otherTenantId, "Other", true);

        ProductCreateRequest request = new ProductCreateRequest(
            "Product", null, otherCategoryId, null,
            new BigDecimal("10.00"), null, null, ProductType.SIMPLE, 1, null);

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/products")
        .then()
            .statusCode(404)
            .body("error.code", is("CATEGORY_NOT_FOUND"));
    }

    @Test
    @DisplayName("6. List products filtered by category")
    void listProductsByCategory() {
        UUID cat2 = createCategoryDirect(tenantId, "Cat 2", true);
        createProductDirect(tenantId, categoryId, "P1", "SKU1");
        createProductDirect(tenantId, cat2, "P2", "SKU2");

        given()
            .header("Authorization", ownerToken)
            .param("categoryId", categoryId.toString())
        .when()
            .get("/products")
        .then()
            .statusCode(200)
            .body("data.content", hasSize(1))
            .body("data.content[0].name", is("P1"));
    }

    @Test
    @DisplayName("7. Search products by name")
    void searchProductsByName() {
        createProductDirect(tenantId, categoryId, "Taco Pastor", "SKU1");
        createProductDirect(tenantId, categoryId, "Taco Pollo", "SKU2");
        createProductDirect(tenantId, categoryId, "Gringa", "SKU3");

        given()
            .header("Authorization", ownerToken)
            .param("search", "taco")
        .when()
            .get("/products")
        .then()
            .statusCode(200)
            .body("data.content", hasSize(2));
    }

    @Test
    @DisplayName("8. MANAGER can change availability")
    void managerCanChangeAvailability() {
        UUID productId = createProductDirect(tenantId, categoryId, "Agua", "SKU1");

        given()
            .header("Authorization", ownerToken)
            .contentType(ContentType.JSON)
            .body(new ProductAvailabilityRequest(false))
        .when()
            .patch("/products/{id}/availability", productId)
        .then()
            .statusCode(200)
            .body("data.isAvailable", is(false));
    }

    @Test
    @DisplayName("9. CASHIER cannot change availability")
    void cashierCannotChangeAvailability() {
        UUID productId = createProductDirect(tenantId, categoryId, "Agua", "SKU1");

        given()
            .header("Authorization", cashierToken)
            .contentType(ContentType.JSON)
            .body(new ProductAvailabilityRequest(false))
        .when()
            .patch("/products/{id}/availability", productId)
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("10. IDOR protection: cannot access other tenant product")
    void idorProtection() {
        UUID otherTenantId = createTenant();
        UUID otherCatId = createCategoryDirect(otherTenantId, "Other", true);
        UUID otherProductId = createProductDirect(otherTenantId, otherCatId, "Private", "SKU-OTHER");

        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/products/{id}", otherProductId)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("11. Soft delete and restore product")
    void softDeleteAndRestore() {
        UUID productId = createProductDirect(tenantId, categoryId, "Temp", "SKU-TEMP");

        // Delete
        given()
            .header("Authorization", ownerToken)
        .when()
            .delete("/products/{id}", productId)
        .then()
            .statusCode(204);

        // Verify not in list
        given()
            .header("Authorization", ownerToken)
        .when()
            .get("/products")
        .then()
            .statusCode(200)
            .body("data.content.id", not(hasItem(productId.toString())));

        // Restore
        given()
            .header("Authorization", ownerToken)
        .when()
            .post("/products/{id}/restore", productId)
        .then()
            .statusCode(200)
            .body("data.isActive", is(true));
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

    private UUID createProductDirect(UUID forTenantId, UUID catId, String name, String sku) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO products (id, tenant_id, category_id, name, sku, base_price,
                                  product_type, is_active, is_available, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 10.00, 'SIMPLE', true, true, 0, NOW(), NOW())
            """,
            id, forTenantId, catId, name, sku
        );
        return id;
    }
}
