/**
 * Password Toggle Utilities
 * Provides functions to toggle password visibility in forms.
 *
 * @author Dean Ammons
 * @version 1.0
 */

/**
 * Toggles visibility for a single password field.
 *
 * @param {string} fieldId - The ID of the password input field
 * @param {HTMLElement} button - The button element that was clicked
 */
function togglePasswordVisibility(fieldId, button) {
    const field = document.getElementById(fieldId);
    if (!field) return;

    if (field.type === "password") {
        field.type = "text";
        button.textContent = "🙈";
        button.setAttribute("aria-label", "Hide password");
    } else {
        field.type = "password";
        button.textContent = "👁️";
        button.setAttribute("aria-label", "Show password");
    }
}

/**
 * Toggles visibility for all password fields when checkbox is checked/unchecked.
 * Expects password fields to have specific IDs and toggle buttons with .toggle-password class.
 *
 * @param {string} checkboxId - The ID of the checkbox controlling visibility
 * @param {string[]} fieldIds - Array of password field IDs to toggle
 */
function toggleAllPasswords(checkboxId, fieldIds) {
    const checkbox = document.getElementById(checkboxId);
    if (!checkbox) return;

    const toggleButtons = document.querySelectorAll(".toggle-password");
    const isChecked = checkbox.checked;

    fieldIds.forEach((fieldId) => {
        const field = document.getElementById(fieldId);
        if (field) {
            field.type = isChecked ? "text" : "password";
        }
    });

    toggleButtons.forEach((btn) => {
        btn.textContent = isChecked ? "🙈" : "👁️";
        btn.setAttribute(
            "aria-label",
            isChecked ? "Hide password" : "Show password"
        );
    });
}

/**
 * Initializes password toggle functionality for a form.
 * Sets up event listeners for individual toggle buttons and show all checkbox.
 *
 * @param {Object} config - Configuration object
 * @param {string} config.showAllCheckboxId - ID of the "show all passwords" checkbox
 * @param {string[]} config.fieldIds - Array of password field IDs
 */
function initPasswordToggles(config) {
    document.addEventListener("DOMContentLoaded", function () {
        // Set up show all checkbox if provided
        if (config.showAllCheckboxId) {
            const checkbox = document.getElementById(config.showAllCheckboxId);
            if (checkbox) {
                checkbox.addEventListener("change", function () {
                    toggleAllPasswords(
                        config.showAllCheckboxId,
                        config.fieldIds
                    );
                });
            }
        }

        // Initialize aria labels for toggle buttons
        const toggleButtons = document.querySelectorAll(".toggle-password");
        toggleButtons.forEach((btn) => {
            btn.setAttribute("aria-label", "Show password");
        });
    });
}

// Expose functions globally
window.togglePasswordVisibility = togglePasswordVisibility;
window.toggleAllPasswords = toggleAllPasswords;
window.initPasswordToggles = initPasswordToggles;
