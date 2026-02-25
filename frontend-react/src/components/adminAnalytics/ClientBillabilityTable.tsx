/**
 * Description: Client Billability Ratio table component. Displays billable vs non-billable
 * hour breakdown per client with visual progress bars, helping admins assess which clients
 * generate trackable billable work.
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
import type { ClientBillabilityDto } from "../../types/reports.types";

interface Props {
    data: ClientBillabilityDto[];
}

/**
 * Returns a bar color based on billability rate: green ≥ 70 %, amber ≥ 40 %, red otherwise.
 */
function barColor(rate: number): "success" | "warning" | "error" {
    if (rate >= 70) return "success";
    if (rate >= 40) return "warning";
    return "error";
}

/**
 * Client Billability Ratio — sortable table with proportional LinearProgress bars
 * showing how billable each client's hours are.
 */
export default function ClientBillabilityTable({ data }: Readonly<Props>) {
    if (!data.length) {
        return (
            <Typography color="text.secondary" sx={{ p: 2 }}>
                No client data available for the selected period.
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
                        <TableCell>Client</TableCell>
                        <TableCell align="right">Total Hrs</TableCell>
                        <TableCell align="right">Billable</TableCell>
                        <TableCell align="right">Non-Billable</TableCell>
                        <TableCell sx={{ minWidth: 200 }}>
                            Billability %
                        </TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {data.map((row, idx) => (
                        <TableRow key={row.client} hover>
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
                                    {row.client}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography variant="body2">
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
                            <TableCell align="right">
                                <Typography
                                    variant="body2"
                                    color="success.dark"
                                    fontWeight={500}
                                >
                                    {row.billableHours.toFixed(1)}
                                </Typography>
                            </TableCell>
                            <TableCell align="right">
                                <Typography
                                    variant="body2"
                                    color="warning.dark"
                                    fontWeight={500}
                                >
                                    {row.nonBillableHours.toFixed(1)}
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
                                            value={row.billabilityRate}
                                            color={barColor(
                                                row.billabilityRate,
                                            )}
                                            sx={{ height: 10, borderRadius: 5 }}
                                        />
                                    </Box>
                                    <Typography
                                        variant="caption"
                                        fontWeight={600}
                                        sx={{
                                            minWidth: 42,
                                            textAlign: "right",
                                        }}
                                    >
                                        {row.billabilityRate.toFixed(1)}%
                                    </Typography>
                                </Box>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
