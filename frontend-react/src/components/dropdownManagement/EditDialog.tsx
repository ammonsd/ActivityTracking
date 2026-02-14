/**
 * Description: Dialog for editing dropdown values
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import { useState, useEffect } from "react";
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    Box,
    FormControlLabel,
    Switch,
} from "@mui/material";
import type { DropdownValue } from "../../types/dropdown.types";

interface EditDialogProps {
    open: boolean;
    dropdownValue: DropdownValue | null;
    onClose: () => void;
    onSave: (
        id: number,
        itemValue: string,
        displayOrder: number,
        isActive: boolean,
        nonBillable: boolean,
    ) => void;
}

export const EditDialog: React.FC<EditDialogProps> = ({
    open,
    dropdownValue,
    onClose,
    onSave,
}) => {
    const [itemValue, setItemValue] = useState("");
    const [displayOrder, setDisplayOrder] = useState(0);
    const [isActive, setIsActive] = useState(true);
    const [nonBillable, setNonBillable] = useState(false);

    useEffect(() => {
        if (dropdownValue) {
            setItemValue(dropdownValue.itemValue);
            setDisplayOrder(dropdownValue.displayOrder);
            setIsActive(dropdownValue.isActive);
            setNonBillable(dropdownValue.nonBillable || false);
        }
    }, [dropdownValue]);

    const handleSubmit = () => {
        if (dropdownValue && itemValue.trim()) {
            onSave(
                dropdownValue.id,
                itemValue.trim(),
                displayOrder,
                isActive,
                nonBillable,
            );
        }
    };

    const handleClose = () => {
        onClose();
    };

    return (
        <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
            <DialogTitle>Edit Dropdown Value</DialogTitle>
            <DialogContent>
                <Box
                    sx={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 2,
                        mt: 1,
                    }}
                >
                    {dropdownValue && (
                        <>
                            <TextField
                                label="Category"
                                value={dropdownValue.category}
                                disabled
                                fullWidth
                            />
                            <TextField
                                label="Subcategory"
                                value={dropdownValue.subcategory}
                                disabled
                                fullWidth
                            />
                            <TextField
                                label="Value *"
                                value={itemValue}
                                onChange={(e) => setItemValue(e.target.value)}
                                fullWidth
                                required
                            />
                            <TextField
                                label="Display Order"
                                type="number"
                                value={displayOrder}
                                onChange={(e) =>
                                    setDisplayOrder(
                                        parseInt(e.target.value) || 0,
                                    )
                                }
                                fullWidth
                            />
                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={nonBillable}
                                        onChange={(e) =>
                                            setNonBillable(e.target.checked)
                                        }
                                    />
                                }
                                label="Non-Billable"
                            />
                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={isActive}
                                        onChange={(e) =>
                                            setIsActive(e.target.checked)
                                        }
                                    />
                                }
                                label="Active"
                            />
                        </>
                    )}
                </Box>
            </DialogContent>
            <DialogActions>
                <Button
                    onClick={handleClose}
                    variant="outlined"
                    color="inherit"
                >
                    Cancel
                </Button>
                <Button
                    onClick={handleSubmit}
                    variant="contained"
                    color="primary"
                    disabled={!itemValue.trim()}
                >
                    Save
                </Button>
            </DialogActions>
        </Dialog>
    );
};
