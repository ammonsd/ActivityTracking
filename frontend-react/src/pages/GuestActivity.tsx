/**
 * Guest Activity Report page - displays login statistics and audit table.
 * Matches Spring Boot backend Guest Activity UI functionality.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import React, { useState, useEffect } from "react";
import {
    Box,
    Typography,
    Button,
    CircularProgress,
    Alert,
    Paper,
    Grid,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import DownloadIcon from "@mui/icons-material/Download";
import LoginIcon from "@mui/icons-material/Login";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import PercentIcon from "@mui/icons-material/Percent";
import { StatsCard } from "../components/guestActivity/StatsCard";
import { LoginAuditTable } from "../components/guestActivity/LoginAuditTable";
import { ExportDialog } from "../components/guestActivity/ExportDialog";
import { guestActivityApi } from "../api/guestActivity.api";
import type { LoginAudit, GuestActivityStats } from "../types/guestActivity.types";
import {
    calculateGuestStats,
    formatDateTime,
    formatDateTimeCompact,
} from "../utils/guestActivityUtils";

export const GuestActivity: React.FC = () => {
    const [loginAudits, setLoginAudits] = useState<LoginAudit[]>([]);
    const [stats, setStats] = useState<GuestActivityStats>({
        totalLogins: 0,
        uniqueLocations: 0,
        lastLogin: null,
        successRate: 0,
    });
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);
    const [deploymentTimestamp] = useState<string>(new Date().toISOString());
    const [exportDialogOpen, setExportDialogOpen] = useState<boolean>(false);
    const [csvData, setCsvData] = useState<string>("");

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await guestActivityApi.fetchLoginAudit("guest", 50);
            setLoginAudits(data);
            setStats(calculateGuestStats(data));
        } catch (err) {
            console.error("Error fetching guest activity data:", err);
            setError(
                "Failed to load guest activity data. Please try again later."
            );
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleRefresh = () => {
        fetchData();
    };

    const handleExportCSV = () => {
        if (loginAudits.length === 0) {
            alert("No data to export");
            return;
        }

        // CSV content
        const headers = ["Date/Time", "IP Address", "Location", "Status"];
        const rows = loginAudits.map((audit) => [
            formatDateTime(audit.loginTime),
            audit.ipAddress,
            audit.location,
            audit.successful ? "Success" : "Failed",
        ]);

        const csvContent = [
            headers.join(","),
            ...rows.map((row) =>
                row.map((cell) => `"${cell}"`).join(",")
            ),
        ].join("\n");

        setCsvData(csvContent);
        setExportDialogOpen(true);
    };

    return (
        <Box>
            {/* Page Header */}
            <Box
                sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    mb: 3,
                }}
            >
                <Box>
                    <Typography
                        variant="h4"
                        gutterBottom
                        fontWeight="bold"
                        sx={{ color: "#1976d2" }}
                    >
                        📊 Guest Activity Report
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        View guest login activity and statistics
                    </Typography>
                </Box>
                <Box sx={{ display: "flex", gap: 2 }}>
                    <Button
                        variant="contained"
                        color="success"
                        startIcon={<DownloadIcon />}
                        onClick={handleExportCSV}
                        disabled={loading || loginAudits.length === 0}
                    >
                        Export CSV
                    </Button>
                    <Button
                        variant="outlined"
                        startIcon={<RefreshIcon />}
                        onClick={handleRefresh}
                        disabled={loading}
                    >
                        Refresh
                    </Button>
                </Box>
            </Box>

            {/* Error Alert */}
            {error && (
                <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            {/* Loading State */}
            {loading ? (
                <Box sx={{ display: "flex", justifyContent: "center", my: 8 }}>
                    <CircularProgress />
                </Box>
            ) : (
                <>
                    {/* Stats Cards */}
                    <Grid container spacing={3} sx={{ mb: 4 }}>
                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <StatsCard
                                title="Total Logins"
                                value={stats.totalLogins}
                                icon={<LoginIcon fontSize="large" />}
                                color="#5e35b1"
                            />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <StatsCard
                                title="Unique Locations"
                                value={stats.uniqueLocations}
                                icon={<LocationOnIcon fontSize="large" />}
                                color="#1976d2"
                            />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <StatsCard
                                title="Last Login"
                                value={
                                    stats.lastLogin
                                        ? formatDateTimeCompact(stats.lastLogin)
                                        : "-"
                                }
                                icon={<AccessTimeIcon fontSize="large" />}
                                color="#d32f2f"
                                compact
                            />
                        </Grid>
                        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                            <StatsCard
                                title="Success Rate"
                                value={`${stats.successRate}%`}
                                icon={<PercentIcon fontSize="large" />}
                                color="#388e3c"
                            />
                        </Grid>
                    </Grid>

                    {/* Table Section */}
                    <Paper elevation={0} sx={{ p: 2, backgroundColor: "#f8f9fa" }}>
                        <Typography variant="h6" gutterBottom fontWeight="bold">
                            Login Activity Since Last Deployment (
                            {formatDateTime(deploymentTimestamp)})
                        </Typography>
                        <LoginAuditTable loginAudits={loginAudits} />
                    </Paper>
                </>
            )}

            {/* Export Dialog */}
            <ExportDialog
                open={exportDialogOpen}
                onClose={() => setExportDialogOpen(false)}
                csvContent={csvData}
                recordCount={loginAudits.length}
                fileName={`guest-activity-${new Date().toISOString().split("T")[0]}.csv`}
            />
        </Box>
    );
};

