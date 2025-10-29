/**
 * Modal Utilities
 * Provides reusable modal management functions for confirmation dialogs.
 *
 * @author Dean Ammons
 * @version 1.0
 */

/**
 * Modal manager for delete confirmations and other modal dialogs.
 * Handles opening, closing, and confirming actions with modals.
 */
const ModalUtils = {
    currentForm: null,

    /**
     * Opens a delete confirmation modal with a custom message.
     *
     * @param {string} formId - The ID of the form to submit on confirmation
     * @param {string} message - The confirmation message to display
     * @param {string} [modalId='deleteModal'] - Optional modal element ID
     */
    openDeleteModal: function (formId, message, modalId = "deleteModal") {
        this.currentForm = document.getElementById(formId);
        const messageElement = document.getElementById("deleteMessage");
        if (messageElement) {
            messageElement.textContent = message;
        }
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.add("show");
        }
    },

    /**
     * Closes the modal dialog.
     *
     * @param {string} [modalId='deleteModal'] - Optional modal element ID
     */
    closeModal: function (modalId = "deleteModal") {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.classList.remove("show");
        }
        this.currentForm = null;
    },

    /**
     * Confirms the delete action and submits the stored form.
     */
    confirmDelete: function () {
        if (this.currentForm) {
            this.currentForm.submit();
        }
    },

    /**
     * Initialize click-outside-to-close behavior for a modal.
     *
     * @param {string} [modalId='deleteModal'] - The modal element ID
     */
    initClickOutsideToClose: function (modalId = "deleteModal") {
        const modal = document.getElementById(modalId);
        if (!modal) return;

        window.addEventListener("click", (event) => {
            if (event.target === modal) {
                this.closeModal(modalId);
            }
        });
    },
};

// Expose functions globally for onclick handlers in HTML
window.openDeleteModal = function (formId, message, modalId) {
    ModalUtils.openDeleteModal(formId, message, modalId);
};

window.closeDeleteModal = function (modalId) {
    ModalUtils.closeModal(modalId);
};

window.confirmDelete = function () {
    ModalUtils.confirmDelete();
};

// Initialize click-outside-to-close on page load
document.addEventListener("DOMContentLoaded", function () {
    ModalUtils.initClickOutsideToClose();
});
