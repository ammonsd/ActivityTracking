/**
 * Description: Main App component with routing
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { useAuthStore } from "./store/authStore";
import { ProtectedRoute } from "./components/common/ProtectedRoute";

function App() {
    const { checkAuth } = useAuthStore();

    useEffect(() => {
        // Check authentication on app mount
        checkAuth();
    }, [checkAuth]);

    return (
        <BrowserRouter>
            <Routes>
                <Route
                    path="/"
                    element={<Navigate to="/dashboard" replace />}
                />
                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <div
                                style={{ padding: "20px", textAlign: "center" }}
                            >
                                <h1>
                                    Task Activity Tracker - React Admin
                                    Dashboard
                                </h1>
                                <p>
                                    Dashboard Home - Phase 3 skeleton coming
                                    next
                                </p>
                                <p style={{ color: "#666", marginTop: "20px" }}>
                                    ✅ Phase 1: Project Setup - Complete
                                </p>
                                <p style={{ color: "#666" }}>
                                    ✅ Phase 2: Authentication - Complete
                                </p>
                                <p style={{ color: "#666" }}>
                                    ⏳ Phase 3: Skeleton Dashboard - Coming next
                                </p>
                            </div>
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
