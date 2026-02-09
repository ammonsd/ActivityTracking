/**
 * Description: Utility functions for calculating Guest Activity statistics
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import type {
    LoginAudit,
    GuestActivityStats,
} from "../types/guestActivity.types";

/**
 * Calculate statistics from login audit data
 * @param loginAudits - Array of login audit records
 * @returns Calculated statistics object
 */
export const calculateGuestStats = (
    loginAudits: LoginAudit[],
): GuestActivityStats => {
    if (!loginAudits || loginAudits.length === 0) {
        return {
            totalLogins: 0,
            uniqueLocations: 0,
            lastLogin: null,
            successRate: 0,
        };
    }

    // Total logins
    const totalLogins = loginAudits.length;

    // Unique locations (distinct count)
    const uniqueLocations = new Set(loginAudits.map((audit) => audit.location))
        .size;

    // Last login (most recent - assuming data is already sorted by loginTime desc)
    const lastLogin = loginAudits[0]?.loginTime || null;

    // Success rate (percentage of successful logins)
    const successfulLogins = loginAudits.filter(
        (audit) => audit.successful,
    ).length;
    const successRate =
        totalLogins > 0 ? (successfulLogins / totalLogins) * 100 : 0;

    return {
        totalLogins,
        uniqueLocations,
        lastLogin,
        successRate: Math.round(successRate), // Round to nearest integer
    };
};

/**
 * Format date/time for display
 * @param isoDateString - ISO 8601 date string from backend
 * @returns Formatted date string (e.g., "Feb 9, 2026, 10:30 AM")
 */
export const formatDateTime = (isoDateString: string): string => {
    const date = new Date(isoDateString);
    return date.toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
    });
};

/**
 * Format date/time for display in compact spaces (without year)
 * @param isoDateString - ISO 8601 date string from backend
 * @returns Formatted date string (e.g., "Feb 9, 11:59 AM")
 */
export const formatDateTimeCompact = (isoDateString: string): string => {
    const date = new Date(isoDateString);
    return date.toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
    });
};

/**
 * Format date/time for display in table
 * @deprecated Use formatDateTime instead
 */
export const formatLoginTime = formatDateTime;

/**
 * Format deployment timestamp for display
 * @deprecated Use formatDateTime instead
 */
export const formatDeploymentTime = formatDateTime;
