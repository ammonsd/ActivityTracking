/**
 * Form Utilities
 * Provides common form functionality like auto-growing textareas.
 *
 * @author Dean Ammons
 * @version 1.0
 */

const FormUtils = {
    /**
     * Makes a textarea auto-grow as the user types.
     *
     * @param {string} textareaId - The ID of the textarea element
     */
    initAutoGrowTextarea: function (textareaId) {
        const textarea = document.getElementById(textareaId);
        if (!textarea) return;

        const autoResize = function () {
            this.style.height = "auto";
            this.style.height = this.scrollHeight + "px";
        };

        // Set initial height
        textarea.addEventListener("input", autoResize);

        // Also resize on window load to handle pre-filled content
        window.addEventListener("load", function () {
            autoResize.call(textarea);
        });
    },

    /**
     * Initializes all auto-grow textareas on the page.
     * Looks for textareas with the 'auto-grow' class.
     */
    initAllAutoGrowTextareas: function () {
        const textareas = document.querySelectorAll("textarea.auto-grow");
        textareas.forEach((textarea) => {
            const autoResize = function () {
                this.style.height = "auto";
                this.style.height = this.scrollHeight + "px";
            };

            textarea.addEventListener("input", autoResize);

            // Set initial height on load
            window.addEventListener("load", function () {
                autoResize.call(textarea);
            });
        });
    },
};

// Auto-initialize on page load
document.addEventListener("DOMContentLoaded", function () {
    FormUtils.initAllAutoGrowTextareas();
});

// Expose globally
window.FormUtils = FormUtils;
