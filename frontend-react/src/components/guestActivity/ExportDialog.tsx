/**
 * Description: Export dialog for Guest Activity CSV export with copy/download options
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import React, { useState } from "react";
import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Typography,
    Box,
    Alert,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import DownloadIcon from "@mui/icons-material/Download";
import CloseIcon from "@mui/icons-material/Close";

interface ExportDialogProps {
    open: boolean;
    onClose: () => void;
    csvContent: string;
    recordCount: number;
    fileName: string;
}

export const ExportDialog: React.FC<ExportDialogProps> = ({
    open,
    onClose,
    csvContent,
    recordCount,
    fileName,
}) => {
    const [copySuccess, setCopySuccess] = useState(false);

    const handleCopyToClipboard = async () => {
        try {
            await navigator.clipboard.writeText(csvContent);
            setCopySuccess(true);
            setTimeout(() => setCopySuccess(false), 3000);
        } catch (err) {
            console.error("Failed to copy to clipboard:", err);
        }
    };

    const handleDownload = () => {
        const blob = new Blob([csvContent], {
            type: "text/csv;charset=utf-8;",
        });
        const link = document.createElement("a");
        const url = URL.createObjectURL(blob);
        link.setAttribute("href", url);
        link.setAttribute("download", fileName);
        link.style.visibility = "hidden";
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle sx={{ backgroundColor: "#1976d2", color: "white" }}>
                Export Guest Activity Data
            </DialogTitle>
            <DialogContent sx={{ mt: 2 }}>
                {copySuccess && (
                    <Alert severity="success" sx={{ mb: 2 }}>
                        Data copied to clipboard!
                    </Alert>
                )}
                <Box sx={{ mb: 2 }}>
                    <Typography variant="body1" gutterBottom>
                        <strong>Records to export:</strong> {recordCount}
                    </Typography>
                    <Typography variant="body1" gutterBottom>
                        <strong>File name:</strong> {fileName}
                    </Typography>
                    <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{ mt: 2 }}
                    >
                        Choose an export option:
                    </Typography>
                </Box>
            </DialogContent>
            <DialogActions sx={{ p: 2, gap: 1 }}>
                <Button
                    variant="outlined"
                    startIcon={<ContentCopyIcon />}
                    onClick={handleCopyToClipboard}
                >
                    Copy to Clipboard
                </Button>
                <Button
                    variant="contained"
                    color="success"
                    startIcon={<DownloadIcon />}
                    onClick={handleDownload}
                >
                    Download CSV
                </Button>
                <Button
                    variant="outlined"
                    color="inherit"
                    startIcon={<CloseIcon />}
                    onClick={onClose}
                >
                    Close
                </Button>
            </DialogActions>
        </Dialog>
    );
};
