package com.quickstack.user.repository;

import com.quickstack.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 * <p>
 * Multi-tenant queries ensure data isolation between tenants.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find active user by email within a tenant.
     * Used for login and email uniqueness validation.
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByTenantIdAndEmail(
        @Param("tenantId") UUID tenantId,
        @Param("email") String email
    );

    /**
     * Check if email exists within a tenant (for registration validation).
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.tenantId = :tenantId AND u.email = :email AND u.deletedAt IS NULL")
    boolean existsByTenantIdAndEmail(
        @Param("tenantId") UUID tenantId,
        @Param("email") String email
    );

    /**
     * Find active user by ID within a tenant.
     * Ensures tenant isolation even when using IDs.
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByTenantIdAndId(
        @Param("tenantId") UUID tenantId,
        @Param("id") UUID id
    );

    /**
     * Find user by email across all tenants (for global checks).
     * Use with caution - mainly for admin/support purposes.
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailGlobal(@Param("email") String email);

    /**
     * List all active users within a tenant (no search filter).
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Page<User> findByTenantId(
        @Param("tenantId") UUID tenantId,
        Pageable pageable
    );

    /**
     * List active users within a tenant filtered by search term.
     * Search must be non-null and non-blank. Searches email and fullName (case-insensitive).
     * Split from findByTenantId to avoid Hibernate null-parameter bytea type inference bug.
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL " +
           "AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByTenantIdAndSearch(
        @Param("tenantId") UUID tenantId,
        @Param("search") String search,
        Pageable pageable
    );
}
