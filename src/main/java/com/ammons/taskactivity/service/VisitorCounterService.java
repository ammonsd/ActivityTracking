package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking page visit counts in memory. This is a simple, privacy-friendly counter that
 * stores only page visit numbers. No personal data, cookies, or tracking information is collected.
 *
 * Note: Counts are stored in memory and will reset when the application restarts. For persistent
 * storage, consider adding database persistence in the future.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Service
public class VisitorCounterService {

    private static final Logger logger = LoggerFactory.getLogger(VisitorCounterService.class);

    private final Map<String, AtomicLong> pageCounters = new ConcurrentHashMap<>();

    /**
     * Increment the visit count for a specific page and return the new count. Thread-safe operation
     * using AtomicLong.
     *
     * @param pageName The name/identifier of the page being visited
     * @return The new visit count after incrementing
     */
    public long incrementAndGet(String pageName) {
        AtomicLong counter =
                pageCounters.computeIfAbsent(sanitizePageName(pageName), k -> new AtomicLong(0));

        long newCount = counter.incrementAndGet();
        logger.debug("Page '{}' visited. Total visits: {}", pageName, newCount);

        return newCount;
    }

    /**
     * Get the current visit count for a specific page without incrementing.
     *
     * @param pageName The name/identifier of the page
     * @return The current visit count, or 0 if the page has not been visited
     */
    public long getCount(String pageName) {
        AtomicLong counter = pageCounters.get(sanitizePageName(pageName));
        return counter != null ? counter.get() : 0;
    }

    /**
     * Get all page visit counts.
     *
     * @return A map of all page names and their visit counts
     */
    public Map<String, Long> getAllCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        pageCounters.forEach((page, counter) -> result.put(page, counter.get()));
        return result;
    }

    /**
     * Reset the count for a specific page.
     *
     * @param pageName The name/identifier of the page to reset
     */
    public void resetCount(String pageName) {
        pageCounters.remove(sanitizePageName(pageName));
        logger.info("Reset visit count for page '{}'", pageName);
    }

    /**
     * Reset all page counts.
     */
    public void resetAllCounts() {
        pageCounters.clear();
        logger.info("Reset all visit counts");
    }

    /**
     * Sanitize page name to prevent injection and ensure consistency.
     *
     * @param pageName The raw page name input
     * @return A sanitized, normalized page name
     */
    private String sanitizePageName(String pageName) {
        if (pageName == null || pageName.trim().isEmpty()) {
            return "unknown";
        }
        return pageName.trim().toLowerCase().replaceAll("[^a-z0-9-_]", "-");
    }
}
