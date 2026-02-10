/**
 * Description: Inline form for adding new dropdown values
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import { useState } from "react";
import {
    Box,
    TextField,
    Button,
    Paper,
    Typography,
    Alert,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";

interface AddValueFormProps {
    selectedCategory: string;
    onAdd: (subcategory: string, value: string) => void;
}

export const AddValueForm: React.FC<AddValueFormProps> = ({
    selectedCategory,
    onAdd,
}) => {
    const [subcategory, setSubcategory] = useState("");
    const [value, setValue] = useState("");

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (subcategory.trim() && value.trim()) {
            onAdd(subcategory.trim(), value.trim());
            setSubcategory("");
            setValue("");
        }
    };

    const isDisabled = !selectedCategory;

    return (
        <Paper elevation={2} sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
                {isDisabled
                    ? "Add New Dropdown Value (Select a category first)"
                    : `Add New ${selectedCategory} Value`}
            </Typography>

            {isDisabled && (
                <Alert severity="info" sx={{ mb: 2 }}>
                    Please select a category from the filter above to add new
                    values
                </Alert>
            )}

            <Box
                component="form"
                onSubmit={handleSubmit}
                sx={{
                    display: "flex",
                    gap: 2,
                    alignItems: "flex-start",
                    opacity: isDisabled ? 0.6 : 1,
                    pointerEvents: isDisabled ? "none" : "auto",
                }}
            >
                <TextField
                    label="Subcategory"
                    value={subcategory}
                    onChange={(e) => setSubcategory(e.target.value)}
                    disabled={isDisabled}
                    required
                    sx={{ flex: 1 }}
                    placeholder="Enter subcategory"
                />
                <TextField
                    label="Value"
                    value={value}
                    onChange={(e) => setValue(e.target.value)}
                    disabled={isDisabled}
                    required
                    sx={{ flex: 2 }}
                    placeholder="Enter dropdown value"
                />
                <Button
                    type="submit"
                    variant="contained"
                    color="success"
                    disabled={isDisabled}
                    startIcon={<AddIcon />}
                    sx={{ mt: 1 }}
                >
                    Add
                </Button>
            </Box>
        </Paper>
    );
};
