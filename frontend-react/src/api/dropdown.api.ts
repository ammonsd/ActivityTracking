/**
 * Description: API functions for Dropdown Management feature
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import apiClient from "./axios.client";
import type { DropdownValue } from "../types/dropdown.types";

export const dropdownApi = {
    /**
     * Fetch all categories
     * @returns Promise with array of category strings
     */
    fetchCategories: async (): Promise<string[]> => {
        const response = await apiClient.get<string[]>("/dropdowns/categories");
        return response.data;
    },

    /**
     * Fetch all dropdown values
     * @returns Promise with array of dropdown values
     */
    fetchAllValues: async (): Promise<DropdownValue[]> => {
        const response = await apiClient.get<DropdownValue[]>("/dropdowns/all");
        return response.data;
    },

    /**
     * Fetch dropdown values by category
     * @param category - Category to filter by
     * @returns Promise with array of dropdown values
     */
    fetchValuesByCategory: async (
        category: string,
    ): Promise<DropdownValue[]> => {
        const response = await apiClient.get<DropdownValue[]>(
            `/dropdowns/category/${encodeURIComponent(category)}`,
        );
        return response.data;
    },

    /**
     * Add a new dropdown value
     * @param valueData - Data for the new dropdown value
     * @returns Promise with the created dropdown value
     */
    addValue: async (
        valueData: Partial<DropdownValue>,
    ): Promise<DropdownValue> => {
        const response = await apiClient.post<DropdownValue>(
            "/dropdowns",
            valueData,
        );
        return response.data;
    },

    /**
     * Update an existing dropdown value
     * @param id - Dropdown value ID to update
     * @param updateData - Updated dropdown value data
     * @returns Promise with the updated dropdown value
     */
    updateValue: async (
        id: number,
        updateData: Partial<DropdownValue>,
    ): Promise<DropdownValue> => {
        const response = await apiClient.put<DropdownValue>(
            `/dropdowns/${id}`,
            updateData,
        );
        return response.data;
    },

    /**
     * Delete a dropdown value
     * @param id - Dropdown value ID to delete
     * @returns Promise<void>
     */
    deleteValue: async (id: number): Promise<void> => {
        await apiClient.delete(`/dropdowns/${id}`);
    },

    /**
     * Toggle the allUsers flag on a dropdown value.
     * @param id - Dropdown value ID to toggle
     * @returns Promise with the updated dropdown value
     */
    toggleAllUsers: async (id: number): Promise<DropdownValue> => {
        const response = await apiClient.put<DropdownValue>(
            `/dropdowns/${id}/toggle-all-users`,
        );
        return response.data;
    },
};
