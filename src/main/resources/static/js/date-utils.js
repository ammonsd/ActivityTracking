/**
 * Date Utilities
 * Provides date manipulation and formatting functions.
 *
 * @author Dean Ammons
 * @version 1.0
 */

const DateUtils = {
    /**
     * Gets the value of a URL parameter by name.
     *
     * @param {string} name - The parameter name to retrieve
     * @returns {string|null} The parameter value or null if not found
     */
    getUrlParameter: function (name) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(name);
    },

    /**
     * Updates the URL with a new date parameter and navigates to it.
     *
     * @param {string} date - The date string to set in the URL (YYYY-MM-DD format)
     * @param {string} endpoint - The endpoint to navigate to
     */
    updateUrl: function (date, endpoint) {
        const url = new URL(endpoint, window.location.origin);
        if (date) {
            url.searchParams.set("date", date);
        } else {
            url.searchParams.delete("date");
        }
        window.location.href = url.toString();
    },

    /**
     * Gets the Monday of the week for a given date.
     *
     * @param {Date} date - The date to find the Monday for
     * @returns {Date} The Monday of the week containing the given date
     */
    getMondayOfWeek: function (date) {
        const d = new Date(date);
        const day = d.getDay(); // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
        const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
        const monday = new Date(d.setDate(diff));
        return monday;
    },

    /**
     * Formats a date as YYYY-MM-DD for input field.
     *
     * @param {Date} date - The date to format
     * @returns {string} The formatted date string
     */
    formatDateForInput: function (date) {
        return date.toISOString().split("T")[0];
    },

    /**
     * Adds or subtracts days from a date.
     *
     * @param {Date} date - The starting date
     * @param {number} days - Number of days to add (positive) or subtract (negative)
     * @returns {Date} The new date
     */
    addDays: function (date, days) {
        const result = new Date(date);
        result.setDate(result.getDate() + days);
        return result;
    },

    /**
     * Parses a date string safely with timezone handling.
     * Adds T12:00:00 to avoid timezone issues.
     *
     * @param {string} dateStr - The date string in YYYY-MM-DD format
     * @returns {Date} The parsed date
     */
    parseDate: function (dateStr) {
        return new Date(dateStr + "T12:00:00");
    },

    /**
     * Gets the current week's Monday or the Monday from a URL date parameter.
     *
     * @param {string} [paramName='date'] - The URL parameter name to check
     * @returns {Date} The Monday of the week
     */
    getCurrentOrUrlMonday: function (paramName = "date") {
        const urlDate = this.getUrlParameter(paramName);

        if (urlDate) {
            const urlDateObj = this.parseDate(urlDate);
            return this.getMondayOfWeek(urlDateObj);
        } else {
            const today = new Date();
            return this.getMondayOfWeek(today);
        }
    },
};

// Expose globally for use in HTML
window.DateUtils = DateUtils;
