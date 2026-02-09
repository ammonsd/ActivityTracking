/**
 * Description: Table component for displaying login audit records
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import React from "react";
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Chip,
    Typography,
} from "@mui/material";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelIcon from "@mui/icons-material/Cancel";
import type { LoginAudit } from "../../types/guestActivity.types";
import { formatDateTime } from "../../utils/guestActivityUtils";

interface LoginAuditTableProps {
    loginAudits: LoginAudit[];
}

export const LoginAuditTable: React.FC<LoginAuditTableProps> = ({
    loginAudits,
}) => {
    if (loginAudits.length === 0) {
        return (
            <Paper elevation={2} sx={{ p: 4, textAlign: "center" }}>
                <Typography variant="h6" color="text.secondary">
                    No login activity recorded yet
                </Typography>
                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ mt: 1 }}
                >
                    Login audit data will appear here after guest users log in
                </Typography>
            </Paper>
        );
    }

    return (
        <TableContainer component={Paper} elevation={2}>
            <Table>
                <TableHead>
                    <TableRow sx={{ backgroundColor: "#f5f5f5" }}>
                        <TableCell>
                            <strong>Date/Time</strong>
                        </TableCell>
                        <TableCell>
                            <strong>IP Address</strong>
                        </TableCell>
                        <TableCell>
                            <strong>Location</strong>
                        </TableCell>
                        <TableCell>
                            <strong>Status</strong>
                        </TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {loginAudits.map((audit) => (
                        <TableRow
                            key={audit.id}
                            sx={{
                                "&:hover": { backgroundColor: "#fafafa" },
                            }}
                        >
                            <TableCell>
                                {formatDateTime(audit.loginTime)}
                            </TableCell>
                            <TableCell>{audit.ipAddress}</TableCell>
                            <TableCell>{audit.location}</TableCell>
                            <TableCell>
                                {audit.successful ? (
                                    <Chip
                                        icon={<CheckCircleIcon />}
                                        label="Success"
                                        color="success"
                                        size="small"
                                        variant="outlined"
                                    />
                                ) : (
                                    <Chip
                                        icon={<CancelIcon />}
                                        label="Failed"
                                        color="error"
                                        size="small"
                                        variant="outlined"
                                    />
                                )}
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
};
