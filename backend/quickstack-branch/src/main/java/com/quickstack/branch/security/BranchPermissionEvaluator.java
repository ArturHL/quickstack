package com.quickstack.branch.security;

import com.quickstack.common.security.JwtAuthenticationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluates branch-specific permissions for use with Spring Security's {@code @PreAuthorize}.
 * <p>
 * Resolves role codes from the role ID stored in the JWT principal to enforce
 * role-based access control for branch, area, and table operations.
 * <p>
 * Usage in controllers:
 * <pre>
 * {@literal @}PreAuthorize("@branchPermissionEvaluator.canManageBranch(authentication)")
 * </pre>
 * <p>
 * Role hierarchy for branch operations:
 * - OWNER:   can manage branches (create, update, delete)
 * - MANAGER: can manage areas and tables, but not branches
 * - CASHIER: read-only access to branches, areas, and tables
 * <p>
 * ASVS V4.1: Access control enforced at method level.
 */
@Component
public class BranchPermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(BranchPermissionEvaluator.class);

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MANAGER = "MANAGER";

    private final JdbcTemplate jdbcTemplate;

    public BranchPermissionEvaluator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns true if the authenticated user can create, update, or delete branches.
     * Only OWNER role is allowed to manage branches.
     */
    public boolean canManageBranch(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can create, update, or delete areas.
     * Allowed roles: OWNER, MANAGER.
     */
    public boolean canManageArea(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can create, update, delete tables,
     * or update table status.
     * Allowed roles: OWNER, MANAGER.
     */
    public boolean canManageTable(Authentication auth) {
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
