package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.entity.Permission;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.repository.PermissionRepository;
import com.ammons.taskactivity.repository.RoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PermissionManagementController. Tests role and permission management
 * endpoints with different user roles.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("PermissionManagementController Integration Tests")
class PermissionManagementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Roles testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // Use existing USER role from test-data.sql (ID = 2) for testing
        // USER role has limited permissions, good for testing grant/revoke
        testRole = roleRepository.findById(2L)
                .orElseThrow(() -> new RuntimeException("USER role not found in test data"));

        // Use a permission that USER role doesn't have by default
        // Using APPROVE permission for EXPENSE (ID = 12) which only ADMIN and EXPENSE_ADMIN have
        testPermission = permissionRepository.findById(12L).orElseThrow(() -> new RuntimeException(
                "APPROVE permission for EXPENSE not found in test data"));
    }

    @Nested
    @DisplayName("GET /api/admin/permissions/roles")
    class GetAllRolesTests {

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should return all roles for admin user")
        void shouldReturnAllRolesForAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/permissions/roles")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Roles retrieved"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
        }

        @Test
        @WithUserDetails("guest") // GUEST role doesn't have USER_MANAGEMENT:READ permission
        @DisplayName("Should deny access for user without permission")
        void shouldDenyAccessForUserWithoutPermission() throws Exception {
            mockMvc.perform(get("/api/admin/permissions/roles")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny access for unauthenticated request")
        void shouldDenyAccessForUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/admin/permissions/roles"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/permissions/permissions")
    class GetAllPermissionsTests {

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should return all permissions for admin user")
        void shouldReturnAllPermissionsForAdmin() throws Exception {
            mockMvc.perform(get("/api/admin/permissions/permissions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Permissions retrieved"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
        }

        @Test
        @WithUserDetails("guest") // GUEST role doesn't have USER_MANAGEMENT:READ permission
        @DisplayName("Should deny access for user without permission")
        void shouldDenyAccessForRegularUser() throws Exception {
            mockMvc.perform(get("/api/admin/permissions/permissions"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/permissions/roles/{roleId}/permissions/{permissionId}")
    class GrantPermissionTests {

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should grant permission to role for admin")
        void shouldGrantPermissionToRole() throws Exception {
            mockMvc.perform(post("/api/admin/permissions/roles/" + testRole.getId()
                    + "/permissions/" + testPermission.getId())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Permission granted"))
                    .andExpect(jsonPath("$.data.id").value(testRole.getId()))
                    .andExpect(jsonPath("$.data.name").value("USER"));
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Should deny permission grant for regular user")
        void shouldDenyPermissionGrantForUser() throws Exception {
            mockMvc.perform(post("/api/admin/permissions/roles/" + testRole.getId()
                    + "/permissions/" + testPermission.getId())).andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should return error for non-existent role")
        void shouldReturnErrorForNonExistentRole() throws Exception {
            mockMvc.perform(post(
                    "/api/admin/permissions/roles/99999/permissions/" + testPermission.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should return error for non-existent permission")
        void shouldReturnErrorForNonExistentPermission() throws Exception {
            mockMvc.perform(
                    post("/api/admin/permissions/roles/" + testRole.getId() + "/permissions/99999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/permissions/roles/{roleId}/permissions/{permissionId}")
    class RevokePermissionTests {

        @BeforeEach
        void setUpPermission() {
            // Ensure the test role has the test permission for revocation tests
            // ADMIN role (ID=1) should already have most permissions, but we'll ensure this one is
            // present
            if (!testRole.getPermissions().contains(testPermission)) {
                testRole.addPermission(testPermission);
                roleRepository.save(testRole);
            }
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should revoke permission from role for admin")
        void shouldRevokePermissionFromRole() throws Exception {
            mockMvc.perform(delete("/api/admin/permissions/roles/" + testRole.getId()
                    + "/permissions/" + testPermission.getId())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Permission revoked"))
                    .andExpect(jsonPath("$.data.id").value(testRole.getId()));
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Should deny permission revocation for regular user")
        void shouldDenyPermissionRevocationForUser() throws Exception {
            mockMvc.perform(delete("/api/admin/permissions/roles/" + testRole.getId()
                    + "/permissions/" + testPermission.getId())).andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should handle revocation of non-existent permission gracefully")
        void shouldHandleNonExistentPermissionRevocation() throws Exception {
            mockMvc.perform(delete(
                    "/api/admin/permissions/roles/" + testRole.getId() + "/permissions/99999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Role Combination Tests")
    class RoleCombinationTests {

        @Test
        @WithUserDetails("guest")
        @DisplayName("Guest role should be denied access to all permission management")
        void guestShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/admin/permissions/roles")).andExpect(status().isForbidden());

            mockMvc.perform(get("/api/admin/permissions/permissions"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/permissions/roles/" + testRole.getId()
                    + "/permissions/" + testPermission.getId())).andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("expenseadmin")
        @DisplayName("Expense admin should be denied access without USER_MANAGEMENT permission")
        void expenseAdminShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/admin/permissions/roles")).andExpect(status().isForbidden());
        }
    }
}
