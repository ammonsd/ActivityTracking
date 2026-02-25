/**
 * Description: API functions for Admin Analytics & Reports feature.
 * Fetches task activity data and dropdown configuration for admin-side reporting.
 * Admins receive all users' data from the backend; non-admins are filtered server-side.
 *
 * Author: Dean Ammons
 * Date: February 2026
 */

import apiClient from "./axios.client";
import type {
    TaskActivity,
    TaskActivityResponse,
} from "../types/reports.types";
import type { DropdownValue } from "../types/dropdown.types";

export const reportsApi = {
    /**
     * Fetch task activities for a given date range.
     * Admin users receive all users' data; the backend enforces role-based filtering.
     *
     * @param startDate - ISO date string "YYYY-MM-DD" (optional)
     * @param endDate - ISO date string "YYYY-MM-DD" (optional)
     * @returns Promise with array of task activity records
     */
    fetchTaskActivities: async (
        startDate?: string,
        endDate?: string,
    ): Promise<TaskActivity[]> => {
        const params: Record<string, string | number> = {
            page: 0,
            size: 10000, // Fetch all records for reporting
        };

        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;

        const response = await apiClient.get<TaskActivityResponse>(
            "/task-activities",
            { params },
        );

        return response.data.data ?? [];
    },

    /**
     * Fetch all dropdown values for billability evaluation.
     * Used to classify tasks as billable or non-billable based on client/project/phase flags.
     *
     * @returns Promise with array of dropdown value records
     */
    fetchDropdownValues: async (): Promise<DropdownValue[]> => {
        const response = await apiClient.get<DropdownValue[]>("/dropdowns/all");
        return response.data ?? [];
    },
};
