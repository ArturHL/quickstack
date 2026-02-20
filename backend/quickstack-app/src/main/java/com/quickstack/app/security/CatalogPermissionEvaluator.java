package com.quickstack.app.security;

import com.quickstack.app.security.JwtAuthenticationFilter.JwtAuthenticationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluates catalog-specific permissions for use with Spring Security's {@code @PreAuthorize}.
 * <p>
 * Resolves role codes from the role ID stored in the JWT principal to enforce
 * role-based access control for catalog operations.
 * <p>
 * Usage in controllers:
 * <pre>
 * {@literal @}PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
 * </pre>
 * <p>
 * Role hierarchy for catalog operations:
 * - OWNER: full access (manage, delete, restore, view inactive)
 * - MANAGER: manage, delete, view inactive (cannot restore)
 * - CASHIER: read-only, active categories only
 * <p>
 * ASVS V4.1: Access control enforced at method level.
 */
@Component
public class CatalogPermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CatalogPermissionEvaluator.class);

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MANAGER = "MANAGER";

    private final JdbcTemplate jdbcTemplate;

    public CatalogPermissionEvaluator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns true if the authenticated user can create, update, and manage catalog categories.
     * Allowed roles: OWNER, MANAGER.
     *
     * @param auth the current Spring Security authentication
     * @return true if the user has catalog management permission
     */
    public boolean canManageCatalog(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can delete categories.
     * Allowed roles: OWNER, MANAGER.
     *
     * @param auth the current Spring Security authentication
     * @return true if the user can delete categories
     */
    public boolean canDeleteCategory(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can restore soft-deleted categories.
     * Only OWNER is allowed to restore deleted resources.
     *
     * @param auth the current Spring Security authentication
     * @return true if the user can restore categories
     */
    public boolean canRestoreCategory(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can view inactive categories.
     * Allowed roles: OWNER, MANAGER.
     * CASHIER users always see only active categories.
     *
     * @param auth the current Spring Security authentication
     * @return true if the user can view inactive categories
     */
    public boolean canViewInactive(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the role code for the authenticated user by querying the roles table.
     * <p>
     * The JWT only carries the role ID (UUID). We resolve the human-readable code
     * (e.g., "OWNER") at permission check time. This is acceptable because:
     * - The roles table is a small, rarely-changing catalog
     * - No Role entity exists yet; a JdbcTemplate query avoids premature abstraction
     *
     * @param auth the current authentication
     * @return the role code string, or null if not resolvable
     */
    private String resolveRoleCode(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (!(auth.getPrincipal() instanceof JwtAuthenticationPrincipal principal)) {
            log.warn("Unexpected principal type: {}", auth.getPrincipal().getClass().getName());
            return null;
        }
        return fetchRoleCode(principal.roleId());
    }

    /**
     * Queries the roles table for the code associated with the given role ID.
     *
     * @param roleId the role UUID from the JWT
     * @return the role code or null if not found
     */
    private String fetchRoleCode(UUID roleId) {
        if (roleId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                "SELECT code FROM roles WHERE id = ?",
                String.class,
                roleId
            );
        } catch (Exception e) {
            log.warn("Failed to resolve role code for roleId={}: {}", roleId, e.getMessage());
            return null;
        }
    }
}
