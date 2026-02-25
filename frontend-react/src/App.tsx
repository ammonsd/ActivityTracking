/**
 * Description: Main App component with routing and layout
 *
 * Modified by: Dean Ammons - February 2026
 * Change: Added /admin-analytics route for Analytics & Reports admin page
 * Reason: Move admin-only User Analysis report from Angular User Dashboard to React Admin Dashboard
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
import { NotifyUsers } from "./pages/NotifyUsers";
import { AdminAnalytics } from "./pages/AdminAnalytics";

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

                {/* Guest Activity - Accessible to all authenticated users */}
                <Route
                    path="/guest-activity"
                    element={
                        <ProtectedRoute>
                            <MainLayout>
                                <GuestActivity />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/notify-users"
                    element={
                        <ProtectedRoute requiredRole="ADMIN">
                            <MainLayout>
                                <NotifyUsers />
                            </MainLayout>
                        </ProtectedRoute>
                    }
                />

                <Route
                    path="/admin-analytics"
                    element={
                        <ProtectedRoute requiredRole="ADMIN">
                            <MainLayout>
                                <AdminAnalytics />
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
