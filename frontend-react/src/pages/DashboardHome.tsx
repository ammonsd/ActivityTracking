/**
 * Dashboard home page with feature cards and quick access.
 * Shows phase status and provides navigation to all features.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { useState } from "react";
import {
    Box,
    Card,
    CardContent,
    CardActionArea,
    Typography,
} from "@mui/material";
import {
    People as PeopleIcon,
    List as ListIcon,
    Security as SecurityIcon,
    Timeline as TimelineIcon,
    Assignment as AssignmentIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";
import { FEATURES } from "../config/features";
import { ComingSoonDialog } from "../components/common/ComingSoonDialog";

interface FeatureCardData {
    title: string;
    description: string;
    icon: React.ReactElement;
    route: string;
    requiresAdmin: boolean;
    comingSoon: boolean;
    enabled: boolean;
    color: string;
}

export const DashboardHome: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuthStore();
    const isAdmin = user?.role === "ADMIN";

    const [comingSoonOpen, setComingSoonOpen] = useState(false);
    const [selectedFeature, setSelectedFeature] = useState("");

    const featureCards: FeatureCardData[] = [
        {
            title: "User Management",
            description: FEATURES.userManagement.description,
            icon: <PeopleIcon sx={{ fontSize: 48 }} />,
            route: FEATURES.userManagement.route,
            requiresAdmin: FEATURES.userManagement.requiresAdmin,
            comingSoon: FEATURES.userManagement.comingSoon,
            enabled: FEATURES.userManagement.enabled,
            color: "#2196f3",
        },
        {
            title: "Dropdown Management",
            description: FEATURES.dropdownManagement.description,
            icon: <ListIcon sx={{ fontSize: 48 }} />,
            route: FEATURES.dropdownManagement.route,
            requiresAdmin: FEATURES.dropdownManagement.requiresAdmin,
            comingSoon: FEATURES.dropdownManagement.comingSoon,
            enabled: FEATURES.dropdownManagement.enabled,
            color: "#4caf50",
        },
        {
            title: "Roles Management",
            description: FEATURES.rolesManagement.description,
            icon: <SecurityIcon sx={{ fontSize: 48 }} />,
            route: FEATURES.rolesManagement.route,
            requiresAdmin: FEATURES.rolesManagement.requiresAdmin,
            comingSoon: FEATURES.rolesManagement.comingSoon,
            enabled: FEATURES.rolesManagement.enabled,
            color: "#ff9800",
        },
        {
            title: "Guest Activity",
            description: FEATURES.guestActivity.description,
            icon: <TimelineIcon sx={{ fontSize: 48 }} />,
            route: FEATURES.guestActivity.route,
            requiresAdmin: FEATURES.guestActivity.requiresAdmin,
            comingSoon: FEATURES.guestActivity.comingSoon,
            enabled: FEATURES.guestActivity.enabled,
            color: "#9c27b0",
        },
        {
            title: "Task Activity Tracker",
            description: FEATURES.taskTracker.description,
            icon: <AssignmentIcon sx={{ fontSize: 48 }} />,
            route: FEATURES.taskTracker.route,
            requiresAdmin: FEATURES.taskTracker.requiresAdmin,
            comingSoon: FEATURES.taskTracker.comingSoon,
            enabled: FEATURES.taskTracker.enabled,
            color: "#f44336",
        },
    ];

    const handleCardClick = (card: FeatureCardData) => {
        // Task Tracker opens Spring Boot UI
        if (card.title === "Task Activity Tracker") {
            globalThis.location.href =
                "http://localhost:8080/task-activity/list";
            return;
        }

        // Check if user lacks admin access
        if (card.requiresAdmin && !isAdmin) {
            return; // Card is disabled, do nothing
        }

        // Check if feature is coming soon
        if (card.comingSoon) {
            setSelectedFeature(card.title);
            setComingSoonOpen(true);
            return;
        }

        // Navigate to the feature
        navigate(card.route);
    };

    const isCardDisabled = (card: FeatureCardData): boolean => {
        // Task Tracker is always enabled
        if (card.title === "Task Activity Tracker") return false;

        // Admin gets access to all features
        if (isAdmin) return false;

        // Non-admin: management features are disabled
        if (card.requiresAdmin) return true;

        return false;
    };

    return (
        <Box>
            {/* Feature Cards */}
            <Box
                sx={{
                    display: "grid",
                    gridTemplateColumns: {
                        xs: "1fr",
                        sm: "repeat(2, 1fr)",
                        lg: "repeat(3, 1fr)",
                    },
                    gap: 3,
                }}
            >
                {featureCards.map((card) => {
                    const isDisabled = isCardDisabled(card);

                    return (
                        <Card
                            key={card.title}
                            elevation={3}
                            sx={{
                                height: "100%",
                                opacity: isDisabled ? 0.5 : 1,
                                cursor: isDisabled ? "not-allowed" : "pointer",
                                transition: "transform 0.2s, box-shadow 0.2s",
                                "&:hover": {
                                    transform: isDisabled
                                        ? "none"
                                        : "translateY(-4px)",
                                    boxShadow: isDisabled ? 3 : 6,
                                },
                            }}
                        >
                            <CardActionArea
                                onClick={() => handleCardClick(card)}
                                disabled={isDisabled}
                                sx={{ height: "100%", p: 2 }}
                            >
                                <CardContent>
                                    <Box
                                        sx={{
                                            display: "flex",
                                            alignItems: "center",
                                            mb: 2,
                                            color: card.color,
                                        }}
                                    >
                                        {card.icon}
                                        <Typography
                                            variant="h6"
                                            component="div"
                                            sx={{ ml: 2, fontWeight: "bold" }}
                                        >
                                            {card.title}
                                        </Typography>
                                    </Box>

                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                        sx={{ mb: 2 }}
                                    >
                                        {card.description}
                                    </Typography>
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    );
                })}
            </Box>

            {/* Coming Soon Dialog */}
            <ComingSoonDialog
                open={comingSoonOpen}
                onClose={() => setComingSoonOpen(false)}
                featureName={selectedFeature}
            />
        </Box>
    );
};
