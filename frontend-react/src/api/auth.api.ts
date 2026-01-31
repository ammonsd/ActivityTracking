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
            data: User;
        }>("/users/me");
        return response.data.data;
    },

    // Logout - calls Spring Boot session-based logout endpoint
    logout: async (): Promise<void> => {
        // Use absolute URL to avoid Vite proxy (direct to Spring Boot)
        window.location.href = "http://localhost:8080/logout";
    },
};
