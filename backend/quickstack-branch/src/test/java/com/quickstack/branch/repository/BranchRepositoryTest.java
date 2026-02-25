package com.quickstack.branch.repository;

import com.quickstack.branch.entity.Branch;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for BranchRepository using Testcontainers.
 * <p>
 * Verifies:
 * - Multi-tenant isolation
 * - Soft delete filtering
 * - Name and code uniqueness constraints
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Branch Repository")
class BranchRepositoryTest {

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
    private BranchRepository branchRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID tenantB;

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

        tenantB = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO tenants (id, name, slug, plan_id, status) VALUES (?, 'Tenant B', ?, ?, 'ACTIVE')")
                .setParameter(1, tenantB).setParameter(2, "tb-" + tenantB).setParameter(3, planId).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. Save and find branch by ID")
    void canSaveAndFindBranch() {
        Branch branch = createBranch(tenantA, "Sucursal Centro", "CENTRO");
        entityManager.persist(branch);
        entityManager.flush();

        Optional<Branch> found = branchRepository.findById(branch.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Sucursal Centro");
        assertThat(found.get().getTenantId()).isEqualTo(tenantA);
    }

    @Test
    @DisplayName("2. findAllByTenantId excludes soft-deleted branches")
    void findAllExcludesSoftDeleted() {
        Branch active = createBranch(tenantA, "Activa", "ACT");
        Branch deleted = createBranch(tenantA, "Eliminada", "ELI");
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<Branch> result = branchRepository.findAllByTenantId(tenantA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Activa");
    }

    @Test
    @DisplayName("3. findAllByTenantId enforces tenant isolation")
    void findAllEnforcesTenantIsolation() {
        entityManager.persist(createBranch(tenantA, "Sucursal A", "SA"));
        entityManager.persist(createBranch(tenantB, "Sucursal B", "SB"));
        entityManager.flush();

        List<Branch> tenantAResults = branchRepository.findAllByTenantId(tenantA);
        List<Branch> tenantBResults = branchRepository.findAllByTenantId(tenantB);

        assertThat(tenantAResults).hasSize(1);
        assertThat(tenantAResults.get(0).getName()).isEqualTo("Sucursal A");
        assertThat(tenantBResults).hasSize(1);
        assertThat(tenantBResults.get(0).getName()).isEqualTo("Sucursal B");
    }

    @Test
    @DisplayName("4. findByIdAndTenantId returns empty for cross-tenant access (IDOR protection)")
    void findByIdAndTenantIdReturnEmptyForCrossTenant() {
        Branch branch = createBranch(tenantA, "Sucursal A", "SA");
        entityManager.persist(branch);
        entityManager.flush();

        Optional<Branch> result = branchRepository.findByIdAndTenantId(branch.getId(), tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("5. findByIdAndTenantId excludes soft-deleted branches")
    void findByIdAndTenantIdExcludesSoftDeleted() {
        Branch branch = createBranch(tenantA, "Eliminada", "ELI");
        branch.setDeletedAt(Instant.now());
        entityManager.persist(branch);
        entityManager.flush();

        Optional<Branch> result = branchRepository.findByIdAndTenantId(branch.getId(), tenantA);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("6. existsByNameAndTenantId detects duplicate names within tenant")
    void existsByNameDetectsDuplicates() {
        entityManager.persist(createBranch(tenantA, "Centro", "CE"));
        entityManager.flush();

        assertThat(branchRepository.existsByNameAndTenantId("Centro", tenantA)).isTrue();
        assertThat(branchRepository.existsByNameAndTenantId("Centro", tenantB)).isFalse();
        assertThat(branchRepository.existsByNameAndTenantId("Norte", tenantA)).isFalse();
    }

    @Test
    @DisplayName("7. existsByCodeAndTenantId detects duplicate codes within tenant")
    void existsByCodeDetectsDuplicates() {
        entityManager.persist(createBranch(tenantA, "Centro", "CENTRO"));
        entityManager.flush();

        assertThat(branchRepository.existsByCodeAndTenantId("CENTRO", tenantA)).isTrue();
        assertThat(branchRepository.existsByCodeAndTenantId("CENTRO", tenantB)).isFalse();
        assertThat(branchRepository.existsByCodeAndTenantId("NORTE", tenantA)).isFalse();
    }

    @Test
    @DisplayName("8. existsByNameAndTenantIdAndIdNot allows same name for same branch (update scenario)")
    void existsByNameAndIdNotAllowsSameBranchUpdate() {
        Branch branch = createBranch(tenantA, "Centro", "CE");
        entityManager.persist(branch);
        entityManager.flush();

        // Same branch should not be flagged as duplicate
        assertThat(branchRepository.existsByNameAndTenantIdAndIdNot("Centro", tenantA, branch.getId())).isFalse();

        // Another branch's name should be flagged
        Branch other = createBranch(tenantA, "Norte", "NO");
        entityManager.persist(other);
        entityManager.flush();
        assertThat(branchRepository.existsByNameAndTenantIdAndIdNot("Centro", tenantA, other.getId())).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Branch createBranch(UUID tenantId, String name, String code) {
        Branch branch = new Branch();
        branch.setTenantId(tenantId);
        branch.setName(name);
        branch.setCode(code);
        return branch;
    }
}
