/**
 * Feature configuration type definitions for the React admin dashboard.
 *
 * Modified by: Dean Ammons - February 2026
 * Change: Added adminAnalytics feature entry
 * Reason: New Analytics & Reports feature for admin-only user analysis reporting
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

export interface FeatureConfig {
    enabled: boolean;
    comingSoon: boolean;
    requiresAdmin: boolean;
    description: string;
    icon: string;
    route: string;
}

export interface FeaturesConfiguration {
    userManagement: FeatureConfig;
    dropdownManagement: FeatureConfig;
    rolesManagement: FeatureConfig;
    guestActivity: FeatureConfig;
    taskTracker: FeatureConfig;
    notifyUsers: FeatureConfig;
    adminAnalytics: FeatureConfig;
}
