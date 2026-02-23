/**
 * Description: API functions for User Management feature
 *
 * Modified by: Dean Ammons - February 2026
 * Change: Added fetchNotifyEligibleUsers and sendUserNotifications functions
 * Reason: Support the Notify Users page in the React Admin Dashboard
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import apiClient from "./axios.client";
import type {
    User,
    Role,
    CurrentUser,
    UserFilters,
    UserResponse,
    RoleResponse,
    UserCreateRequest,
    UserUpdateRequest,
    UserAccessData,
    UserAccessUpdateRequest,
    NotifyEligibleUser,
    NotifyRequest,
    NotifyResult,
} from "../types/userManagement.types";

export const userManagementApi = {
    /**
     * Fetch current authenticated user
     * @returns Promise with current user data
     */
    fetchCurrentUser: async (): Promise<CurrentUser> => {
        const response = await apiClient.get<{
            success: boolean;
            message: string;
            data: CurrentUser;
        }>("/users/me");
        return response.data.data;
    },

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
    /**
     * Fetch dropdown access assignments for a user.
     * @param username - Username to retrieve access data for
     * @returns Promise with UserAccessData containing all items and assigned IDs
     */
    fetchUserAccess: async (username: string): Promise<UserAccessData> => {
        const response = await apiClient.get<{
            success: boolean;
            message: string;
            data: UserAccessData;
        }>(`/users/${encodeURIComponent(username)}/access`);
        return response.data.data;
    },

    /**
     * Save dropdown access assignments for a user.
     * @param username - Username whose access to update
     * @param payload - Access update request with view and selected IDs
     * @returns Promise that resolves when saved
     */
    saveUserAccess: async (
        username: string,
        payload: UserAccessUpdateRequest,
    ): Promise<void> => {
        await apiClient.put(
            `/users/${encodeURIComponent(username)}/access`,
            payload,
        );
    },

    /**
     * Fetch active users who have an email address, eligible for profile notification.
     * @param lastNameFilter - Optional last-name prefix to filter results
     * @returns Promise with list of eligible users
     */
    fetchNotifyEligibleUsers: async (
        lastNameFilter?: string,
    ): Promise<NotifyEligibleUser[]> => {
        const params: Record<string, string> = {};
        if (lastNameFilter?.trim()) {
            params.lastNameFilter = lastNameFilter.trim();
        }
        const response = await apiClient.get<{
            success: boolean;
            message: string;
            data: NotifyEligibleUser[];
        }>("/users/notify-eligible", { params });
        return response.data.data;
    },

    /**
     * Send profile notification emails to the selected users.
     * @param payload - Request containing the list of usernames to notify
     * @returns Promise with the notification result (sent/skipped counts)
     */
    sendUserNotifications: async (
        payload: NotifyRequest,
    ): Promise<NotifyResult> => {
        const response = await apiClient.post<{
            success: boolean;
            message: string;
            data: NotifyResult;
        }>("/users/notify", payload);
        return response.data.data;
    },
};