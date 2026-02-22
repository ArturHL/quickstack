package com.quickstack.product.repository;

import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.entity.ProductVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Variant Repository")
class VariantRepositoryTest {

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
    private VariantRepository variantRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        // Create a test plan and tenant using native SQL as entities might not be available in this module
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
            "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
            "VALUES (?, 'Test Plan', 'TEST', 0, 1, 5)"
        ).setParameter(1, planId).executeUpdate();

        tenantId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
            "INSERT INTO tenants (id, name, slug, plan_id, status) " +
            "VALUES (?, 'Tenant A', 'tenant-a', ?, 'ACTIVE')"
        ).setParameter(1, tenantId).setParameter(2, planId).executeUpdate();

        entityManager.flush();

        Category category = new Category();
        category.setTenantId(tenantId);
        category.setName("Category");
        category.setSortOrder(0);
        category.setActive(true);
        entityManager.persist(category);
        
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setCategoryId(category.getId());
        product.setName("Product");
        product.setBasePrice(new BigDecimal("10.00"));
        product.setProductType(ProductType.VARIANT);
        product.setActive(true);
        product.setAvailable(true);
        entityManager.persist(product);
        
        entityManager.flush();
        productId = product.getId();
    }

    @Test
    void shouldFindAllByProductIdAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc() {
        createVariant("Chico", "SKU-CH", 1, false);
        createVariant("Grande", "SKU-GR", 3, false);
        createVariant("Mediano", "SKU-ME", 2, true);
        
        ProductVariant deleted = createVariant("Borrado", "SKU-DEL", 4, false);
        deleted.softDelete();
        variantRepository.save(deleted);

        List<ProductVariant> variants = variantRepository.findAllByProductIdAndTenantIdAndDeletedAtIsNullOrderBySortOrderAsc(productId, tenantId);

        assertThat(variants).hasSize(3);
        assertThat(variants.get(0).getName()).isEqualTo("Chico");
        assertThat(variants.get(1).getName()).isEqualTo("Mediano");
        assertThat(variants.get(2).getName()).isEqualTo("Grande");
    }

    @Test
    void shouldFindByIdAndProductIdAndTenantId() {
        ProductVariant v1 = createVariant("Unica", "SKU-U", 1, true);

        Optional<ProductVariant> found = variantRepository.findByIdAndProductIdAndTenantId(v1.getId(), productId, tenantId);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Unica");
    }

    @Test
    void shouldCountActiveVariants() {
        createVariant("V1", "SKU-1", 1, true);
        createVariant("V2", "SKU-2", 2, false);
        
        ProductVariant deleted = createVariant("V3", "SKU-3", 3, false);
        deleted.softDelete();
        variantRepository.save(deleted);

        long count = variantRepository.countByProductIdAndTenantIdAndDeletedAtIsNull(productId, tenantId);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCheckIfSkuExistsAndTenantId() {
        createVariant("V1", "EXISTING-SKU", 1, true);

        assertThat(variantRepository.existsBySkuAndTenantId("EXISTING-SKU", tenantId)).isTrue();
        assertThat(variantRepository.existsBySkuAndTenantId("NON-EXISTING", tenantId)).isFalse();
    }

    @Test
    void shouldCheckIfNameExistsExcludingId() {
        ProductVariant v1 = createVariant("DuplicateName", "SKU-1", 1, true);
        ProductVariant v2 = createVariant("OtherName", "SKU-2", 2, false);

        assertThat(variantRepository.existsByNameAndProductIdAndTenantIdAndIdNot("DuplicateName", productId, tenantId, v2.getId())).isTrue();
        assertThat(variantRepository.existsByNameAndProductIdAndTenantIdAndIdNot("DuplicateName", productId, tenantId, v1.getId())).isFalse();
    }

    private ProductVariant createVariant(String name, String sku, int sortOrder, boolean isDefault) {
        ProductVariant variant = new ProductVariant();
        variant.setTenantId(tenantId);
        variant.setProductId(productId);
        variant.setName(name);
        variant.setSku(sku);
        variant.setPriceAdjustment(BigDecimal.ZERO);
        variant.setSortOrder(sortOrder);
        variant.setDefault(isDefault);
        variant.setActive(true);
        return variantRepository.save(variant);
    }
}
