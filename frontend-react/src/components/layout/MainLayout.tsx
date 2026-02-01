/**
 * Main layout component with app bar, navigation drawer, and content area.
 * Provides consistent structure across all pages.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import type { ReactNode } from "react";
import { AppBar, Box, Toolbar, Typography, Button } from "@mui/material";
import { Logout } from "@mui/icons-material";
import { Sidebar } from "./Sidebar";
import { useAuthStore } from "../../store/authStore";
import { NonAdminBanner } from "../common/NonAdminBanner";

interface MainLayoutProps {
    children: ReactNode;
}

export const MainLayout: React.FC<MainLayoutProps> = ({ children }) => {
    const { user, logout } = useAuthStore();
    const isGuest = user?.role === "GUEST";

    const handleLogout = async () => {
        await logout();
    };

    return (
        <Box sx={{ display: "flex" }}>
            <AppBar
                position="fixed"
                sx={{
                    zIndex: (theme) => theme.zIndex.drawer + 1,
                    backgroundColor: "primary.main",
                }}
            >
                <Toolbar>
                    <Typography
                        variant="h6"
                        component="div"
                        sx={{ flexGrow: 1, fontWeight: "bold" }}
                    >
                        Task Activity Tracker - Admin Dashboard
                    </Typography>

                    <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                        <Typography
                            variant="body2"
                            sx={{ display: { xs: "none", sm: "block" } }}
                        >
                            {user?.fullName || user?.username} ({user?.username}
                            )
                        </Typography>

                        <Typography
                            variant="body2"
                            sx={{
                                display: { xs: "none", md: "block" },
                                fontWeight: 500,
                            }}
                        >
                            {new Date().toLocaleDateString("en-US", {
                                month: "numeric",
                                day: "numeric",
                                year: "2-digit",
                            })}
                        </Typography>

                        <Button
                            color="inherit"
                            startIcon={<Logout />}
                            onClick={handleLogout}
                            sx={{ textTransform: "none" }}
                        >
                            Logout
                        </Button>
                    </Box>
                </Toolbar>
            </AppBar>

            <Sidebar />

            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    bgcolor: "background.default",
                    minHeight: "100vh",
                    display: "flex",
                    flexDirection: "column",
                }}
            >
                <Toolbar />

                {/* Show welcome banner for GUEST role only */}
                {isGuest && <NonAdminBanner />}

                <Box sx={{ flexGrow: 1, p: 3 }}>{children}</Box>
            </Box>
        </Box>
    );
};
