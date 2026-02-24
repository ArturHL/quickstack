package com.quickstack.product.repository;

import com.quickstack.product.entity.Modifier;
import com.quickstack.product.entity.ModifierGroup;
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
 * Repository tests for ModifierRepository using Testcontainers.
 * <p>
 * These tests verify:
 * - Multi-tenant isolation
 * - Soft delete filtering
 * - Active status filtering
 * - Correct ordering by sort_order
 * - Active modifier counting for business rule validation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Modifier Repository")
class ModifierRepositoryTest {

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
    private ModifierRepository modifierRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;
    private UUID groupA;
    private UUID groupB;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Test Plan', 'TEST', 0, 1, 5)")
                .setParameter(1, planId).executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) " +
                        "VALUES (?, 'Tenant A', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantA)
                .setParameter(2, "tenant-a-" + tenantA)
                .setParameter(3, planId).executeUpdate();

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) " +
                        "VALUES (?, 'Tenant B', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantB)
                .setParameter(2, "tenant-b-" + tenantB)
                .setParameter(3, planId).executeUpdate();

        entityManager.flush();

        Product productA = createProduct(tenantA, "Hamburguesa");
        entityManager.persist(productA);
        entityManager.flush();

        Product productB = createProduct(tenantB, "Pizza");
        entityManager.persist(productB);
        entityManager.flush();

        ModifierGroup mgA = createGroup(tenantA, productA.getId(), "Extras");
        entityManager.persist(mgA);
        entityManager.flush();
        groupA = mgA.getId();

        ModifierGroup mgB = createGroup(tenantB, productB.getId(), "Extras B");
        entityManager.persist(mgB);
        entityManager.flush();
        groupB = mgB.getId();

        entityManager.clear();
    }

    @Test
    @DisplayName("Should find active non-deleted modifiers by group and tenant")
    void shouldFindActiveModifiersByGroupAndTenant() {
        // Given
        Modifier active = createModifier(tenantA, groupA, "Extra Queso", 0);
        Modifier deleted = createModifier(tenantA, groupA, "Extra Jalapeño", 1);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        // When
        List<Modifier> result = modifierRepository.findAllByModifierGroupIdAndTenantId(groupA, tenantA);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Extra Queso");
    }

    @Test
    @DisplayName("Should exclude inactive modifiers")
    void shouldExcludeInactiveModifiers() {
        // Given
        Modifier active = createModifier(tenantA, groupA, "Extra Queso", 0);
        Modifier inactive = createModifier(tenantA, groupA, "Extra Aguacate", 1);
        inactive.setActive(false);

        entityManager.persist(active);
        entityManager.persist(inactive);
        entityManager.flush();

        // When
        List<Modifier> result = modifierRepository.findAllByModifierGroupIdAndTenantId(groupA, tenantA);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Extra Queso");
    }

    @Test
    @DisplayName("Should return modifiers ordered by sort_order ascending")
    void shouldReturnModifiersOrderedBySortOrder() {
        // Given
        Modifier third = createModifier(tenantA, groupA, "Extra Aguacate", 2);
        Modifier first = createModifier(tenantA, groupA, "Extra Queso", 0);
        Modifier second = createModifier(tenantA, groupA, "Extra Jalapeño", 1);

        entityManager.persist(third);
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();

        // When
        List<Modifier> result = modifierRepository.findAllByModifierGroupIdAndTenantId(groupA, tenantA);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Extra Queso");
        assertThat(result.get(1).getName()).isEqualTo("Extra Jalapeño");
        assertThat(result.get(2).getName()).isEqualTo("Extra Aguacate");
    }

    @Test
    @DisplayName("Should find modifier by ID and tenant ID")
    void shouldFindModifierByIdAndTenantId() {
        // Given
        Modifier modifier = createModifier(tenantA, groupA, "Extra Queso", 0);
        entityManager.persist(modifier);
        entityManager.flush();

        // When
        Optional<Modifier> result = modifierRepository.findByIdAndTenantId(modifier.getId(), tenantA);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Extra Queso");
    }

    @Test
    @DisplayName("Should return empty when modifier is soft-deleted")
    void shouldReturnEmptyWhenModifierIsSoftDeleted() {
        // Given
        Modifier modifier = createModifier(tenantA, groupA, "Extra Queso", 0);
        modifier.setDeletedAt(Instant.now());
        entityManager.persist(modifier);
        entityManager.flush();

        // When
        Optional<Modifier> result = modifierRepository.findByIdAndTenantId(modifier.getId(), tenantA);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when modifier belongs to different tenant")
    void shouldReturnEmptyWhenModifierBelongsToDifferentTenant() {
        // Given
        Modifier modifier = createModifier(tenantA, groupA, "Extra Queso", 0);
        entityManager.persist(modifier);
        entityManager.flush();

        // When
        Optional<Modifier> result = modifierRepository.findByIdAndTenantId(modifier.getId(), tenantB);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should detect existing name in same group and tenant")
    void shouldDetectExistingNameInSameGroupAndTenant() {
        // Given
        Modifier modifier = createModifier(tenantA, groupA, "Extra Queso", 0);
        entityManager.persist(modifier);
        entityManager.flush();

        // When
        boolean exists = modifierRepository.existsByNameAndModifierGroupIdAndTenantId("Extra Queso", groupA, tenantA);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-existing modifier name")
    void shouldReturnFalseForNonExistingModifierName() {
        // When
        boolean exists = modifierRepository.existsByNameAndModifierGroupIdAndTenantId("Extra Queso", groupA, tenantA);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should count only active non-deleted modifiers in group")
    void shouldCountOnlyActiveNonDeletedModifiers() {
        // Given
        Modifier active = createModifier(tenantA, groupA, "Extra Queso", 0);
        Modifier inactive = createModifier(tenantA, groupA, "Extra Aguacate", 1);
        inactive.setActive(false);
        Modifier deleted = createModifier(tenantA, groupA, "Extra Jalapeño", 2);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(inactive);
        entityManager.persist(deleted);
        entityManager.flush();

        // When
        long count = modifierRepository.countActiveByModifierGroupId(groupA, tenantA);

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should enforce tenant isolation in countActiveByModifierGroupId")
    void shouldEnforceTenantIsolationInCount() {
        // Given
        Modifier modifierB = createModifier(tenantB, groupB, "Extra Queso B", 0);
        entityManager.persist(modifierB);
        entityManager.flush();

        // When - Count using tenantA's groupA (should be 0, not tenantB's modifiers)
        long count = modifierRepository.countActiveByModifierGroupId(groupA, tenantA);

        // Then
        assertThat(count).isZero();
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private Product createProduct(UUID tenantId, String name) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setName(name);
        product.setBasePrice(new BigDecimal("50.00"));
        product.setProductType(ProductType.SIMPLE);
        product.setActive(true);
        product.setAvailable(true);
        return product;
    }

    private ModifierGroup createGroup(UUID tenantId, UUID productId, String name) {
        ModifierGroup group = new ModifierGroup();
        group.setTenantId(tenantId);
        group.setProductId(productId);
        group.setName(name);
        return group;
    }

    private Modifier createModifier(UUID tenantId, UUID modifierGroupId, String name, int sortOrder) {
        Modifier modifier = new Modifier();
        modifier.setTenantId(tenantId);
        modifier.setModifierGroupId(modifierGroupId);
        modifier.setName(name);
        modifier.setPriceAdjustment(BigDecimal.ZERO);
        modifier.setSortOrder(sortOrder);
        return modifier;
    }
}
