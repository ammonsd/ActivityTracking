/**
 * Navigation sidebar component with feature buttons.
 * Shows all features with appropriate enabled/disabled states based on user role.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { useState } from "react";
import {
    Drawer,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Tooltip,
    Toolbar,
    Typography,
    Box,
} from "@mui/material";
import {
    People as PeopleIcon,
    List as ListIcon,
    Security as SecurityIcon,
    Timeline as TimelineIcon,
    Assignment as AssignmentIcon,
    Home as HomeIcon,
    Dashboard as DashboardIcon,
} from "@mui/icons-material";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import { FEATURES } from "../../config/features";
import { ComingSoonDialog } from "../common/ComingSoonDialog";

const DRAWER_WIDTH = 260;

interface MenuItem {
    name: string;
    icon: React.ReactElement;
    route: string;
    requiresAdmin: boolean;
    comingSoon: boolean;
    enabled: boolean;
}

export const Sidebar: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { user } = useAuthStore();
    const isAdmin = user?.role === "ADMIN";

    const [comingSoonOpen, setComingSoonOpen] = useState(false);
    const [selectedFeature, setSelectedFeature] = useState("");

    const menuItems: MenuItem[] = [
        {
            name: "Dashboard Home",
            icon: <HomeIcon />,
            route: "/",
            requiresAdmin: false,
            comingSoon: false,
            enabled: true,
        },
        {
            name: "User Management",
            icon: <PeopleIcon />,
            route: FEATURES.userManagement.route,
            requiresAdmin: FEATURES.userManagement.requiresAdmin,
            comingSoon: FEATURES.userManagement.comingSoon,
            enabled: FEATURES.userManagement.enabled,
        },
        {
            name: "Dropdown Management",
            icon: <ListIcon />,
            route: FEATURES.dropdownManagement.route,
            requiresAdmin: FEATURES.dropdownManagement.requiresAdmin,
            comingSoon: FEATURES.dropdownManagement.comingSoon,
            enabled: FEATURES.dropdownManagement.enabled,
        },
        {
            name: "Roles Management",
            icon: <SecurityIcon />,
            route: FEATURES.rolesManagement.route,
            requiresAdmin: FEATURES.rolesManagement.requiresAdmin,
            comingSoon: FEATURES.rolesManagement.comingSoon,
            enabled: FEATURES.rolesManagement.enabled,
        },
        {
            name: "Guest Activity",
            icon: <TimelineIcon />,
            route: FEATURES.guestActivity.route,
            requiresAdmin: FEATURES.guestActivity.requiresAdmin,
            comingSoon: FEATURES.guestActivity.comingSoon,
            enabled: FEATURES.guestActivity.enabled,
        },
        {
            name: "User Dashboard",
            icon: <DashboardIcon />,
            route: "/app",
            requiresAdmin: false,
            comingSoon: false,
            enabled: true,
        },
        {
            name: "Task Activity Tracker",
            icon: <AssignmentIcon />,
            route: FEATURES.taskTracker.route,
            requiresAdmin: FEATURES.taskTracker.requiresAdmin,
            comingSoon: FEATURES.taskTracker.comingSoon,
            enabled: FEATURES.taskTracker.enabled,
        },
    ];

    const handleMenuItemClick = (item: MenuItem) => {
        // Task Tracker opens Spring Boot UI in same tab
        if (item.name === "Task Activity Tracker") {
            globalThis.location.href = "/task-activity/list";
            return;
        }

        // User Dashboard opens Angular app in same tab
        if (item.name === "User Dashboard") {
            globalThis.location.href = "/app";
            return;
        }

        // Check if feature requires admin access
        if (item.requiresAdmin && !isAdmin) {
            return; // Button is disabled, do nothing
        }

        // Check if feature is coming soon
        if (item.comingSoon) {
            setSelectedFeature(item.name);
            setComingSoonOpen(true);
            return;
        }

        // Navigate to the route
        navigate(item.route);
    };

    const isButtonDisabled = (item: MenuItem): boolean => {
        // Dashboard Home is always enabled
        if (item.name === "Dashboard Home") return false;

        // User Dashboard is always enabled for all roles
        if (item.name === "User Dashboard") return false;

        // Task Tracker is always enabled for all roles
        if (item.name === "Task Activity Tracker") return false;

        // Admin gets access to all features (but may show coming soon dialog)
        if (isAdmin) return false;

        // Non-admin users: management buttons are disabled
        if (item.requiresAdmin) return true;

        return false;
    };

    const getTooltipText = (item: MenuItem): string => {
        if (item.requiresAdmin && !isAdmin) {
            return "Admin access required. Contact your administrator for elevated permissions.";
        }
        return "";
    };

    return (
        <>
            <Drawer
                variant="permanent"
                sx={{
                    width: DRAWER_WIDTH,
                    flexShrink: 0,
                    "& .MuiDrawer-paper": {
                        width: DRAWER_WIDTH,
                        boxSizing: "border-box",
                    },
                }}
            >
                <Toolbar />
                <Box sx={{ overflow: "auto" }}>
                    <List>
                        <ListItem>
                            <Typography
                                variant="overline"
                                color="text.secondary"
                                sx={{ fontWeight: "bold" }}
                            >
                                Navigation
                            </Typography>
                        </ListItem>
                        {menuItems.map((item) => {
                            const isDisabled = isButtonDisabled(item);
                            const isSelected = location.pathname === item.route;
                            const tooltipText = getTooltipText(item);

                            return (
                                <Tooltip
                                    key={item.name}
                                    title={tooltipText}
                                    placement="right"
                                    arrow
                                >
                                    <span>
                                        <ListItem disablePadding>
                                            <ListItemButton
                                                onClick={() =>
                                                    handleMenuItemClick(item)
                                                }
                                                disabled={isDisabled}
                                                selected={isSelected}
                                                sx={{
                                                    "&.Mui-selected": {
                                                        backgroundColor:
                                                            "primary.light",
                                                        "&:hover": {
                                                            backgroundColor:
                                                                "primary.light",
                                                        },
                                                    },
                                                }}
                                            >
                                                <ListItemIcon
                                                    sx={{
                                                        color: isDisabled
                                                            ? "action.disabled"
                                                            : "inherit",
                                                    }}
                                                >
                                                    {item.icon}
                                                </ListItemIcon>
                                                <ListItemText
                                                    primary={item.name}
                                                    slotProps={{
                                                        primary: {
                                                            fontSize: "0.9rem",
                                                            fontWeight:
                                                                isSelected
                                                                    ? "bold"
                                                                    : "normal",
                                                        },
                                                    }}
                                                />
                                            </ListItemButton>
                                        </ListItem>
                                    </span>
                                </Tooltip>
                            );
                        })}
                    </List>
                </Box>
            </Drawer>

            <ComingSoonDialog
                open={comingSoonOpen}
                onClose={() => setComingSoonOpen(false)}
                featureName={selectedFeature}
            />
        </>
    );
};
