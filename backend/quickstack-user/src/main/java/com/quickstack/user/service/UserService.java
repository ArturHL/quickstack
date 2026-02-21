package com.quickstack.user.service;

import com.quickstack.common.exception.ApiException;
import com.quickstack.common.security.PasswordBreachChecker;
import com.quickstack.common.security.PasswordService;
import com.quickstack.user.entity.User;
import com.quickstack.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for user management operations.
 * <p>
 * Handles user registration, lookup, and management with:
 * - Multi-tenant data isolation
 * - Secure password handling
 * - Transaction management
 * <p>
 * ASVS Compliance:
 * - V2.1.1: Minimum 12 character passwords (via PasswordService)
 * - V2.1.7: Breach detection (via PasswordBreachChecker)
 * - V2.4.1: Argon2id password storage (via PasswordService)
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final PasswordBreachChecker breachChecker;

    public UserService(
            UserRepository userRepository,
            PasswordService passwordService,
            PasswordBreachChecker breachChecker) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.breachChecker = breachChecker;
    }

    /**
     * Register a new user.
     * <p>
     * Validates:
     * - Email uniqueness within tenant
     * - Password policy (length)
     * - Password not in breach database
     *
     * @param command registration data
     * @return the created user
     * @throws ApiException if validation fails
     */
    @Transactional
    public User registerUser(RegisterUserCommand command) {
        log.info("Registering user: tenantId={}, email={}",
            command.tenantId(), maskEmail(command.email()));

        // Validate email uniqueness within tenant
        if (userRepository.existsByTenantIdAndEmail(command.tenantId(), command.email())) {
            log.warn("Registration failed: email already exists in tenant");
            throw new EmailAlreadyExistsException();
        }

        // Validate password policy (length, not compromised)
        passwordService.validatePasswordPolicy(command.password());
        breachChecker.checkPassword(command.password());

        // Hash password with Argon2id + pepper
        String passwordHash = passwordService.hashPassword(command.password());

        // Create user entity
        User user = new User();
        user.setTenantId(command.tenantId());
        user.setEmail(command.email().toLowerCase().trim());
        user.setFullName(command.fullName());
        user.setPasswordHash(passwordHash);
        user.setRoleId(command.roleId());
        user.setBranchId(command.branchId());
        user.setPhone(command.phone());

        // Save and return
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={}", savedUser.getId());

        return savedUser;
    }

    /**
     * Find user by email within a tenant.
     */
    public Optional<User> findByEmail(UUID tenantId, String email) {
        return userRepository.findByTenantIdAndEmail(tenantId, email.toLowerCase().trim());
    }

    /**
     * Find user by ID within a tenant.
     */
    public Optional<User> findById(UUID tenantId, UUID userId) {
        return userRepository.findByTenantIdAndId(tenantId, userId);
    }

    /**
     * Check if email exists within a tenant.
     */
    public boolean emailExists(UUID tenantId, String email) {
        return userRepository.existsByTenantIdAndEmail(tenantId, email.toLowerCase().trim());
    }

    /**
     * Update user password.
     * <p>
     * Validates new password and updates hash.
     *
     * @param tenantId the tenant
     * @param userId the user ID
     * @param newPassword the new password (plaintext)
     * @throws ApiException if user not found or password invalid
     */
    @Transactional
    public void changePassword(UUID tenantId, UUID userId, String newPassword) {
        User user = userRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Validate new password
        passwordService.validatePasswordPolicy(newPassword);
        breachChecker.checkPassword(newPassword);

        // Hash and update
        String passwordHash = passwordService.hashPassword(newPassword);
        user.setPasswordHash(passwordHash);
        user.setMustChangePassword(false);

        userRepository.save(user);
        log.info("Password changed for user: userId={}", userId);
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private String maskEmail(String email) {
        if (email == null || email.length() < 4) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 2) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    // -------------------------------------------------------------------------
    // Command/Exception classes
    // -------------------------------------------------------------------------

    /**
     * Command for user registration.
     */
    public record RegisterUserCommand(
        UUID tenantId,
        String email,
        String password,
        String fullName,
        UUID roleId,
        UUID branchId,
        String phone
    ) {
        public RegisterUserCommand {
            if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
            if (email == null || email.isBlank()) throw new IllegalArgumentException("email is required");
            if (password == null || password.isEmpty()) throw new IllegalArgumentException("password is required");
            if (fullName == null || fullName.isBlank()) throw new IllegalArgumentException("fullName is required");
            if (roleId == null) throw new IllegalArgumentException("roleId is required");
        }
    }

    /**
     * Exception for duplicate email within tenant.
     */
    public static class EmailAlreadyExistsException extends ApiException {
        public EmailAlreadyExistsException() {
            super(HttpStatus.CONFLICT, "EMAIL_EXISTS", "An account with this email already exists");
        }
    }

    /**
     * Exception for user not found.
     */
    public static class UserNotFoundException extends ApiException {
        public UserNotFoundException(UUID userId) {
            super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found");
        }
    }
}
