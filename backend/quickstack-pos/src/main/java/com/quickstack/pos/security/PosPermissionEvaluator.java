package com.quickstack.pos.security;

import com.quickstack.common.security.JwtAuthenticationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluates POS-specific permissions for use with Spring Security's {@code @PreAuthorize}.
 * <p>
 * Resolves role codes from the role ID stored in the JWT principal to enforce
 * role-based access control for customer, order, and payment operations.
 * <p>
 * Usage in controllers:
 * <pre>
 * {@literal @}PreAuthorize("@posPermissionEvaluator.canCreateCustomer(authentication)")
 * </pre>
 * <p>
 * Role hierarchy for POS operations:
 * - OWNER:   full access to all POS resources
 * - MANAGER: can create and update customers; cannot delete
 * - CASHIER: can create customers (needed for delivery orders); cannot delete
 * <p>
 * ASVS V4.1: Access control enforced at method level.
 */
@Component("posPermissionEvaluator")
public class PosPermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PosPermissionEvaluator.class);

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_CASHIER = "CASHIER";

    private final JdbcTemplate jdbcTemplate;

    public PosPermissionEvaluator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns true if the authenticated user can create or update customers.
     * Allowed roles: OWNER, MANAGER, CASHIER.
     * Cashiers need this permission to register customers during delivery orders.
     */
    public boolean canCreateCustomer(Authentication auth) {
        String roleCode = resolveRoleCode(auth);
        return ROLE_OWNER.equals(roleCode)
                || ROLE_MANAGER.equals(roleCode)
                || ROLE_CASHIER.equals(roleCode);
    }

    /**
     * Returns true if the authenticated user can soft-delete customers.
     * Only OWNER and MANAGER roles are allowed to delete customers.
     */
    public boolean canDeleteCustomer(Authentication auth) {
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
