/**
 * Description: TypeScript types for User Management feature
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

/**
 * User data from backend API
 * Matches UserDto from Spring Boot backend
 */
export interface User {
    id: number;
    username: string;
    firstname: string | null;
    lastname: string | null;
    company: string | null;
    email: string | null;
    role: string; // Role name as string (e.g., "ADMIN", "USER", "GUEST", "EXPENSE_ADMIN")
    enabled: boolean;
    forcePasswordUpdate: boolean;
    expirationDate: string | null; // ISO 8601 date string
    createdDate: string | null; // ISO 8601 datetime string
    lastLogin: string | null; // ISO 8601 datetime string
    failedLoginAttempts: number;
    accountLocked: boolean;
}

/**
 * Role data from backend API
 * Simplified version of Roles entity
 */
export interface Role {
    id: number;
    name: string; // Role name (e.g., "ADMIN", "USER", "GUEST", "EXPENSE_ADMIN")
    description: string | null;
}

/**
 * Filter parameters for user search
 */
export interface UserFilters {
    username?: string;
    role?: string;
    company?: string;
}

/**
 * API response wrapper for user data
 */
export interface UserResponse {
    success: boolean;
    message: string;
    data: User[];
    count?: number;
}

/**
 * API response wrapper for role data
 */
export interface RoleResponse {
    success: boolean;
    message: string;
    data: Role[];
}

/**
 * User creation request payload
 */
export interface UserCreateRequest {
    username: string;
    password: string;
    firstname: string | null;
    lastname: string;
    company: string | null;
    email: string | null;
    role: {
        id: number;
        name: string;
    };
    enabled: boolean;
    forcePasswordUpdate: boolean;
}

/**
 * User update request payload
 */
export interface UserUpdateRequest {
    username: string;
    firstname: string | null;
    lastname: string;
    company: string | null;
    email: string | null;
    role: {
        id: number;
        name: string;
    };
    enabled: boolean;
    accountLocked: boolean;
    forcePasswordUpdate: boolean;
}
