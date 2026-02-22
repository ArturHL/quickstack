package com.quickstack.app.catalog;

import com.quickstack.app.BaseE2ETest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for catalog module schema (V3 migration).
 * <p>
 * These tests verify:
 * - Foreign key constraints enforce tenant isolation
 * - Unique constraints prevent duplicates within tenant
 * - Cross-tenant constraints prevent data leakage
 * - Soft delete doesn't violate constraints
 * <p>
 * ASVS V4.1: Access control - database-level multi-tenancy enforcement.
 */
@DisplayName("Catalog Schema E2E Tests")
class CatalogSchemaE2ETest extends BaseE2ETest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    @DisplayName("Should enforce tenant_id foreign key on categories")
    void shouldEnforceTenantIdForeignKeyOnCategories() {
        // Given - invalid tenant_id that doesn't exist in tenants table
        UUID invalidTenantId = UUID.randomUUID();

        // When/Then - Insert should fail with FK violation
        assertThatThrownBy(() -> {
            jdbcTemplate.update(
                "INSERT INTO categories (id, tenant_id, name) VALUES (?, ?, ?)",
                UUID.randomUUID(), invalidTenantId, "Test Category"
            );
        }).isInstanceOf(Exception.class);
    }

    @Test
    @Transactional
    @DisplayName("Should prevent duplicate category name within same parent and tenant")
    void shouldPreventDuplicateCategoryNameWithinSameParent() {
        // Given - Create tenant and parent category
        UUID tenantId = createTenant();
        UUID parentId = createCategory(tenantId, "Bebidas", null);

        // Create first category
        createCategory(tenantId, "Calientes", parentId);

        // When/Then - Duplicate name in same parent should fail
        assertThatThrownBy(() -> {
            createCategory(tenantId, "Calientes", parentId);
        }).isInstanceOf(Exception.class)
          .hasMessageContaining("uq_categories_tenant_name_parent");
    }

    @Test
    @Transactional
    @DisplayName("Should allow same category name in different parent levels")
    void shouldAllowSameCategoryNameInDifferentParents() {
        // Given
        UUID tenantId = createTenant();
        UUID parent1 = createCategory(tenantId, "Bebidas", null);
        UUID parent2 = createCategory(tenantId, "Comidas", null);

        // When - Create categories with same name under different parents
        UUID child1 = createCategory(tenantId, "Especiales", parent1);
        UUID child2 = createCategory(tenantId, "Especiales", parent2);

        // Then - Both should succeed
        assertThat(child1).isNotNull();
        assertThat(child2).isNotNull();
        assertThat(child1).isNotEqualTo(child2);
    }

    @Test
    @Transactional
    @DisplayName("Should prevent parent_id referencing category from different tenant")
    void shouldPreventParentIdCrossTenantReference() {
        // Given
        UUID tenantA = createTenant();
        UUID tenantB = createTenant();
        UUID parentInTenantA = createCategory(tenantA, "Bebidas", null);

        // When/Then - Category in tenantB cannot reference parent in tenantA
        assertThatThrownBy(() -> {
            createCategory(tenantB, "Calientes", parentInTenantA);
        }).isInstanceOf(Exception.class)
          .hasMessageContaining("fk_categories_parent");
    }

    @Test
    @Transactional
    @DisplayName("Should prevent duplicate product SKU within tenant")
    void shouldPreventDuplicateProductSkuWithinTenant() {
        // Given
        UUID tenantId = createTenant();
        UUID categoryId = createCategory(tenantId, "Bebidas", null);

        // Create first product with SKU
        createProduct(tenantId, categoryId, "Coca Cola", "COCA-001");

        // When/Then - Duplicate SKU should fail
        assertThatThrownBy(() -> {
            createProduct(tenantId, categoryId, "Pepsi", "COCA-001");
        }).isInstanceOf(Exception.class)
          .hasMessageContaining("uq_products_tenant_sku");
    }

    @Test
    @Transactional
    @DisplayName("Should allow same product SKU in different tenants")
    void shouldAllowSameProductSkuInDifferentTenants() {
        // Given
        UUID tenantA = createTenant();
        UUID tenantB = createTenant();
        UUID categoryA = createCategory(tenantA, "Bebidas", null);
        UUID categoryB = createCategory(tenantB, "Bebidas", null);

        // When - Same SKU in different tenants
        UUID productA = createProduct(tenantA, categoryA, "Coca Cola", "COCA-001");
        UUID productB = createProduct(tenantB, categoryB, "Coca Cola", "COCA-001");

        // Then - Both should succeed
        assertThat(productA).isNotNull();
        assertThat(productB).isNotNull();
    }

    @Test
    @Transactional
    @DisplayName("Should preserve constraints after soft delete")
    void shouldPreserveConstraintsAfterSoftDelete() {
        // Given
        UUID tenantId = createTenant();
        UUID categoryId = createCategory(tenantId, "Bebidas", null);

        // Soft delete category
        jdbcTemplate.update(
            "UPDATE categories SET deleted_at = NOW() WHERE id = ?",
            categoryId
        );

        // When - Create new category with same name
        UUID newCategoryId = createCategory(tenantId, "Bebidas", null);

        // Then - Should succeed (soft deleted category doesn't block unique constraint)
        assertThat(newCategoryId).isNotNull();

        // Verify old category still exists
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT id, deleted_at FROM categories WHERE tenant_id = ? AND name = ?",
            tenantId, "Bebidas"
        );
        assertThat(results).hasSize(2); // Both exist
        assertThat(results.stream().filter(r -> r.get("deleted_at") != null).count()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private UUID createTenant() {
        UUID tenantId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO tenants (id, name, created_at, updated_at)
            VALUES (?, ?, NOW(), NOW())
            """,
            tenantId, "Test Tenant " + tenantId
        );
        return tenantId;
    }

    private UUID createCategory(UUID tenantId, String name, UUID parentId) {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO categories (id, tenant_id, parent_id, name, created_at, updated_at)
            VALUES (?, ?, ?, ?, NOW(), NOW())
            """,
            categoryId, tenantId, parentId, name
        );
        return categoryId;
    }

    private UUID createProduct(UUID tenantId, UUID categoryId, String name, String sku) {
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update(
            """
            INSERT INTO products (id, tenant_id, category_id, name, sku, base_price,
                                  product_type, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 10.00, 'SIMPLE', NOW(), NOW())
            """,
            productId, tenantId, categoryId, name, sku
        );
        return productId;
    }
}
