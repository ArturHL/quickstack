package com.quickstack.product.security;

import com.quickstack.common.security.JwtAuthenticationPrincipal;
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
     */
    public boolean canManageCatalog(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can delete categories.
     * Allowed roles: OWNER, MANAGER.
     */
    public boolean canDeleteCategory(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can restore soft-deleted categories.
     * Only OWNER is allowed to restore deleted resources.
     */
    public boolean canRestoreCategory(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can view inactive categories.
     * Allowed roles: OWNER, MANAGER.
     * CASHIER users always see only active categories.
     */
    public boolean canViewInactive(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

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
