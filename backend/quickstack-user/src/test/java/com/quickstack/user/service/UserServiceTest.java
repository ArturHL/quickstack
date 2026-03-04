package com.quickstack.user.service;

import com.quickstack.common.exception.ApiException;
import com.quickstack.common.exception.PasswordCompromisedException;
import com.quickstack.common.exception.PasswordValidationException;
import com.quickstack.common.security.PasswordBreachChecker;
import com.quickstack.common.security.PasswordService;
import com.quickstack.user.dto.request.UserUpdateAdminRequest;
import com.quickstack.user.dto.response.UserResponse;
import com.quickstack.user.entity.User;
import com.quickstack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private PasswordBreachChecker breachChecker;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private UserService userService;

    // Test data
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "SecurePassword123!";
    private static final String FULL_NAME = "Test User";
    private static final String HASHED_PASSWORD = "v1$$argon2id$...hashed...";

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordService, breachChecker, jdbcTemplate);
    }

    @Nested
    @DisplayName("User Registration")
    class RegistrationTests {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() {
            // Given
            when(userRepository.existsByTenantIdAndEmail(TENANT_ID, VALID_EMAIL)).thenReturn(false);
            when(passwordService.hashPassword(VALID_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            var command = new UserService.RegisterUserCommand(
                TENANT_ID, VALID_EMAIL, VALID_PASSWORD, FULL_NAME, ROLE_ID, null, null
            );

            // When
            User result = userService.registerUser(command);

            // Then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getEmail()).isEqualTo(VALID_EMAIL);
            assertThat(result.getFullName()).isEqualTo(FULL_NAME);
            assertThat(result.getPasswordHash()).isEqualTo(HASHED_PASSWORD);

            // Verify password validation was called
            verify(passwordService).validatePasswordPolicy(VALID_PASSWORD);
            verify(breachChecker).checkPassword(VALID_PASSWORD);
            verify(passwordService).hashPassword(VALID_PASSWORD);
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeEmail() {
            when(userRepository.existsByTenantIdAndEmail(TENANT_ID, "TEST@EXAMPLE.COM")).thenReturn(false);
            when(passwordService.hashPassword(anyString())).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var command = new UserService.RegisterUserCommand(
                TENANT_ID, "TEST@EXAMPLE.COM", VALID_PASSWORD, FULL_NAME, ROLE_ID, null, null
            );

            User result = userService.registerUser(command);

            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should throw when email already exists")
        void shouldThrowWhenEmailExists() {
            when(userRepository.existsByTenantIdAndEmail(TENANT_ID, VALID_EMAIL)).thenReturn(true);

            var command = new UserService.RegisterUserCommand(
                TENANT_ID, VALID_EMAIL, VALID_PASSWORD, FULL_NAME, ROLE_ID, null, null
            );

            assertThatThrownBy(() -> userService.registerUser(command))
                .isInstanceOf(UserService.EmailAlreadyExistsException.class);

            // Should not proceed to password validation or hashing
            verify(passwordService, never()).hashPassword(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when password is too short")
        void shouldThrowWhenPasswordTooShort() {
            when(userRepository.existsByTenantIdAndEmail(TENANT_ID, VALID_EMAIL)).thenReturn(false);
            doThrow(PasswordValidationException.tooShort(12))
                .when(passwordService).validatePasswordPolicy("short");

            var command = new UserService.RegisterUserCommand(
                TENANT_ID, VALID_EMAIL, "short", FULL_NAME, ROLE_ID, null, null
            );

            assertThatThrownBy(() -> userService.registerUser(command))
                .isInstanceOf(PasswordValidationException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when password is compromised")
        void shouldThrowWhenPasswordCompromised() {
            when(userRepository.existsByTenantIdAndEmail(TENANT_ID, VALID_EMAIL)).thenReturn(false);
            doThrow(new PasswordCompromisedException())
                .when(breachChecker).checkPassword("compromised123");

            var command = new UserService.RegisterUserCommand(
                TENANT_ID, VALID_EMAIL, "compromised123", FULL_NAME, ROLE_ID, null, null
            );

            assertThatThrownBy(() -> userService.registerUser(command))
                .isInstanceOf(PasswordCompromisedException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should set branch and phone when provided")
        void shouldSetOptionalFields() {
            UUID branchId = UUID.randomUUID();
            String phone = "+52 55 1234 5678";

            when(userRepository.existsByTenantIdAndEmail(TENANT_ID, VALID_EMAIL)).thenReturn(false);
            when(passwordService.hashPassword(anyString())).thenReturn(HASHED_PASSWORD);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            var command = new UserService.RegisterUserCommand(
                TENANT_ID, VALID_EMAIL, VALID_PASSWORD, FULL_NAME, ROLE_ID, branchId, phone
            );

            userService.registerUser(command);

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getBranchId()).isEqualTo(branchId);
            assertThat(savedUser.getPhone()).isEqualTo(phone);
        }
    }

    @Nested
    @DisplayName("User Lookup")
    class LookupTests {

        @Test
        @DisplayName("should find user by email")
        void shouldFindByEmail() {
            User user = createTestUser();
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, VALID_EMAIL))
                .thenReturn(Optional.of(user));

            Optional<User> result = userService.findByEmail(TENANT_ID, VALID_EMAIL);

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("should normalize email when searching")
        void shouldNormalizeEmailOnSearch() {
            when(userRepository.findByTenantIdAndEmail(TENANT_ID, "test@example.com"))
                .thenReturn(Optional.empty());

            userService.findByEmail(TENANT_ID, " TEST@EXAMPLE.COM ");

            verify(userRepository).findByTenantIdAndEmail(TENANT_ID, "test@example.com");
        }

        @Test
        @DisplayName("should find user by ID")
        void shouldFindById() {
            User user = createTestUser();
            when(userRepository.findByTenantIdAndId(TENANT_ID, user.getId()))
                .thenReturn(Optional.of(user));

            Optional<User> result = userService.findById(TENANT_ID, user.getId());

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should check email existence")
        void shouldCheckEmailExists() {
            when(userRepository.existsByTenantIdAndEmail(TENANT_ID, VALID_EMAIL))
                .thenReturn(true);

            boolean exists = userService.emailExists(TENANT_ID, VALID_EMAIL);

            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("Password Change")
    class PasswordChangeTests {

        @Test
        @DisplayName("should change password successfully")
        void shouldChangePassword() {
            User user = createTestUser();
            String newPassword = "NewSecurePassword456!";
            String newHash = "v1$$argon2id$...newhash...";

            when(userRepository.findByTenantIdAndId(TENANT_ID, user.getId()))
                .thenReturn(Optional.of(user));
            when(passwordService.hashPassword(newPassword)).thenReturn(newHash);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            userService.changePassword(TENANT_ID, user.getId(), newPassword);

            verify(passwordService).validatePasswordPolicy(newPassword);
            verify(breachChecker).checkPassword(newPassword);
            verify(passwordService).hashPassword(newPassword);
            verify(userRepository).save(user);
            assertThat(user.getPasswordHash()).isEqualTo(newHash);
            assertThat(user.isMustChangePassword()).isFalse();
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findByTenantIdAndId(TENANT_ID, userId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword(TENANT_ID, userId, "newpass123456"))
                .isInstanceOf(UserService.UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Registration Command Validation")
    class CommandValidationTests {

        @Test
        @DisplayName("should reject null tenantId")
        void shouldRejectNullTenantId() {
            assertThatThrownBy(() -> new UserService.RegisterUserCommand(
                null, VALID_EMAIL, VALID_PASSWORD, FULL_NAME, ROLE_ID, null, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("tenantId");
        }

        @Test
        @DisplayName("should reject blank email")
        void shouldRejectBlankEmail() {
            assertThatThrownBy(() -> new UserService.RegisterUserCommand(
                TENANT_ID, "  ", VALID_PASSWORD, FULL_NAME, ROLE_ID, null, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("email");
        }

        @Test
        @DisplayName("should reject empty password")
        void shouldRejectEmptyPassword() {
            assertThatThrownBy(() -> new UserService.RegisterUserCommand(
                TENANT_ID, VALID_EMAIL, "", FULL_NAME, ROLE_ID, null, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("password");
        }

        @Test
        @DisplayName("should reject null roleId")
        void shouldRejectNullRoleId() {
            assertThatThrownBy(() -> new UserService.RegisterUserCommand(
                TENANT_ID, VALID_EMAIL, VALID_PASSWORD, FULL_NAME, null, null, null
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("roleId");
        }
    }

    @Nested
    @DisplayName("listUsers")
    class ListUsersTests {

        @Test
        @DisplayName("should return page with roleCode mapped correctly (no search)")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void shouldReturnPageWithRoleCode() {
            User user = createTestUser();
            Page<User> userPage = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);
            when(userRepository.findByTenantId(eq(TENANT_ID), any()))
                .thenReturn(userPage);
            when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(Map.entry(ROLE_ID, "CASHIER")));

            Page<UserResponse> result = userService.listUsers(TENANT_ID, null, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).roleCode()).isEqualTo("CASHIER");
            assertThat(result.getContent().get(0).email()).isEqualTo(VALID_EMAIL);
        }

        @Test
        @DisplayName("should use findByTenantId for blank search")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void shouldUseFindByTenantIdForBlankSearch() {
            when(userRepository.findByTenantId(eq(TENANT_ID), any()))
                .thenReturn(Page.empty());
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

            userService.listUsers(TENANT_ID, "   ", PageRequest.of(0, 20));

            verify(userRepository).findByTenantId(eq(TENANT_ID), any());
            verify(userRepository, never()).findByTenantIdAndSearch(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("should update fullName when provided")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void shouldUpdateFullName() {
            User user = createTestUser();
            when(userRepository.findByTenantIdAndId(TENANT_ID, user.getId())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

            UserUpdateAdminRequest request = new UserUpdateAdminRequest("New Name", null, null);
            userService.updateUser(TENANT_ID, user.getId(), UUID.randomUUID(), request);

            assertThat(user.getFullName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findByTenantIdAndId(TENANT_ID, unknownId)).thenReturn(Optional.empty());

            UserUpdateAdminRequest request = new UserUpdateAdminRequest("Name", null, null);

            assertThatThrownBy(() -> userService.updateUser(TENANT_ID, unknownId, UUID.randomUUID(), request))
                .isInstanceOf(UserService.UserNotFoundException.class);
        }

        @Test
        @DisplayName("should update roleId when provided")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void shouldUpdateRoleId() {
            UUID newRoleId = UUID.randomUUID();
            User user = createTestUser();
            when(userRepository.findByTenantIdAndId(TENANT_ID, user.getId())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

            UserUpdateAdminRequest request = new UserUpdateAdminRequest(null, null, newRoleId);
            userService.updateUser(TENANT_ID, user.getId(), UUID.randomUUID(), request);

            assertThat(user.getRoleId()).isEqualTo(newRoleId);
        }
    }

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUserTests {

        @Test
        @DisplayName("should soft delete user")
        void shouldSoftDeleteUser() {
            User user = createTestUser();
            UUID requestingId = UUID.randomUUID();
            when(userRepository.findByTenantIdAndId(TENANT_ID, user.getId())).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.deactivateUser(TENANT_ID, user.getId(), requestingId);

            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("should throw when user tries to delete themselves")
        void shouldThrowOnSelfDelete() {
            UUID userId = UUID.randomUUID();

            assertThatThrownBy(() -> userService.deactivateUser(TENANT_ID, userId, userId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot deactivate your own account");
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            UUID unknownId = UUID.randomUUID();
            UUID requestingId = UUID.randomUUID();
            when(userRepository.findByTenantIdAndId(TENANT_ID, unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivateUser(TENANT_ID, unknownId, requestingId))
                .isInstanceOf(UserService.UserNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTenantId(TENANT_ID);
        user.setEmail(VALID_EMAIL);
        user.setFullName(FULL_NAME);
        user.setPasswordHash(HASHED_PASSWORD);
        user.setRoleId(ROLE_ID);
        return user;
    }
}
