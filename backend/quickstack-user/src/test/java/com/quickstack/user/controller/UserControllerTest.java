package com.quickstack.user.controller;

import com.quickstack.common.exception.ApiException;
import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.user.dto.request.UserCreateAdminRequest;
import com.quickstack.user.dto.request.UserUpdateAdminRequest;
import com.quickstack.user.dto.response.UserResponse;
import com.quickstack.user.entity.User;
import com.quickstack.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController.
 * <p>
 * Tests controller logic in isolation. Security enforcement is tested via
 * integration tests in quickstack-app.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController")
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController userController;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();
    private static final UUID TARGET_USER_ID = UUID.randomUUID();

    private JwtAuthenticationPrincipal ownerPrincipal;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
        ownerPrincipal = new JwtAuthenticationPrincipal(USER_ID, TENANT_ID, ROLE_ID, null, "owner@test.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listUsers")
    class ListUsersTests {

        @Test
        @DisplayName("should return 200 with page of users")
        void shouldReturn200WithUsers() {
            UserResponse response = buildUserResponse("cajero@test.com", "Juan García");
            Page<UserResponse> page = new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
            when(userService.listUsers(TENANT_ID, null, PageRequest.of(0, 20))).thenReturn(page);

            ResponseEntity<?> result = userController.listUsers(ownerPrincipal, null, PageRequest.of(0, 20));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
        }

        @Test
        @DisplayName("should use tenantId from JWT principal")
        void shouldUseTenantIdFromPrincipal() {
            when(userService.listUsers(eq(TENANT_ID), any(), any())).thenReturn(Page.empty());

            userController.listUsers(ownerPrincipal, null, PageRequest.of(0, 20));

            verify(userService).listUsers(eq(TENANT_ID), any(), any());
        }

        @Test
        @DisplayName("should pass search param to service")
        void shouldPassSearchParam() {
            when(userService.listUsers(eq(TENANT_ID), eq("Juan"), any())).thenReturn(Page.empty());

            userController.listUsers(ownerPrincipal, "Juan", PageRequest.of(0, 20));

            verify(userService).listUsers(TENANT_ID, "Juan", PageRequest.of(0, 20));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/users
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("should return 201 with created user")
        void shouldReturn201WithCreatedUser() {
            UserCreateAdminRequest request = new UserCreateAdminRequest(
                "cajero@test.com", "Juan García", "SecurePass1234", ROLE_ID, null, null);
            User createdUser = buildUser(TARGET_USER_ID);
            UserResponse response = buildUserResponse("cajero@test.com", "Juan García");

            when(userService.registerUser(any())).thenReturn(createdUser);
            when(userService.getUserResponse(TENANT_ID, TARGET_USER_ID)).thenReturn(response);

            ResponseEntity<?> result = userController.createUser(ownerPrincipal, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("should propagate EmailAlreadyExistsException as 409")
        void shouldPropagateEmailExists() {
            UserCreateAdminRequest request = new UserCreateAdminRequest(
                "existing@test.com", "Juan García", "SecurePass1234", ROLE_ID, null, null);
            when(userService.registerUser(any())).thenThrow(new UserService.EmailAlreadyExistsException());

            assertThatThrownBy(() -> userController.createUser(ownerPrincipal, request))
                .isInstanceOf(UserService.EmailAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should delegate with tenantId from JWT")
        void shouldDelegateWithTenantIdFromJwt() {
            UserCreateAdminRequest request = new UserCreateAdminRequest(
                "cajero@test.com", "Juan García", "SecurePass1234", ROLE_ID, null, null);
            User createdUser = buildUser(TARGET_USER_ID);
            UserResponse response = buildUserResponse("cajero@test.com", "Juan García");

            when(userService.registerUser(any())).thenReturn(createdUser);
            when(userService.getUserResponse(any(), any())).thenReturn(response);

            userController.createUser(ownerPrincipal, request);

            verify(userService).registerUser(argThat(cmd -> cmd.tenantId().equals(TENANT_ID)));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateUser")
    class UpdateUserTests {

        @Test
        @DisplayName("should return 200 with updated user")
        void shouldReturn200WithUpdatedUser() {
            UserUpdateAdminRequest request = new UserUpdateAdminRequest("Juan García Updated", null, null);
            UserResponse response = buildUserResponse("cajero@test.com", "Juan García Updated");
            when(userService.updateUser(TENANT_ID, TARGET_USER_ID, USER_ID, request)).thenReturn(response);

            ResponseEntity<?> result = userController.updateUser(ownerPrincipal, TARGET_USER_ID, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("should propagate UserNotFoundException as 404")
        void shouldPropagateUserNotFound() {
            UserUpdateAdminRequest request = new UserUpdateAdminRequest("Name", null, null);
            when(userService.updateUser(any(), any(), any(), any()))
                .thenThrow(new UserService.UserNotFoundException(TARGET_USER_ID));

            assertThatThrownBy(() -> userController.updateUser(ownerPrincipal, TARGET_USER_ID, request))
                .isInstanceOf(UserService.UserNotFoundException.class);
        }

        @Test
        @DisplayName("should delegate with tenantId and userId from JWT")
        void shouldDelegateWithCorrectIds() {
            UserUpdateAdminRequest request = new UserUpdateAdminRequest("Name", null, null);
            when(userService.updateUser(eq(TENANT_ID), eq(TARGET_USER_ID), eq(USER_ID), eq(request)))
                .thenReturn(buildUserResponse("cajero@test.com", "Name"));

            userController.updateUser(ownerPrincipal, TARGET_USER_ID, request);

            verify(userService).updateUser(TENANT_ID, TARGET_USER_ID, USER_ID, request);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("should return 204 on successful delete")
        void shouldReturn204OnDelete() {
            doNothing().when(userService).deactivateUser(TENANT_ID, TARGET_USER_ID, USER_ID);

            ResponseEntity<?> result = userController.deleteUser(ownerPrincipal, TARGET_USER_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("should propagate self-delete exception as 400")
        void shouldPropagateSelfDeleteException() {
            doThrow(new ApiException(HttpStatus.BAD_REQUEST, "SELF_DELETE", "Cannot deactivate your own account"))
                .when(userService).deactivateUser(TENANT_ID, USER_ID, USER_ID);

            assertThatThrownBy(() -> userController.deleteUser(ownerPrincipal, USER_ID))
                .isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("should delegate with correct tenantId and userId from JWT")
        void shouldDelegateWithCorrectIds() {
            doNothing().when(userService).deactivateUser(TENANT_ID, TARGET_USER_ID, USER_ID);

            userController.deleteUser(ownerPrincipal, TARGET_USER_ID);

            verify(userService).deactivateUser(TENANT_ID, TARGET_USER_ID, USER_ID);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private UserResponse buildUserResponse(String email, String fullName) {
        return new UserResponse(
            TARGET_USER_ID.toString(),
            email,
            fullName,
            null,
            TENANT_ID.toString(),
            ROLE_ID.toString(),
            "CASHIER",
            null,
            true,
            Instant.now()
        );
    }

    private User buildUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setTenantId(TENANT_ID);
        user.setEmail("cajero@test.com");
        user.setFullName("Juan García");
        user.setRoleId(ROLE_ID);
        return user;
    }
}
