/**
 * Feature flags configuration for the React admin dashboard.
 * Controls which features are enabled vs. mocked with "Coming Soon" dialogs.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import type { FeaturesConfiguration } from "../types/features.types";

export const FEATURES: FeaturesConfiguration = {
    userManagement: {
        enabled: false,
        comingSoon: true,
        requiresAdmin: true,
        description: "Manage users, roles, and permissions",
        icon: "People",
        route: "/user-management",
    },
    dropdownManagement: {
        enabled: false,
        comingSoon: true,
        requiresAdmin: true,
        description: "Manage dropdown values for tasks and expenses",
        icon: "List",
        route: "/dropdown-management",
    },
    rolesManagement: {
        enabled: false,
        comingSoon: true,
        requiresAdmin: true,
        description: "Manage roles and permissions",
        icon: "Security",
        route: "/roles-management",
    },
    guestActivity: {
        enabled: true,
        comingSoon: false,
        requiresAdmin: true,
        description: "View guest login activity and statistics",
        icon: "Timeline",
        route: "/guest-activity",
    },
    taskTracker: {
        enabled: true,
        comingSoon: false,
        requiresAdmin: false,
        description: "Track daily tasks and activities",
        icon: "Assignment",
        route: "/tasks", // This will open Spring Boot UI
    },
};
