/**
 * Description: User Performance Summary table for Admin Analytics.
 * Displays per-user statistics including total hours, billable/non-billable split,
 * average daily hours, task count, top client/project, and last activity date.
 * Admin-only component; data is pre-computed by the parent via reportsUtils.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import React from "react";
import {
    Box,
    Typography,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Chip,
    LinearProgress,
    Tooltip,
} from "@mui/material";
import EmojiEventsIcon from "@mui/icons-material/EmojiEvents";
import type { UserSummaryDto } from "../../types/reports.types";

interface UserSummaryTableProps {
    summaries: UserSummaryDto[];
}

/**
 * Returns a medal icon color for top-3 ranks.
 */
function getRankColor(index: number): string {
    if (index === 0) return "#FFD700"; // Gold
    if (index === 1) return "#C0C0C0"; // Silver
    if (index === 2) return "#CD7F32"; // Bronze
    return "transparent";
}

/**
 * Returns the billability chip color based on percentage.
 * Green >= 80%, Yellow >= 50%, Red < 50%.
 */
function getBillabilityChipColor(
    rate: number,
): "success" | "warning" | "error" | "default" {
    if (rate >= 80) return "success";
    if (rate >= 50) return "warning";
    if (rate > 0) return "error";
    return "default";
}

/**
 * Returns the LinearProgress color (no 'default' in MUI progress).
 */
function getBillabilityBarColor(
    rate: number,
): "success" | "warning" | "error" | "inherit" {
    if (rate >= 80) return "success";
    if (rate >= 50) return "warning";
    if (rate > 0) return "error";
    return "inherit";
}

export const UserSummaryTable: React.FC<UserSummaryTableProps> = ({
    summaries,
}) => {
    if (summaries.length === 0) {
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
                    No user data available for the selected period.
                </Typography>
            </Box>
        );
    }

    return (
        <Box>
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
                User Performance Summary
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Total hours, billable split, and activity metrics per user for
                the selected period.
            </Typography>

            <TableContainer component={Paper} elevation={0} variant="outlined">
                <Table size="small" stickyHeader>
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 700, width: 40 }}>
                                #
                            </TableCell>
                            <TableCell sx={{ fontWeight: 700 }}>User</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>
                                Total Hrs
                            </TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>
                                Billable
                            </TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>
                                Non-Billable
                            </TableCell>
                            <TableCell
                                align="center"
                                sx={{ fontWeight: 700, minWidth: 130 }}
                            >
                                Billability %
                            </TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>
                                Avg Bill/Day
                            </TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>
                                Tasks
                            </TableCell>
                            <TableCell sx={{ fontWeight: 700 }}>
                                Top Client
                            </TableCell>
                            <TableCell sx={{ fontWeight: 700 }}>
                                Top Project
                            </TableCell>
                            <TableCell sx={{ fontWeight: 700 }}>
                                Last Active
                            </TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {summaries.map((row, idx) => {
                            const rankColor = getRankColor(idx);
                            const showMedal = idx < 3;

                            return (
                                <TableRow
                                    key={row.username}
                                    hover
                                    sx={{
                                        backgroundColor:
                                            idx === 0
                                                ? "rgba(255, 215, 0, 0.04)"
                                                : "inherit",
                                    }}
                                >
                                    {/* Rank */}
                                    <TableCell>
                                        {showMedal ? (
                                            <Tooltip title={`Rank #${idx + 1}`}>
                                                <EmojiEventsIcon
                                                    sx={{
                                                        fontSize: 18,
                                                        color: rankColor,
                                                        verticalAlign: "middle",
                                                    }}
                                                />
                                            </Tooltip>
                                        ) : (
                                            <Typography
                                                variant="body2"
                                                color="text.secondary"
                                            >
                                                {idx + 1}
                                            </Typography>
                                        )}
                                    </TableCell>

                                    {/* Username */}
                                    <TableCell>
                                        <Typography
                                            variant="body2"
                                            fontWeight={idx === 0 ? 700 : 400}
                                        >
                                            {row.username}
                                        </Typography>
                                    </TableCell>

                                    {/* Total Hours */}
                                    <TableCell align="right">
                                        <Typography
                                            variant="body2"
                                            fontWeight={600}
                                        >
                                            {row.totalHours}h
                                        </Typography>
                                    </TableCell>

                                    {/* Billable */}
                                    <TableCell align="right">
                                        <Typography
                                            variant="body2"
                                            color="success.main"
                                        >
                                            {row.billableHours}h
                                        </Typography>
                                    </TableCell>

                                    {/* Non-Billable */}
                                    <TableCell align="right">
                                        <Typography
                                            variant="body2"
                                            color={
                                                row.nonBillableHours > 0
                                                    ? "error.main"
                                                    : "text.secondary"
                                            }
                                            fontWeight={
                                                row.nonBillableHours > 0
                                                    ? 600
                                                    : 400
                                            }
                                        >
                                            {row.nonBillableHours}h
                                        </Typography>
                                    </TableCell>

                                    {/* Billability Rate */}
                                    <TableCell>
                                        <Box
                                            sx={{
                                                display: "flex",
                                                alignItems: "center",
                                                gap: 1,
                                            }}
                                        >
                                            <Box sx={{ flexGrow: 1 }}>
                                                <LinearProgress
                                                    variant="determinate"
                                                    value={row.billabilityRate}
                                                    color={getBillabilityBarColor(
                                                        row.billabilityRate,
                                                    )}
                                                    sx={{
                                                        height: 6,
                                                        borderRadius: 1,
                                                    }}
                                                />
                                            </Box>
                                            <Chip
                                                label={`${row.billabilityRate}%`}
                                                size="small"
                                                color={getBillabilityChipColor(
                                                    row.billabilityRate,
                                                )}
                                                variant="outlined"
                                                sx={{
                                                    fontSize: "0.7rem",
                                                    height: 20,
                                                    minWidth: 50,
                                                }}
                                            />
                                        </Box>
                                    </TableCell>

                                    {/* Avg Billable Per Day */}
                                    <TableCell align="right">
                                        <Typography variant="body2">
                                            {row.avgHoursPerDay}h
                                        </Typography>
                                    </TableCell>

                                    {/* Task Count */}
                                    <TableCell align="right">
                                        <Typography variant="body2">
                                            {row.taskCount}
                                        </Typography>
                                    </TableCell>

                                    {/* Top Client */}
                                    <TableCell>
                                        <Typography
                                            variant="body2"
                                            sx={{
                                                maxWidth: 140,
                                                overflow: "hidden",
                                                textOverflow: "ellipsis",
                                                whiteSpace: "nowrap",
                                            }}
                                            title={row.topClient}
                                        >
                                            {row.topClient}
                                        </Typography>
                                    </TableCell>

                                    {/* Top Project */}
                                    <TableCell>
                                        <Typography
                                            variant="body2"
                                            sx={{
                                                maxWidth: 140,
                                                overflow: "hidden",
                                                textOverflow: "ellipsis",
                                                whiteSpace: "nowrap",
                                            }}
                                            title={row.topProject}
                                        >
                                            {row.topProject}
                                        </Typography>
                                    </TableCell>

                                    {/* Last Activity */}
                                    <TableCell>
                                        <Typography
                                            variant="body2"
                                            color="text.secondary"
                                        >
                                            {row.lastActivityDate}
                                        </Typography>
                                    </TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};
