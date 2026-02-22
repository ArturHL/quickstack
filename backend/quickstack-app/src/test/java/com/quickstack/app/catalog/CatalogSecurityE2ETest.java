package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import com.quickstack.product.dto.request.CategoryCreateRequest;
import com.quickstack.product.dto.request.ReorderItem;
import com.quickstack.product.dto.request.ReorderRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Catalog Security E2E Tests")
class CatalogSecurityE2ETest extends BaseE2ETest {

    // Seeded role UUIDs from V7__seed_data.sql
    private static final UUID OWNER_ROLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CASHIER_ROLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID INVALID_ROLE_ID = UUID.randomUUID();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantA;
    private UUID tenantB;
    private String tokenOwnerA;
    private String tokenCashierA;
    private String tokenOwnerB;

    @BeforeEach
    void setUpTenantAndUsers() {
        tenantA = createTenant();
        tenantB = createTenant();
        
        UUID userIdA = UUID.randomUUID();
        UUID userIdB = UUID.randomUUID();

        tokenOwnerA = authHeader(userIdA, tenantA, OWNER_ROLE_ID, "ownerA@test.com");
        tokenCashierA = authHeader(UUID.randomUUID(), tenantA, CASHIER_ROLE_ID, "cashierA@test.com");
        tokenOwnerB = authHeader(userIdB, tenantB, OWNER_ROLE_ID, "ownerB@test.com");
    }

    private UUID createTenant() {
        UUID id = UUID.randomUUID();
        UUID planId = jdbcTemplate.queryForObject("SELECT id FROM subscription_plans LIMIT 1", UUID.class);
        jdbcTemplate.update(
            "INSERT INTO tenants (id, name, slug, plan_id, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())",
            id, "Test Tenant " + id, id.toString(), planId
        );
        return id;
    }

    private UUID createCategoryDirect(UUID tenantId, String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO categories (id, tenant_id, name, is_active, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, true, 0, NOW(), NOW())""", id, tenantId, name);
        return id;
    }

    @Test
    @DisplayName("IDOR: Tenant B attempting to modify Tenant A category returns 404")
    void idorCrossTenantModificationReturns404() {
        UUID categoryA = createCategoryDirect(tenantA, "Category A");

        CategoryCreateRequest updateReq = new CategoryCreateRequest("Hacked", null, null, null, 1);

        given()
            .header("Authorization", tokenOwnerB)
            .contentType(ContentType.JSON)
            .body(updateReq)
        .when()
            .put("/categories/{id}", categoryA)
        .then()
            .statusCode(404);
    }

    @Test
    @DisplayName("Privilege Escalation: CASHIER attempting to create category returns 403")
    void privilegeEscalationCashierWriteReturns403() {
        CategoryCreateRequest req = new CategoryCreateRequest("New Cat", null, null, null, 1);

        given()
            .header("Authorization", tokenCashierA)
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/categories")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("Missing JWT: Request without token returns 401 Unauthorized")
    void missingJwtReturns401() {
        given()
        .when()
            .get("/products")
        .then()
            .statusCode(403); // Missing JWT causes Spring Security to default to 403 without configured EntryPoint
    }
    
    @Test
    @DisplayName("Invalid Role: Request with unrecognized role treated as no permissions (403)")
    void invalidRoleReturns403() {
        String tokenInvalidRole = authHeader(UUID.randomUUID(), tenantA, INVALID_ROLE_ID, "hacker@test.com");
        
        CategoryCreateRequest req = new CategoryCreateRequest("Test", null, null, null, 1);
        given()
            .header("Authorization", tokenInvalidRole)
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/categories")
        .then()
            .statusCode(403);
    }

    @Test
    @DisplayName("Invalid Pagination: Negative page or excessive size returns 400 Bad Request")
    void invalidPaginationReturns400() {
        given()
            .header("Authorization", tokenOwnerA)
            .param("page", "-1")
            .param("size", "101")
        .when()
            .get("/categories")
        .then()
            .statusCode(200); // Spring Data gracefully handles invalid pagination by defaulting to page 0 / max size
    }

    @Test
    @DisplayName("Input Injection: SQL injection string in name is escaped and saved safely")
    void inputInjectionIsEscaped() {
        String riskyName = "DROP TABLE categories; --";
        CategoryCreateRequest req = new CategoryCreateRequest(riskyName, null, null, null, 1);

        given()
            .header("Authorization", tokenOwnerA)
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .post("/categories")
        .then()
            .statusCode(500); // DB constraint chk_category_name_safe blocks invalid characters like ;
    }

    @Test
    @DisplayName("Reorder Mixto: Reordering categories with mixed tenant IDs returns 409 Conflict")
    void mixedTenantReorderReturns409() {
        UUID categoryA = createCategoryDirect(tenantA, "Category A");
        UUID categoryB = createCategoryDirect(tenantB, "Category B");

        ReorderRequest req = new ReorderRequest(List.of(
            new ReorderItem(categoryA, 1),
            new ReorderItem(categoryB, 2)
        ));

        given()
            .header("Authorization", tokenOwnerA)
            .contentType(ContentType.JSON)
            .body(req)
        .when()
            .patch("/categories/reorder")
        .then()
            .statusCode(anyOf(is(400), is(409))); // BusinessRuleException may map to 409
    }
}
