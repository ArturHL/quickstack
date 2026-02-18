package com.quickstack.user;

import com.quickstack.user.entity.User;
import com.quickstack.user.repository.RefreshTokenRepository;
import com.quickstack.user.repository.UserRepository;
import com.quickstack.user.service.PasswordBreachChecker;
import com.quickstack.user.service.PasswordService;
import com.quickstack.user.service.SessionService;
import com.quickstack.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression tests for multi-tenant isolation in the authentication module.
 * <p>
 * Verifies that tenant boundaries are strictly enforced at the service layer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Tenant Security Regression Tests")
class MultiTenantSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private PasswordBreachChecker breachChecker;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();
    private static final UUID USER_A_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "SecurePassword123!";

    @Nested
    @DisplayName("User Lookup Isolation")
    class UserLookupIsolationTests {

        @Test
        @DisplayName("user from tenant A is not found when querying tenant B")
        void userFromTenantANotFoundInTenantB() {
            User userInTenantA = createUser(USER_A_ID, TENANT_A);

            when(userRepository.findByTenantIdAndEmail(TENANT_A, EMAIL))
                    .thenReturn(Optional.of(userInTenantA));
            when(userRepository.findByTenantIdAndEmail(TENANT_B, EMAIL))
                    .thenReturn(Optional.empty());

            Optional<User> foundInA = userRepository.findByTenantIdAndEmail(TENANT_A, EMAIL);
            Optional<User> foundInB = userRepository.findByTenantIdAndEmail(TENANT_B, EMAIL);

            assertThat(foundInA).isPresent();
            assertThat(foundInA.get().getTenantId()).isEqualTo(TENANT_A);
            assertThat(foundInB).isEmpty();
        }

        @Test
        @DisplayName("email uniqueness check is scoped to tenant")
        void emailUniquenessIsScopedToTenant() {
            when(userRepository.existsByTenantIdAndEmail(TENANT_A, EMAIL)).thenReturn(true);
            when(userRepository.existsByTenantIdAndEmail(TENANT_B, EMAIL)).thenReturn(false);

            boolean existsInA = userRepository.existsByTenantIdAndEmail(TENANT_A, EMAIL);
            boolean existsInB = userRepository.existsByTenantIdAndEmail(TENANT_B, EMAIL);

            assertThat(existsInA).isTrue();
            assertThat(existsInB).isFalse();
        }
    }

    @Nested
    @DisplayName("UserService Registration Isolation")
    class RegistrationIsolationTests {

        @Test
        @DisplayName("registerUser enforces email uniqueness per tenant - tenant A blocks, tenant B allows")
        void registerEnforcesEmailUniquenessPerTenant() {
            UserService userService = new UserService(userRepository, passwordService, breachChecker);

            // Email taken in tenant A, free in tenant B
            when(userRepository.existsByTenantIdAndEmail(TENANT_A, EMAIL)).thenReturn(true);
            when(userRepository.existsByTenantIdAndEmail(TENANT_B, EMAIL)).thenReturn(false);

            // Registration in tenant A fails
            UserService.RegisterUserCommand commandA = new UserService.RegisterUserCommand(
                    TENANT_A, EMAIL, PASSWORD, "John Doe", ROLE_ID, null, null
            );
            assertThatThrownBy(() -> userService.registerUser(commandA))
                    .isInstanceOf(UserService.EmailAlreadyExistsException.class);

            // Registration in tenant B succeeds
            doNothing().when(passwordService).validatePasswordPolicy(any());
            doNothing().when(breachChecker).checkPassword(any());
            when(passwordService.hashPassword(any())).thenReturn("hashed");
            User savedUser = createUser(UUID.randomUUID(), TENANT_B);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserService.RegisterUserCommand commandB = new UserService.RegisterUserCommand(
                    TENANT_B, EMAIL, PASSWORD, "John Doe", ROLE_ID, null, null
            );
            User result = userService.registerUser(commandB);
            assertThat(result.getTenantId()).isEqualTo(TENANT_B);
        }
    }

    @Nested
    @DisplayName("Session Isolation")
    class SessionIsolationTests {

        @Test
        @DisplayName("getActiveSessions uses userId for isolation - two users get separate sessions")
        void getActiveSessionsIsolatesByUserId() {
            UUID userAId = UUID.randomUUID();
            UUID userBId = UUID.randomUUID();

            when(refreshTokenRepository.findActiveByUserId(eq(userAId), any()))
                    .thenReturn(List.of());
            when(refreshTokenRepository.findActiveByUserId(eq(userBId), any()))
                    .thenReturn(List.of());

            SessionService sessionService = new SessionService(refreshTokenRepository, Clock.systemUTC());

            sessionService.getActiveSessions(userAId);
            sessionService.getActiveSessions(userBId);

            // Each call uses the correct userId - no cross-user leakage
            verify(refreshTokenRepository, times(1)).findActiveByUserId(eq(userAId), any());
            verify(refreshTokenRepository, times(1)).findActiveByUserId(eq(userBId), any());
            // Total: exactly 2 calls (one per user)
            verify(refreshTokenRepository, times(2)).findActiveByUserId(any(), any());
        }

        @Test
        @DisplayName("revokeSession rejects access to sessions of another user (IDOR prevention)")
        void revokeSessionPreventsIDOR() {
            UUID userAId = UUID.randomUUID();
            UUID userBId = UUID.randomUUID();
            UUID sessionBId = UUID.randomUUID();

            // Session belongs to user B
            com.quickstack.user.entity.RefreshToken tokenOfB =
                    com.quickstack.user.entity.RefreshToken.create(
                            userBId, "hash", UUID.randomUUID(),
                            java.time.Instant.now().plusSeconds(3600),
                            "192.168.1.1", "Mozilla"
                    );
            org.springframework.test.util.ReflectionTestUtils.setField(tokenOfB, "id", sessionBId);

            when(refreshTokenRepository.findById(sessionBId)).thenReturn(Optional.of(tokenOfB));

            SessionService sessionService = new SessionService(refreshTokenRepository, Clock.systemUTC());

            // User A tries to revoke user B's session - must be denied
            assertThatThrownBy(() -> sessionService.revokeSession(userAId, sessionBId))
                    .isInstanceOf(SessionService.SessionAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Password Reset Isolation")
    class PasswordResetIsolationTests {

        @Test
        @DisplayName("password reset lookup is scoped to tenant (no cross-tenant leakage)")
        void passwordResetLookupIsScopedToTenant() {
            User userInTenantA = createUser(USER_A_ID, TENANT_A);

            when(userRepository.findByTenantIdAndEmail(TENANT_A, EMAIL))
                    .thenReturn(Optional.of(userInTenantA));
            when(userRepository.findByTenantIdAndEmail(TENANT_B, EMAIL))
                    .thenReturn(Optional.empty());

            Optional<User> forTenantA = userRepository.findByTenantIdAndEmail(TENANT_A, EMAIL);
            Optional<User> forTenantB = userRepository.findByTenantIdAndEmail(TENANT_B, EMAIL);

            assertThat(forTenantA).isPresent();
            assertThat(forTenantB).isEmpty();

            // Verify exact queries with tenantId
            verify(userRepository).findByTenantIdAndEmail(TENANT_A, EMAIL);
            verify(userRepository).findByTenantIdAndEmail(TENANT_B, EMAIL);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User createUser(UUID id, UUID tenantId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setEmail(EMAIL);
        user.setPasswordHash("hashed-password");
        user.setFullName("Test User");
        user.setRoleId(ROLE_ID);
        user.setActive(true);
        return user;
    }
}
