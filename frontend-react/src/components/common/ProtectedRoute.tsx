/**
 * Description: Protected route wrapper that checks authentication
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import React, { useEffect } from "react";
import { Navigate } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import { CircularProgress, Box } from "@mui/material";

interface ProtectedRouteProps {
    children: React.ReactNode;
    requiredRole?: "ADMIN" | "USER" | "GUEST";
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
    children,
    requiredRole,
}) => {
    const { isAuthenticated, isLoading, user, checkAuth } = useAuthStore();

    useEffect(() => {
        if (!isAuthenticated && !isLoading) {
            checkAuth();
        }
    }, [isAuthenticated, isLoading, checkAuth]);

    if (isLoading) {
        return (
            <Box
                display="flex"
                justifyContent="center"
                alignItems="center"
                minHeight="100vh"
            >
                <CircularProgress />
            </Box>
        );
    }

    if (!isAuthenticated) {
        // Redirect to Spring Boot login - use relative URL to work in local and AWS
        window.location.href = "/login";
        return null;
    }

    // Check role-based access
    if (requiredRole && user?.role !== requiredRole && user?.role !== "ADMIN") {
        return <Navigate to="/dashboard" replace />;
    }

    return <>{children}</>;
};
