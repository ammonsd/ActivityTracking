package com.ammons.taskactivity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * HealthController - REST API Controller
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final LocalDateTime STARTUP_TIME = LocalDateTime.now();

    @Autowired
    private DataSource dataSource;

    /**
     * Formats a Duration into a human-readable string
     * 
     * @param duration The duration to format
     * @return Human-readable string like "3 days, 8 hours, 34 minutes"
     */
    private String formatUptime(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder uptime = new StringBuilder();
        if (days > 0) {
            uptime.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (uptime.length() > 0)
                uptime.append(", ");
            uptime.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0) {
            if (uptime.length() > 0)
                uptime.append(", ");
            uptime.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0 || uptime.length() == 0) {
            if (uptime.length() > 0)
                uptime.append(", ");
            uptime.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return uptime.toString();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Verify database connectivity using Windows Authentication
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                health.put("database", isValid ? "UP (Windows Auth)" : "DOWN");
                health.put("databaseUser", connection.getMetaData().getUserName());
            }

            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("application", "Task Activity & Expense Management API");
            health.put("version", "1.0.0");
            health.put("authentication", "Windows Authentication");
            health.put("startupTime", STARTUP_TIME);
            health.put("uptime",
                    formatUptime(Duration.between(STARTUP_TIME, LocalDateTime.now())));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("database", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(503).body(health);
        }
    }

    @GetMapping("/simple")
    public ResponseEntity<String> simpleHealth() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/startup")
    public ResponseEntity<Map<String, Object>> getStartupInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("startupTime", STARTUP_TIME);
        info.put("uptime",
                formatUptime(Duration.between(STARTUP_TIME, LocalDateTime.now())));
        return ResponseEntity.ok(info);
    }
}
