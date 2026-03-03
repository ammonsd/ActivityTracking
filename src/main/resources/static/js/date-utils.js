/**
 * Date Utilities
 * Provides date manipulation and formatting functions.
 *
 * Author: Dean Ammons
 * Date: January 2026
 *
 * Modified by: Dean Ammons - March 2026
 * Change: Added getWeekStartDate() supporting configurable week start day (MONDAY or SATURDAY)
 * Reason: Support Saturday-Friday work week for clients who use that billing cycle
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
     * Gets the Monday of the week for a given date (backwards-compatible helper).
     *
     * @param {Date} date - The date to find the Monday for
     * @returns {Date} The Monday of the week containing the given date
     */
    getMondayOfWeek: function (date) {
        return this.getWeekStartDate(date, 1); // 1 = Monday in JS getDay()
    },

    /**
     * Gets the start date of the week for a given date and configurable start day.
     * Uses previous-or-same logic: if the date IS the start day, it is returned as-is.
     *
     * @param {Date} date - The reference date
     * @param {number} startDayValue - JS day-of-week for the week start:
     *   0=Sunday, 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday
     * @returns {Date} The week start date
     */
    getWeekStartDate: function (date, startDayValue) {
        const d = new Date(date);
        const day = d.getDay();
        const diff = (day - startDayValue + 7) % 7; // days since last occurrence of startDay
        d.setDate(d.getDate() - diff);
        return d;
    },

    /**
     * Converts a server-side week start day string (MONDAY/SATURDAY) to a JS day-of-week number.
     *
     * @param {string} weekStartDayStr - "MONDAY" or "SATURDAY"
     * @returns {number} JS day-of-week value (1 for Monday, 6 for Saturday)
     */
    weekStartDayToJsValue: function (weekStartDayStr) {
        if (weekStartDayStr === "SATURDAY") {
            return 6;
        }
        return 1; // Default to Monday
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
     * Gets the current week's start date or the start date from a URL date parameter,
     * using the provided week start day.
     *
     * @param {string} [paramName='date'] - The URL parameter name to check
     * @param {number} [startDayValue=1] - JS day-of-week for week start (1=Monday, 6=Saturday)
     * @returns {Date} The week start date
     */
    getCurrentOrUrlWeekStart: function (paramName = "date", startDayValue = 1) {
        const urlDate = this.getUrlParameter(paramName);

        if (urlDate) {
            const urlDateObj = this.parseDate(urlDate);
            return this.getWeekStartDate(urlDateObj, startDayValue);
        } else {
            const today = new Date();
            return this.getWeekStartDate(today, startDayValue);
        }
    },

    /**
     * Gets the current week's Monday or the Monday from a URL date parameter.
     * Kept for backwards compatibility; prefer getCurrentOrUrlWeekStart().
     *
     * @param {string} [paramName='date'] - The URL parameter name to check
     * @returns {Date} The Monday of the week
     */
    getCurrentOrUrlMonday: function (paramName = "date") {
        return this.getCurrentOrUrlWeekStart(paramName, 1);
    },
};

// Expose globally for use in HTML
window.DateUtils = DateUtils;
