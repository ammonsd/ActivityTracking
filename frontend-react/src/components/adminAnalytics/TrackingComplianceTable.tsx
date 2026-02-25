/**
 * Description: Tracking Compliance table component. Shows per-user compliance rates
 * for logging time on weekdays within the selected date range, surfacing users who
 * frequently miss days. Requires a specific date range to function.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import {
    Alert,
    Box,
    Chip,
    LinearProgress,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tooltip,
    Typography,
} from "@mui/material";
import type { TrackingComplianceDto } from "../../types/reports.types";

interface Props {
    data: TrackingComplianceDto[];
    /** If false, a date range hasn't been selected and we show an info message */
    hasDateRange: boolean;
}

/**
 * Returns a chip color based on compliance bracket.
 */
function complianceColor(rate: number): "success" | "warning" | "error" {
    if (rate >= 90) return "success";
    if (rate >= 70) return "warning";
    return "error";
}

/**
 * Tracking Compliance — table sorted by compliance rate ascending (worst first).
 * Shows missed dates in a collapsed tooltip to avoid overwhelming the view.
 */
export default function TrackingComplianceTable({ data, hasDateRange }: Readonly<Props>) {
    if (!hasDateRange) {
        return (
            <Alert severity="info">
                Select a specific start and end date to compute tracking
                compliance. The report counts weekdays (Mon–Fri) in the selected
                range and checks how many each user actually logged hours on.
            </Alert>
        );
    }

    if (!data.length) {
        return (
            <Typography color="text.secondary" sx={{ p: 2 }}>
                No user activity found for the selected period.
            </Typography>
        );
    }

    return (
        <TableContainer component={Paper} variant="outlined">
            <Table size="small">
                <TableHead>
                    <TableRow sx={{ bgcolor: "grey.100" }}>
                        <TableCell>User</TableCell>
                        <TableCell align="right">Workdays in Range</TableCell>
                        <TableCell align="right">Days Logged</TableCell>
                        <TableCell align="right">Days Missing</TableCell>
                        <TableCell sx={{ minWidth: 220 }}>
                            Compliance Rate
                        </TableCell>
                        <TableCell>Recent Missed Dates</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {data.map((row) => (
                        <TableRow key={row.username} hover>
                            <TableCell>
                                <Typography variant="body2" fontWeight={600}>
                                    {row.username}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography variant="body2">
                                    {row.totalWorkdays}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography
                                    variant="body2"
                                    color="success.dark"
                                >
                                    {row.daysLogged}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography
                                    variant="body2"
                                    color={
                                        row.daysMissing > 0
                                            ? "error.dark"
                                            : "text.primary"
                                    }
                                >
                                    {row.daysMissing}
                                </Typography>
                            </TableCell>
                            <TableCell>
                                <Box
                                    sx={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: 1,
                                    }}
                                >
                                    <Box sx={{ flex: 1 }}>
                                        <LinearProgress
                                            variant="determinate"
                                            value={row.complianceRate}
                                            color={complianceColor(
                                                row.complianceRate,
                                            )}
                                            sx={{ height: 10, borderRadius: 5 }}
                                        />
                                    </Box>
                                    <Chip
                                        label={`${row.complianceRate.toFixed(0)}%`}
                                        size="small"
                                        color={complianceColor(
                                            row.complianceRate,
                                        )}
                                    />
                                </Box>
                            </TableCell>
                            <TableCell>
                                {row.recentMissedDates.length > 0 ? (
                                    <Tooltip
                                        title={
                                            <>
                                                <Typography
                                                    variant="caption"
                                                    fontWeight={600}
                                                >
                                                    Most recent missed days:
                                                </Typography>
                                                <br />
                                                {row.recentMissedDates.join(
                                                    ", ",
                                                )}
                                            </>
                                        }
                                    >
                                        <Typography
                                            variant="caption"
                                            sx={{
                                                cursor: "help",
                                                borderBottom: "1px dotted",
                                            }}
                                        >
                                            {row.recentMissedDates
                                                .slice(-3)
                                                .join(", ")}
                                            {row.recentMissedDates.length > 3 &&
                                                ` …+${row.recentMissedDates.length - 3} more`}
                                        </Typography>
                                    </Tooltip>
                                ) : (
                                    <Typography
                                        variant="caption"
                                        color="success.dark"
                                    >
                                        None
                                    </Typography>
                                )}
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
