package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.TaskActivityDto;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.service.TaskActivityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TaskActivitiesController - REST API Controller
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/task-activities") // Base URL for all endpoints in this controller
public class TaskActivitiesController {

    private static final Logger logger = LoggerFactory.getLogger(TaskActivitiesController.class);
    private static final String SUCCESS = "success";
    private static final String MESSAGE = "message";
    private static final String COUNT = "count";
    private static final String FAILED_TO_RETRIEVE_TASK_ACTIVITIES =
            "Failed to retrieve task activities: ";

    // Constructor injection is preferred for mandatory dependencies over @Autowired
    // because it makes the dependencies explicit.
    private final TaskActivityService taskActivityService;

    public TaskActivitiesController(TaskActivityService taskActivityService) {
        this.taskActivityService = taskActivityService;
    }

    /**
     * Create a new task activity
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTaskActivity(
            @Valid @RequestBody TaskActivityDto taskActivityDto, BindingResult bindingResult,
            Authentication authentication) {

        logger.info("User {} creating task activity", authentication.getName());
        Map<String, Object> response = new HashMap<>();

        // Validation Check
        if (bindingResult.hasErrors()) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, "Validation failed");
            response.put("errors", getValidationErrors(bindingResult)); // Helper method for error
                                                                        // formatting
            return ResponseEntity.badRequest().body(response); // HTTP 400
        }

        try {
            TaskActivity createdTaskActivity =
                    taskActivityService.createTaskActivity(taskActivityDto);

            response.put(SUCCESS, true);
            response.put(MESSAGE, "Task activity created successfully");
            response.put("data", createdTaskActivity);
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // HTTP 201

        } catch (Exception e) {
            // Error response with details
            response.put(SUCCESS, false);
            response.put(MESSAGE, "Failed to create task activity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // HTTP
                                                                                           // 500
        }
    }

    /**
     * Get all task activities
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTaskActivities(Authentication authentication) {
        logger.info("User {} requesting all task activities", authentication.getName());
        Map<String, Object> response = new HashMap<>();

        try {
            List<TaskActivity> taskActivities = taskActivityService.getAllTaskActivities();
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Task activities retrieved successfully");
            response.put("data", taskActivities);
            response.put(COUNT, taskActivities.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, FAILED_TO_RETRIEVE_TASK_ACTIVITIES + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get task activity by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTaskActivityById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<TaskActivity> taskActivity = taskActivityService.getTaskActivityById(id);
            if (taskActivity.isPresent()) {
                response.put(SUCCESS, true);
                response.put(MESSAGE, "Task activity retrieved successfully");
                response.put("data", taskActivity.get());
                return ResponseEntity.ok(response);
            } else {
                response.put(SUCCESS, false);
                response.put(MESSAGE, "Task activity not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, "Failed to retrieve task activity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update task activity
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTaskActivity(@PathVariable Long id,
            @Valid @RequestBody TaskActivityDto taskActivityDto, BindingResult bindingResult) {

        Map<String, Object> response = new HashMap<>();

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, "Validation failed");
            response.put("errors", getValidationErrors(bindingResult));
            return ResponseEntity.badRequest().body(response);
        }

        try {
            TaskActivity updatedTaskActivity =
                    taskActivityService.updateTaskActivity(id, taskActivityDto);
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Task activity updated successfully");
            response.put("data", updatedTaskActivity);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, "Failed to update task activity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete task activity
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTaskActivity(@PathVariable Long id,
            Authentication authentication) {
        logger.info("Admin {} deleting task activity with ID: {}", authentication.getName(), id);
        Map<String, Object> response = new HashMap<>();

        try {
            taskActivityService.deleteTaskActivity(id);
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Task activity deleted successfully");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, "Failed to delete task activity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get task activities by date
     */
    @GetMapping("/by-date")
    public ResponseEntity<Map<String, Object>> getTaskActivitiesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<TaskActivity> taskActivities = taskActivityService.getTaskActivitiesByDate(date);
            Double totalHours = taskActivityService.getTotalHoursByDate(date);

            response.put(SUCCESS, true);
            response.put(MESSAGE, "Task activities for " + date + " retrieved successfully");
            response.put("data", taskActivities);
            response.put(COUNT, taskActivities.size());
            response.put("totalHours", totalHours);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, FAILED_TO_RETRIEVE_TASK_ACTIVITIES + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get task activities by date range
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<Map<String, Object>> getTaskActivitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<TaskActivity> taskActivities =
                    taskActivityService.getTaskActivitiesInDateRange(startDate, endDate);
            response.put(SUCCESS, true);
            response.put(MESSAGE, "Task activities from " + startDate + " to " + endDate
                    + " retrieved successfully");
            response.put("data", taskActivities);
            response.put(COUNT, taskActivities.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, FAILED_TO_RETRIEVE_TASK_ACTIVITIES + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get task activities by client
     */
    @GetMapping("/by-client")
    public ResponseEntity<Map<String, Object>> getTaskActivitiesByClient(
            @RequestParam String client) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<TaskActivity> taskActivities =
                    taskActivityService.getTaskActivitiesByClient(client);
            response.put(SUCCESS, true);
            response.put(MESSAGE,
                    "Task activities for client '" + client + "' retrieved successfully");
            response.put("data", taskActivities);
            response.put(COUNT, taskActivities.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put(SUCCESS, false);
            response.put(MESSAGE, FAILED_TO_RETRIEVE_TASK_ACTIVITIES + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to extract validation errors
     */
    private Map<String, String> getValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }
}
