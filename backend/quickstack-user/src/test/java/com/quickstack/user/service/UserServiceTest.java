package com.quickstack.user.service;

import com.quickstack.common.exception.PasswordCompromisedException;
import com.quickstack.common.exception.PasswordValidationException;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        userService = new UserService(userRepository, passwordService, breachChecker);
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
