/**
 * Coming Soon dialog component for unimplemented features.
 * Displays when users click on features that are not yet implemented.
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    Typography,
    Box,
} from "@mui/material";
import ConstructionIcon from "@mui/icons-material/Construction";

interface ComingSoonDialogProps {
    open: boolean;
    onClose: () => void;
    featureName: string;
}

export const ComingSoonDialog: React.FC<ComingSoonDialogProps> = ({
    open,
    onClose,
    featureName,
}) => {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle sx={{ textAlign: "center", pb: 1 }}>
                <Box
                    sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        gap: 1,
                    }}
                >
                    <ConstructionIcon
                        sx={{ fontSize: 32, color: "warning.main" }}
                    />
                    <span>Coming Soon</span>
                </Box>
            </DialogTitle>
            <DialogContent>
                <Typography variant="body1" sx={{ textAlign: "center", mb: 2 }}>
                    The <strong>{featureName}</strong> feature is currently
                    under development.
                </Typography>
                <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ textAlign: "center" }}
                >
                    This feature will be available in a future release. Please
                    check back later or contact your administrator for more
                    information.
                </Typography>
            </DialogContent>
            <DialogActions sx={{ justifyContent: "center", pb: 2 }}>
                <Button onClick={onClose} variant="contained" color="primary">
                    Got It
                </Button>
            </DialogActions>
        </Dialog>
    );
};
