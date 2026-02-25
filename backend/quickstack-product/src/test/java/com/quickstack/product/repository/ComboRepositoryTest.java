package com.quickstack.product.repository;

import com.quickstack.product.entity.Combo;
import com.quickstack.product.entity.ComboItem;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
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

/**
 * Repository tests for ComboRepository and ComboItemRepository using
 * Testcontainers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Combo Repository")
class ComboRepositoryTest {

    @SuppressWarnings("resource")
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
    private ComboRepository comboRepository;

    @Autowired
    private ComboItemRepository comboItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;
    private UUID productId;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) "
                        +
                        "VALUES (?, 'Test Plan', 'TEST', 0, 1, 5)")
                .setParameter(1, planId).executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant A', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantA)
                .setParameter(2, "tenant-a-" + tenantA)
                .setParameter(3, planId).executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant B', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantB)
                .setParameter(2, "tenant-b-" + tenantB)
                .setParameter(3, planId).executeUpdate();

        entityManager.flush();

        Product product = new Product();
        product.setTenantId(tenantA);
        product.setName("Hamburguesa");
        product.setBasePrice(new BigDecimal("50.00"));
        product.setProductType(ProductType.SIMPLE);
        product.setActive(true);
        product.setAvailable(true);
        entityManager.persist(product);
        entityManager.flush();
        productId = product.getId();

        entityManager.clear();
    }

    // -------------------------------------------------------------------------
    // ComboRepository tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAllByTenantId returns only non-deleted combos ordered by sort_order")
    void findAllByTenantIdExcludesSoftDeleted() {
        Combo active1 = buildCombo(tenantA, "Combo 1", 0);
        Combo active2 = buildCombo(tenantA, "Combo 2", 1);
        Combo deleted = buildCombo(tenantA, "Combo Deleted", 2);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active1);
        entityManager.persist(active2);
        entityManager.persist(deleted);
        entityManager.flush();

        List<Combo> result = comboRepository.findAllByTenantId(tenantA);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Combo 1");
        assertThat(result.get(1).getName()).isEqualTo("Combo 2");
    }

    @Test
    @DisplayName("findAllByTenantId enforces tenant isolation")
    void findAllByTenantIdEnforcesTenantIsolation() {
        entityManager.persist(buildCombo(tenantA, "Combo A", 0));
        entityManager.persist(buildCombo(tenantB, "Combo B", 0));
        entityManager.flush();

        List<Combo> resultsA = comboRepository.findAllByTenantId(tenantA);
        List<Combo> resultsB = comboRepository.findAllByTenantId(tenantB);

        assertThat(resultsA).hasSize(1);
        assertThat(resultsA.get(0).getName()).isEqualTo("Combo A");
        assertThat(resultsB).hasSize(1);
        assertThat(resultsB.get(0).getName()).isEqualTo("Combo B");
    }

    @Test
    @DisplayName("findByIdAndTenantId returns combo when found")
    void findByIdAndTenantIdReturnsCombo() {
        Combo combo = buildCombo(tenantA, "Combo 1", 0);
        entityManager.persist(combo);
        entityManager.flush();

        Optional<Combo> result = comboRepository.findByIdAndTenantId(combo.getId(), tenantA);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Combo 1");
    }

    @Test
    @DisplayName("findByIdAndTenantId returns empty when soft-deleted")
    void findByIdAndTenantIdReturnsEmptyWhenDeleted() {
        Combo combo = buildCombo(tenantA, "Combo 1", 0);
        combo.setDeletedAt(Instant.now());
        entityManager.persist(combo);
        entityManager.flush();

        Optional<Combo> result = comboRepository.findByIdAndTenantId(combo.getId(), tenantA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndTenantId enforces tenant isolation (returns empty for wrong tenant)")
    void findByIdAndTenantIdReturnEmptyForWrongTenant() {
        Combo combo = buildCombo(tenantA, "Combo 1", 0);
        entityManager.persist(combo);
        entityManager.flush();

        Optional<Combo> result = comboRepository.findByIdAndTenantId(combo.getId(), tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByNameAndTenantId returns true when combo with name exists")
    void existsByNameAndTenantIdReturnsTrueWhenExists() {
        entityManager.persist(buildCombo(tenantA, "Combo 1", 0));
        entityManager.flush();

        assertThat(comboRepository.existsByNameAndTenantId("Combo 1", tenantA)).isTrue();
    }

    @Test
    @DisplayName("existsByNameAndTenantIdAndIdNot returns false when only own entity has the name")
    void existsByNameAndIdNotReturnsFalseForOwnEntity() {
        Combo combo = buildCombo(tenantA, "Combo 1", 0);
        entityManager.persist(combo);
        entityManager.flush();

        assertThat(comboRepository.existsByNameAndTenantIdAndIdNot("Combo 1", tenantA, combo.getId())).isFalse();
    }

    // -------------------------------------------------------------------------
    // ComboItemRepository tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findAllByComboIdAndTenantId returns items ordered by sort_order")
    void findAllByComboIdAndTenantIdOrderedBySortOrder() {
        Combo combo = buildCombo(tenantA, "Combo 1", 0);
        entityManager.persist(combo);
        entityManager.flush();

        entityManager.persist(buildItem(combo.getId(), tenantA, productId, 1));
        entityManager.flush();

        List<ComboItem> items = comboItemRepository.findAllByComboIdAndTenantId(combo.getId(), tenantA);

        assertThat(items).hasSize(1);
    }

    @Test
    @DisplayName("deleteAllByComboIdAndTenantId removes all items for a combo")
    void deleteAllByComboIdAndTenantIdRemovesItems() {
        Combo combo = buildCombo(tenantA, "Combo 1", 0);
        entityManager.persist(combo);
        entityManager.flush();

        entityManager.persist(buildItem(combo.getId(), tenantA, productId, 0));
        entityManager.flush();
        entityManager.clear();

        comboItemRepository.deleteAllByComboIdAndTenantId(combo.getId(), tenantA);

        List<ComboItem> remaining = comboItemRepository.findAllByComboIdAndTenantId(combo.getId(), tenantA);
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("findAllByTenantIdAndComboIdIn batch loads items for multiple combos")
    void findAllByTenantIdAndComboIdInBatchLoads() {
        Combo combo1 = buildCombo(tenantA, "Combo 1", 0);
        Combo combo2 = buildCombo(tenantA, "Combo 2", 1);
        entityManager.persist(combo1);
        entityManager.persist(combo2);
        entityManager.flush();

        entityManager.persist(buildItem(combo1.getId(), tenantA, productId, 0));
        entityManager.persist(buildItem(combo2.getId(), tenantA, productId, 0));
        entityManager.flush();

        List<ComboItem> items = comboItemRepository.findAllByTenantIdAndComboIdIn(
                tenantA, List.of(combo1.getId(), combo2.getId()));

        assertThat(items).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Combo buildCombo(UUID tenantId, String name, int sortOrder) {
        Combo combo = new Combo();
        combo.setTenantId(tenantId);
        combo.setName(name);
        combo.setPrice(new BigDecimal("99.00"));
        combo.setActive(true);
        combo.setSortOrder(sortOrder);
        return combo;
    }

    private ComboItem buildItem(UUID comboId, UUID tenantId, UUID itemProductId, int sortOrder) {
        ComboItem item = new ComboItem();
        item.setTenantId(tenantId);
        item.setComboId(comboId);
        item.setProductId(itemProductId);
        item.setQuantity(1);
        item.setSortOrder(sortOrder);
        return item;
    }
}
