package com.ammons.taskactivity.util;

/**
 * Utility class for common string operations across the application.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
public final class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Build full name from first and last name components.
     *
     * @param firstName the first name (optional)
     * @param lastName the last name (optional)
     * @return formatted full name, or null if both are empty/null
     */
    public static String buildFullName(String firstName, String lastName) {
        boolean hasFirst = firstName != null && !firstName.trim().isEmpty();
        boolean hasLast = lastName != null && !lastName.trim().isEmpty();

        if (!hasFirst && !hasLast) {
            return null;
        }

        if (hasFirst && hasLast) {
            return firstName.trim() + " " + lastName.trim();
        }

        if (hasFirst) {
            return firstName.trim();
        }

        return lastName != null ? lastName.trim() : null;
    }

    /**
     * Get a greeting string, preferring full name over username.
     *
     * @param fullName the full name (optional)
     * @param username the username (fallback)
     * @return full name if available, otherwise username
     */
    public static String getGreeting(String fullName, String username) {
        return (fullName != null && !fullName.trim().isEmpty()) ? fullName : username;
    }
}
