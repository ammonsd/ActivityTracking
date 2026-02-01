/**
 * Description: Main App component with routing and layout
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useAuthStore } from "./store/authStore";
import { ProtectedRoute } from "./components/common/ProtectedRoute";
import { MainLayout } from "./components/layout/MainLayout";
import { DashboardHome } from "./pages/DashboardHome";
import { UserManagement } from "./pages/UserManagement";
import { DropdownManagement } from "./pages/DropdownManagement";
import { RolesManagement } from "./pages/RolesManagement";
import { GuestActivity } from "./pages/GuestActivity";

function App() {
    const { checkAuth } = useAuthStore();

    useEffect(() => {
        // Check authentication on app mount
        checkAuth();
    }, [checkAuth]);

    return (
        <BrowserRouter basename="/dashboard">
            <Routes>
                {/* All routes wrapped in MainLayout and protected by auth */}
                <Route
                    path="/"
                    element={
                        <ProtectedRoute>
                            <MainLayout>
                                <DashboardHome />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />

                {/* Management Features - Admin only routes */}
                <Route
                    path="/user-management"
                    element={
                        <ProtectedRoute requiredRole="ADMIN">
                            <MainLayout>
                                <UserManagement />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/dropdown-management"
                    element={
                        <ProtectedRoute requiredRole="ADMIN">
                            <MainLayout>
                                <DropdownManagement />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/roles-management"
                    element={
                        <ProtectedRoute requiredRole="ADMIN">
                            <MainLayout>
                                <RolesManagement />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/guest-activity"
                    element={
                        <ProtectedRoute requiredRole="ADMIN">
                            <MainLayout>
                                <GuestActivity />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />

                {/* Catch-all route: redirect any unmatched paths (like /index.html) to root */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
