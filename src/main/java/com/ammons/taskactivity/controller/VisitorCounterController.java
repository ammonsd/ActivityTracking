package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.service.VisitorCounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for tracking page visit counts. Provides privacy-friendly visitor counting with
 * no personal data collection.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class VisitorCounterController {

    private static final LocalDateTime STARTUP_TIME = LocalDateTime.now();

    private final VisitorCounterService visitorCounterService;

    @Autowired
    public VisitorCounterController(VisitorCounterService visitorCounterService) {
        this.visitorCounterService = visitorCounterService;
    }

    /**
     * Increment and return the visitor count for a specific page. This endpoint is public and
     * requires no authentication.
     *
     * @param pageName The name/identifier of the page being visited
     * @return JSON response with page name, count, and timestamp
     */
    @PostMapping("/visit/{pageName}")
    public ResponseEntity<Map<String, Object>> incrementVisit(@PathVariable String pageName) {
        long count = visitorCounterService.incrementAndGet(pageName);

        Map<String, Object> response = new HashMap<>();
        response.put("page", pageName);
        response.put("count", count);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * Get the current visitor count for a specific page without incrementing.
     *
     * @param pageName The name/identifier of the page
     * @return JSON response with page name and current count
     */
    @GetMapping("/visit/{pageName}")
    public ResponseEntity<Map<String, Object>> getVisitCount(@PathVariable String pageName) {
        long count = visitorCounterService.getCount(pageName);

        Map<String, Object> response = new HashMap<>();
        response.put("page", pageName);
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all page visit counts. Useful for analytics and monitoring.
     *
     * @return JSON map of all page names and their visit counts, plus startup time
     */
    @GetMapping("/visit/stats")
    public ResponseEntity<Map<String, Object>> getAllStats() {
        Map<String, Object> response = new HashMap<>();

        // Add all page counts
        visitorCounterService.getAllCounts().forEach(response::put);

        // Add startup time as the last entry
        response.put("startupTime", STARTUP_TIME);

        return ResponseEntity.ok(response);
    }
}
