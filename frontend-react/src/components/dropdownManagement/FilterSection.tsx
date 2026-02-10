/**
 * Description: Filter section with category and subcategory dropdowns
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import { Box, FormControl, InputLabel, Select, MenuItem } from "@mui/material";
import type { SelectChangeEvent } from "@mui/material";

interface FilterSectionProps {
    categories: string[];
    subcategories: string[];
    selectedCategory: string;
    selectedSubcategory: string;
    onCategoryChange: (category: string) => void;
    onSubcategoryChange: (subcategory: string) => void;
}

export const FilterSection: React.FC<FilterSectionProps> = ({
    categories,
    subcategories,
    selectedCategory,
    selectedSubcategory,
    onCategoryChange,
    onSubcategoryChange,
}) => {
    const handleCategoryChange = (event: SelectChangeEvent<string>) => {
        onCategoryChange(event.target.value);
    };

    const handleSubcategoryChange = (event: SelectChangeEvent<string>) => {
        onSubcategoryChange(event.target.value);
    };

    return (
        <Box
            sx={{
                display: "flex",
                gap: 2,
                mb: 3,
                flexWrap: "wrap",
            }}
        >
            <FormControl sx={{ minWidth: 200 }}>
                <InputLabel>Filter by Category</InputLabel>
                <Select
                    value={selectedCategory}
                    label="Filter by Category"
                    onChange={handleCategoryChange}
                >
                    <MenuItem value="">All Categories</MenuItem>
                    {categories.map((cat) => (
                        <MenuItem key={cat} value={cat}>
                            {cat}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>

            <FormControl sx={{ minWidth: 200 }}>
                <InputLabel>Filter by Subcategory</InputLabel>
                <Select
                    value={selectedSubcategory}
                    label="Filter by Subcategory"
                    onChange={handleSubcategoryChange}
                >
                    <MenuItem value="">All Subcategories</MenuItem>
                    {subcategories.map((sub) => (
                        <MenuItem key={sub} value={sub}>
                            {sub}
                        </MenuItem>
                    ))}
                </Select>
            </FormControl>
        </Box>
    );
};
