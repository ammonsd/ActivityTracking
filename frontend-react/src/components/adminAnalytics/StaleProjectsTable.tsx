/**
 * Description: Stale Projects table component. Highlights projects that have had no
 * time entries for more than a configurable number of days, helping project managers
 * identify abandoned or dormant work.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import {
    Alert,
    Box,
    Chip,
    Paper,
    Slider,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tooltip,
    Typography,
} from "@mui/material";
import { useState } from "react";
import type { StaleProjectDto } from "../../types/reports.types";

interface Props {
    data: StaleProjectDto[];
    /** Initial staleness threshold in days */
    defaultStaleDays?: number;
    /** Called when threshold changes so the parent can re-compute */
    onStaleDaysChange?: (days: number) => void;
}

/** Severity colour ramp: green → amber → red based on staleness bracket. */
function staleColor(days: number): "success" | "warning" | "error" {
    if (days < 60) return "success";
    if (days < 120) return "warning";
    return "error";
}

/**
 * Stale Projects table — renders projects sorted by most-stale first.
 * Includes an inline threshold slider so the user can adjust without re-fetching.
 */
export default function StaleProjectsTable({
    data,
    defaultStaleDays = 30,
    onStaleDaysChange,
}: Readonly<Props>) {
    const [localThreshold, setLocalThreshold] = useState(defaultStaleDays);

    function handleChange(_: Event, val: number | number[]) {
        const v = Array.isArray(val) ? val[0] : val;
        setLocalThreshold(v);
        onStaleDaysChange?.(v);
    }

    if (!data.length) {
        return (
            <Box>
                <StaleSlider value={localThreshold} onChange={handleChange} />
                <Alert severity="success" sx={{ mt: 1 }}>
                    No stale projects — all projects have recent activity within
                    the last {localThreshold} days.
                </Alert>
            </Box>
        );
    }

    return (
        <Box>
            <StaleSlider value={localThreshold} onChange={handleChange} />
            <TableContainer component={Paper} variant="outlined" sx={{ mt: 1 }}>
                <Table size="small">
                    <TableHead>
                        <TableRow sx={{ bgcolor: "grey.100" }}>
                            <TableCell>Project</TableCell>
                            <TableCell>Primary Client</TableCell>
                            <TableCell align="right">Total Hrs</TableCell>
                            <TableCell align="right">Last Activity</TableCell>
                            <TableCell align="right">Days Inactive</TableCell>
                            <TableCell>Contributors</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {data.map((row) => (
                            <TableRow key={row.project} hover>
                                <TableCell>
                                    <Typography
                                        variant="body2"
                                        fontWeight={600}
                                    >
                                        {row.project}
                                    </Typography>
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2">
                                        {row.primaryClient}
                                    </Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Typography variant="body2">
                                        {row.totalHours.toFixed(1)}
                                    </Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Typography variant="body2">
                                        {row.lastActivityDate}
                                    </Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Chip
                                        label={`${row.daysSinceActivity} days`}
                                        size="small"
                                        color={staleColor(
                                            row.daysSinceActivity,
                                        )}
                                    />
                                </TableCell>
                                <TableCell>
                                    <Tooltip title={row.activeUsers.join(", ")}>
                                        <Typography
                                            variant="body2"
                                            noWrap
                                            sx={{ maxWidth: 180 }}
                                        >
                                            {row.activeUsers
                                                .slice(0, 3)
                                                .join(", ")}
                                            {row.activeUsers.length > 3 &&
                                                ` +${row.activeUsers.length - 3}`}
                                        </Typography>
                                    </Tooltip>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}

// ── Internal sub-component ───────────────────────────────────────────────────

interface SliderProps {
    value: number;
    onChange: (_: Event, val: number | number[]) => void;
}

/**
 * Staleness threshold slider rendered above the table.
 */
function StaleSlider({ value, onChange }: Readonly<SliderProps>) {
    return (
        <Box
            sx={{
                display: "flex",
                alignItems: "center",
                gap: 2,
                maxWidth: 480,
            }}
        >
            <Typography variant="body2" noWrap>
                Stale after:
            </Typography>
            <Slider
                value={value}
                min={7}
                max={180}
                step={7}
                marks={[
                    { value: 7, label: "7d" },
                    { value: 30, label: "30d" },
                    { value: 60, label: "60d" },
                    { value: 90, label: "90d" },
                    { value: 180, label: "180d" },
                ]}
                valueLabelDisplay="auto"
                valueLabelFormat={(v) => `${v} days`}
                onChange={onChange}
                sx={{ flex: 1 }}
            />
        </Box>
    );
}
