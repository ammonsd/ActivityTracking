/**
 * Description: User Form Dialog for creating and editing users
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
    MenuItem,
    FormControlLabel,
    Checkbox,
    Alert,
    Box,
    Grid,
    InputAdornment,
    IconButton,
} from "@mui/material";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import type { User, Role } from "../../types/userManagement.types";

interface UserFormDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (userData: any) => Promise<void>;
    user: User | null; // null for "Add", populated for "Edit"
    roles: Role[];
}

export const UserFormDialog: React.FC<UserFormDialogProps> = ({
    open,
    onClose,
    onSave,
    user,
    roles,
}) => {
    const isEditMode = user !== null;

    const [formData, setFormData] = useState({
        username: "",
        password: "",
        confirmPassword: "",
        firstname: "",
        lastname: "",
        company: "",
        email: "",
        roleId: "",
        enabled: true,
        forcePasswordUpdate: true,
        accountLocked: false,
    });

    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    // Populate form when editing
    useEffect(() => {
        if (user) {
            const userRole = roles.find((r) => r.name === user.role);
            setFormData({
                username: user.username || "",
                password: "",
                confirmPassword: "",
                firstname: user.firstname || "",
                lastname: user.lastname || "",
                company: user.company || "",
                email: user.email || "",
                roleId: userRole?.id.toString() || "",
                enabled: user.enabled,
                forcePasswordUpdate: user.forcePasswordUpdate,
                accountLocked: user.accountLocked,
            });
        } else {
            // Reset for add mode
            setFormData({
                username: "",
                password: "",
                confirmPassword: "",
                firstname: "",
                lastname: "",
                company: "",
                email: "",
                roleId: "",
                enabled: true,
                forcePasswordUpdate: true,
                accountLocked: false,
            });
        }
        setError(null);
    }, [user, roles, open]);

    const handleChange = (field: string, value: any) => {
        setFormData((prev) => ({ ...prev, [field]: value }));
        setError(null);
    };

    const validateForm = (): boolean => {
        if (!formData.username.trim()) {
            setError("Username is required");
            return false;
        }
        if (!formData.lastname.trim()) {
            setError("Last name is required");
            return false;
        }
        if (!formData.roleId) {
            setError("Role is required");
            return false;
        }

        // Password validation only for new users
        if (!isEditMode) {
            if (!formData.password) {
                setError("Password is required");
                return false;
            }
            if (formData.password.length < 10) {
                setError("Password must be at least 10 characters long");
                return false;
            }
            if (!/[A-Z]/.test(formData.password)) {
                setError("Password must include at least 1 uppercase letter");
                return false;
            }
            if (!/[0-9]/.test(formData.password)) {
                setError("Password must include at least 1 digit");
                return false;
            }
            if (!/[+&%$#@!~*]/.test(formData.password)) {
                setError(
                    "Password must include at least 1 special character (+&%$#@!~*)",
                );
                return false;
            }
            if (/(.)\1{2,}/.test(formData.password)) {
                setError(
                    "Password cannot contain more than 2 consecutive identical characters",
                );
                return false;
            }
            if (formData.password !== formData.confirmPassword) {
                setError("Passwords do not match");
                return false;
            }
        }

        if (formData.email && !formData.email.includes("@")) {
            setError("Please provide a valid email address");
            return false;
        }

        return true;
    };

    const handleSave = async () => {
        if (!validateForm()) return;

        setSaving(true);
        setError(null);

        try {
            const selectedRole = roles.find(
                (r) => r.id === parseInt(formData.roleId),
            );

            const userData: any = {
                username: formData.username.trim(),
                firstname: formData.firstname.trim() || null,
                lastname: formData.lastname.trim(),
                company: formData.company.trim() || null,
                email: formData.email.trim() || null,
                role: {
                    id: selectedRole!.id,
                    name: selectedRole!.name,
                },
                enabled: formData.enabled,
                forcePasswordUpdate: formData.forcePasswordUpdate,
            };

            // Add password for new users
            if (!isEditMode) {
                userData.password = formData.password;
            } else {
                // Add accountLocked for edit mode
                userData.accountLocked = formData.accountLocked;
            }

            await onSave(userData);
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || "Failed to save user");
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>
                {isEditMode ? "Edit User" : "Add New User"}
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
                                label="Username"
                                value={formData.username}
                                onChange={(e) =>
                                    handleChange("username", e.target.value)
                                }
                                required
                                disabled={isEditMode} // Username can't be changed
                            />
                        </Grid>

                        {!isEditMode && (
                            <>
                                <Grid size={{ xs: 12 }}>
                                    <TextField
                                        fullWidth
                                        label="Password"
                                        type={
                                            showPassword ? "text" : "password"
                                        }
                                        value={formData.password}
                                        onChange={(e) =>
                                            handleChange(
                                                "password",
                                                e.target.value,
                                            )
                                        }
                                        required
                                        InputProps={{
                                            endAdornment: (
                                                <InputAdornment position="end">
                                                    <IconButton
                                                        onClick={() =>
                                                            setShowPassword(
                                                                !showPassword,
                                                            )
                                                        }
                                                        edge="end"
                                                    >
                                                        {showPassword ? (
                                                            <VisibilityOffIcon />
                                                        ) : (
                                                            <VisibilityIcon />
                                                        )}
                                                    </IconButton>
                                                </InputAdornment>
                                            ),
                                        }}
                                    />
                                </Grid>
                                <Grid size={{ xs: 12 }}>
                                    <TextField
                                        fullWidth
                                        label="Confirm Password"
                                        type={
                                            showPassword ? "text" : "password"
                                        }
                                        value={formData.confirmPassword}
                                        onChange={(e) =>
                                            handleChange(
                                                "confirmPassword",
                                                e.target.value,
                                            )
                                        }
                                        required
                                        InputProps={{
                                            endAdornment: (
                                                <InputAdornment position="end">
                                                    <IconButton
                                                        onClick={() =>
                                                            setShowPassword(
                                                                !showPassword,
                                                            )
                                                        }
                                                        edge="end"
                                                    >
                                                        {showPassword ? (
                                                            <VisibilityOffIcon />
                                                        ) : (
                                                            <VisibilityIcon />
                                                        )}
                                                    </IconButton>
                                                </InputAdornment>
                                            ),
                                        }}
                                    />
                                </Grid>
                                <Grid size={{ xs: 12 }}>
                                    <Alert severity="info" sx={{ mt: 1 }}>
                                        <strong>Password must be:</strong>
                                        <ul
                                            style={{
                                                margin: "8px 0 0 0",
                                                paddingLeft: "20px",
                                            }}
                                        >
                                            <li>At least 10 characters long</li>
                                            <li>
                                                Include at least 1 uppercase
                                                letter
                                            </li>
                                            <li>Include at least 1 digit</li>
                                            <li>
                                                Include at least 1 special
                                                character (+&%$#@!~*)
                                            </li>
                                            <li>
                                                Not contain more than 2
                                                consecutive identical characters
                                            </li>
                                        </ul>
                                    </Alert>
                                </Grid>
                                <Grid size={{ xs: 12 }}>
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={showPassword}
                                                onChange={(e) =>
                                                    setShowPassword(
                                                        e.target.checked,
                                                    )
                                                }
                                            />
                                        }
                                        label="Show passwords"
                                    />
                                </Grid>
                            </>
                        )}

                        <Grid size={{ xs: 12, sm: 6 }}>
                            <TextField
                                fullWidth
                                label="First Name"
                                value={formData.firstname}
                                onChange={(e) =>
                                    handleChange("firstname", e.target.value)
                                }
                            />
                        </Grid>

                        <Grid size={{ xs: 12, sm: 6 }}>
                            <TextField
                                fullWidth
                                label="Last Name"
                                value={formData.lastname}
                                onChange={(e) =>
                                    handleChange("lastname", e.target.value)
                                }
                                required
                            />
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <TextField
                                fullWidth
                                label="Company"
                                value={formData.company}
                                onChange={(e) =>
                                    handleChange("company", e.target.value)
                                }
                            />
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <TextField
                                fullWidth
                                label="Email"
                                type="email"
                                value={formData.email}
                                onChange={(e) =>
                                    handleChange("email", e.target.value)
                                }
                            />
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <TextField
                                fullWidth
                                select
                                label="Role"
                                value={formData.roleId}
                                onChange={(e) =>
                                    handleChange("roleId", e.target.value)
                                }
                                required
                            >
                                {roles.map((role) => (
                                    <MenuItem key={role.id} value={role.id}>
                                        {role.name}
                                    </MenuItem>
                                ))}
                            </TextField>
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        checked={formData.enabled}
                                        onChange={(e) =>
                                            handleChange(
                                                "enabled",
                                                e.target.checked,
                                            )
                                        }
                                    />
                                }
                                label="Account Enabled"
                            />
                        </Grid>

                        <Grid size={{ xs: 12 }}>
                            <FormControlLabel
                                control={
                                    <Checkbox
                                        checked={formData.forcePasswordUpdate}
                                        onChange={(e) =>
                                            handleChange(
                                                "forcePasswordUpdate",
                                                e.target.checked,
                                            )
                                        }
                                    />
                                }
                                label="Force Password Change on Next Login"
                            />
                        </Grid>

                        {isEditMode && (
                            <Grid size={{ xs: 12 }}>
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            checked={formData.accountLocked}
                                            onChange={(e) =>
                                                handleChange(
                                                    "accountLocked",
                                                    e.target.checked,
                                                )
                                            }
                                        />
                                    }
                                    label="Account Locked"
                                />
                            </Grid>
                        )}
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
