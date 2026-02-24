package com.quickstack.product.repository;

import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for ProductRepository using Testcontainers.
 * <p>
 * These tests verify:
 * - Multi-tenant isolation
 * - Soft delete filtering
 * - Search and filtering capabilities
 * - SKU and name uniqueness validation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Product Repository")
class ProductRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("quickstack_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        // Create a test plan and tenant using native SQL
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) "
                        +
                        "VALUES (?, 'Test Plan', 'TEST-PRODUCT', 0, 1, 5)")
                .setParameter(1, planId).executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) " +
                        "VALUES (?, 'Tenant A Product', 'tenant-a-prod', ?, 'ACTIVE')")
                .setParameter(1, tenantA).setParameter(2, planId).executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) " +
                        "VALUES (?, 'Tenant B Product', 'tenant-b-prod', ?, 'ACTIVE')")
                .setParameter(1, tenantB).setParameter(2, planId).executeUpdate();

        // Create a category for products
        Category category = new Category();
        category.setTenantId(tenantA);
        category.setName("Bebidas");
        entityManager.persist(category);
        entityManager.flush();
        categoryId = category.getId();

        entityManager.clear();
    }

    @Test
    @DisplayName("Should find active and available products only")
    void shouldFindActiveAndAvailableProductsOnly() {
        // Given
        Product active = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        Product inactive = createProduct(tenantA, categoryId, "Pepsi", false, true, false);
        Product unavailable = createProduct(tenantA, categoryId, "Fanta", true, false, false);
        Product deleted = createProduct(tenantA, categoryId, "Sprite", true, true, true);

        entityManager.persist(active);
        entityManager.persist(inactive);
        entityManager.persist(unavailable);
        entityManager.persist(deleted);
        entityManager.flush();

        // When
        Page<Product> result = productRepository.findAllByTenantId(tenantA, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Coca Cola");
    }

    @Test
    @DisplayName("Should enforce tenant isolation")
    void shouldEnforceTenantIsolation() {
        // Given
        Product productA = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);

        // Create category for tenantB
        Category categoryB = new Category();
        categoryB.setTenantId(tenantB);
        categoryB.setName("Bebidas");
        entityManager.persist(categoryB);
        entityManager.flush();

        Product productB = createProduct(tenantB, categoryB.getId(), "Pepsi", true, true, false);

        entityManager.persist(productA);
        entityManager.persist(productB);
        entityManager.flush();

        // When
        Page<Product> tenantAResults = productRepository.findAllByTenantId(tenantA, PageRequest.of(0, 10));
        Page<Product> tenantBResults = productRepository.findAllByTenantId(tenantB, PageRequest.of(0, 10));

        // Then
        assertThat(tenantAResults.getContent()).hasSize(1);
        assertThat(tenantAResults.getContent().get(0).getName()).isEqualTo("Coca Cola");

        assertThat(tenantBResults.getContent()).hasSize(1);
        assertThat(tenantBResults.getContent().get(0).getName()).isEqualTo("Pepsi");
    }

    @DisplayName("Should find product by ID and tenant ID")
    @SuppressWarnings("resource")
    @Test
    void canSaveAndFindProduct() {
        // Given
        Product product = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        entityManager.persist(product);
        entityManager.flush();

        // When
        Optional<Product> result = productRepository.findByIdAndTenantId(product.getId(), tenantA);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Coca Cola");
    }

    @Test
    @DisplayName("Should return empty when product belongs to different tenant")
    void shouldReturnEmptyWhenProductBelongsToDifferentTenant() {
        // Given
        Product product = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        entityManager.persist(product);
        entityManager.flush();

        // When
        Optional<Product> result = productRepository.findByIdAndTenantId(product.getId(), tenantB);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should filter by category ID")
    void shouldFilterByCategoryId() {
        // Given
        Category category2 = new Category();
        category2.setTenantId(tenantA);
        category2.setName("Comidas");
        entityManager.persist(category2);
        entityManager.flush();

        Product product1 = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        Product product2 = createProduct(tenantA, category2.getId(), "Hamburguesa", true, true, false);

        entityManager.persist(product1);
        entityManager.persist(product2);
        entityManager.flush();

        // When
        Page<Product> result = productRepository.findAllByTenantIdWithFilters(
                tenantA, categoryId, null, null, null, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Coca Cola");
    }

    @Test
    @DisplayName("Should filter by availability")
    void shouldFilterByAvailability() {
        // Given
        Product available = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        Product unavailable = createProduct(tenantA, categoryId, "Pepsi", true, false, false);

        entityManager.persist(available);
        entityManager.persist(unavailable);
        entityManager.flush();

        // When
        Page<Product> availableResults = productRepository.findAllByTenantIdWithFilters(
                tenantA, null, null, true, null, PageRequest.of(0, 10));
        Page<Product> unavailableResults = productRepository.findAllByTenantIdWithFilters(
                tenantA, null, null, false, null, PageRequest.of(0, 10));

        // Then
        assertThat(availableResults.getContent()).hasSize(1);
        assertThat(availableResults.getContent().get(0).getName()).isEqualTo("Coca Cola");

        assertThat(unavailableResults.getContent()).hasSize(1);
        assertThat(unavailableResults.getContent().get(0).getName()).isEqualTo("Pepsi");
    }

    @Test
    @DisplayName("Should filter by active status")
    void shouldFilterByActiveStatus() {
        // Given
        Product active = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        Product inactive = createProduct(tenantA, categoryId, "Pepsi", false, true, false);

        entityManager.persist(active);
        entityManager.persist(inactive);
        entityManager.flush();

        // When
        Page<Product> activeResults = productRepository.findAllByTenantIdWithFilters(
                tenantA, null, true, null, null, PageRequest.of(0, 10));
        Page<Product> inactiveResults = productRepository.findAllByTenantIdWithFilters(
                tenantA, null, false, null, null, PageRequest.of(0, 10));

        // Then
        assertThat(activeResults.getContent()).hasSize(1);
        assertThat(activeResults.getContent().get(0).getName()).isEqualTo("Coca Cola");

        assertThat(inactiveResults.getContent()).hasSize(1);
        assertThat(inactiveResults.getContent().get(0).getName()).isEqualTo("Pepsi");
    }

    @Test
    @DisplayName("Should search by name case-insensitive")
    void shouldSearchByNameCaseInsensitive() {
        // Given
        Product taco = createProduct(tenantA, categoryId, "Taco al Pastor", true, true, false);
        Product quesadilla = createProduct(tenantA, categoryId, "Quesadilla de Pastor", true, true, false);
        Product burrito = createProduct(tenantA, categoryId, "Burrito de Pollo", true, true, false);

        entityManager.persist(taco);
        entityManager.persist(quesadilla);
        entityManager.persist(burrito);
        entityManager.flush();

        // When
        Page<Product> result = productRepository.findAllByTenantIdWithFilters(
                tenantA, null, null, null, "pastor", PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Taco al Pastor", "Quesadilla de Pastor");
    }

    @Test
    @DisplayName("Should combine multiple filters")
    void shouldCombineMultipleFilters() {
        // Given
        Category category2 = new Category();
        category2.setTenantId(tenantA);
        category2.setName("Comidas");
        entityManager.persist(category2);
        entityManager.flush();

        Product match = createProduct(tenantA, categoryId, "Taco al Pastor", true, true, false);
        Product wrongCategory = createProduct(tenantA, category2.getId(), "Taco de Pollo", true, true, false);
        Product unavailable = createProduct(tenantA, categoryId, "Taco de Asada", true, false, false);

        entityManager.persist(match);
        entityManager.persist(wrongCategory);
        entityManager.persist(unavailable);
        entityManager.flush();

        // When
        Page<Product> result = productRepository.findAllByTenantIdWithFilters(
                tenantA, categoryId, null, true, "taco", PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Taco al Pastor");
    }

    @Test
    @DisplayName("Should detect duplicate SKU in tenant")
    void shouldDetectDuplicateSkuInTenant() {
        // Given
        Product existing = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        existing.setSku("COCA-001");
        entityManager.persist(existing);
        entityManager.flush();

        // When
        boolean exists = productRepository.existsBySkuAndTenantId("COCA-001", tenantA);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should allow same SKU in different tenants")
    void shouldAllowSameSkuInDifferentTenants() {
        // Given
        Product productA = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        productA.setSku("COCA-001");
        entityManager.persist(productA);
        entityManager.flush();

        // When
        boolean exists = productRepository.existsBySkuAndTenantId("COCA-001", tenantB);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should allow same SKU for update of same product")
    void shouldAllowSameSkuForUpdateOfSameProduct() {
        // Given
        Product product = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        product.setSku("COCA-001");
        entityManager.persist(product);
        entityManager.flush();

        // When
        boolean exists = productRepository.existsBySkuAndTenantIdAndIdNot(
                "COCA-001", tenantA, product.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should detect duplicate name in same category")
    void shouldDetectDuplicateNameInSameCategory() {
        // Given
        Product existing = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        entityManager.persist(existing);
        entityManager.flush();

        // When
        boolean exists = productRepository.existsByNameAndTenantIdAndCategoryId(
                "Coca Cola", tenantA, categoryId);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should allow same name in different categories")
    void shouldAllowSameNameInDifferentCategories() {
        // Given
        Category category2 = new Category();
        category2.setTenantId(tenantA);
        category2.setName("Comidas");
        entityManager.persist(category2);
        entityManager.flush();

        Product product1 = createProduct(tenantA, categoryId, "Especial", true, true, false);
        entityManager.persist(product1);
        entityManager.flush();

        // When
        boolean exists = productRepository.existsByNameAndTenantIdAndCategoryId(
                "Especial", tenantA, category2.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should allow same name for update of same product")
    void shouldAllowSameNameForUpdateOfSameProduct() {
        // Given
        Product product = createProduct(tenantA, categoryId, "Coca Cola", true, true, false);
        entityManager.persist(product);
        entityManager.flush();

        // When
        boolean exists = productRepository.existsByNameAndTenantIdAndCategoryIdAndIdNot(
                "Coca Cola", tenantA, categoryId, product.getId());

        // Then
        assertThat(exists).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private Product createProduct(
            UUID tenantId,
            UUID categoryId,
            String name,
            boolean isActive,
            boolean isAvailable,
            boolean isDeleted) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setBasePrice(new BigDecimal("10.00"));
        product.setProductType(ProductType.SIMPLE);
        product.setActive(isActive);
        product.setAvailable(isAvailable);
        if (isDeleted) {
            product.setDeletedAt(Instant.now());
        }
        return product;
    }
}
