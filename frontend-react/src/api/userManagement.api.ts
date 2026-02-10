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
};
