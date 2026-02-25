/**
 * Description: Hours by User comparison chart for Admin Analytics.
 * Renders a horizontal bar chart showing total hours per user using MUI components.
 * No external chart library required — uses LinearProgress for visual bar segments
 * with separate billable vs. non-billable breakdown.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import React from "react";
import { Box, Typography, Paper, Tooltip, LinearProgress } from "@mui/material";
import type { UserHoursDto } from "../../types/reports.types";

interface HoursByUserChartProps {
    data: UserHoursDto[];
}

export const HoursByUserChart: React.FC<HoursByUserChartProps> = ({ data }) => {
    if (data.length === 0) {
        return (
            <Box
                sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    py: 6,
                    color: "text.secondary",
                }}
            >
                <Typography>
                    No data available for the selected period.
                </Typography>
            </Box>
        );
    }

    const maxHours = data[0]?.hours ?? 1; // Data is sorted descending

    return (
        <Box>
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                Hours by User
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Total hours per user with billable (green) and non-billable
                (orange) breakdown.
            </Typography>

            {/* Legend */}
            <Box sx={{ display: "flex", gap: 3, mb: 3 }}>
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
                    <Box
                        sx={{
                            width: 14,
                            height: 14,
                            borderRadius: 0.5,
                            bgcolor: "success.main",
                        }}
                    />
                    <Typography variant="caption">Billable</Typography>
                </Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
                    <Box
                        sx={{
                            width: 14,
                            height: 14,
                            borderRadius: 0.5,
                            bgcolor: "warning.main",
                        }}
                    />
                    <Typography variant="caption">Non-Billable</Typography>
                </Box>
            </Box>

            <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                {data.map((item) => {
                    const billablePct =
                        item.hours > 0
                            ? (item.billableHours / item.hours) * 100
                            : 0;
                    const nonBillablePct = 100 - billablePct;
                    const barWidthPct =
                        maxHours > 0 ? (item.hours / maxHours) * 100 : 0;

                    return (
                        <Paper
                            key={item.username}
                            elevation={0}
                            variant="outlined"
                            sx={{ p: 1.5, borderRadius: 1 }}
                        >
                            <Box
                                sx={{
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "space-between",
                                    mb: 1,
                                }}
                            >
                                {/* Username */}
                                <Typography
                                    variant="body2"
                                    fontWeight={600}
                                    sx={{ minWidth: 120 }}
                                >
                                    {item.username}
                                </Typography>

                                {/* Stats */}
                                <Box
                                    sx={{
                                        display: "flex",
                                        gap: 2,
                                        alignItems: "center",
                                    }}
                                >
                                    <Tooltip title="Total hours">
                                        <Typography
                                            variant="body2"
                                            fontWeight={700}
                                        >
                                            {item.hours}h
                                        </Typography>
                                    </Tooltip>
                                    <Tooltip
                                        title={`Billable: ${item.billableHours}h`}
                                    >
                                        <Typography
                                            variant="caption"
                                            color="success.main"
                                        >
                                            ✓ {item.billableHours}h
                                        </Typography>
                                    </Tooltip>
                                    <Tooltip
                                        title={`Non-Billable: ${(item.hours - item.billableHours).toFixed(1)}h`}
                                    >
                                        <Typography
                                            variant="caption"
                                            color="warning.main"
                                        >
                                            ✗{" "}
                                            {(
                                                item.hours - item.billableHours
                                            ).toFixed(1)}
                                            h
                                        </Typography>
                                    </Tooltip>
                                    <Typography
                                        variant="caption"
                                        color="text.secondary"
                                    >
                                        ({item.percentage}% of total)
                                    </Typography>
                                </Box>
                            </Box>

                            {/* Proportional width bar relative to max user */}
                            <Box
                                sx={{ width: `${barWidthPct}%`, minWidth: 40 }}
                            >
                                <Tooltip
                                    title={`${item.billableHours}h billable / ${(item.hours - item.billableHours).toFixed(1)}h non-billable`}
                                >
                                    {/* Segmented bar: billable (green) + non-billable (orange) */}
                                    <Box
                                        sx={{
                                            display: "flex",
                                            height: 16,
                                            borderRadius: 1,
                                            overflow: "hidden",
                                            bgcolor: "grey.200",
                                        }}
                                    >
                                        {billablePct > 0 && (
                                            <Box
                                                sx={{
                                                    width: `${billablePct}%`,
                                                    bgcolor: "success.main",
                                                    transition:
                                                        "width 0.4s ease",
                                                }}
                                            />
                                        )}
                                        {nonBillablePct > 0 && (
                                            <Box
                                                sx={{
                                                    width: `${nonBillablePct}%`,
                                                    bgcolor: "warning.main",
                                                    transition:
                                                        "width 0.4s ease",
                                                }}
                                            />
                                        )}
                                    </Box>
                                </Tooltip>
                            </Box>

                            {/* Billability progress indicator */}
                            <Box sx={{ mt: 0.5 }}>
                                <LinearProgress
                                    variant="determinate"
                                    value={billablePct}
                                    color="success"
                                    sx={{
                                        height: 3,
                                        borderRadius: 1,
                                        bgcolor: "warning.light",
                                    }}
                                />
                            </Box>
                        </Paper>
                    );
                })}
            </Box>
        </Box>
    );
};
