/**
 * Description: Delete confirmation dialog for dropdown values
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Typography,
} from "@mui/material";
import type { DropdownValue } from "../../types/dropdown.types";

interface DeleteConfirmDialogProps {
    open: boolean;
    dropdownValue: DropdownValue | null;
    onClose: () => void;
    onConfirm: () => void;
}

export const DeleteConfirmDialog: React.FC<DeleteConfirmDialogProps> = ({
    open,
    dropdownValue,
    onClose,
    onConfirm,
}) => {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Confirm Delete</DialogTitle>
            <DialogContent>
                {dropdownValue && (
                    <Typography>
                        Are you sure you want to delete "
                        {dropdownValue.itemValue}" from {dropdownValue.category}{" "}
                        / {dropdownValue.subcategory}?
                    </Typography>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} variant="outlined" color="inherit">
                    Cancel
                </Button>
                <Button
                    onClick={onConfirm}
                    variant="contained"
                    color="error"
                    autoFocus
                >
                    Delete
                </Button>
            </DialogActions>
        </Dialog>
    );
};
