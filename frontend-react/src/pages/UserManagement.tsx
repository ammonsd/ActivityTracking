/**
 * Description: User Management page - displays and manages users with filtering and pagination
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import React, { useState, useEffect } from "react";
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
    TablePagination,
    TextField,
    MenuItem,
    IconButton,
    Chip,
    Grid,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import SearchIcon from "@mui/icons-material/Search";
import ClearIcon from "@mui/icons-material/Clear";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import EditIcon from "@mui/icons-material/Edit";
import VpnKeyIcon from "@mui/icons-material/VpnKey";
import DeleteIcon from "@mui/icons-material/Delete";
import ManageAccountsIcon from "@mui/icons-material/ManageAccounts";
import LockIcon from "@mui/icons-material/Lock";
import LockOpenIcon from "@mui/icons-material/LockOpen";
import { userManagementApi } from "../api/userManagement.api";
import type {
    User,
    Role,
    UserFilters,
    CurrentUser,
} from "../types/userManagement.types";
import { UserFormDialog } from "../components/userManagement/UserFormDialog";
import { DeleteConfirmDialog } from "../components/userManagement/DeleteConfirmDialog";
import { ChangePasswordDialog } from "../components/userManagement/ChangePasswordDialog";
import { UserAccessDialog } from "../components/userManagement/UserAccessDialog";

export const UserManagement: React.FC = () => {
    // State management
    const [users, setUsers] = useState<User[]>([]);
    const [roles, setRoles] = useState<Role[]>([]);
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // Filter state
    const [filters, setFilters] = useState<UserFilters>({
        username: "",
        role: "",
        company: "",
    });

    // Pagination state
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(10);

    // Dialog state
    const [formDialogOpen, setFormDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
    const [accessDialogOpen, setAccessDialogOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [accessUser, setAccessUser] = useState<User | null>(null);

    // Fetch current user, roles, and users on component mount
    useEffect(() => {
        const initializeData = async () => {
            try {
                const currentUserData =
                    await userManagementApi.fetchCurrentUser();
                setCurrentUser(currentUserData);
            } catch (err) {
                console.error("Error fetching current user:", err);
            }
        };

        initializeData();
        fetchRoles();
        fetchUsers();
    }, []);

    const fetchRoles = async () => {
        try {
            const rolesData = await userManagementApi.fetchRoles();
            setRoles(rolesData);
        } catch (err) {
            console.error("Error fetching roles:", err);
        }
    };

    const fetchUsers = async (appliedFilters?: UserFilters) => {
        setLoading(true);
        setError(null);
        try {
            const data = await userManagementApi.fetchUsers(appliedFilters);
            setUsers(data);
            setPage(0); // Reset to first page when data changes
        } catch (err) {
            console.error("Error fetching users:", err);
            setError("Failed to load users. Please try again later.");
        } finally {
            setLoading(false);
        }
    };

    /**
     * Check if delete button should be disabled for a user
     * Matches backend rules: cannot delete yourself or users with tasks
     */
    const isDeleteDisabled = (user: User): boolean => {
        if (!currentUser) return true; // Disable if current user not loaded yet
        return user.username === currentUser.username || user.hasTasks;
    };

    /**
     * Get tooltip text explaining why delete is disabled
     */
    const getDeleteTooltip = (user: User): string => {
        if (!currentUser) return "Loading...";
        if (user.username === currentUser.username) {
            return "Cannot delete your own account";
        }
        if (user.hasTasks) {
            return "Cannot delete user with existing task entries";
        }
        return "Delete User";
    };

    const handleFilterChange = (field: keyof UserFilters, value: string) => {
        setFilters((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleSearch = () => {
        // Build filters object with only non-empty values
        const appliedFilters: UserFilters = {};
        if (filters.username?.trim()) {
            appliedFilters.username = filters.username.trim();
        }
        if (filters.role?.trim()) {
            appliedFilters.role = filters.role.trim();
        }
        if (filters.company?.trim()) {
            appliedFilters.company = filters.company.trim();
        }

        fetchUsers(appliedFilters);
    };

    const handleReset = () => {
        setFilters({
            username: "",
            role: "",
            company: "",
        });
        fetchUsers(); // Fetch all users without filters
    };

    const handleRefresh = () => {
        // Re-apply current filters
        handleSearch();
    };

    const handleChangePage = (_event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (
        event: React.ChangeEvent<HTMLInputElement>,
    ) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    // Action handlers
    const handleAddUser = () => {
        setSelectedUser(null);
        setFormDialogOpen(true);
    };

    const handleEditUser = (user: User) => {
        setSelectedUser(user);
        setFormDialogOpen(true);
    };

    const handleChangePassword = (user: User) => {
        setSelectedUser(user);
        setPasswordDialogOpen(true);
    };

    const handleDeleteUser = (user: User) => {
        setSelectedUser(user);
        setDeleteDialogOpen(true);
    };

    const handleOpenAccess = (user: User) => {
        setAccessUser(user);
        setAccessDialogOpen(true);
    };

    const handleFormSave = async (userData: any) => {
        if (selectedUser === null) {
            // Add mode - creating new user
            await userManagementApi.createUser(userData);
        } else {
            // Edit mode - updating existing user
            await userManagementApi.updateUser(selectedUser.id, userData);
        }
        setFormDialogOpen(false);
        setSelectedUser(null);
        handleRefresh();
    };

    const handlePasswordChange = async (
        password: string,
        forceChange: boolean,
    ) => {
        if (selectedUser) {
            await userManagementApi.changePassword(
                selectedUser.id,
                password,
                forceChange,
            );
        }
        setPasswordDialogOpen(false);
        setSelectedUser(null);
        handleRefresh();
    };

    const handleConfirmDelete = async () => {
        if (selectedUser) {
            await userManagementApi.deleteUser(selectedUser.id);
        }
        setDeleteDialogOpen(false);
        setSelectedUser(null);
        handleRefresh();
    };

    // Format datetime to local time
    const formatLocalDateTime = (dateTimeString: string | null): string => {
        if (!dateTimeString) return "-";
        try {
            const date = new Date(dateTimeString);
            return date.toLocaleString("en-US", {
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
                hour: "2-digit",
                minute: "2-digit",
                second: "2-digit",
                hour12: true,
            });
        } catch {
            return dateTimeString;
        }
    };

    // Paginated data
    const paginatedUsers = users.slice(
        page * rowsPerPage,
        page * rowsPerPage + rowsPerPage,
    );

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
                        👥 User Management
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Manage users, roles, and permissions
                    </Typography>
                </Box>
                <Box sx={{ display: "flex", gap: 2 }}>
                    <Button
                        variant="contained"
                        color="success"
                        startIcon={<PersonAddIcon />}
                        onClick={handleAddUser}
                        disabled={loading}
                    >
                        Add New User
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
                <Alert
                    severity="error"
                    sx={{ mb: 3 }}
                    onClose={() => setError(null)}
                >
                    {error}
                </Alert>
            )}

            {/* Filters Section */}
            <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
                <Typography variant="h6" gutterBottom fontWeight="bold">
                    🔍 Filter Users
                </Typography>
                <Grid container spacing={2} alignItems="center">
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            fullWidth
                            label="Username"
                            placeholder="Search by username..."
                            value={filters.username}
                            onChange={(e) =>
                                handleFilterChange("username", e.target.value)
                            }
                            size="small"
                        />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            fullWidth
                            select
                            label="Role"
                            value={filters.role}
                            onChange={(e) =>
                                handleFilterChange("role", e.target.value)
                            }
                            size="small"
                        >
                            <MenuItem value="">-- All Roles --</MenuItem>
                            {roles.map((role) => (
                                <MenuItem key={role.id} value={role.name}>
                                    {role.name}
                                </MenuItem>
                            ))}
                        </TextField>
                    </Grid>
                    <Grid size={{ xs: 12, sm: 4 }}>
                        <TextField
                            fullWidth
                            label="Company"
                            placeholder="Search by company..."
                            value={filters.company}
                            onChange={(e) =>
                                handleFilterChange("company", e.target.value)
                            }
                            size="small"
                        />
                    </Grid>
                    <Grid size={{ xs: 12 }}>
                        <Box
                            sx={{
                                display: "flex",
                                gap: 2,
                                justifyContent: "flex-start",
                            }}
                        >
                            <Button
                                variant="contained"
                                startIcon={<SearchIcon />}
                                onClick={handleSearch}
                                disabled={loading}
                            >
                                Search
                            </Button>
                            <Button
                                variant="outlined"
                                startIcon={<ClearIcon />}
                                onClick={handleReset}
                                disabled={loading}
                            >
                                Reset
                            </Button>
                        </Box>
                    </Grid>
                </Grid>
            </Paper>

            {/* Loading State */}
            {loading ? (
                <Box sx={{ display: "flex", justifyContent: "center", my: 8 }}>
                    <CircularProgress />
                </Box>
            ) : (
                <>
                    {/* Users Table */}
                    {users.length === 0 ? (
                        <Paper elevation={2} sx={{ p: 4, textAlign: "center" }}>
                            <Typography variant="h6" color="text.secondary">
                                No users found
                            </Typography>
                            <Typography
                                variant="body2"
                                color="text.secondary"
                                sx={{ mt: 1 }}
                            >
                                Try adjusting your filters or click Reset to
                                view all users
                            </Typography>
                        </Paper>
                    ) : (
                        <Paper elevation={2}>
                            <TableContainer>
                                <Table>
                                    <TableHead>
                                        <TableRow
                                            sx={{ backgroundColor: "#f5f5f5" }}
                                        >
                                            <TableCell>
                                                <strong>Username</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Company</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Email</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Role</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Status</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Last Login</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Account Locked</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Actions</strong>
                                            </TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {paginatedUsers.map((user) => (
                                            <TableRow
                                                key={user.id}
                                                sx={{
                                                    "&:hover": {
                                                        backgroundColor:
                                                            "#fafafa",
                                                    },
                                                }}
                                            >
                                                <TableCell>
                                                    {user.username}
                                                </TableCell>
                                                <TableCell>
                                                    {user.company || "-"}
                                                </TableCell>
                                                <TableCell>
                                                    {user.email || "-"}
                                                </TableCell>
                                                <TableCell>
                                                    {user.role}
                                                </TableCell>
                                                <TableCell>
                                                    {user.enabled ? (
                                                        <Typography
                                                            variant="body2"
                                                            sx={{
                                                                color: "#388e3c",
                                                                fontWeight: 500,
                                                            }}
                                                        >
                                                            Enabled
                                                        </Typography>
                                                    ) : (
                                                        <Typography
                                                            variant="body2"
                                                            sx={{
                                                                color: "#d32f2f",
                                                                fontWeight: 500,
                                                            }}
                                                        >
                                                            Disabled
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    {formatLocalDateTime(
                                                        user.lastLogin,
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    {user.accountLocked ? (
                                                        <Chip
                                                            icon={<LockIcon />}
                                                            label="Locked"
                                                            color="error"
                                                            size="small"
                                                            variant="outlined"
                                                        />
                                                    ) : (
                                                        <Chip
                                                            icon={
                                                                <LockOpenIcon />
                                                            }
                                                            label="Unlocked"
                                                            color="success"
                                                            size="small"
                                                            variant="outlined"
                                                        />
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    <Box
                                                        sx={{
                                                            display: "flex",
                                                            gap: 0.5,
                                                        }}
                                                    >
                                                        <IconButton
                                                            size="small"
                                                            color="primary"
                                                            onClick={() =>
                                                                handleEditUser(
                                                                    user,
                                                                )
                                                            }
                                                            title="Edit User"
                                                        >
                                                            <EditIcon fontSize="small" />
                                                        </IconButton>
                                                        <IconButton
                                                            size="small"
                                                            color="secondary"
                                                            onClick={() =>
                                                                handleChangePassword(
                                                                    user,
                                                                )
                                                            }
                                                            title="Change Password"
                                                        >
                                                            <VpnKeyIcon fontSize="small" />
                                                        </IconButton>
                                                        <IconButton
                                                            size="small"
                                                            color="success"
                                                            onClick={() =>
                                                                handleOpenAccess(
                                                                    user,
                                                                )
                                                            }
                                                            title={
                                                                user.role ===
                                                                "ADMIN"
                                                                    ? "ADMIN users have full access"
                                                                    : "Manage Access"
                                                            }
                                                            disabled={
                                                                user.role ===
                                                                "ADMIN"
                                                            }
                                                        >
                                                            <ManageAccountsIcon fontSize="small" />
                                                        </IconButton>
                                                        <IconButton
                                                            size="small"
                                                            color="error"
                                                            onClick={() =>
                                                                handleDeleteUser(
                                                                    user,
                                                                )
                                                            }
                                                            title={getDeleteTooltip(
                                                                user,
                                                            )}
                                                            disabled={isDeleteDisabled(
                                                                user,
                                                            )}
                                                        >
                                                            <DeleteIcon fontSize="small" />
                                                        </IconButton>
                                                    </Box>
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                            <TablePagination
                                rowsPerPageOptions={[5, 10, 25, 50]}
                                component="div"
                                count={users.length}
                                rowsPerPage={rowsPerPage}
                                page={page}
                                onPageChange={handleChangePage}
                                onRowsPerPageChange={handleChangeRowsPerPage}
                            />
                        </Paper>
                    )}
                </>
            )}

            {/* User Form Dialog (Add/Edit) */}
            <UserFormDialog
                open={formDialogOpen}
                onClose={() => {
                    setFormDialogOpen(false);
                    setSelectedUser(null);
                }}
                onSave={handleFormSave}
                user={selectedUser}
                roles={roles}
            />

            {/* Delete Confirmation Dialog */}
            <DeleteConfirmDialog
                open={deleteDialogOpen}
                onClose={() => {
                    setDeleteDialogOpen(false);
                    setSelectedUser(null);
                }}
                onConfirm={handleConfirmDelete}
                username={selectedUser?.username || ""}
            />

            {/* Change Password Dialog */}
            <ChangePasswordDialog
                open={passwordDialogOpen}
                onClose={() => {
                    setPasswordDialogOpen(false);
                    setSelectedUser(null);
                }}
                onSave={handlePasswordChange}
                username={selectedUser?.username || ""}
            />

            {/* User Access Dialog */}
            <UserAccessDialog
                open={accessDialogOpen}
                onClose={() => {
                    setAccessDialogOpen(false);
                    setAccessUser(null);
                }}
                user={accessUser}
            />
        </Box>
    );
};
