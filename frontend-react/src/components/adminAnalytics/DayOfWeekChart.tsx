/**
 * Description: Day of Week Hours chart component. Renders a horizontal bar chart built
 * with pure MUI showing average and total hours logged on each weekday, helping admins
 * identify peak work days and potential coverage gaps.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import {
    Box,
    LinearProgress,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from "@mui/material";
import type { DayOfWeekDto } from "../../types/reports.types";

interface Props {
    data: DayOfWeekDto[];
}

// Weekday/weekend bar color distinction
const WEEKDAY_COLOR = "#1976D2"; // blue
const WEEKEND_COLOR = "#9E9E9E"; // grey

/**
 * Day of Week Hours — combined table + horizontal bar chart.
 * Bars represent total hours; the table shows total, average, and occurrence count.
 */
export default function DayOfWeekChart({ data }: Readonly<Props>) {
    if (!data.length) {
        return (
            <Typography color="text.secondary" sx={{ p: 2 }}>
                No time entry data available for the selected period.
            </Typography>
        );
    }

    const maxTotal = Math.max(...data.map((d) => d.totalHours), 1);
    const maxAvg = Math.max(...data.map((d) => d.avgHoursPerOccurrence), 1);

    return (
        <Box>
            {/* Visual bar chart */}
            <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                    Total Hours by Day of Week
                </Typography>
                <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
                    {data.map((d) => {
                        const isWeekend = d.dayIndex === 0 || d.dayIndex === 6;
                        return (
                            <Box
                                key={d.dayName}
                                sx={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: 1,
                                }}
                            >
                                <Typography
                                    variant="body2"
                                    sx={{
                                        width: 96,
                                        fontWeight: isWeekend ? 400 : 600,
                                        color: isWeekend
                                            ? "text.secondary"
                                            : "text.primary",
                                    }}
                                >
                                    {d.dayName}
                                </Typography>
                                <Box sx={{ flex: 1, position: "relative" }}>
                                    <LinearProgress
                                        variant="determinate"
                                        value={(d.totalHours / maxTotal) * 100}
                                        sx={{
                                            height: 20,
                                            borderRadius: 2,
                                            bgcolor: "grey.200",
                                            "& .MuiLinearProgress-bar": {
                                                bgcolor: isWeekend
                                                    ? WEEKEND_COLOR
                                                    : WEEKDAY_COLOR,
                                            },
                                        }}
                                    />
                                </Box>
                                <Typography
                                    variant="body2"
                                    sx={{ width: 64, textAlign: "right" }}
                                >
                                    {d.totalHours.toFixed(1)} hrs
                                </Typography>
                            </Box>
                        );
                    })}
                </Box>
            </Paper>

            {/* Detail table */}
            <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow sx={{ bgcolor: "grey.100" }}>
                            <TableCell>Day</TableCell>
                            <TableCell align="right">Total Hrs</TableCell>
                            <TableCell align="right">
                                Avg / Occurrence
                            </TableCell>
                            <TableCell align="right">
                                Occurrences in Range
                            </TableCell>
                            <TableCell sx={{ minWidth: 160 }}>
                                Avg Hrs Relative
                            </TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {data.map((d) => {
                            const isWeekend =
                                d.dayIndex === 0 || d.dayIndex === 6;
                            return (
                                <TableRow key={d.dayName} hover>
                                    <TableCell>
                                        <Typography
                                            variant="body2"
                                            fontWeight={isWeekend ? 400 : 600}
                                            color={
                                                isWeekend
                                                    ? "text.secondary"
                                                    : "text.primary"
                                            }
                                        >
                                            {d.dayName}
                                        </Typography>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Typography variant="body2">
                                            {d.totalHours.toFixed(1)}
                                        </Typography>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Typography variant="body2">
                                            {d.avgHoursPerOccurrence.toFixed(2)}
                                        </Typography>
                                    </TableCell>
                                    <TableCell align="right">
                                        <Typography variant="body2">
                                            {d.occurrencesInRange}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <LinearProgress
                                            variant="determinate"
                                            value={
                                                (d.avgHoursPerOccurrence /
                                                    maxAvg) *
                                                100
                                            }
                                            sx={{
                                                height: 8,
                                                borderRadius: 4,
                                                bgcolor: "grey.200",
                                                "& .MuiLinearProgress-bar": {
                                                    bgcolor: isWeekend
                                                        ? WEEKEND_COLOR
                                                        : WEEKDAY_COLOR,
                                                },
                                            }}
                                        />
                                    </TableCell>
                                </TableRow>
                            );
                        })}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}
