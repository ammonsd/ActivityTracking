/**
 * Description: Authentication API functions
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import apiClient from "./axios.client";
import type { User } from "../types/auth.types";

export const authApi = {
    // Check if user is authenticated and get current user
    getCurrentUser: async (): Promise<User> => {
        const response = await apiClient.get<{
            success: boolean;
            data: any;
        }>("/users/me");
        const user = response.data.data;
        // Transform backend lowercase fields to camelCase
        return {
            ...user,
            firstName: user.firstname,
            lastName: user.lastname,
            fullName:
                user.firstname && user.lastname
                    ? `${user.firstname} ${user.lastname}`
                    : user.username,
        };
    },

    // Logout - calls Spring Boot session-based logout endpoint
    logout: async (): Promise<void> => {
        // Use relative URL to work in both local and AWS
        window.location.href = "/logout";
    },
};
