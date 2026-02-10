/**
 * Description: Dialog for adding a new category with subcategory and initial value
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import { useState } from "react";
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    TextField,
    Box,
} from "@mui/material";

interface AddCategoryDialogProps {
    open: boolean;
    onClose: () => void;
    onAdd: (category: string, subcategory: string, value: string) => void;
}

export const AddCategoryDialog: React.FC<AddCategoryDialogProps> = ({
    open,
    onClose,
    onAdd,
}) => {
    const [category, setCategory] = useState("");
    const [subcategory, setSubcategory] = useState("");
    const [value, setValue] = useState("");

    const handleSubmit = () => {
        if (category.trim() && subcategory.trim() && value.trim()) {
            onAdd(
                category.trim().toUpperCase(),
                subcategory.trim().toUpperCase(),
                value.trim(),
            );
            handleClose();
        }
    };

    const handleClose = () => {
        setCategory("");
        setSubcategory("");
        setValue("");
        onClose();
    };

    return (
        <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
            <DialogTitle>Add New Category</DialogTitle>
            <DialogContent>
                <Box
                    sx={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 2,
                        mt: 1,
                    }}
                >
                    <TextField
                        label="Category *"
                        value={category}
                        onChange={(e) =>
                            setCategory(e.target.value.toUpperCase())
                        }
                        fullWidth
                        inputProps={{ maxLength: 50 }}
                        placeholder="Enter category name"
                    />
                    <TextField
                        label="Subcategory *"
                        value={subcategory}
                        onChange={(e) =>
                            setSubcategory(e.target.value.toUpperCase())
                        }
                        fullWidth
                        inputProps={{ maxLength: 50 }}
                        placeholder="Enter subcategory"
                    />
                    <TextField
                        label="Value *"
                        value={value}
                        onChange={(e) => setValue(e.target.value)}
                        fullWidth
                        inputProps={{ maxLength: 255 }}
                        placeholder="Enter first dropdown value"
                    />
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
                    disabled={
                        !category.trim() || !subcategory.trim() || !value.trim()
                    }
                >
                    Add Category
                </Button>
            </DialogActions>
        </Dialog>
    );
};
