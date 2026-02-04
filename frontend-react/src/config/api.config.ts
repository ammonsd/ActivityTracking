/**
 * Description: API configuration for connecting to Spring Boot backend
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

// Use relative path in development (Vite proxy handles it)
// In production, this gets served from Spring Boot, so relative path works there too
export const API_BASE_URL =
    import.meta.env.VITE_API_URL || "/api";

export const API_ENDPOINTS = {
    auth: {
        login: "/auth/login",
        logout: "/auth/logout",
        me: "/users/me",
    },
    tasks: {
        list: "/tasks",
        create: "/tasks",
        update: (id: number) => `/tasks/${id}`,
        delete: (id: number) => `/tasks/${id}`,
    },
    expenses: {
        list: "/expenses",
        create: "/expenses",
        update: (id: number) => `/expenses/${id}`,
        delete: (id: number) => `/expenses/${id}`,
    },
    users: {
        list: "/users",
        create: "/users",
        update: (id: number) => `/users/${id}`,
        delete: (id: number) => `/users/${id}`,
    },
} as const;
