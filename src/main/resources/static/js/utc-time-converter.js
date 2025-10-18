/**
 * UTC Time Converter
 *
 * Automatically converts UTC timestamps to user's local timezone
 *
 * Usage:
 * 1. Add class "utc-time" to any element containing a UTC timestamp
 * 2. Add data-utc-time attribute with the UTC time in format: yyyy-MM-dd HH:mm
 * 3. Include this script in your HTML
 *
 * Example:
 * <td class="utc-time" data-utc-time="2025-10-15 18:00">2025-10-15 18:00</td>
 *
 * Result:
 * - Displays: "10/15/2025, 02:00 PM" (user's local time)
 * - Tooltip: "UTC: 2025-10-15 18:00" (on hover)
 *
 * @author Dean Ammons
 * @version 1.0
 */

/**
 * Convert all elements with class 'utc-time' to local timezone
 */
function convertUTCToLocal() {
    document.querySelectorAll(".utc-time").forEach(function (element) {
        const utcTime = element.getAttribute("data-utc-time");

        // Skip if no time or special values
        if (
            !utcTime ||
            utcTime === "Never" ||
            utcTime === "-" ||
            utcTime === "N/A"
        ) {
            return;
        }

        try {
            // Parse the UTC time (format: yyyy-MM-dd HH:mm)
            // Add seconds and Z to indicate UTC timezone
            const date = new Date(utcTime + ":00Z");

            // Check if date is valid
            if (isNaN(date.getTime())) {
                console.warn("Invalid date format:", utcTime);
                return;
            }

            // Format in user's local timezone
            const localTime = date.toLocaleString("en-US", {
                year: "numeric",
                month: "2-digit",
                day: "2-digit",
                hour: "2-digit",
                minute: "2-digit",
                hour12: true,
            });

            // Update the display text
            element.textContent = localTime;

            // Add tooltip showing original UTC time
            element.title = "UTC: " + utcTime;
            element.style.cursor = "help";

            // Add a subtle visual indicator that this is a converted time
            element.classList.add("time-converted");
        } catch (e) {
            console.error("Error converting UTC time:", utcTime, e);
        }
    });
}

/**
 * Format a JavaScript Date object to local time string
 * @param {Date} date - The date to format
 * @returns {string} Formatted local time string
 */
function formatLocalTime(date) {
    return date.toLocaleString("en-US", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: true,
    });
}

/**
 * Get the user's timezone abbreviation (e.g., "EDT", "PST")
 * @returns {string} Timezone abbreviation
 */
function getUserTimezone() {
    const date = new Date();
    const timezone = date
        .toLocaleTimeString("en-us", { timeZoneName: "short" })
        .split(" ")[2];
    return timezone || Intl.DateTimeFormat().resolvedOptions().timeZone;
}

// Auto-initialize when DOM is ready
if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", convertUTCToLocal);
} else {
    // DOM already loaded
    convertUTCToLocal();
}

// Export functions for use in other scripts
window.UTCTimeConverter = {
    convert: convertUTCToLocal,
    formatLocalTime: formatLocalTime,
    getUserTimezone: getUserTimezone,
};
