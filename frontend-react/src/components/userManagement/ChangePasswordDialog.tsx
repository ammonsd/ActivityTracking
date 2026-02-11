/**
 * Description: Change Password Dialog for updating user passwords
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
    FormControlLabel,
    Checkbox,
    InputAdornment,
    IconButton,
} from "@mui/material";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";

interface ChangePasswordDialogProps {
    open: boolean;
    onClose: () => void;
    onSave: (password: string, forceChange: boolean) => Promise<void>;
    username: string;
}

export const ChangePasswordDialog: React.FC<ChangePasswordDialogProps> = ({
    open,
    onClose,
    onSave,
    username,
}) => {
    const [formData, setFormData] = useState({
        password: "",
        confirmPassword: "",
        forcePasswordUpdate: true,
    });
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    useEffect(() => {
        if (open) {
            setFormData({
                password: "",
                confirmPassword: "",
                forcePasswordUpdate: true,
            });
            setError(null);
        }
    }, [open]);

    const validateForm = (): boolean => {
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
        return true;
    };

    const handleSave = async () => {
        if (!validateForm()) return;

        setSaving(true);
        setError(null);

        try {
            await onSave(formData.password, formData.forcePasswordUpdate);
            onClose();
        } catch (err: any) {
            setError(
                err.response?.data?.message || "Failed to change password",
            );
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Change Password for {username}</DialogTitle>
            <DialogContent>
                {error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {error}
                    </Alert>
                )}

                <Alert severity="warning" sx={{ mt: 1, mb: 2 }}>
                    <strong>🔒 Changing password for user:</strong>
                    <br />
                    {username}
                </Alert>

                <Box sx={{ pt: 1 }}>
                    <TextField
                        fullWidth
                        label="New Password"
                        type={showPassword ? "text" : "password"}
                        value={formData.password}
                        onChange={(e) =>
                            setFormData((prev) => ({
                                ...prev,
                                password: e.target.value,
                            }))
                        }
                        sx={{ mb: 2 }}
                        required
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        onClick={() =>
                                            setShowPassword(!showPassword)
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

                    <TextField
                        fullWidth
                        label="Confirm Password"
                        type={showPassword ? "text" : "password"}
                        value={formData.confirmPassword}
                        onChange={(e) =>
                            setFormData((prev) => ({
                                ...prev,
                                confirmPassword: e.target.value,
                            }))
                        }
                        sx={{ mb: 2 }}
                        required
                        InputProps={{
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        onClick={() =>
                                            setShowPassword(!showPassword)
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

                    <Alert severity="info" sx={{ mb: 2 }}>
                        <strong>Password must be:</strong>
                        <ul
                            style={{ margin: "8px 0 0 0", paddingLeft: "20px" }}
                        >
                            <li>At least 10 characters long</li>
                            <li>Include at least 1 uppercase letter</li>
                            <li>Include at least 1 digit</li>
                            <li>
                                Include at least 1 special character (+&%$#@!~*)
                            </li>
                            <li>
                                Not contain more than 2 consecutive identical
                                characters
                            </li>
                            <li>Not contain the username (case-insensitive)</li>
                            <li>Not be the same as your current password</li>
                            <li>
                                Cannot match any of your previous 5 passwords
                            </li>
                        </ul>
                    </Alert>

                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={showPassword}
                                onChange={(e) =>
                                    setShowPassword(e.target.checked)
                                }
                            />
                        }
                        label="Show passwords"
                        sx={{ mb: 1 }}
                    />

                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={formData.forcePasswordUpdate}
                                onChange={(e) =>
                                    setFormData((prev) => ({
                                        ...prev,
                                        forcePasswordUpdate: e.target.checked,
                                    }))
                                }
                            />
                        }
                        label="Force Password Update (user must change password on next login)"
                    />
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
                    {saving ? "Saving..." : "Change Password"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
