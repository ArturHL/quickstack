package com.quickstack.pos.repository;

import com.quickstack.pos.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.quickstack.pos.AbstractRepositoryTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for CustomerRepository using Testcontainers.
 * <p>
 * Verifies:
 * - Multi-tenant isolation
 * - Soft delete filtering
 * - Phone and email uniqueness constraints
 * - Search functionality
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Customer Repository")
class CustomerRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

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
    @DisplayName("1. findAllByTenantId excludes soft-deleted customers")
    void findAllExcludesSoftDeleted() {
        Customer active = buildCustomer(tenantA, "Juan Activo", "5551111111", null);
        Customer deleted = buildCustomer(tenantA, "Pedro Borrado", "5552222222", null);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        Page<Customer> result = customerRepository.findAllByTenantId(tenantA, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Juan Activo");
    }

    @Test
    @DisplayName("2. findByIdAndTenantId returns empty for cross-tenant access (IDOR protection)")
    void findByIdAndTenantIdReturnEmptyForCrossTenant() {
        Customer customer = buildCustomer(tenantA, "Maria A", "5553333333", null);
        entityManager.persist(customer);
        entityManager.flush();

        Optional<Customer> result = customerRepository.findByIdAndTenantId(customer.getId(), tenantB);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("3. findByPhoneAndTenantId returns customer when found")
    void findByPhoneAndTenantIdReturnsCustomer() {
        Customer customer = buildCustomer(tenantA, "Carlos", "5554444444", null);
        entityManager.persist(customer);
        entityManager.flush();

        Optional<Customer> result = customerRepository.findByPhoneAndTenantId("5554444444", tenantA);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Carlos");
    }

    @Test
    @DisplayName("4. existsByPhoneAndTenantIdAndDeletedAtIsNull returns true when phone exists")
    void existsByPhoneReturnsTrueWhenExists() {
        entityManager.persist(buildCustomer(tenantA, "Ana", "5555555555", null));
        entityManager.flush();

        assertThat(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNull("5555555555", tenantA)).isTrue();
        assertThat(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNull("5555555555", tenantB)).isFalse();
        assertThat(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNull("9999999999", tenantA)).isFalse();
    }

    @Test
    @DisplayName("5. existsByEmailAndTenantIdAndDeletedAtIsNull returns true when email exists")
    void existsByEmailReturnsTrueWhenExists() {
        entityManager.persist(buildCustomer(tenantA, "Luis", null, "luis@test.com"));
        entityManager.flush();

        assertThat(customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNull("luis@test.com", tenantA)).isTrue();
        assertThat(customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNull("luis@test.com", tenantB)).isFalse();
        assertThat(customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNull("other@test.com", tenantA)).isFalse();
    }

    @Test
    @DisplayName("6. existsByPhoneAndIdNot excludes the specified customer ID (update scenario)")
    void existsByPhoneAndIdNotExcludesSpecifiedCustomer() {
        Customer customer = buildCustomer(tenantA, "Rosa", "5556666666", null);
        entityManager.persist(customer);
        entityManager.flush();

        // Same customer: should not flag as duplicate
        assertThat(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNullAndIdNot(
                "5556666666", tenantA, customer.getId())).isFalse();

        // Another customer: should flag as duplicate
        Customer other = buildCustomer(tenantA, "Pedro", "5557777777", null);
        entityManager.persist(other);
        entityManager.flush();
        assertThat(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNullAndIdNot(
                "5556666666", tenantA, other.getId())).isTrue();
    }

    @Test
    @DisplayName("7. searchCustomers finds by name")
    void searchCustomersByName() {
        entityManager.persist(buildCustomer(tenantA, "Maria Garcia", "5558888881", null));
        entityManager.persist(buildCustomer(tenantA, "Juan Perez", "5558888882", null));
        entityManager.flush();

        Page<Customer> result = customerRepository.searchCustomers(tenantA, "Garcia", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Maria Garcia");
    }

    @Test
    @DisplayName("8. searchCustomers finds by phone")
    void searchCustomersByPhone() {
        entityManager.persist(buildCustomer(tenantA, "Roberto", "5559990001", null));
        entityManager.persist(buildCustomer(tenantA, "Sandra", "5559990002", null));
        entityManager.flush();

        Page<Customer> result = customerRepository.searchCustomers(tenantA, "9990001", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Roberto");
    }

    @Test
    @DisplayName("9. searchCustomers finds by email")
    void searchCustomersByEmail() {
        entityManager.persist(buildCustomer(tenantA, "Elena", null, "elena@company.com"));
        entityManager.persist(buildCustomer(tenantA, "Marco", null, "marco@other.com"));
        entityManager.flush();

        Page<Customer> result = customerRepository.searchCustomers(tenantA, "company", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Elena");
    }

    @Test
    @DisplayName("10. searchCustomers returns empty page when no match")
    void searchCustomersReturnsEmptyWhenNoMatch() {
        entityManager.persist(buildCustomer(tenantA, "Jorge", "5551234567", null));
        entityManager.flush();

        Page<Customer> result = customerRepository.searchCustomers(tenantA, "nonexistent", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("11. searchCustomers is case-insensitive for name")
    void searchCustomersCaseInsensitive() {
        entityManager.persist(buildCustomer(tenantA, "Alejandra Lopez", "5550000001", null));
        entityManager.flush();

        Page<Customer> lower = customerRepository.searchCustomers(tenantA, "alejandra", PageRequest.of(0, 10));
        Page<Customer> upper = customerRepository.searchCustomers(tenantA, "ALEJANDRA", PageRequest.of(0, 10));
        Page<Customer> mixed = customerRepository.searchCustomers(tenantA, "Lopez", PageRequest.of(0, 10));

        assertThat(lower.getTotalElements()).isEqualTo(1);
        assertThat(upper.getTotalElements()).isEqualTo(1);
        assertThat(mixed.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("12. searchCustomers excludes soft-deleted customers")
    void searchCustomersExcludesSoftDeleted() {
        Customer active = buildCustomer(tenantA, "Carmen Visible", "5550000002", null);
        Customer deleted = buildCustomer(tenantA, "Carmen Borrada", "5550000003", null);
        deleted.setDeletedAt(Instant.now());

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        Page<Customer> result = customerRepository.searchCustomers(tenantA, "Carmen", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Carmen Visible");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Customer buildCustomer(UUID tenantId, String name, String phone, String email) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(email);
        // Ensure at least one contact is always set (DB constraint)
        if (phone == null && email == null) {
            customer.setWhatsapp("5550000000");
        }
        return customer;
    }
}
