/**
 * Description: API functions for User Management feature
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import apiClient from "./axios.client";
import type {
    User,
    Role,
    UserFilters,
    UserResponse,
    RoleResponse,
    UserCreateRequest,
    UserUpdateRequest,
} from "../types/userManagement.types";

export const userManagementApi = {
    /**
     * Fetch users with optional filters
     * @param filters - Optional filters for username, role, and company
     * @returns Promise with array of user records
     */
    fetchUsers: async (filters?: UserFilters): Promise<User[]> => {
        const params: Record<string, string> = {};

        if (filters?.username) {
            params.username = filters.username;
        }
        if (filters?.role) {
            params.role = filters.role;
        }
        if (filters?.company) {
            params.company = filters.company;
        }

        const response = await apiClient.get<UserResponse>("/users", {
            params,
        });
        return response.data.data;
    },

    /**
     * Fetch all available roles for filtering
     * @returns Promise with array of role records
     */
    fetchRoles: async (): Promise<Role[]> => {
        const response = await apiClient.get<RoleResponse>("/users/roles");
        return response.data.data;
    },

    /**
     * Create a new user
     * @param userData - User data for creation
     * @returns Promise with the created user
     */
    createUser: async (userData: UserCreateRequest): Promise<User> => {
        const response = await apiClient.post<{
            success: boolean;
            message: string;
            data: User;
        }>("/users", userData);
        return response.data.data;
    },

    /**
     * Update an existing user
     * @param id - User ID to update
     * @param userData - Updated user data
     * @returns Promise with the updated user
     */
    updateUser: async (
        id: number,
        userData: UserUpdateRequest,
    ): Promise<User> => {
        const response = await apiClient.put<{
            success: boolean;
            message: string;
            data: User;
        }>(`/users/${id}`, userData);
        return response.data.data;
    },

    /**
     * Delete a user
     * @param id - User ID to delete
     * @returns Promise that resolves when deletion is complete
     */
    deleteUser: async (id: number): Promise<void> => {
        await apiClient.delete(`/users/${id}`);
    },

    /**
     * Change user password (admin function)
     * @param id - User ID whose password to change
     * @param newPassword - New password to set
     * @param forcePasswordUpdate - Whether to force password change on next login
     * @returns Promise that resolves when password is changed
     */
    changePassword: async (
        id: number,
        newPassword: string,
        forcePasswordUpdate: boolean,
    ): Promise<void> => {
        await apiClient.put(`/users/${id}/password`, {
            newPassword,
            forcePasswordUpdate,
        });
    },
};