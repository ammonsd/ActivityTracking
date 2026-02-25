/**
 * Description: Phase Distribution by Project table component. Displays a ranked breakdown
 * of hours per project segmented by phase, with proportional visual bars. Helps PM teams
 * understand where effort is concentrated within each project.
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
import type { PhaseDistributionRow } from "../../types/reports.types";

interface Props {
    data: PhaseDistributionRow[];
}

/**
 * Returns a deterministic pastel color for a given phase string so each phase
 * gets a consistent color across rows.
 */
function phaseColor(phase: string): string {
    const palette = [
        "#1976D2",
        "#388E3C",
        "#F57C00",
        "#7B1FA2",
        "#C62828",
        "#00838F",
        "#6D4C41",
        "#37474F",
    ];
    let hash = 0;
    for (let i = 0; i < phase.length; i++)
        hash = (phase.codePointAt(i) ?? 0) + ((hash << 5) - hash);
    return palette[Math.abs(hash) % palette.length];
}

/**
 * Phase Distribution by Project — renders a hierarchical table where each project
 * row expands to show its phase breakdown as proportional horizontal bars.
 */
export default function PhaseDistributionTable({ data }: Readonly<Props>) {
    if (!data.length) {
        return (
            <Typography color="text.secondary" sx={{ p: 2 }}>
                No phase data available for the selected period.
            </Typography>
        );
    }

    const maxHours = data[0]?.totalHours ?? 1;

    return (
        <TableContainer component={Paper} variant="outlined">
            <Table size="small">
                <TableHead>
                    <TableRow sx={{ bgcolor: "grey.100" }}>
                        <TableCell width={28}>#</TableCell>
                        <TableCell>Project</TableCell>
                        <TableCell>Top Client</TableCell>
                        <TableCell align="right">Total Hrs</TableCell>
                        <TableCell>Phase Breakdown</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {data.map((row, idx) => (
                        <TableRow key={row.project} hover>
                            <TableCell>
                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                >
                                    {idx + 1}
                                </Typography>
                            </TableCell>
                            <TableCell>
                                <Typography variant="body2" fontWeight={600}>
                                    {row.project}
                                </Typography>
                                <Typography
                                    variant="caption"
                                    color="text.secondary"
                                >
                                    Top phase: {row.topPhase}
                                </Typography>
                            </TableCell>
                            <TableCell>
                                <Typography variant="body2">
                                    {row.topClient}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography variant="body2" fontWeight={600}>
                                    {row.totalHours.toFixed(1)}
                                </Typography>
                                <Box sx={{ width: 60, ml: "auto" }}>
                                    <LinearProgress
                                        variant="determinate"
                                        value={
                                            (row.totalHours / maxHours) * 100
                                        }
                                        sx={{ height: 4, borderRadius: 2 }}
                                    />
                                </Box>
                            </TableCell>
                            <TableCell sx={{ minWidth: 260 }}>
                                <Box
                                    sx={{
                                        display: "flex",
                                        flexWrap: "wrap",
                                        gap: 0.5,
                                    }}
                                >
                                    {row.phases.slice(0, 8).map((p) => (
                                        <Tooltip
                                            key={p.phase}
                                            title={`${p.hours.toFixed(1)} hrs (${p.percentage.toFixed(1)}%)`}
                                        >
                                            <Chip
                                                label={`${p.phase} ${p.percentage.toFixed(0)}%`}
                                                size="small"
                                                sx={{
                                                    bgcolor: phaseColor(
                                                        p.phase,
                                                    ),
                                                    color: "#fff",
                                                    fontSize: "0.68rem",
                                                }}
                                            />
                                        </Tooltip>
                                    ))}
                                    {row.phases.length > 8 && (
                                        <Chip
                                            label={`+${row.phases.length - 8} more`}
                                            size="small"
                                            variant="outlined"
                                        />
                                    )}
                                </Box>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
