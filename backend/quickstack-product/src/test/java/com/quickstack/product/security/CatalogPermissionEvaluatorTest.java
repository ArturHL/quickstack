package com.quickstack.product.security;

import com.quickstack.common.security.JwtAuthenticationPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogPermissionEvaluator")
class CatalogPermissionEvaluatorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private CatalogPermissionEvaluator evaluator;

    private static final UUID OWNER_ROLE_ID = UUID.randomUUID();
    private static final UUID MANAGER_ROLE_ID = UUID.randomUUID();
    private static final UUID CASHIER_ROLE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        evaluator = new CatalogPermissionEvaluator(jdbcTemplate);
    }

    @Nested
    @DisplayName("canManageCatalog")
    class CanManageCatalogTests {

        @Test
        @DisplayName("should return true for OWNER")
        void shouldAllowOwner() {
            Authentication auth = buildAuthentication(OWNER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(OWNER_ROLE_ID)))
                .thenReturn("OWNER");

            assertThat(evaluator.canManageCatalog(auth)).isTrue();
        }

        @Test
        @DisplayName("should return true for MANAGER")
        void shouldAllowManager() {
            Authentication auth = buildAuthentication(MANAGER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(MANAGER_ROLE_ID)))
                .thenReturn("MANAGER");

            assertThat(evaluator.canManageCatalog(auth)).isTrue();
        }

        @Test
        @DisplayName("should return false for CASHIER")
        void shouldDenyCashier() {
            Authentication auth = buildAuthentication(CASHIER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(CASHIER_ROLE_ID)))
                .thenReturn("CASHIER");

            assertThat(evaluator.canManageCatalog(auth)).isFalse();
        }
    }

    @Nested
    @DisplayName("canDeleteCategory")
    class CanDeleteCategoryTests {

        @Test
        @DisplayName("should return true for OWNER")
        void shouldAllowOwner() {
            Authentication auth = buildAuthentication(OWNER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(OWNER_ROLE_ID)))
                .thenReturn("OWNER");

            assertThat(evaluator.canDeleteCategory(auth)).isTrue();
        }

        @Test
        @DisplayName("should return true for MANAGER")
        void shouldAllowManager() {
            Authentication auth = buildAuthentication(MANAGER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(MANAGER_ROLE_ID)))
                .thenReturn("MANAGER");

            assertThat(evaluator.canDeleteCategory(auth)).isTrue();
        }

        @Test
        @DisplayName("should return false for CASHIER")
        void shouldDenyCashier() {
            Authentication auth = buildAuthentication(CASHIER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(CASHIER_ROLE_ID)))
                .thenReturn("CASHIER");

            assertThat(evaluator.canDeleteCategory(auth)).isFalse();
        }
    }

    @Nested
    @DisplayName("canRestoreCategory")
    class CanRestoreCategoryTests {

        @Test
        @DisplayName("should return true only for OWNER")
        void shouldAllowOnlyOwner() {
            Authentication auth = buildAuthentication(OWNER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(OWNER_ROLE_ID)))
                .thenReturn("OWNER");

            assertThat(evaluator.canRestoreCategory(auth)).isTrue();
        }

        @Test
        @DisplayName("should return false for MANAGER")
        void shouldDenyManager() {
            Authentication auth = buildAuthentication(MANAGER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(MANAGER_ROLE_ID)))
                .thenReturn("MANAGER");

            assertThat(evaluator.canRestoreCategory(auth)).isFalse();
        }

        @Test
        @DisplayName("should return false for CASHIER")
        void shouldDenyCashier() {
            Authentication auth = buildAuthentication(CASHIER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(CASHIER_ROLE_ID)))
                .thenReturn("CASHIER");

            assertThat(evaluator.canRestoreCategory(auth)).isFalse();
        }
    }

    @Nested
    @DisplayName("canViewInactive")
    class CanViewInactiveTests {

        @Test
        @DisplayName("should return true for OWNER")
        void shouldAllowOwner() {
            Authentication auth = buildAuthentication(OWNER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(OWNER_ROLE_ID)))
                .thenReturn("OWNER");

            assertThat(evaluator.canViewInactive(auth)).isTrue();
        }

        @Test
        @DisplayName("should return true for MANAGER")
        void shouldAllowManager() {
            Authentication auth = buildAuthentication(MANAGER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(MANAGER_ROLE_ID)))
                .thenReturn("MANAGER");

            assertThat(evaluator.canViewInactive(auth)).isTrue();
        }

        @Test
        @DisplayName("should return false for CASHIER")
        void shouldDenyCashier() {
            Authentication auth = buildAuthentication(CASHIER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(CASHIER_ROLE_ID)))
                .thenReturn("CASHIER");

            assertThat(evaluator.canViewInactive(auth)).isFalse();
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should return false when authentication is null")
        void shouldReturnFalseWhenAuthNull() {
            assertThat(evaluator.canManageCatalog(null)).isFalse();
        }

        @Test
        @DisplayName("should return false when role lookup fails")
        void shouldReturnFalseWhenRoleLookupFails() {
            Authentication auth = buildAuthentication(OWNER_ROLE_ID);
            when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(OWNER_ROLE_ID)))
                .thenThrow(new RuntimeException("DB error"));

            assertThat(evaluator.canManageCatalog(auth)).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Authentication buildAuthentication(UUID roleId) {
        JwtAuthenticationPrincipal principal = new JwtAuthenticationPrincipal(
            UUID.randomUUID(), UUID.randomUUID(), roleId, null, "user@example.com");
        return new UsernamePasswordAuthenticationToken(
            principal, null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
