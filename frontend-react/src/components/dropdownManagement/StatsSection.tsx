/**
 * Description: Summary statistics section showing total and active values
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import { Box, Typography } from "@mui/material";
import type { DropdownValue } from "../../types/dropdown.types";

interface StatsSectionProps {
    values: DropdownValue[];
}

export const StatsSection: React.FC<StatsSectionProps> = ({ values }) => {
    const total = values.length;
    const active = values.filter((v) => v.isActive).length;

    if (total === 0) {
        return null;
    }

    return (
        <Box sx={{ mt: 2, mb: 2 }}>
            <Typography variant="body2" color="text.secondary">
                <strong>Total Values:</strong> {total} |{" "}
                <strong>Active:</strong> {active}
            </Typography>
        </Box>
    );
};
