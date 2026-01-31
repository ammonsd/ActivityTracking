/**
 * Dropdown Management placeholder page.
 * Will be implemented in Phase 5.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { Box, Typography, Paper } from "@mui/material";
import ConstructionIcon from "@mui/icons-material/Construction";

export const DropdownManagement: React.FC = () => {
    return (
        <Box>
            <Typography variant="h4" gutterBottom fontWeight="bold">
                Dropdown Management
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                Manage dropdown values for tasks and expenses
            </Typography>

            <Paper
                elevation={3}
                sx={{
                    p: 6,
                    textAlign: "center",
                    backgroundColor: "#f5f5f5",
                }}
            >
                <ConstructionIcon
                    sx={{ fontSize: 64, color: "warning.main", mb: 2 }}
                />
                <Typography variant="h5" gutterBottom>
                    Feature Under Development
                </Typography>
                <Typography variant="body1" color="text.secondary">
                    Dropdown Management functionality will be available in Phase
                    5 implementation.
                </Typography>
            </Paper>
        </Box>
    );
};
