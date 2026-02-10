/**
 * Description: Role Form Dialog for creating and editing roles
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import React, { useState, useEffect } from "react";
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    Alert,
    Box,
    Grid,
    Typography,
    Checkbox,
    FormControlLabel,
    Divider,
    Paper,
} from "@mui/material";
import type { RoleDetail, Permission } from "../../types/rolesManagement.types";

interface RoleFormDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (roleData: any) => Promise<void>;
    role: RoleDetail | null; // null for "Add", populated for "Edit"
    allPermissions: Permission[];
}

export const RoleFormDialog: React.FC<RoleFormDialogProps> = ({
    open,
    onClose,
    onSave,
    role,
    allPermissions,
}) => {
    const isEditMode = role !== null;

    const [formData, setFormData] = useState({
        name: "",
        description: "",
        permissionIds: [] as number[],
    });

    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    // Group permissions by resource
    const permissionsByResource = allPermissions.reduce(
        (acc, perm) => {
            if (!acc[perm.resource]) {
                acc[perm.resource] = [];
            }
            acc[perm.resource].push(perm);
            return acc;
        },
        {} as Record<string, Permission[]>,
    );

    // Populate form when editing
    useEffect(() => {
        if (role) {
            setFormData({
                name: role.name || "",
                description: role.description || "",
                permissionIds: role.permissions.map((p) => p.id),
            });
        } else {
            // Reset for add mode
            setFormData({
                name: "",
                description: "",
                permissionIds: [],
            });
        }
        setError(null);
    }, [role, open]);

    const handleChange = (field: string, value: any) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
        setError(null);
    };

    const handlePermissionToggle = (permissionId: number) => {
        setFormData((prev) => {
            const isSelected = prev.permissionIds.includes(permissionId);
            return {
                ...prev,
                permissionIds: isSelected
                    ? prev.permissionIds.filter((id) => id !== permissionId)
                    : [...prev.permissionIds, permissionId],
            };
        });
    };

    const handleResourceToggle = (resource: string) => {
        const resourcePermissions = permissionsByResource[resource];
        const resourcePermissionIds = resourcePermissions.map((p) => p.id);
        const allSelected = resourcePermissionIds.every((id) =>
            formData.permissionIds.includes(id),
        );

        setFormData((prev) => ({
            ...prev,
            permissionIds: allSelected
                ? prev.permissionIds.filter(
                      (id) => !resourcePermissionIds.includes(id),
                  )
                : [
                      ...prev.permissionIds,
                      ...resourcePermissionIds.filter(
                          (id) => !prev.permissionIds.includes(id),
                      ),
                  ],
        }));
    };

    const validateForm = (): boolean => {
        if (!formData.name.trim()) {
            setError("Role name is required");
            return false;
        }
        if (formData.name.length < 2) {
            setError("Role name must be at least 2 characters");
            return false;
        }
        return true;
    };

    const handleSave = async () => {
        if (!validateForm()) return;

        setSaving(true);
        setError(null);

        try {
            const roleData: any = {
                description: formData.description.trim() || null,
                permissionIds: formData.permissionIds,
            };

            // Add name for new roles
            if (!isEditMode) {
                roleData.name = formData.name.trim();
            }

            await onSave(roleData);
            onClose();
        } catch (err: any) {
            setError(
                err.response?.data?.message ||
                    "Failed to save role. Please try again.",
            );
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>
                {isEditMode ? `Edit Role: ${role.name}` : "Add New Role"}
            </DialogTitle>
            <DialogContent>
                {error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {error}
                    </Alert>
                )}

                <Box sx={{ pt: 1 }}>
                    <Grid container spacing={2}>
                        <Grid size={{ xs: 12 }}>
                            <TextField
                                fullWidth
                                label="Role Name"
                                value={formData.name}
                                onChange={(e) =>
                                    handleChange("name", e.target.value)
                                }
                                required
                                disabled={isEditMode} // Role name can't be changed
                                helperText={
                                    isEditMode
                                        ? "Role name cannot be changed"
                                        : "Enter a unique role name (e.g., MANAGER, AUDITOR)"
                                }
                            />
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <TextField
                                fullWidth
                                label="Description"
                                value={formData.description}
                                onChange={(e) =>
                                    handleChange("description", e.target.value)
                                }
                                multiline
                                rows={2}
                                helperText="Optional: Describe the purpose of this role"
                            />
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <Divider sx={{ my: 2 }} />
                            <Typography
                                variant="h6"
                                gutterBottom
                                fontWeight="bold"
                            >
                                Assign Permissions
                            </Typography>
                            <Typography
                                variant="body2"
                                color="text.secondary"
                                sx={{ mb: 2 }}
                            >
                                Select the permissions this role should have.
                                Permissions are grouped by resource.
                            </Typography>

                            {Object.entries(permissionsByResource).map(
                                ([resource, permissions]) => {
                                    const resourcePermissionIds =
                                        permissions.map((p) => p.id);
                                    const allSelected =
                                        resourcePermissionIds.every((id) =>
                                            formData.permissionIds.includes(id),
                                        );
                                    const someSelected =
                                        resourcePermissionIds.some((id) =>
                                            formData.permissionIds.includes(id),
                                        );

                                    return (
                                        <Paper
                                            key={resource}
                                            elevation={1}
                                            sx={{ p: 2, mb: 2 }}
                                        >
                                            <FormControlLabel
                                                control={
                                                    <Checkbox
                                                        checked={allSelected}
                                                        indeterminate={
                                                            someSelected &&
                                                            !allSelected
                                                        }
                                                        onChange={() =>
                                                            handleResourceToggle(
                                                                resource,
                                                            )
                                                        }
                                                    />
                                                }
                                                label={
                                                    <Typography
                                                        variant="subtitle1"
                                                        fontWeight="bold"
                                                    >
                                                        {resource}
                                                    </Typography>
                                                }
                                            />
                                            <Box sx={{ ml: 4 }}>
                                                <Grid container spacing={1}>
                                                    {permissions.map(
                                                        (permission) => (
                                                            <Grid
                                                                key={
                                                                    permission.id
                                                                }
                                                                size={{
                                                                    xs: 12,
                                                                    sm: 6,
                                                                    md: 4,
                                                                }}
                                                            >
                                                                <FormControlLabel
                                                                    control={
                                                                        <Checkbox
                                                                            checked={formData.permissionIds.includes(
                                                                                permission.id,
                                                                            )}
                                                                            onChange={() =>
                                                                                handlePermissionToggle(
                                                                                    permission.id,
                                                                                )
                                                                            }
                                                                        />
                                                                    }
                                                                    label={
                                                                        <Box>
                                                                            <Typography variant="body2">
                                                                                {
                                                                                    permission.action
                                                                                }
                                                                            </Typography>
                                                                            <Typography
                                                                                variant="caption"
                                                                                color="text.secondary"
                                                                            >
                                                                                {permission.description ||
                                                                                    "-"}
                                                                            </Typography>
                                                                        </Box>
                                                                    }
                                                                />
                                                            </Grid>
                                                        ),
                                                    )}
                                                </Grid>
                                            </Box>
                                        </Paper>
                                    );
                                },
                            )}
                        </Grid>
                    </Grid>
                </Box>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={saving}>
                    Cancel
                </Button>
                <Button
                    onClick={handleSave}
                    variant="contained"
                    disabled={saving}
                >
                    {saving ? "Saving..." : "Save"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
