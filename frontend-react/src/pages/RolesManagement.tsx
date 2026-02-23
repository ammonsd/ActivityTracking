/**
 * Description: Roles Management page - displays and manages roles with permissions
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
    IconButton,
    Chip,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import SecurityIcon from "@mui/icons-material/Security";
import { rolesManagementApi } from "../api/rolesManagement.api";
import type { RoleDetail, Permission } from "../types/rolesManagement.types";
import { RoleFormDialog } from "../components/rolesManagement/RoleFormDialog";
import { DeleteConfirmDialog } from "../components/rolesManagement/DeleteConfirmDialog";

export const RolesManagement: React.FC = () => {
    // State management
    const [roles, setRoles] = useState<RoleDetail[]>([]);
    const [permissions, setPermissions] = useState<Permission[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // Dialog state
    const [formDialogOpen, setFormDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedRole, setSelectedRole] = useState<RoleDetail | null>(null);
    const [isCloneMode, setIsCloneMode] = useState(false);

    // Fetch roles and permissions on component mount
    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const [rolesData, permissionsData] = await Promise.all([
                rolesManagementApi.fetchRoles(),
                rolesManagementApi.fetchPermissions(),
            ]);
            setRoles(rolesData);
            setPermissions(permissionsData);
        } catch (err) {
            console.error("Error fetching data:", err);
            setError("Failed to load data. Please try again later.");
        } finally {
            setLoading(false);
        }
    };

    const handleRefresh = () => {
        fetchData();
    };

    // Action handlers
    const handleAddRole = () => {
        setSelectedRole(null);
        setIsCloneMode(false);
        setFormDialogOpen(true);
    };

    const handleEditRole = (role: RoleDetail) => {
        setSelectedRole(role);
        setIsCloneMode(false);
        setFormDialogOpen(true);
    };

    const handleCloneRole = (role: RoleDetail) => {
        setSelectedRole(role);
        setIsCloneMode(true);
        setFormDialogOpen(true);
    };

    const handleDeleteRole = (role: RoleDetail) => {
        setSelectedRole(role);
        setDeleteDialogOpen(true);
    };

    const handleFormSave = async (roleData: any) => {
        if (selectedRole === null || isCloneMode) {
            // Add or Clone mode - creating new role
            await rolesManagementApi.createRole(roleData);
        } else {
            // Edit mode - updating existing role
            await rolesManagementApi.updateRole(selectedRole.id, roleData);
        }
        setFormDialogOpen(false);
        setSelectedRole(null);
        setIsCloneMode(false);
        handleRefresh();
    };

    const handleConfirmDelete = async () => {
        if (selectedRole) {
            try {
                await rolesManagementApi.deleteRole(selectedRole.id);
                setDeleteDialogOpen(false);
                setSelectedRole(null);
                handleRefresh();
            } catch (err: any) {
                // Show error to user
                setError(
                    err.response?.data?.message ||
                        "Failed to delete role. It may be assigned to users.",
                );
                setDeleteDialogOpen(false);
                setSelectedRole(null);
            }
        }
    };

    // Group permissions by resource for display
    const groupPermissionsByResource = (
        rolePermissions: Permission[],
    ): string[] => {
        const grouped = rolePermissions.reduce(
            (acc, perm) => {
                if (!acc[perm.resource]) {
                    acc[perm.resource] = [];
                }
                acc[perm.resource].push(perm.action);
                return acc;
            },
            {} as Record<string, string[]>,
        );

        return Object.entries(grouped).map(
            ([resource, actions]) => `${resource}: ${actions.join(", ")}`,
        );
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
                        🔒 Role & Permission Management
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        Manage roles and permissions
                    </Typography>
                </Box>
                <Box sx={{ display: "flex", gap: 2 }}>
                    <Button
                        variant="contained"
                        color="success"
                        startIcon={<AddIcon />}
                        onClick={handleAddRole}
                        disabled={loading}
                    >
                        Add New Role
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

            {/* Loading State */}
            {loading ? (
                <Box sx={{ display: "flex", justifyContent: "center", my: 8 }}>
                    <CircularProgress />
                </Box>
            ) : (
                <>
                    {/* Roles Table */}
                    {roles.length === 0 ? (
                        <Paper elevation={2} sx={{ p: 4, textAlign: "center" }}>
                            <Typography variant="h6" color="text.secondary">
                                No roles found
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
                                                <strong>Role Name</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Description</strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>
                                                    Assigned Permissions
                                                </strong>
                                            </TableCell>
                                            <TableCell>
                                                <strong>Actions</strong>
                                            </TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {roles.map((role) => (
                                            <TableRow
                                                key={role.id}
                                                sx={{
                                                    "&:hover": {
                                                        backgroundColor:
                                                            "#fafafa",
                                                    },
                                                }}
                                            >
                                                <TableCell>
                                                    <Box
                                                        sx={{
                                                            display: "flex",
                                                            alignItems:
                                                                "center",
                                                            gap: 1,
                                                        }}
                                                    >
                                                        <SecurityIcon
                                                            color="primary"
                                                            fontSize="small"
                                                        />
                                                        <Typography
                                                            fontWeight={500}
                                                        >
                                                            {role.name}
                                                        </Typography>
                                                    </Box>
                                                </TableCell>
                                                <TableCell>
                                                    {role.description || "-"}
                                                </TableCell>
                                                <TableCell>
                                                    <Box
                                                        sx={{
                                                            display: "flex",
                                                            flexWrap: "wrap",
                                                            gap: 0.5,
                                                        }}
                                                    >
                                                        {groupPermissionsByResource(
                                                            role.permissions,
                                                        ).map((perm) => (
                                                            <Chip
                                                                key={perm}
                                                                label={perm}
                                                                size="small"
                                                                color="primary"
                                                                variant="outlined"
                                                            />
                                                        ))}
                                                        {role.permissions
                                                            .length === 0 && (
                                                            <Typography
                                                                variant="body2"
                                                                color="text.secondary"
                                                            >
                                                                No permissions
                                                                assigned
                                                            </Typography>
                                                        )}
                                                    </Box>
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
                                                                handleEditRole(
                                                                    role,
                                                                )
                                                            }
                                                            title="Edit Role"
                                                        >
                                                            <EditIcon fontSize="small" />
                                                        </IconButton>
                                                        <IconButton
                                                            size="small"
                                                            color="info"
                                                            onClick={() =>
                                                                handleCloneRole(
                                                                    role,
                                                                )
                                                            }
                                                            title="Clone Role"
                                                        >
                                                            <ContentCopyIcon fontSize="small" />
                                                        </IconButton>
                                                        <IconButton
                                                            size="small"
                                                            color="error"
                                                            onClick={() =>
                                                                handleDeleteRole(
                                                                    role,
                                                                )
                                                            }
                                                            title="Delete Role"
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
                        </Paper>
                    )}
                </>
            )}

            {/* Role Form Dialog (Add/Edit/Clone) */}
            <RoleFormDialog
                open={formDialogOpen}
                onClose={() => {
                    setFormDialogOpen(false);
                    setSelectedRole(null);
                    setIsCloneMode(false);
                }}
                onSave={handleFormSave}
                role={selectedRole}
                allPermissions={permissions}
                isClone={isCloneMode}
            />

            {/* Delete Confirmation Dialog */}
            <DeleteConfirmDialog
                open={deleteDialogOpen}
                onClose={() => {
                    setDeleteDialogOpen(false);
                    setSelectedRole(null);
                }}
                onConfirm={handleConfirmDelete}
                roleName={selectedRole?.name || ""}
            />
        </Box>
    );
};
