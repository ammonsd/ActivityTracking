/**
 * Description: Period-over-Period Delta table component. Compares hours in the current
 * date range versus the immediately preceding equivalent period, segmented by user and
 * by client. Trend arrows help admins quickly identify growth or reduction in activity.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import FiberNewIcon from "@mui/icons-material/FiberNew";
import RemoveIcon from "@mui/icons-material/Remove";
import {
    Alert,
    Box,
    Paper,
    Tab,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tabs,
    Typography,
} from "@mui/material";
import { useState } from "react";
import type {
    PeriodDeltaDto,
    PeriodDeltaResult,
    TrendDirection,
} from "../../types/reports.types";

interface Props {
    data: PeriodDeltaResult | null;
    /** If false, no date range selected — show a guidance message */
    hasPriorPeriod: boolean;
}

/**
 * Returns a colored icon for the trend direction.
 */
function TrendIcon({ trend }: Readonly<{ trend: TrendDirection }>) {
    switch (trend) {
        case "up":
            return <ArrowUpwardIcon fontSize="small" color="success" />;
        case "down":
            return <ArrowDownwardIcon fontSize="small" color="error" />;
        case "new":
            return <FiberNewIcon fontSize="small" color="primary" />;
        case "dropped":
            return (
                <ArrowDownwardIcon
                    fontSize="small"
                    sx={{ color: "text.disabled" }}
                />
            );
        default:
            return (
                <RemoveIcon fontSize="small" sx={{ color: "text.secondary" }} />
            );
    }
}

interface DeltaTableProps {
    readonly rows: PeriodDeltaDto[];
    readonly currentLabel: string;
    readonly priorLabel: string;
}

/**
 * Renders the delta rows table for either byUser or byClient data.
 */
function DeltaTable({ rows, currentLabel, priorLabel }: DeltaTableProps) {
    if (!rows.length) {
        return (
            <Typography color="text.secondary" sx={{ p: 2 }}>
                No data to compare.
            </Typography>
        );
    }

    function deltaCell(row: PeriodDeltaDto) {
        const sign = row.delta > 0 ? "+" : "";
        const pctSign = row.delta > 0 ? "+" : "";
        const pct =
            row.deltaPercent === null
                ? ""
                : ` (${pctSign}${row.deltaPercent.toFixed(1)}%)`;
        let textColor: string;
        if (row.delta > 0) textColor = "success.dark";
        else if (row.delta < 0) textColor = "error.dark";
        else textColor = "text.secondary";
        return (
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                <TrendIcon trend={row.trend} />
                <Typography
                    variant="body2"
                    fontWeight={600}
                    color={textColor}
                >
                    {sign}
                    {row.delta.toFixed(1)}
                    {pct}
                </Typography>
            </Box>
        );
    }

    return (
        <TableContainer component={Paper} variant="outlined">
            <Table size="small">
                <TableHead>
                    <TableRow sx={{ bgcolor: "grey.100" }}>
                        <TableCell>Name</TableCell>
                        <TableCell align="right">{currentLabel}</TableCell>
                        <TableCell align="right">{priorLabel}</TableCell>
                        <TableCell>Change</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {rows.map((row) => (
                        <TableRow key={row.name} hover>
                            <TableCell>
                                <Typography variant="body2" fontWeight={600}>
                                    {row.name}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography
                                    variant="body2"
                                    color={
                                        row.trend === "dropped"
                                            ? "text.disabled"
                                            : "text.primary"
                                    }
                                >
                                    {row.currentHours.toFixed(1)}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography
                                    variant="body2"
                                    color={
                                        row.trend === "new"
                                            ? "text.disabled"
                                            : "text.primary"
                                    }
                                >
                                    {row.priorHours.toFixed(1)}
                                </Typography>
                            </TableCell>
                            <TableCell>{deltaCell(row)}</TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}

/**
 * Period-over-Period Delta — tabbed view switching between byUser and byClient deltas.
 * Requires a selected date range to compute the prior period automatically.
 */
export default function PeriodDeltaTable({ data, hasPriorPeriod }: Readonly<Props>) {
    const [subTab, setSubTab] = useState(0);

    if (!hasPriorPeriod || !data) {
        return (
            <Alert severity="info">
                Select a specific start and end date to enable
                period-over-period comparison. The prior period will
                automatically be set to the equivalent duration immediately
                before the selected range.
            </Alert>
        );
    }

    return (
        <Box>
            <Tabs
                value={subTab}
                onChange={(_, v) => setSubTab(v)}
                sx={{ mb: 1, borderBottom: 1, borderColor: "divider" }}
            >
                <Tab label="By User" />
                <Tab label="By Client" />
            </Tabs>

            {subTab === 0 && (
                <DeltaTable
                    rows={data.byUser}
                    currentLabel={data.currentLabel}
                    priorLabel={data.priorLabel}
                />
            )}
            {subTab === 1 && (
                <DeltaTable
                    rows={data.byClient}
                    currentLabel={data.currentLabel}
                    priorLabel={data.priorLabel}
                />
            )}
        </Box>
    );
}
