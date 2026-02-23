/**
 * Description: Notify Users admin page — allows an administrator to select one or more active
 * users and send each a profile notification email containing their username, name, company,
 * and assigned task/expense clients and projects.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import React, { useState, useEffect, useCallback } from "react";
import {
    Box,
    Typography,
    Button,
    CircularProgress,
    Alert,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Checkbox,
    FormControlLabel,
    Grid,
    Chip,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import SearchIcon from "@mui/icons-material/Search";
import ClearIcon from "@mui/icons-material/Clear";
import SendIcon from "@mui/icons-material/Send";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { userManagementApi } from "../api/userManagement.api";
import type { NotifyEligibleUser } from "../types/userManagement.types";

export const NotifyUsers: React.FC = () => {
    const [users, setUsers] = useState<NotifyEligibleUser[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [sending, setSending] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    const [mailWarning, setMailWarning] = useState<boolean>(false);

    // Filter state
    const [lastNameFilter, setLastNameFilter] = useState<string>("");
    const [appliedFilter, setAppliedFilter] = useState<string>("");

    // Selection state
    const [selectedUsernames, setSelectedUsernames] = useState<Set<string>>(
        new Set(),
    );

    const fetchUsers = useCallback(async (filter: string) => {
        setLoading(true);
        setError(null);
        try {
            const data =
                await userManagementApi.fetchNotifyEligibleUsers(filter);
            setUsers(data);
            // Clear selection when list refreshes
            setSelectedUsernames(new Set());
        } catch {
            setError("Failed to load users. Please try again.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchUsers("");
    }, [fetchUsers]);

    const handleSearch = () => {
        setAppliedFilter(lastNameFilter);
        fetchUsers(lastNameFilter);
    };

    const handleReset = () => {
        setLastNameFilter("");
        setAppliedFilter("");
        fetchUsers("");
    };

    const handleRefresh = () => {
        fetchUsers(appliedFilter);
    };

    const handleToggleUser = (username: string) => {
        setSelectedUsernames((prev) => {
            const next = new Set(prev);
            if (next.has(username)) {
                next.delete(username);
            } else {
                next.add(username);
            }
            return next;
        });
    };

    const handleSelectAll = () => {
        setSelectedUsernames(new Set(users.map((u) => u.username)));
    };

    const handleDeselectAll = () => {
        setSelectedUsernames(new Set());
    };

    const allSelected =
        users.length > 0 && selectedUsernames.size === users.length;
    const someSelected =
        selectedUsernames.size > 0 && selectedUsernames.size < users.length;

    const handleSendNotifications = async () => {
        if (selectedUsernames.size === 0) {
            setError("Please select at least one user to notify.");
            return;
        }

        setSending(true);
        setError(null);
        setSuccessMessage(null);
        setMailWarning(false);

        try {
            const result = await userManagementApi.sendUserNotifications({
                usernames: Array.from(selectedUsernames),
            });

            let message = `Profile notification sent to ${result.sent} user(s).`;
            if (result.skipped > 0) {
                message += ` ${result.skipped} user(s) skipped (no email or not found).`;
            }
            setSuccessMessage(message);

            if (!result.mailEnabled) {
                setMailWarning(true);
            }

            // Clear selection after successful send
            setSelectedUsernames(new Set());
        } catch {
            setError("Failed to send notifications. Please try again.");
        } finally {
            setSending(false);
        }
    };

    /**
     * Build a display name string for a user row.
     */
    const displayName = (user: NotifyEligibleUser): string => {
        const parts = [user.firstname, user.lastname].filter(Boolean).join(" ");
        return parts || "-";
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
                        📧 Notify Users
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Send profile notification emails to active users
                    </Typography>
                </Box>
                <Button
                    variant="outlined"
                    startIcon={<RefreshIcon />}
                    onClick={handleRefresh}
                    disabled={loading || sending}
                >
                    Refresh
                </Button>
            </Box>

            {/* Mail disabled warning */}
            {mailWarning && (
                <Alert
                    severity="warning"
                    icon={<WarningAmberIcon />}
                    sx={{ mb: 3 }}
                    onClose={() => setMailWarning(false)}
                >
                    Email sending is currently <strong>disabled</strong> on this
                    server. Notifications were logged but not delivered.
                </Alert>
            )}

            {/* Success message */}
            {successMessage && (
                <Alert
                    severity="success"
                    sx={{ mb: 3 }}
                    onClose={() => setSuccessMessage(null)}
                >
                    {successMessage}
                </Alert>
            )}

            {/* Error message */}
            {error && (
                <Alert
                    severity="error"
                    sx={{ mb: 3 }}
                    onClose={() => setError(null)}
                >
                    {error}
                </Alert>
            )}

            {/* Filter Section */}
            <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
                <Typography variant="h6" gutterBottom fontWeight="bold">
                    🔍 Filter by Last Name
                </Typography>
                <Grid container spacing={2} alignItems="center">
                    <Grid size={{ xs: 12, sm: 6, md: 4 }}>
                        <TextField
                            fullWidth
                            label="Last Name Prefix"
                            placeholder="e.g. Sm"
                            value={lastNameFilter}
                            onChange={(e) => setLastNameFilter(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === "Enter") handleSearch();
                            }}
                            size="small"
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                        <Box sx={{ display: "flex", gap: 2 }}>
                            <Button
                                variant="contained"
                                startIcon={<SearchIcon />}
                                onClick={handleSearch}
                                disabled={loading || sending}
                            >
                                Filter
                            </Button>
                            <Button
                                variant="outlined"
                                startIcon={<ClearIcon />}
                                onClick={handleReset}
                                disabled={loading || sending}
                            >
                                Reset
                            </Button>
                        </Box>
                    </Grid>
                </Grid>
            </Paper>

            {/* Users Table */}
            {loading ? (
                <Box sx={{ display: "flex", justifyContent: "center", my: 8 }}>
                    <CircularProgress />
                </Box>
            ) : (
                <>
                    {users.length === 0 ? (
                        <Paper elevation={2} sx={{ p: 4, textAlign: "center" }}>
                            <Typography variant="h6" color="text.secondary">
                                No eligible users found
                            </Typography>
                            <Typography
                                variant="body2"
                                color="text.secondary"
                                sx={{ mt: 1 }}
                            >
                                Only active users with an email address appear
                                here.
                            </Typography>
                        </Paper>
                    ) : (
                        <Paper elevation={2}>
                            {/* Action bar above table */}
                            <Box
                                sx={{
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "space-between",
                                    px: 2,
                                    py: 1.5,
                                    borderBottom: "1px solid #e0e0e0",
                                }}
                            >
                                <Box
                                    sx={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: 2,
                                    }}
                                >
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={allSelected}
                                                indeterminate={someSelected}
                                                onChange={(e) =>
                                                    e.target.checked
                                                        ? handleSelectAll()
                                                        : handleDeselectAll()
                                                }
                                                disabled={sending}
                                            />
                                        }
                                        label="Select All"
                                    />
                                    {selectedUsernames.size > 0 && (
                                        <Chip
                                            label={`${selectedUsernames.size} selected`}
                                            size="small"
                                            color="primary"
                                            variant="outlined"
                                        />
                                    )}
                                </Box>
                                <Button
                                    variant="contained"
                                    color="primary"
                                    startIcon={
                                        sending ? (
                                            <CircularProgress
                                                size={16}
                                                color="inherit"
                                            />
                                        ) : (
                                            <SendIcon />
                                        )
                                    }
                                    onClick={handleSendNotifications}
                                    disabled={
                                        sending || selectedUsernames.size === 0
                                    }
                                >
                                    {sending
                                        ? "Sending…"
                                        : "Send Profile Notifications"}
                                </Button>
                            </Box>

                            <TableContainer>
                                <Table>
                                    <TableHead>
                                        <TableRow
                                            sx={{ backgroundColor: "#f5f5f5" }}
                                        >
                                            <TableCell padding="checkbox" />
                                            <TableCell>
                                                <strong>Username</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Name</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Email</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Company</strong>
                                            </TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {users.map((user) => (
                                            <TableRow
                                                key={user.username}
                                                hover
                                                selected={selectedUsernames.has(
                                                    user.username,
                                                )}
                                                onClick={() =>
                                                    !sending &&
                                                    handleToggleUser(
                                                        user.username,
                                                    )
                                                }
                                                sx={{ cursor: "pointer" }}
                                            >
                                                <TableCell padding="checkbox">
                                                    <Checkbox
                                                        checked={selectedUsernames.has(
                                                            user.username,
                                                        )}
                                                        onChange={() =>
                                                            handleToggleUser(
                                                                user.username,
                                                            )
                                                        }
                                                        disabled={sending}
                                                        onClick={(e) =>
                                                            e.stopPropagation()
                                                        }
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    {user.username}
                                                </TableCell>
                                                <TableCell>
                                                    {displayName(user)}
                                                </TableCell>
                                                <TableCell>
                                                    {user.email}
                                                </TableCell>
                                                <TableCell>
                                                    {user.company || "-"}
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>

                            {/* Count footer */}
                            <Box
                                sx={{
                                    px: 2,
                                    py: 1.5,
                                    borderTop: "1px solid #e0e0e0",
                                }}
                            >
                                <Typography
                                    variant="body2"
                                    color="text.secondary"
                                >
                                    Showing {users.length} eligible user
                                    {users.length === 1 ? "" : "s"}
                                </Typography>
                            </Box>
                        </Paper>
                    )}
                </>
            )}
        </Box>
    );
};
