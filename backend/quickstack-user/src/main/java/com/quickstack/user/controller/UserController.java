package com.quickstack.user.controller;

import com.quickstack.common.dto.ApiResponse;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.user.dto.request.UserCreateAdminRequest;
import com.quickstack.user.dto.request.UserUpdateAdminRequest;
import com.quickstack.user.dto.response.UserResponse;
import com.quickstack.user.entity.User;
import com.quickstack.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for user management by OWNER.
 * <p>
 * Endpoints:
 * - GET    /api/v1/users         - List users (paginated, optional ?search=)
 * - POST   /api/v1/users         - Create user (OWNER only)
 * - PUT    /api/v1/users/{id}    - Update user profile (OWNER only)
 * - DELETE /api/v1/users/{id}    - Soft delete user (OWNER only, cannot self-delete)
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always from JWT, never from request
 * - V4.1: IDOR — cross-tenant returns 404
 * - V4.2: @PreAuthorize for OWNER-only operations
 */
@RestController
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lists users for the authenticated tenant. Requires OWNER role.
     */
    @GetMapping("/api/v1/users")
    @PreAuthorize("@userPermissionEvaluator.canManageUsers(authentication)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.debug("Listing users for tenant={}, search={}", principal.tenantId(), search);
        Page<UserResponse> page = userService.listUsers(principal.tenantId(), search, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Creates a new user. Requires OWNER role.
     */
    @PostMapping("/api/v1/users")
    @PreAuthorize("@userPermissionEvaluator.canManageUsers(authentication)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @Valid @RequestBody UserCreateAdminRequest request
    ) {
        log.info("[USER] ACTION=CREATE tenantId={} email={}", principal.tenantId(), request.email());

        var command = new UserService.RegisterUserCommand(
            principal.tenantId(),
            request.email(),
            request.password(),
            request.fullName(),
            request.roleId(),
            request.branchId(),
            request.phone()
        );

        User created = userService.registerUser(command);
        UserResponse response = userService.getUserResponse(principal.tenantId(), created.getId());

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * Updates a user. Requires OWNER role.
     */
    @PutMapping("/api/v1/users/{id}")
    @PreAuthorize("@userPermissionEvaluator.canManageUsers(authentication)")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID userId,
            @Valid @RequestBody UserUpdateAdminRequest request
    ) {
        log.info("[USER] ACTION=UPDATE_REQ tenantId={} userId={}", principal.tenantId(), userId);
        UserResponse response = userService.updateUser(
            principal.tenantId(), userId, principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Soft-deletes a user. Requires OWNER role. Cannot delete own account.
     */
    @DeleteMapping("/api/v1/users/{id}")
    @PreAuthorize("@userPermissionEvaluator.canManageUsers(authentication)")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable("id") UUID userId
    ) {
        log.info("[USER] ACTION=DELETE_REQ tenantId={} userId={}", principal.tenantId(), userId);
        userService.deactivateUser(principal.tenantId(), userId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
