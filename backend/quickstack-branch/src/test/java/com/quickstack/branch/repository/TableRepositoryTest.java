package com.quickstack.branch.repository;

import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;
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
 * Repository tests for TableRepository using Testcontainers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Table Repository")
class TableRepositoryTest {

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
    private TableRepository tableRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID tenantA;
    private UUID branchA;
    private UUID areaA;

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

        areaA = UUID.randomUUID();
        entityManager.getEntityManager().createNativeQuery(
                "INSERT INTO areas (id, tenant_id, branch_id, name, sort_order, is_active) VALUES (?, ?, ?, 'Interior', 0, true)")
                .setParameter(1, areaA).setParameter(2, tenantA).setParameter(3, branchA).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("1. findAllByAreaIdAndTenantId excludes soft-deleted tables")
    void findAllExcludesSoftDeleted() {
        RestaurantTable active = createTable(tenantA, areaA, "1", 0);
        RestaurantTable deleted = createTable(tenantA, areaA, "2", 1);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<RestaurantTable> result = tableRepository.findAllByAreaIdAndTenantId(areaA, tenantA);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumber()).isEqualTo("1");
    }

    @Test
    @DisplayName("2. findByIdAndTenantId returns empty for cross-tenant access (IDOR protection)")
    void findByIdAndTenantIdReturnEmptyForCrossTenant() {
        UUID tenantB = UUID.randomUUID();
        RestaurantTable table = createTable(tenantA, areaA, "1", 0);
        entityManager.persist(table);
        entityManager.flush();

        Optional<RestaurantTable> result = tableRepository.findByIdAndTenantId(table.getId(), tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("3. existsByNumberAndAreaIdAndTenantId detects duplicate numbers within area")
    void existsByNumberDetectsDuplicates() {
        entityManager.persist(createTable(tenantA, areaA, "1", 0));
        entityManager.flush();

        assertThat(tableRepository.existsByNumberAndAreaIdAndTenantId("1", areaA, tenantA)).isTrue();
        assertThat(tableRepository.existsByNumberAndAreaIdAndTenantId("2", areaA, tenantA)).isFalse();
    }

    @Test
    @DisplayName("4. findAvailableByBranchIdAndTenantId returns only AVAILABLE active tables")
    void findAvailableReturnsOnlyAvailableTables() {
        RestaurantTable available = createTable(tenantA, areaA, "1", 0);
        RestaurantTable occupied = createTable(tenantA, areaA, "2", 1);
        occupied.setStatus(TableStatus.OCCUPIED);
        RestaurantTable reserved = createTable(tenantA, areaA, "3", 2);
        reserved.setStatus(TableStatus.RESERVED);

        entityManager.persist(available);
        entityManager.persist(occupied);
        entityManager.persist(reserved);
        entityManager.flush();

        List<RestaurantTable> result = tableRepository.findAvailableByBranchIdAndTenantId(
                branchA, tenantA, TableStatus.AVAILABLE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumber()).isEqualTo("1");
    }

    @Test
    @DisplayName("5. findAvailableByBranchIdAndTenantId excludes soft-deleted tables")
    void findAvailableExcludesSoftDeleted() {
        RestaurantTable available = createTable(tenantA, areaA, "1", 0);
        RestaurantTable deletedAvailable = createTable(tenantA, areaA, "2", 1);
        deletedAvailable.setDeletedAt(Instant.now());

        entityManager.persist(available);
        entityManager.persist(deletedAvailable);
        entityManager.flush();

        List<RestaurantTable> result = tableRepository.findAvailableByBranchIdAndTenantId(
                branchA, tenantA, TableStatus.AVAILABLE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumber()).isEqualTo("1");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RestaurantTable createTable(UUID tenantId, UUID areaId, String number, int sortOrder) {
        RestaurantTable table = new RestaurantTable();
        table.setTenantId(tenantId);
        table.setAreaId(areaId);
        table.setNumber(number);
        table.setSortOrder(sortOrder);
        table.setStatus(TableStatus.AVAILABLE);
        table.setActive(true);
        return table;
    }
}
