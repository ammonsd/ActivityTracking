/**
 * Feature configuration type definitions for the React admin dashboard.
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
}
