package com.quickstack.user.security;

import com.quickstack.common.security.JwtAuthenticationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluates user management permissions for use with Spring Security's {@code @PreAuthorize}.
 * <p>
 * Only OWNER role is allowed to manage (create, update, delete) users.
 * <p>
 * Usage in controllers:
 * <pre>
 * {@literal @}PreAuthorize("@userPermissionEvaluator.canManageUsers(authentication)")
 * </pre>
 * <p>
 * ASVS V4.1: Access control enforced at method level.
 */
@Component("userPermissionEvaluator")
public class UserPermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(UserPermissionEvaluator.class);

    private static final String ROLE_OWNER = "OWNER";

    private final JdbcTemplate jdbcTemplate;

    public UserPermissionEvaluator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns true only if the authenticated user has the OWNER role.
     */
    public boolean canManageUsers(Authentication auth) {
        return ROLE_OWNER.equals(resolveRoleCode(auth));
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
