/**
 * Description: API functions for Roles Management feature
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import apiClient from "./axios.client";
import type {
    RoleDetail,
    Permission,
    RoleResponse,
    RoleSingleResponse,
    PermissionResponse,
    RoleCreateRequest,
    RoleUpdateRequest,
    DeleteResponse,
} from "../types/rolesManagement.types";

export const rolesManagementApi = {
    /**
     * Fetch all roles with their permissions
     * @returns Promise with array of role records
     */
    fetchRoles: async (): Promise<RoleDetail[]> => {
        const response = await apiClient.get<RoleResponse>("/roles");
        return response.data.data;
    },

    /**
     * Fetch a single role by ID with all permissions
     * @param id - Role ID to retrieve
     * @returns Promise with role details
     */
    fetchRoleById: async (id: number): Promise<RoleDetail> => {
        const response = await apiClient.get<RoleSingleResponse>(
            `/roles/${id}`,
        );
        return response.data.data;
    },

    /**
     * Fetch all available permissions in the system
     * @returns Promise with array of permission records
     */
    fetchPermissions: async (): Promise<Permission[]> => {
        const response =
            await apiClient.get<PermissionResponse>("/roles/permissions");
        return response.data.data;
    },

    /**
     * Create a new role with specified permissions
     * @param roleData - Role data for creation including name, description, and permission IDs
     * @returns Promise with the created role
     */
    createRole: async (roleData: RoleCreateRequest): Promise<RoleDetail> => {
        const response = await apiClient.post<RoleSingleResponse>(
            "/roles",
            roleData,
        );
        return response.data.data;
    },

    /**
     * Update an existing role's description and permissions
     * @param id - Role ID to update
     * @param roleData - Updated role data (description and permission IDs)
     * @returns Promise with the updated role
     */
    updateRole: async (
        id: number,
        roleData: RoleUpdateRequest,
    ): Promise<RoleDetail> => {
        const response = await apiClient.put<RoleSingleResponse>(
            `/roles/${id}`,
            roleData,
        );
        return response.data.data;
    },

    /**
     * Delete a role by ID
     * Note: Cannot delete roles that are currently assigned to users
     * @param id - Role ID to delete
     * @returns Promise that resolves when deletion is complete
     */
    deleteRole: async (id: number): Promise<void> => {
        await apiClient.delete<DeleteResponse>(`/roles/${id}`);
    },
};
