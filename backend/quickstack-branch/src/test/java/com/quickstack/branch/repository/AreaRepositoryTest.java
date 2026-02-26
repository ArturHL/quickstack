package com.quickstack.branch.repository;

import com.quickstack.branch.entity.Area;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.quickstack.branch.AbstractRepositoryTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for AreaRepository using Testcontainers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Area Repository")
class AreaRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private AreaRepository areaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID branchA;

    @BeforeEach
    void setUp() {
        UUID planId = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO subscription_plans (id, name, code, price_monthly_mxn, max_branches, max_users_per_branch) " +
                        "VALUES (?, 'Test Plan', 'TEST', 0, 10, 5)")
                .setParameter(1, planId).executeUpdate();

        tenantA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant A', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantA).setParameter(2, "ta-" + tenantA).setParameter(3, planId).executeUpdate();

        branchA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO branches (id, tenant_id, name, code, is_active, settings) VALUES (?, ?, 'Branch A', 'BA', true, '{}')")
                .setParameter(1, branchA).setParameter(2, tenantA).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. findAllByBranchIdAndTenantId excludes soft-deleted areas")
    void findAllExcludesSoftDeleted() {
        Area active = createArea(tenantA, branchA, "Terraza", 0);
        Area deleted = createArea(tenantA, branchA, "Interior", 1);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<Area> result = areaRepository.findAllByBranchIdAndTenantId(branchA, tenantA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Terraza");
    }

    @Test
    @DisplayName("2. findAllByBranchIdAndTenantId returns areas ordered by sort_order")
    void findAllReturnsOrderedBySortOrder() {
        entityManager.persist(createArea(tenantA, branchA, "Barra", 2));
        entityManager.persist(createArea(tenantA, branchA, "Interior", 1));
        entityManager.persist(createArea(tenantA, branchA, "Terraza", 0));
        entityManager.flush();

        List<Area> result = areaRepository.findAllByBranchIdAndTenantId(branchA, tenantA);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Area::getName)
                .containsExactly("Terraza", "Interior", "Barra");
    }

    @Test
    @DisplayName("3. findByIdAndTenantId returns empty for cross-tenant access (IDOR protection)")
    void findByIdAndTenantIdReturnEmptyForCrossTenant() {
        UUID tenantB = UUID.randomUUID();
        Area area = createArea(tenantA, branchA, "Terraza", 0);
        entityManager.persist(area);
        entityManager.flush();

        Optional<Area> result = areaRepository.findByIdAndTenantId(area.getId(), tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("4. existsByNameAndBranchIdAndTenantId detects duplicate names within branch")
    void existsByNameDetectsDuplicates() {
        entityManager.persist(createArea(tenantA, branchA, "Terraza", 0));
        entityManager.flush();

        assertThat(areaRepository.existsByNameAndBranchIdAndTenantId("Terraza", branchA, tenantA)).isTrue();
        assertThat(areaRepository.existsByNameAndBranchIdAndTenantId("Interior", branchA, tenantA)).isFalse();
    }

    @Test
    @DisplayName("5. existsByNameAndBranchIdAndTenantIdAndIdNot allows same name for same area (update)")
    void existsByNameAndIdNotAllowsSameAreaUpdate() {
        Area area = createArea(tenantA, branchA, "Terraza", 0);
        entityManager.persist(area);
        entityManager.flush();

        assertThat(areaRepository.existsByNameAndBranchIdAndTenantIdAndIdNot(
                "Terraza", branchA, tenantA, area.getId())).isFalse();

        Area other = createArea(tenantA, branchA, "Interior", 1);
        entityManager.persist(other);
        entityManager.flush();
        assertThat(areaRepository.existsByNameAndBranchIdAndTenantIdAndIdNot(
                "Terraza", branchA, tenantA, other.getId())).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Area createArea(UUID tenantId, UUID branchId, String name, int sortOrder) {
        Area area = new Area();
        area.setTenantId(tenantId);
        area.setBranchId(branchId);
        area.setName(name);
        area.setSortOrder(sortOrder);
        area.setActive(true);
        return area;
    }
}
