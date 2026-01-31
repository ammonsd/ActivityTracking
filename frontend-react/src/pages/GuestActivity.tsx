/**
 * Guest Activity placeholder page.
 * Will be implemented in Phase 6.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { Box, Typography, Paper } from "@mui/material";
import ConstructionIcon from "@mui/icons-material/Construction";

export const GuestActivity: React.FC = () => {
    return (
        <Box>
            <Typography variant="h4" gutterBottom fontWeight="bold">
                Guest Activity
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                View guest login activity and statistics
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
                    Guest Activity functionality will be available in Phase 6
                    implementation.
                </Typography>
            </Paper>
        </Box>
    );
};
