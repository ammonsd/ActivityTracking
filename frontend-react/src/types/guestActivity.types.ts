/**
 * Description: TypeScript types for Guest Activity feature
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

/**
 * Login audit record from backend API
 * Matches LoginAuditDto from Spring Boot backend
 */
export interface LoginAudit {
    id: number;
    username: string;
    loginTime: string; // ISO 8601 datetime string from backend
    ipAddress: string;
    location: string;
    successful: boolean;
}

/**
 * Calculated statistics for Guest Activity dashboard
 * Computed from login audit data
 */
export interface GuestActivityStats {
    totalLogins: number;
    uniqueLocations: number;
    lastLogin: string | null; // ISO 8601 datetime or null if no logins
    successRate: number; // Percentage (0-100)
}

/**
 * API response wrapper for login audit data
 */
export interface LoginAuditResponse {
    success: boolean;
    message: string;
    data: LoginAudit[];
}
