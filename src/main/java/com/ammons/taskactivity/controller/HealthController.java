package com.ammons.taskactivity.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
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
            health.put("application", "Task Activity Management API");
            health.put("version", "1.0.0");
            health.put("authentication", "Windows Authentication");
            health.put("startupTime", STARTUP_TIME);
            health.put("uptime",
                    java.time.Duration.between(STARTUP_TIME, LocalDateTime.now()).toString());

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
                java.time.Duration.between(STARTUP_TIME, LocalDateTime.now()).toString());
        return ResponseEntity.ok(info);
    }
}
