/**
 * User Management placeholder page.
 * Will be implemented in Phase 4.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { Box, Typography, Paper } from "@mui/material";
import ConstructionIcon from "@mui/icons-material/Construction";

export const UserManagement: React.FC = () => {
    return (
        <Box>
            <Typography variant="h4" gutterBottom fontWeight="bold">
                User Management
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                Manage users, roles, and permissions
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
                    User Management functionality will be available in Phase 4
                    implementation.
                </Typography>
            </Paper>
        </Box>
    );
};
