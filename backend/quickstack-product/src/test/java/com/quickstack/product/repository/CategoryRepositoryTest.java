package com.quickstack.product.repository;

import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.quickstack.product.AbstractRepositoryTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for CategoryRepository using Testcontainers.
 * <p>
 * These tests verify:
 * - Multi-tenant isolation
 * - Soft delete filtering
 * - Uniqueness constraints
 * - Product counting for delete validation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Category Repository")
class CategoryRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        // Create a test plan and tenant using native SQL as entities might not be
        // available in this module
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) "
                        +
                        "VALUES (?, 'Test Plan', 'TEST', 0, 1, 5)")
                .setParameter(1, planId).executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) " +
                        "VALUES (?, 'Tenant A', 'tenant-a', ?, 'ACTIVE')")
                .setParameter(1, tenantA).setParameter(2, planId).executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) " +
                        "VALUES (?, 'Tenant B', 'tenant-b', ?, 'ACTIVE')")
                .setParameter(1, tenantB).setParameter(2, planId).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should find categories by tenant ID excluding soft-deleted")
    void shouldFindCategoriesByTenantIdExcludingSoftDeleted() {
        // Given
        Category active = createCategory(tenantA, "Bebidas", null);
        Category deleted = createCategory(tenantA, "Postres", null);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        // When
        Page<Category> result = categoryRepository.findAllByTenantId(tenantA, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Bebidas");
    }

    @Test
    @DisplayName("Should enforce tenant isolation in findAll")
    void shouldEnforceTenantIsolationInFindAll() {
        // Given
        Category tenantACategory = createCategory(tenantA, "Bebidas", null);
        Category tenantBCategory = createCategory(tenantB, "Comidas", null);

        entityManager.persist(tenantACategory);
        entityManager.persist(tenantBCategory);
        entityManager.flush();

        // When
        Page<Category> tenantAResults = categoryRepository.findAllByTenantId(tenantA, PageRequest.of(0, 10));
        Page<Category> tenantBResults = categoryRepository.findAllByTenantId(tenantB, PageRequest.of(0, 10));

        // Then
        assertThat(tenantAResults.getContent()).hasSize(1);
        assertThat(tenantAResults.getContent().get(0).getName()).isEqualTo("Bebidas");

        assertThat(tenantBResults.getContent()).hasSize(1);
        assertThat(tenantBResults.getContent().get(0).getName()).isEqualTo("Comidas");
    }

    @Test
    void canSaveAndFindCategory() {
        // Given
        Category category = createCategory(tenantA, "Bebidas", null);
        entityManager.persist(category);
        entityManager.flush();

        // When
        Optional<Category> foundCategory = categoryRepository.findById(category.getId());

        // Then
        assertThat(foundCategory).isPresent();
        assertThat(foundCategory.get().getName()).isEqualTo("Bebidas");
        assertThat(foundCategory.get().getTenantId()).isEqualTo(tenantA);
    }

    @Test
    @DisplayName("Should find category by ID and tenant ID")
    void shouldFindCategoryByIdAndTenantId() {
        // Given
        Category category = createCategory(tenantA, "Bebidas", null);
        entityManager.persist(category);
        entityManager.flush();

        // When
        Optional<Category> result = categoryRepository.findByIdAndTenantId(category.getId(), tenantA);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Bebidas");
    }

    @Test
    @DisplayName("Should return empty when category belongs to different tenant")
    void shouldReturnEmptyWhenCategoryBelongsToDifferentTenant() {
        // Given
        Category category = createCategory(tenantA, "Bebidas", null);
        entityManager.persist(category);
        entityManager.flush();

        // When
        Optional<Category> result = categoryRepository.findByIdAndTenantId(category.getId(), tenantB);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when category is soft-deleted")
    void shouldReturnEmptyWhenCategoryIsSoftDeleted() {
        // Given
        Category category = createCategory(tenantA, "Bebidas", null);
        category.setDeletedAt(Instant.now());
        entityManager.persist(category);
        entityManager.flush();

        // When
        Optional<Category> result = categoryRepository.findByIdAndTenantId(category.getId(), tenantA);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should detect duplicate name in same parent level")
    void shouldDetectDuplicateNameInSameParentLevel() {
        // Given
        Category parent = createCategory(tenantA, "Bebidas", null);
        entityManager.persist(parent);
        entityManager.flush();

        Category existing = createCategory(tenantA, "Calientes", parent.getId());
        entityManager.persist(existing);
        entityManager.flush();

        // When
        boolean exists = categoryRepository.existsByNameAndTenantIdAndParentId(
                "Calientes",
                tenantA,
                parent.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should allow same name in different parent levels")
    void shouldAllowSameNameInDifferentParentLevels() {
        // Given
        Category parent1 = createCategory(tenantA, "Bebidas", null);
        Category parent2 = createCategory(tenantA, "Comidas", null);
        entityManager.persist(parent1);
        entityManager.persist(parent2);
        entityManager.flush();

        Category child1 = createCategory(tenantA, "Especiales", parent1.getId());
        entityManager.persist(child1);
        entityManager.flush();

        // When
        boolean exists = categoryRepository.existsByNameAndTenantIdAndParentId(
                "Especiales",
                tenantA,
                parent2.getId());

        // Then
        assertThat(exists).isFalse(); // Different parent, so name can be reused
    }

    @Test
    @DisplayName("Should allow same name for update of same category")
    void shouldAllowSameNameForUpdateOfSameCategory() {
        // Given
        Category category = createCategory(tenantA, "Bebidas", null);
        entityManager.persist(category);
        entityManager.flush();

        // When
        boolean exists = categoryRepository.existsByNameAndTenantIdAndParentIdAndIdNot(
                "Bebidas",
                tenantA,
                null,
                category.getId());

        // Then
        assertThat(exists).isFalse(); // Same category ID excluded, so no duplicate
    }

    @Test
    @DisplayName("Should detect duplicate name for different category")
    void shouldDetectDuplicateNameForDifferentCategory() {
        // Given
        Category existing = createCategory(tenantA, "Bebidas", null);
        Category other = createCategory(tenantA, "Comidas", null);
        entityManager.persist(existing);
        entityManager.persist(other);
        entityManager.flush();

        // When
        boolean exists = categoryRepository.existsByNameAndTenantIdAndParentIdAndIdNot(
                "Bebidas",
                tenantA,
                null,
                other.getId());

        // Then
        assertThat(exists).isTrue(); // Different category, so duplicate detected
    }

    @Test
    @DisplayName("Should count active products in category")
    void shouldCountActiveProductsInCategory() {
        // Given
        Category category = createCategory(tenantA, "Bebidas", null);
        entityManager.persist(category);
        entityManager.flush();

        Product activeProduct = createProduct(tenantA, category.getId(), "Coca Cola", true, false);
        Product inactiveProduct = createProduct(tenantA, category.getId(), "Pepsi", false, false);
        Product deletedProduct = createProduct(tenantA, category.getId(), "Fanta", true, true);

        entityManager.persist(activeProduct);
        entityManager.persist(inactiveProduct);
        entityManager.persist(deletedProduct);
        entityManager.flush();

        // When
        long count = categoryRepository.countActiveProductsByCategory(category.getId(), tenantA);

        // Then
        assertThat(count).isEqualTo(1); // Only active non-deleted product
    }

    @Test
    @DisplayName("Should return zero count when category has no active products")
    void shouldReturnZeroCountWhenCategoryHasNoActiveProducts() {
        // Given
        Category category = createCategory(tenantA, "Bebidas", null);
        entityManager.persist(category);
        entityManager.flush();

        // When
        long count = categoryRepository.countActiveProductsByCategory(category.getId(), tenantA);

        // Then
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should enforce tenant isolation in product count")
    void shouldEnforceTenantIsolationInProductCount() {
        // Given
        Category categoryA = createCategory(tenantA, "Bebidas", null);
        Category categoryB = createCategory(tenantB, "Bebidas", null);
        entityManager.persist(categoryA);
        entityManager.persist(categoryB);
        entityManager.flush();

        Product productB = createProduct(tenantB, categoryB.getId(), "Coca Cola", true, false);
        entityManager.persist(productB);
        entityManager.flush();

        // When - Try to count products in tenantA's category with tenantA filter
        long count = categoryRepository.countActiveProductsByCategory(categoryA.getId(), tenantA);

        // Then
        assertThat(count).isZero(); // No cross-tenant leakage
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private Category createCategory(UUID tenantId, String name, UUID parentId) {
        Category category = new Category();
        category.setTenantId(tenantId);
        category.setName(name);
        category.setParentId(parentId);
        category.setSortOrder(0);
        category.setActive(true);
        return category;
    }

    private Product createProduct(UUID tenantId, UUID categoryId, String name, boolean isActive, boolean isDeleted) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setBasePrice(new BigDecimal("10.00"));
        product.setProductType(ProductType.SIMPLE);
        product.setActive(isActive);
        product.setAvailable(true);
        if (isDeleted) {
            product.setDeletedAt(Instant.now());
        }
        return product;
    }
}
