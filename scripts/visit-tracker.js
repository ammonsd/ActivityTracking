/**
 * Privacy-friendly visitor counter using application backend.
 * No personal data collected - only increments a counter.
 *
 * Usage: Add data-page-name attribute to body element
 * <body data-page-name="your-page-identifier">
 *
 * Author: Dean Ammons
 * Date: January 2026
 */
(function () {
    const pageName = document.body.dataset.pageName;
    if (!pageName) return;

    const API_URL =
        "https://taskactivitytracker.com/api/public/visit/" + pageName;

    fetch(API_URL, { method: "POST" }).catch(() => {});
})();
