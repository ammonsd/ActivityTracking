/**
 * Description: Zustand store for authentication state management
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { create } from "zustand";
import type { AuthState, User } from "../types/auth.types";
import { authApi } from "../api/auth.api";

interface AuthActions {
    setUser: (user: User | null) => void;
    setLoading: (loading: boolean) => void;
    setError: (error: string | null) => void;
    checkAuth: () => Promise<void>;
    logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState & AuthActions>((set) => ({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,

    setUser: (user) =>
        set({
            user,
            isAuthenticated: !!user,
            error: null,
        }),

    setLoading: (isLoading) => set({ isLoading }),

    setError: (error) => set({ error, isLoading: false }),

    checkAuth: async () => {
        try {
            set({ isLoading: true, error: null });
            const user = await authApi.getCurrentUser();
            set({
                user,
                isAuthenticated: true,
                isLoading: false,
            });
        } catch (error: any) {
            set({
                user: null,
                isAuthenticated: false,
                isLoading: false,
                error: error.message || "Authentication failed",
            });
            // If 401, axios interceptor will redirect to login
        }
    },

    logout: async () => {
        try {
            await authApi.logout();
            set({
                user: null,
                isAuthenticated: false,
                error: null,
            });
        } catch (error: any) {
            set({ error: error.message || "Logout failed" });
        }
    },
}));
