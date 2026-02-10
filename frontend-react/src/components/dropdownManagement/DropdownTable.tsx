/**
 * Description: Table displaying dropdown values with edit and delete actions
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import {
    Box,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    IconButton,
    Chip,
    Typography,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import type { DropdownValue } from "../../types/dropdown.types";

interface DropdownTableProps {
    values: DropdownValue[];
    onEdit: (value: DropdownValue) => void;
    onDelete: (value: DropdownValue) => void;
}

export const DropdownTable: React.FC<DropdownTableProps> = ({
    values,
    onEdit,
    onDelete,
}) => {
    if (values.length === 0) {
        return (
            <Paper
                elevation={2}
                sx={{
                    p: 6,
                    textAlign: "center",
                    backgroundColor: "#f5f5f5",
                }}
            >
                <Typography variant="body1" color="text.secondary">
                    No dropdown values found.
                </Typography>
            </Paper>
        );
    }

    return (
        <TableContainer component={Paper} elevation={2}>
            <Table>
                <TableHead>
                    <TableRow sx={{ backgroundColor: "#f8f9fa" }}>
                        <TableCell sx={{ fontWeight: "bold" }}>
                            Category
                        </TableCell>
                        <TableCell sx={{ fontWeight: "bold" }}>
                            Subcategory
                        </TableCell>
                        <TableCell sx={{ fontWeight: "bold" }}>Value</TableCell>
                        <TableCell sx={{ fontWeight: "bold" }}>
                            Display Order
                        </TableCell>
                        <TableCell sx={{ fontWeight: "bold" }}>
                            Status
                        </TableCell>
                        <TableCell sx={{ fontWeight: "bold" }}>
                            Actions
                        </TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {values.map((value) => (
                        <TableRow
                            key={value.id}
                            hover
                            sx={{
                                "&:hover": {
                                    backgroundColor: "#f8f9fa",
                                },
                            }}
                        >
                            <TableCell>{value.category}</TableCell>
                            <TableCell>{value.subcategory}</TableCell>
                            <TableCell>{value.itemValue}</TableCell>
                            <TableCell>{value.displayOrder}</TableCell>
                            <TableCell>
                                <Chip
                                    label={
                                        value.isActive ? "Active" : "Inactive"
                                    }
                                    color={
                                        value.isActive ? "success" : "default"
                                    }
                                    size="small"
                                />
                            </TableCell>
                            <TableCell>
                                <Box sx={{ display: "flex", gap: 1 }}>
                                    <IconButton
                                        size="small"
                                        color="warning"
                                        onClick={() => onEdit(value)}
                                        aria-label="Edit"
                                    >
                                        <EditIcon fontSize="small" />
                                    </IconButton>
                                    <IconButton
                                        size="small"
                                        color="error"
                                        onClick={() => onDelete(value)}
                                        aria-label="Delete"
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
    );
};
