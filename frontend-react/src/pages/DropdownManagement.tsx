/**
 * Description: Dropdown Management page - manage dropdown values for tasks and expenses
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import { useState, useEffect } from "react";
import {
    Box,
    Typography,
    Button,
    CircularProgress,
    Alert,
    Snackbar,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import { dropdownApi } from "../api/dropdown.api";
import type { DropdownValue } from "../types/dropdown.types";
import { FilterSection } from "../components/dropdownManagement/FilterSection";
import { AddCategoryDialog } from "../components/dropdownManagement/AddCategoryDialog";
import { AddValueForm } from "../components/dropdownManagement/AddValueForm";
import { DropdownTable } from "../components/dropdownManagement/DropdownTable";
import { EditDialog } from "../components/dropdownManagement/EditDialog";
import { DeleteConfirmDialog } from "../components/dropdownManagement/DeleteConfirmDialog";
import { StatsSection } from "../components/dropdownManagement/StatsSection";

export const DropdownManagement: React.FC = () => {
    // State management
    const [allValues, setAllValues] = useState<DropdownValue[]>([]);
    const [filteredValues, setFilteredValues] = useState<DropdownValue[]>([]);
    const [categories, setCategories] = useState<string[]>([]);
    const [subcategories, setSubcategories] = useState<string[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // Filter state
    const [selectedCategory, setSelectedCategory] = useState<string>("");
    const [selectedSubcategory, setSelectedSubcategory] = useState<string>("");

    // Dialog state
    const [addCategoryDialogOpen, setAddCategoryDialogOpen] = useState(false);
    const [editDialogOpen, setEditDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [selectedValue, setSelectedValue] = useState<DropdownValue | null>(null);

    // Snackbar state
    const [snackbar, setSnackbar] = useState<{
        open: boolean;
        message: string;
        severity: "success" | "error" | "info" | "warning";
    }>({
        open: false,
        message: "",
        severity: "success",
    });

    // Fetch initial data
    useEffect(() => {
        fetchCategories();
        fetchAllValues();
    }, []);

    // Update filtered values when category/subcategory changes
    useEffect(() => {
        filterValues();
    }, [selectedCategory, selectedSubcategory, allValues]);

    // Update subcategories when category changes
    useEffect(() => {
        if (selectedCategory) {
            const subsSet = new Set(
                allValues
                    .filter((v) => v.category === selectedCategory)
                    .map((v) => v.subcategory)
            );
            const subs = Array.from(subsSet).sort((a, b) => a.localeCompare(b));
            setSubcategories(subs);
        } else {
            setSubcategories([]);
        }
        setSelectedSubcategory("");
    }, [selectedCategory, allValues]);

    const fetchCategories = async () => {
        try {
            const cats = await dropdownApi.fetchCategories();
            const sortedCats = [...cats].sort((a, b) => a.localeCompare(b));
            setCategories(sortedCats);
        } catch (err) {
            console.error("Error fetching categories:", err);
            showSnackbar("Failed to load categories", "error");
        }
    };

    const fetchAllValues = async () => {
        setLoading(true);
        setError(null);
        try {
            const values = await dropdownApi.fetchAllValues();
            setAllValues(values);
        } catch (err) {
            console.error("Error fetching dropdown values:", err);
            setError("Failed to load dropdown values. Please try again later.");
            showSnackbar("Failed to load dropdown values", "error");
        } finally {
            setLoading(false);
        }
    };

    const filterValues = () => {
        let filtered = [...allValues];

        if (selectedCategory) {
            filtered = filtered.filter((v) => v.category === selectedCategory);
        }

        if (selectedSubcategory) {
            filtered = filtered.filter((v) => v.subcategory === selectedSubcategory);
        }

        // Sort by category, subcategory, displayOrder
        filtered.sort((a, b) => {
            if (a.category !== b.category) {
                return a.category.localeCompare(b.category);
            }
            if (a.subcategory !== b.subcategory) {
                return a.subcategory.localeCompare(b.subcategory);
            }
            return a.displayOrder - b.displayOrder;
        });

        setFilteredValues(filtered);
    };

    const showSnackbar = (
        message: string,
        severity: "success" | "error" | "info" | "warning"
    ) => {
        setSnackbar({ open: true, message, severity });
    };

    const handleSnackbarClose = () => {
        setSnackbar((prev) => ({ ...prev, open: false }));
    };

    // Add category handler
    const handleAddCategory = async (
        category: string,
        subcategory: string,
        value: string
    ) => {
        try {
            await dropdownApi.addValue({
                category,
                subcategory,
                itemValue: value,
                displayOrder: 0,
                isActive: true,
            });
            showSnackbar("Category added successfully", "success");
            fetchCategories();
            fetchAllValues();
        } catch (err) {
            console.error("Error adding category:", err);
            showSnackbar("Failed to add category", "error");
        }
    };

    // Add value handler
    const handleAddValue = async (subcategory: string, value: string) => {
        if (!selectedCategory) return;

        try {
            await dropdownApi.addValue({
                category: selectedCategory,
                subcategory,
                itemValue: value,
                displayOrder: 0,
                isActive: true,
            });
            showSnackbar("Value added successfully", "success");
            fetchAllValues();
        } catch (err) {
            console.error("Error adding value:", err);
            showSnackbar("Failed to add value", "error");
        }
    };

    // Edit handler
    const handleEdit = (value: DropdownValue) => {
        setSelectedValue(value);
        setEditDialogOpen(true);
    };

    const handleSaveEdit = async (
        id: number,
        itemValue: string,
        displayOrder: number,
        isActive: boolean
    ) => {
        try {
            await dropdownApi.updateValue(id, {
                itemValue,
                displayOrder,
                isActive,
            });
            showSnackbar("Value updated successfully", "success");
            setEditDialogOpen(false);
            fetchAllValues();
        } catch (err) {
            console.error("Error updating value:", err);
            showSnackbar("Failed to update value", "error");
        }
    };

    // Delete handler
    const handleDelete = (value: DropdownValue) => {
        setSelectedValue(value);
        setDeleteDialogOpen(true);
    };

    const handleConfirmDelete = async () => {
        if (!selectedValue) return;

        try {
            await dropdownApi.deleteValue(selectedValue.id);
            showSnackbar("Value deleted successfully", "success");
            setDeleteDialogOpen(false);
            fetchAllValues();
            fetchCategories();
        } catch (err) {
            console.error("Error deleting value:", err);
            showSnackbar("Failed to delete value", "error");
        }
    };

    if (loading) {
        return (
            <Box
                sx={{
                    display: "flex",
                    justifyContent: "center",
                    alignItems: "center",
                    height: "400px",
                }}
            >
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Box>
            {/* Header */}
            <Box
                sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    mb: 3,
                }}
            >
                <Box>
                    <Typography variant="h4" gutterBottom fontWeight="bold">
                        Dropdown Management
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                        Manage dropdown values for tasks and expenses
                    </Typography>
                </Box>
                <Button
                    variant="contained"
                    color="primary"
                    startIcon={<AddIcon />}
                    onClick={() => setAddCategoryDialogOpen(true)}
                >
                    Add New Category
                </Button>
            </Box>

            {/* Error Alert */}
            {error && (
                <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            {/* Filters */}
            <FilterSection
                categories={categories}
                subcategories={subcategories}
                selectedCategory={selectedCategory}
                selectedSubcategory={selectedSubcategory}
                onCategoryChange={setSelectedCategory}
                onSubcategoryChange={setSelectedSubcategory}
            />

            {/* Add Value Form */}
            <AddValueForm
                selectedCategory={selectedCategory}
                onAdd={handleAddValue}
            />

            {/* Table */}
            <Typography variant="h5" gutterBottom fontWeight="bold" sx={{ mt: 3 }}>
                {selectedCategory
                    ? `Current ${selectedCategory} Values`
                    : "All Dropdown Values"}
            </Typography>

            <DropdownTable
                values={filteredValues}
                onEdit={handleEdit}
                onDelete={handleDelete}
            />

            {/* Stats */}
            <StatsSection values={filteredValues} />

            {/* Dialogs */}
            <AddCategoryDialog
                open={addCategoryDialogOpen}
                onClose={() => setAddCategoryDialogOpen(false)}
                onAdd={handleAddCategory}
            />

            <EditDialog
                open={editDialogOpen}
                dropdownValue={selectedValue}
                onClose={() => setEditDialogOpen(false)}
                onSave={handleSaveEdit}
            />

            <DeleteConfirmDialog
                open={deleteDialogOpen}
                dropdownValue={selectedValue}
                onClose={() => setDeleteDialogOpen(false)}
                onConfirm={handleConfirmDelete}
            />

            {/* Snackbar */}
            <Snackbar
                open={snackbar.open}
                autoHideDuration={6000}
                onClose={handleSnackbarClose}
                anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
            >
                <Alert
                    onClose={handleSnackbarClose}
                    severity={snackbar.severity}
                    sx={{ width: "100%" }}
                >
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </Box>
    );
};
