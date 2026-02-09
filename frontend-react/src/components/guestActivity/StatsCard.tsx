/**
 * Description: Statistics card component for displaying metrics
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import React from "react";
import { Card, CardContent, Typography, Box } from "@mui/material";

interface StatsCardProps {
    title: string;
    value: string | number;
    icon?: React.ReactNode;
    color?: string;
    compact?: boolean; // For date values that need single-line display
}

export const StatsCard: React.FC<StatsCardProps> = ({
    title,
    value,
    icon,
    color = "#5e35b1",
    compact = false,
}) => {
    return (
        <Card
            elevation={2}
            sx={{
                height: "100%",
                background: `linear-gradient(135deg, ${color} 0%, ${color}dd 100%)`,
                color: "white",
            }}
        >
            <CardContent>
                <Box
                    sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        mb: 1,
                    }}
                >
                    <Typography variant="body2" sx={{ opacity: 0.9 }}>
                        {title}
                    </Typography>
                    {icon && <Box sx={{ opacity: 0.8 }}>{icon}</Box>}
                </Box>
                <Typography
                    variant={compact ? "h6" : "h3"}
                    fontWeight="bold"
                    sx={{
                        whiteSpace: compact ? "nowrap" : "normal",
                        fontSize: compact ? "1.1rem" : undefined,
                    }}
                >
                    {value}
                </Typography>
            </CardContent>
        </Card>
    );
};
