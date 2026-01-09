package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Permission;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.PermissionRepository;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PermissionService. Tests the core permission checking logic for the
 * database-driven authorization system.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Tests")
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private User testUser;
    private Roles testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // Create test permission
        testPermission = new Permission("TASK_ACTIVITY", "CREATE");
        testPermission.setId(1L);

        // Create test role with permissions
        testRole = new Roles("USER");
        testRole.setId(1L);
        testRole.addPermission(testPermission);

        // Create test user with role
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(testRole);
    }

    @Nested
    @DisplayName("userHasPermission Tests")
    class UserHasPermissionTests {

        @Test
        @DisplayName("Should return true when user has the permission")
        void shouldReturnTrueWhenUserHasPermission() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            boolean result =
                    permissionService.userHasPermission("testuser", "TASK_ACTIVITY:CREATE");

            // Assert
            assertThat(result).isTrue();
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should return false when user does not have the permission")
        void shouldReturnFalseWhenUserDoesNotHavePermission() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = permissionService.userHasPermission("testuser", "EXPENSE:DELETE");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should return false when user not found")
        void shouldReturnFalseWhenUserNotFound() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act
            boolean result =
                    permissionService.userHasPermission("nonexistent", "TASK_ACTIVITY:CREATE");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should return false when user has no role")
        void shouldReturnFalseWhenUserHasNoRole() {
            // Arrange
            User userWithoutRole = new User();
            userWithoutRole.setUsername("noroleuser");
            userWithoutRole.setRole(null);
            when(userRepository.findByUsername("noroleuser"))
                    .thenReturn(Optional.of(userWithoutRole));

            // Act
            boolean result =
                    permissionService.userHasPermission("noroleuser", "TASK_ACTIVITY:CREATE");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername("noroleuser");
        }

        @Test
        @DisplayName("Should return false when permission key is invalid format")
        void shouldReturnFalseWhenPermissionKeyInvalid() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = permissionService.userHasPermission("testuser", "INVALID_FORMAT");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should handle permission key with too many colons")
        void shouldHandlePermissionKeyWithTooManyColons() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            boolean result =
                    permissionService.userHasPermission("testuser", "RESOURCE:ACTION:EXTRA");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should handle empty permission key")
        void shouldHandleEmptyPermissionKey() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            boolean result = permissionService.userHasPermission("testuser", "");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername("testuser");
        }
    }

    @Nested
    @DisplayName("getUserPermissions Tests")
    class GetUserPermissionsTests {

        @Test
        @DisplayName("Should return all permissions for user")
        void shouldReturnAllPermissionsForUser() {
            // Arrange
            Permission permission2 = new Permission("TASK_ACTIVITY", "READ");
            permission2.setId(2L);
            testRole.addPermission(permission2);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            Set<Permission> permissions = permissionService.getUserPermissions("testuser");

            // Assert
            assertThat(permissions).hasSize(2);
            assertThat(permissions).extracting(Permission::getPermissionKey)
                    .containsExactlyInAnyOrder("TASK_ACTIVITY:CREATE", "TASK_ACTIVITY:READ");
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should return empty set when user not found")
        void shouldReturnEmptySetWhenUserNotFound() {
            // Arrange
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // Act
            Set<Permission> permissions = permissionService.getUserPermissions("nonexistent");

            // Assert
            assertThat(permissions).isEmpty();
            verify(userRepository).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("Should return empty set when user has no role")
        void shouldReturnEmptySetWhenUserHasNoRole() {
            // Arrange
            User userWithoutRole = new User();
            userWithoutRole.setUsername("noroleuser");
            userWithoutRole.setRole(null);
            when(userRepository.findByUsername("noroleuser"))
                    .thenReturn(Optional.of(userWithoutRole));

            // Act
            Set<Permission> permissions = permissionService.getUserPermissions("noroleuser");

            // Assert
            assertThat(permissions).isEmpty();
            verify(userRepository).findByUsername("noroleuser");
        }
    }

    @Nested
    @DisplayName("roleHasPermission Tests")
    class RoleHasPermissionTests {

        @Test
        @DisplayName("Should return true when role has permission")
        void shouldReturnTrueWhenRoleHasPermission() {
            // Arrange
            when(roleRepository.roleHasPermission(1L, "TASK_ACTIVITY", "CREATE")).thenReturn(true);

            // Act
            boolean result = permissionService.roleHasPermission(1L, "TASK_ACTIVITY", "CREATE");

            // Assert
            assertThat(result).isTrue();
            verify(roleRepository).roleHasPermission(1L, "TASK_ACTIVITY", "CREATE");
        }

        @Test
        @DisplayName("Should return false when role does not have permission")
        void shouldReturnFalseWhenRoleDoesNotHavePermission() {
            // Arrange
            when(roleRepository.roleHasPermission(1L, "EXPENSE", "DELETE")).thenReturn(false);

            // Act
            boolean result = permissionService.roleHasPermission(1L, "EXPENSE", "DELETE");

            // Assert
            assertThat(result).isFalse();
            verify(roleRepository).roleHasPermission(1L, "EXPENSE", "DELETE");
        }
    }

    @Nested
    @DisplayName("Multiple Permissions Tests")
    class MultiplePermissionsTests {

        @Test
        @DisplayName("Should correctly handle admin role with multiple permissions")
        void shouldHandleAdminRoleWithMultiplePermissions() {
            // Arrange
            Roles adminRole = new Roles("ADMIN");
            adminRole.setId(2L);
            adminRole.addPermission(new Permission("TASK_ACTIVITY", "CREATE"));
            adminRole.addPermission(new Permission("TASK_ACTIVITY", "READ"));
            adminRole.addPermission(new Permission("TASK_ACTIVITY", "UPDATE"));
            adminRole.addPermission(new Permission("TASK_ACTIVITY", "DELETE"));
            adminRole.addPermission(new Permission("EXPENSE", "APPROVE"));

            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setRole(adminRole);

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

            // Act & Assert
            assertThat(permissionService.userHasPermission("admin", "TASK_ACTIVITY:CREATE"))
                    .isTrue();
            assertThat(permissionService.userHasPermission("admin", "TASK_ACTIVITY:READ")).isTrue();
            assertThat(permissionService.userHasPermission("admin", "TASK_ACTIVITY:UPDATE"))
                    .isTrue();
            assertThat(permissionService.userHasPermission("admin", "TASK_ACTIVITY:DELETE"))
                    .isTrue();
            assertThat(permissionService.userHasPermission("admin", "EXPENSE:APPROVE")).isTrue();
            assertThat(permissionService.userHasPermission("admin", "USER_MANAGEMENT:DELETE"))
                    .isFalse();

            verify(userRepository, times(6)).findByUsername("admin");
        }

        @Test
        @DisplayName("Should correctly handle guest role with limited permissions")
        void shouldHandleGuestRoleWithLimitedPermissions() {
            // Arrange
            Roles guestRole = new Roles("GUEST");
            guestRole.setId(3L);
            guestRole.addPermission(new Permission("TASK_ACTIVITY", "READ"));

            User guestUser = new User();
            guestUser.setUsername("guest");
            guestUser.setRole(guestRole);

            when(userRepository.findByUsername("guest")).thenReturn(Optional.of(guestUser));

            // Act & Assert
            assertThat(permissionService.userHasPermission("guest", "TASK_ACTIVITY:READ")).isTrue();
            assertThat(permissionService.userHasPermission("guest", "TASK_ACTIVITY:CREATE"))
                    .isFalse();
            assertThat(permissionService.userHasPermission("guest", "EXPENSE:READ")).isFalse();

            verify(userRepository, times(3)).findByUsername("guest");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null username")
        void shouldHandleNullUsername() {
            // Arrange
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

            // Act
            boolean result = permissionService.userHasPermission(null, "TASK_ACTIVITY:CREATE");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername(null);
        }

        @Test
        @DisplayName("Should handle null permission key")
        void shouldHandleNullPermissionKey() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> permissionService.userHasPermission("testuser", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should be case-sensitive for permission resource")
        void shouldBeCaseSensitiveForResource() {
            // Arrange
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // Act
            boolean upperCase =
                    permissionService.userHasPermission("testuser", "TASK_ACTIVITY:CREATE");
            boolean lowerCase =
                    permissionService.userHasPermission("testuser", "task_activity:create");

            // Assert
            assertThat(upperCase).isTrue();
            assertThat(lowerCase).isFalse();
        }

        @Test
        @DisplayName("Should handle role with no permissions")
        void shouldHandleRoleWithNoPermissions() {
            // Arrange
            Roles emptyRole = new Roles("EMPTY");
            emptyRole.setId(4L);
            // No permissions added

            User userWithEmptyRole = new User();
            userWithEmptyRole.setUsername("emptyuser");
            userWithEmptyRole.setRole(emptyRole);

            when(userRepository.findByUsername("emptyuser"))
                    .thenReturn(Optional.of(userWithEmptyRole));

            // Act
            boolean result =
                    permissionService.userHasPermission("emptyuser", "TASK_ACTIVITY:CREATE");

            // Assert
            assertThat(result).isFalse();
            verify(userRepository).findByUsername("emptyuser");
        }
    }
}
