/**
 * Description: Client Activity Timeline table component. Shows month-over-month hours
 * per client in a scrollable matrix view, helping admins spot seasonal trends and
 * client engagement patterns over time.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import {
    Chip,
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
import type { ClientTimelineDto } from "../../types/reports.types";

interface Props {
    readonly data: ClientTimelineDto[];
}

/**
 * Returns a background intensity proportional to hours relative to the global max,
 * used as a heatmap effect on each month cell.
 */
function cellBg(hours: number, maxHours: number): string {
    if (hours === 0 || maxHours === 0) return "transparent";
    const intensity = Math.round((hours / maxHours) * 6); // 1–6
    const alphas = ["0.06", "0.12", "0.20", "0.30", "0.42", "0.55"];
    return `rgba(25, 118, 210, ${alphas[intensity - 1]})`;
}

/**
 * Client Activity Timeline — matrix table with clients as rows and months as columns.
 * Each cell shows the hours logged; peak month is highlighted with a chip.
 */
/** Lookup hours for a client/month combination. */
function getHours(row: ClientTimelineDto, month: string): number {
    return row.months.find((m) => m.month === month)?.hours ?? 0;
}

/** Shorten "YYYY-MM" to "MMM YY" for display. */
function fmtMonth(ym: string): string {
    const [yr, mo] = ym.split("-");
    const d = new Date(+yr, +mo - 1, 1);
    return d.toLocaleDateString("en-US", { month: "short", year: "2-digit" });
}

export default function ClientTimelineTable({ data }: Readonly<Props>) {
    if (!data.length) {
        return (
            <Typography color="text.secondary" sx={{ p: 2 }}>
                No client timeline data available for the selected period.
            </Typography>
        );
    }

    // Build a sorted, de-duplicated list of all months present in the data
    const allMonths: string[] = [
        ...new Set(data.flatMap((c) => c.months.map((m) => m.month))),
    ].sort((a, b) => a.localeCompare(b));

    // Global max cell value used for heatmap scaling
    const maxCellHours = Math.max(
        ...data.flatMap((c) => c.months.map((m) => m.hours)),
        1,
    );

    return (
        <TableContainer
            component={Paper}
            variant="outlined"
            sx={{ overflowX: "auto" }}
        >
            <Table size="small" sx={{ minWidth: 600 }}>
                <TableHead>
                    <TableRow sx={{ bgcolor: "grey.100" }}>
                        <TableCell
                            sx={{
                                minWidth: 160,
                                position: "sticky",
                                left: 0,
                                bgcolor: "grey.100",
                                zIndex: 1,
                            }}
                        >
                            Client
                        </TableCell>
                        <TableCell align="right" sx={{ minWidth: 80 }}>
                            Total Hrs
                        </TableCell>
                        <TableCell sx={{ minWidth: 100 }}>Peak Month</TableCell>
                        {allMonths.map((m) => (
                            <TableCell
                                key={m}
                                align="right"
                                sx={{ minWidth: 72, whiteSpace: "nowrap" }}
                            >
                                {fmtMonth(m)}
                            </TableCell>
                        ))}
                    </TableRow>
                </TableHead>
                <TableBody>
                    {data.map((row) => (
                        <TableRow key={row.client} hover>
                            <TableCell
                                sx={{
                                    position: "sticky",
                                    left: 0,
                                    bgcolor: "background.paper",
                                    zIndex: 1,
                                }}
                            >
                                <Typography variant="body2" fontWeight={600}>
                                    {row.client}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography variant="body2">
                                    {row.totalHours.toFixed(1)}
                                </Typography>
                            </TableCell>
                            <TableCell>
                                {row.peakMonth ? (
                                    <Chip
                                        label={fmtMonth(row.peakMonth)}
                                        size="small"
                                        color="primary"
                                        variant="outlined"
                                    />
                                ) : (
                                    "—"
                                )}
                            </TableCell>
                            {allMonths.map((m) => {
                                const h = getHours(row, m);
                                return (
                                    <TableCell
                                        key={m}
                                        align="right"
                                        sx={{
                                            bgcolor: cellBg(h, maxCellHours),
                                        }}
                                    >
                                        {h > 0 ? (
                                            <Tooltip
                                                title={`${row.client} · ${fmtMonth(m)} · ${h.toFixed(1)} hrs`}
                                            >
                                                <Typography variant="caption">
                                                    {h.toFixed(1)}
                                                </Typography>
                                            </Tooltip>
                                        ) : (
                                            <Typography
                                                variant="caption"
                                                color="text.disabled"
                                            >
                                                —
                                            </Typography>
                                        )}
                                    </TableCell>
                                );
                            })}
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
