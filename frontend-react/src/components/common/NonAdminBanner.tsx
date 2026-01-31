/**
 * Welcome banner component for non-admin users (USER and GUEST roles).
 * Displays informative message about view-only access.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import { Box, Typography } from "@mui/material";

export const NonAdminBanner: React.FC = () => {
    return (
        <Box
            sx={{
                background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
                color: "white",
                padding: "12px 20px",
                textAlign: "center",
                boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
            }}
        >
            <Box sx={{ maxWidth: 1400, margin: "0 auto" }}>
                <Typography
                    variant="h6"
                    component="strong"
                    sx={{ display: "block", mb: 0.5 }}
                >
                    👋 Welcome, Guest User!
                </Typography>
                <Typography variant="body2">
                    You have view-only access to the system. If you need to make
                    changes, please contact your administrator.
                </Typography>
            </Box>
        </Box>
    );
};
