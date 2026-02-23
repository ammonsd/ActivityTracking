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
    hasTasks: boolean; // True if user has task activities (affects delete permission)
}

/**
 * Current user data from /api/users/me endpoint
 * Used to identify the logged-in user
 */
export interface CurrentUser {
    id: number;
    username: string;
    firstname: string | null;
    lastname: string | null;
    company: string | null;
    email: string | null;
    enabled: boolean;
    passwordExpiringWarning?: string | null;
    daysUntilExpiration?: number | null;
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

/**
 * Response from GET /api/users/{username}/access.
 * Contains all available dropdown items and the IDs explicitly assigned to this user.
 */
export interface UserAccessData {
    allClients: import('./dropdown.types').DropdownValue[];
    allProjects: import('./dropdown.types').DropdownValue[];
    allExpenseClients: import('./dropdown.types').DropdownValue[];
    allExpenseProjects: import('./dropdown.types').DropdownValue[];
    assignedIds: number[];
}

/**
 * Request body for PUT /api/users/{username}/access.
 */
export interface UserAccessUpdateRequest {
    view: 'TASK' | 'EXPENSE';
    clientIds: number[];
    projectIds: number[];
    expenseClientIds: number[];
    expenseProjectIds: number[];
}

/**
 * A user eligible to receive a profile notification email.
 * Matches NotifyEligibleUserDto from the backend.
 */
export interface NotifyEligibleUser {
    username: string;
    firstname: string | null;
    lastname: string | null;
    email: string | null;
    company: string | null;
}

/**
 * Request body for POST /api/users/notify.
 */
export interface NotifyRequest {
    usernames: string[];
}

/**
 * Result returned by POST /api/users/notify.
 */
export interface NotifyResult {
    sent: number;
    skipped: number;
    mailEnabled: boolean;
}
