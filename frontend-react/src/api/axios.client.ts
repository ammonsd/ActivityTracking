/**
 * Description: Axios HTTP client configuration with session-based authentication
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

import axios from "axios";

// Use relative path to leverage Vite proxy for session cookie handling
const API_BASE_URL = "/api";

export const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        "Content-Type": "application/json",
    },
    withCredentials: true, // Important: Include session cookies
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            // Unauthorized - redirect to login using relative path
            // This works in all environments (dev with proxy, production)
            window.location.href =
                "/login?returnUrl=" +
                encodeURIComponent(
                    window.location.pathname + window.location.search,
                );
        }
        return Promise.reject(error);
    },
);

export default apiClient;
