/**
 * Description: Authentication and user type definitions
 *
 * Author: Dean Ammons
 * Date: January 2026
 */

export interface User {
    id: number;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    fullName: string;
    role: "ADMIN" | "USER" | "GUEST";
    active: boolean;
}

export interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;
}
