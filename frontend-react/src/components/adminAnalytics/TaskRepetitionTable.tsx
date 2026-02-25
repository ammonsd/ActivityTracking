/**
 * Description: Task Repetition Rate table component. Lists frequently recurring task IDs
 * across the selected period, helping admins identify repetitive work that may be a
 * candidate for automation, templates, or process review.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import {
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
import type { TaskRepetitionDto } from "../../types/reports.types";

interface Props {
    data: TaskRepetitionDto[];
}

/**
 * Task Repetition Rate — sorted by occurrence count descending (top 50).
 * Provides cross-user visibility into the most frequently logged task IDs.
 */
export default function TaskRepetitionTable({ data }: Readonly<Props>) {
    if (!data.length) {
        return (
            <Typography color="text.secondary" sx={{ p: 2 }}>
                No recurring task IDs found for the selected period.
            </Typography>
        );
    }

    const maxOccurrences = data[0]?.occurrences ?? 1;

    return (
        <TableContainer component={Paper} variant="outlined">
            <Table size="small">
                <TableHead>
                    <TableRow sx={{ bgcolor: "grey.100" }}>
                        <TableCell width={28}>#</TableCell>
                        <TableCell>Task ID</TableCell>
                        <TableCell>Sample Description</TableCell>
                        <TableCell align="right">Occurrences</TableCell>
                        <TableCell align="right">Total Hrs</TableCell>
                        <TableCell align="right">Avg Hrs</TableCell>
                        <TableCell align="right">Users</TableCell>
                        <TableCell>Top Client / Project</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {data.map((row, idx) => (
                        <TableRow key={row.taskId} hover>
                            <TableCell>
                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                >
                                    {idx + 1}
                                </Typography>
                            </TableCell>
                            <TableCell>
                                <Typography
                                    variant="body2"
                                    fontWeight={600}
                                    fontFamily="monospace"
                                >
                                    {row.taskId}
                                </Typography>
                            </TableCell>
                            <TableCell sx={{ maxWidth: 200 }}>
                                <Tooltip title={row.sampleDetails}>
                                    <Typography
                                        variant="caption"
                                        color="text.secondary"
                                        noWrap
                                        sx={{ display: "block" }}
                                    >
                                        {row.sampleDetails || "—"}
                                    </Typography>
                                </Tooltip>
                            </TableCell>
                            <TableCell align="right">
                                <Box
                                    sx={{
                                        display: "flex",
                                        alignItems: "center",
                                        gap: 1,
                                        justifyContent: "flex-end",
                                    }}
                                >
                                    <Typography
                                        variant="body2"
                                        fontWeight={600}
                                    >
                                        {row.occurrences}
                                    </Typography>
                                    <Box sx={{ width: 56 }}>
                                        <LinearProgress
                                            variant="determinate"
                                            value={
                                                (row.occurrences /
                                                    maxOccurrences) *
                                                100
                                            }
                                            sx={{ height: 6, borderRadius: 3 }}
                                        />
                                    </Box>
                                </Box>
                            </TableCell>
                            <TableCell align="right">
                                <Typography variant="body2">
                                    {row.totalHours.toFixed(1)}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography variant="body2">
                                    {row.avgHoursPerOccurrence.toFixed(2)}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Chip
                                    label={row.uniqueUsers}
                                    size="small"
                                    variant="outlined"
                                />
                            </TableCell>
                            <TableCell>
                                <Typography variant="caption" display="block">
                                    {row.topClient}
                                </Typography>
                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                    display="block"
                                >
                                    {row.topProject}
                                </Typography>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
