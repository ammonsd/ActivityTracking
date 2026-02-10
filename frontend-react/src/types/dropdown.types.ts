/**
 * Description: TypeScript type definitions for Dropdown Management feature
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

export interface DropdownValue {
    id: number;
    category: string;
    subcategory: string;
    itemValue: string;
    displayOrder: number;
    isActive: boolean;
}

export interface DropdownFilters {
    category?: string;
    subcategory?: string;
}

export interface AddCategoryRequest {
    category: string;
    subcategory: string;
    value: string;
}

export interface AddValueRequest {
    category: string;
    subcategory: string;
    itemValue: string;
}

export interface UpdateValueRequest {
    itemValue: string;
    displayOrder: number;
    isActive: boolean;
}

export interface DropdownStats {
    total: number;
    active: number;
    inactive: number;
}
