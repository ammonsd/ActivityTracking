/**
 * Description: API functions for Guest Activity feature
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import apiClient from "./axios.client";
import type {
    LoginAudit,
    LoginAuditResponse,
} from "../types/guestActivity.types";

export const guestActivityApi = {
    /**
     * Fetch login audit data for a specific user
     * @param username - Username to filter by (default: 'guest')
     * @param limit - Maximum number of records to return (default: 50)
     * @returns Promise with array of login audit records
     */
    fetchLoginAudit: async (
        username: string = "guest",
        limit: number = 50,
    ): Promise<LoginAudit[]> => {
        const response = await apiClient.get<LoginAuditResponse>(
            "/users/login-audit",
            {
                params: { username, limit },
            },
        );
        return response.data.data;
    },
};
