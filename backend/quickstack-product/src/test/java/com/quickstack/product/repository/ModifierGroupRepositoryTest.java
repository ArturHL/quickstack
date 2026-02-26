package com.quickstack.product.repository;

import com.quickstack.product.entity.ModifierGroup;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for ModifierGroupRepository using Testcontainers.
 * <p>
 * These tests verify:
 * - Multi-tenant isolation
 * - Soft delete filtering
 * - Correct ordering by sort_order
 * - Uniqueness constraint helpers
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("ModifierGroup Repository")
class ModifierGroupRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;
    private UUID productA;
    private UUID productB;

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

        Product pA = createProduct(tenantA, "Hamburguesa");
        entityManager.persist(pA);
        entityManager.flush();
        productA = pA.getId();

        Product pB = createProduct(tenantB, "Pizza");
        entityManager.persist(pB);
        entityManager.flush();
        productB = pB.getId();

        entityManager.clear();
    }

    @Test
    @DisplayName("Should find modifier groups by product and tenant excluding soft-deleted")
    void shouldFindModifierGroupsByProductAndTenantExcludingSoftDeleted() {
        // Given
        ModifierGroup active = createGroup(tenantA, productA, "Extras", 0);
        ModifierGroup deleted = createGroup(tenantA, productA, "Sin ingredientes", 1);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        // When
        List<ModifierGroup> result = modifierGroupRepository.findAllByProductIdAndTenantId(productA, tenantA);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Extras");
    }

    @Test
    @DisplayName("Should enforce tenant isolation in findAllByProductIdAndTenantId")
    void shouldEnforceTenantIsolationInFindAll() {
        // Given
        ModifierGroup groupA = createGroup(tenantA, productA, "Extras A", 0);
        ModifierGroup groupB = createGroup(tenantB, productB, "Extras B", 0);

        entityManager.persist(groupA);
        entityManager.persist(groupB);
        entityManager.flush();

        // When
        List<ModifierGroup> resultsA = modifierGroupRepository.findAllByProductIdAndTenantId(productA, tenantA);
        List<ModifierGroup> resultsB = modifierGroupRepository.findAllByProductIdAndTenantId(productB, tenantB);

        // Then
        assertThat(resultsA).hasSize(1);
        assertThat(resultsA.get(0).getName()).isEqualTo("Extras A");

        assertThat(resultsB).hasSize(1);
        assertThat(resultsB.get(0).getName()).isEqualTo("Extras B");
    }

    @Test
    @DisplayName("Should return modifier groups ordered by sort_order ascending")
    void shouldReturnModifierGroupsOrderedBySortOrder() {
        // Given
        ModifierGroup third = createGroup(tenantA, productA, "Tamaño", 2);
        ModifierGroup first = createGroup(tenantA, productA, "Extras", 0);
        ModifierGroup second = createGroup(tenantA, productA, "Sin ingredientes", 1);

        entityManager.persist(third);
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();

        // When
        List<ModifierGroup> result = modifierGroupRepository.findAllByProductIdAndTenantId(productA, tenantA);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Extras");
        assertThat(result.get(1).getName()).isEqualTo("Sin ingredientes");
        assertThat(result.get(2).getName()).isEqualTo("Tamaño");
    }

    @Test
    @DisplayName("Should find modifier group by ID and tenant ID")
    void shouldFindModifierGroupByIdAndTenantId() {
        // Given
        ModifierGroup group = createGroup(tenantA, productA, "Extras", 0);
        entityManager.persist(group);
        entityManager.flush();

        // When
        Optional<ModifierGroup> result = modifierGroupRepository.findByIdAndTenantId(group.getId(), tenantA);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Extras");
    }

    @Test
    @DisplayName("Should return empty when modifier group is soft-deleted")
    void shouldReturnEmptyWhenModifierGroupIsSoftDeleted() {
        // Given
        ModifierGroup group = createGroup(tenantA, productA, "Extras", 0);
        group.setDeletedAt(Instant.now());
        entityManager.persist(group);
        entityManager.flush();

        // When
        Optional<ModifierGroup> result = modifierGroupRepository.findByIdAndTenantId(group.getId(), tenantA);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when modifier group belongs to different tenant")
    void shouldReturnEmptyWhenModifierGroupBelongsToDifferentTenant() {
        // Given
        ModifierGroup group = createGroup(tenantA, productA, "Extras", 0);
        entityManager.persist(group);
        entityManager.flush();

        // When
        Optional<ModifierGroup> result = modifierGroupRepository.findByIdAndTenantId(group.getId(), tenantB);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should detect existing name in same product and tenant")
    void shouldDetectExistingNameInSameProductAndTenant() {
        // Given
        ModifierGroup group = createGroup(tenantA, productA, "Extras", 0);
        entityManager.persist(group);
        entityManager.flush();

        // When
        boolean exists = modifierGroupRepository.existsByNameAndProductIdAndTenantId("Extras", productA, tenantA);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-existing name")
    void shouldReturnFalseForNonExistingName() {
        // When
        boolean exists = modifierGroupRepository.existsByNameAndProductIdAndTenantId("Extras", productA, tenantA);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false for existsByNameAndIdNot when checking the same entity")
    void shouldReturnFalseForExistsByNameAndIdNotWhenCheckingSameEntity() {
        // Given
        ModifierGroup group = createGroup(tenantA, productA, "Extras", 0);
        entityManager.persist(group);
        entityManager.flush();

        // When
        boolean exists = modifierGroupRepository.existsByNameAndProductIdAndTenantIdAndIdNot(
                "Extras", productA, tenantA, group.getId());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return true for existsByNameAndIdNot when another entity has same name")
    void shouldReturnTrueForExistsByNameAndIdNotWhenAnotherEntityHasSameName() {
        // Given
        ModifierGroup group1 = createGroup(tenantA, productA, "Extras", 0);
        ModifierGroup group2 = createGroup(tenantA, productA, "Extras copia", 1);

        entityManager.persist(group1);
        entityManager.persist(group2);
        entityManager.flush();

        // When - Try to rename group2 to "Extras" (which belongs to group1)
        boolean exists = modifierGroupRepository.existsByNameAndProductIdAndTenantIdAndIdNot(
                "Extras", productA, tenantA, group2.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return empty list when product has no modifier groups")
    void shouldReturnEmptyListWhenProductHasNoModifierGroups() {
        // When
        List<ModifierGroup> result = modifierGroupRepository.findAllByProductIdAndTenantId(productA, tenantA);

        // Then
        assertThat(result).isEmpty();
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

    private ModifierGroup createGroup(UUID tenantId, UUID productId, String name, int sortOrder) {
        ModifierGroup group = new ModifierGroup();
        group.setTenantId(tenantId);
        group.setProductId(productId);
        group.setName(name);
        group.setSortOrder(sortOrder);
        return group;
    }
}
