/**
 * Description: TypeScript types for Roles Management feature
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

/**
 * Permission data from backend API
 * Matches PermissionDto from Spring Boot backend
 */
export interface Permission {
    id: number;
    resource: string;
    action: string;
    description: string | null;
    permissionKey: string; // "RESOURCE:ACTION" format
}

/**
 * Role data from backend API with full permission details
 * Matches RoleDto from Spring Boot backend
 */
export interface RoleDetail {
    id: number;
    name: string;
    description: string | null;
    createdDate: string | null; // ISO 8601 datetime string
    permissions: Permission[];
}

/**
 * Simplified role data for list display
 */
export interface Role {
    id: number;
    name: string;
    description: string | null;
}

/**
 * Role creation request payload
 */
export interface RoleCreateRequest {
    name: string;
    description: string | null;
    permissionIds: number[];
}

/**
 * Role update request payload
 */
export interface RoleUpdateRequest {
    description: string | null;
    permissionIds: number[];
}

/**
 * API response wrapper for role data
 */
export interface RoleResponse {
    success: boolean;
    message: string;
    data: RoleDetail[];
    count?: number;
}

/**
 * API response wrapper for single role
 */
export interface RoleSingleResponse {
    success: boolean;
    message: string;
    data: RoleDetail;
}

/**
 * API response wrapper for permissions data
 */
export interface PermissionResponse {
    success: boolean;
    message: string;
    data: Permission[];
    count?: number;
}

/**
 * API response wrapper for delete operations
 */
export interface DeleteResponse {
    success: boolean;
    message: string;
    data: null;
}
