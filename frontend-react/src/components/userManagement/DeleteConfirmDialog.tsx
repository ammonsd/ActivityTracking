/**
 * Description: Delete Confirmation Dialog for user deletion
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import React from "react";
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Typography,
    Alert,
} from "@mui/material";
import WarningIcon from "@mui/icons-material/Warning";

interface DeleteConfirmDialogProps {
    open: boolean;
    onClose: () => void;
    onConfirm: () => Promise<void>;
    username: string;
}

export const DeleteConfirmDialog: React.FC<DeleteConfirmDialogProps> = ({
    open,
    onClose,
    onConfirm,
    username,
}) => {
    const [deleting, setDeleting] = React.useState(false);

    const handleConfirm = async () => {
        setDeleting(true);
        try {
            await onConfirm();
            onClose();
        } catch (error) {
            console.error("Error deleting user:", error);
        } finally {
            setDeleting(false);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <WarningIcon color="error" />
                Confirm Delete
            </DialogTitle>
            <DialogContent>
                <Alert severity="warning" sx={{ mb: 2 }}>
                    This action cannot be undone!
                </Alert>
                <Typography>
                    Are you sure you want to delete the user{" "}
                    <strong>{username}</strong>?
                </Typography>
                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ mt: 2 }}
                >
                    All data associated with this user will be permanently
                    removed.
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={deleting}>
                    Cancel
                </Button>
                <Button
                    onClick={handleConfirm}
                    color="error"
                    variant="contained"
                    disabled={deleting}
                >
                    {deleting ? "Deleting..." : "Delete"}
                </Button>
            </DialogActions>
        </Dialog>
    );
};
