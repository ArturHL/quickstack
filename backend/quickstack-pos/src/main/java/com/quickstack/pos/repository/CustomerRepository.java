package com.quickstack.pos.repository;

import com.quickstack.pos.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Customer entity with tenant-safe queries.
 * <p>
 * All queries automatically filter by tenant_id to enforce multi-tenancy.
 * Soft-deleted customers (deleted_at != null) are excluded from default queries.
 * <p>
 * ASVS V4.1: Access control - all queries enforce tenant isolation.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Finds all non-deleted customers for a tenant, with pagination.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of active customers
     */
    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL")
    Page<Customer> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    /**
     * Finds a customer by ID and tenant ID, excluding soft-deleted.
     * Returns empty if the customer belongs to another tenant (IDOR protection).
     *
     * @param id       the customer ID
     * @param tenantId the tenant ID
     * @return Optional containing the customer if found and belongs to tenant
     */
    @Query("SELECT c FROM Customer c WHERE c.id = :id AND c.tenantId = :tenantId AND c.deletedAt IS NULL")
    Optional<Customer> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /**
     * Finds a customer by phone number within a tenant.
     * Used for quick lookup during order creation.
     *
     * @param phone    the phone number
     * @param tenantId the tenant ID
     * @return Optional containing the customer if found
     */
    @Query("SELECT c FROM Customer c WHERE c.phone = :phone AND c.tenantId = :tenantId AND c.deletedAt IS NULL")
    Optional<Customer> findByPhoneAndTenantId(@Param("phone") String phone, @Param("tenantId") UUID tenantId);

    /**
     * Checks if a customer with the given phone already exists for the tenant.
     * Excludes soft-deleted customers.
     */
    boolean existsByPhoneAndTenantIdAndDeletedAtIsNull(String phone, UUID tenantId);

    /**
     * Checks if a customer with the given email already exists for the tenant.
     * Excludes soft-deleted customers.
     */
    boolean existsByEmailAndTenantIdAndDeletedAtIsNull(String email, UUID tenantId);

    /**
     * Checks if another customer with the given phone exists for the tenant,
     * excluding a specific customer ID. Used during update validation.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c " +
           "WHERE c.phone = :phone AND c.tenantId = :tenantId AND c.deletedAt IS NULL AND c.id != :excludeId")
    boolean existsByPhoneAndTenantIdAndDeletedAtIsNullAndIdNot(
            @Param("phone") String phone,
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);

    /**
     * Checks if another customer with the given email exists for the tenant,
     * excluding a specific customer ID. Used during update validation.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c " +
           "WHERE c.email = :email AND c.tenantId = :tenantId AND c.deletedAt IS NULL AND c.id != :excludeId")
    boolean existsByEmailAndTenantIdAndDeletedAtIsNullAndIdNot(
            @Param("email") String email,
            @Param("tenantId") UUID tenantId,
            @Param("excludeId") UUID excludeId);

    /**
     * Searches customers by name, phone, or email within a tenant.
     * Excludes soft-deleted customers. Case-insensitive for name and email.
     *
     * @param tenantId the tenant ID
     * @param search   the search term
     * @param pageable pagination parameters
     * @return page of matching customers
     */
    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR c.phone LIKE CONCAT('%', :search, '%') " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchCustomers(
            @Param("tenantId") UUID tenantId,
            @Param("search") String search,
            Pageable pageable);
}
