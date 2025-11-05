package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
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

    // Constructor injection is preferred for mandatory dependencies over @Autowired
    // because it makes the dependencies explicit.
    private final TaskActivityService taskActivityService;

    public TaskActivitiesController(TaskActivityService taskActivityService) {
        this.taskActivityService = taskActivityService;
    }

    /**
     * Create a new task activity
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<TaskActivity>> createTaskActivity(
            @Valid @RequestBody TaskActivityDto taskActivityDto,
            Authentication authentication) {

        logger.info("User {} creating task activity", authentication.getName());

        TaskActivity createdTaskActivity = taskActivityService.createTaskActivity(taskActivityDto);

        ApiResponse<TaskActivity> response =
                ApiResponse.success("Task activity created successfully", createdTaskActivity);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all task activities - ADMIN users: see all tasks - GUEST/USER: see only their own tasks
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskActivity>>> getAllTaskActivities(
            Authentication authentication) {

            String username = authentication.getName();
            logger.info("User {} requesting all task activities", username);

        // Check if user has ADMIN role
        boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        List<TaskActivity> taskActivities;

        if (isAdmin) {
                // Admin sees all tasks
                taskActivities = taskActivityService.getAllTaskActivities();
        } else {
                // GUEST and USER see only their own tasks
                taskActivities = taskActivityService.getTaskActivitiesByUsername(username);
        }

        ApiResponse<List<TaskActivity>> response =
                ApiResponse.success("Task activities retrieved successfully", taskActivities)
                        .withCount(taskActivities.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get task activity by ID
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskActivity>> getTaskActivityById(@PathVariable Long id) {

        Optional<TaskActivity> taskActivity = taskActivityService.getTaskActivityById(id);

        if (taskActivity.isPresent()) {
            ApiResponse<TaskActivity> response =
                    ApiResponse.success("Task activity retrieved successfully", taskActivity.get());
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<TaskActivity> response =
                    ApiResponse.error("Task activity not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Update task activity
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskActivity>> updateTaskActivity(@PathVariable Long id,
                    @Valid @RequestBody TaskActivityDto taskActivityDto,
                    Authentication authentication) {

            // Security check: verify task belongs to logged-in user before updating (unless admin)
            boolean isUserAdmin = authentication.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (!isUserAdmin) {
                    String username = authentication.getName();
                    Optional<TaskActivity> existingTask =
                                    taskActivityService.getTaskActivityById(id);

                    if (existingTask.isEmpty()) {
                            ApiResponse<TaskActivity> response = ApiResponse
                                            .error("Task activity not found with ID: " + id);
                            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    }

                    if (!existingTask.get().getUsername().equals(username)) {
                            logger.warn("User {} attempted to update task {} owned by {}", username,
                                            id, existingTask.get().getUsername());
                            ApiResponse<TaskActivity> response = ApiResponse.error(
                                            "Access denied: You can only update your own tasks");
                            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
            }

        TaskActivity updatedTaskActivity =
                taskActivityService.updateTaskActivity(id, taskActivityDto);

        ApiResponse<TaskActivity> response =
                ApiResponse.success("Task activity updated successfully", updatedTaskActivity);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete task activity
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTaskActivity(@PathVariable Long id,
            Authentication authentication) {

        logger.info("Admin {} deleting task activity with ID: {}", authentication.getName(), id);

        taskActivityService.deleteTaskActivity(id);

        ApiResponse<Void> response = ApiResponse.success("Task activity deleted successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get task activities by date
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/by-date")
    public ResponseEntity<ApiResponse<List<TaskActivity>>> getTaskActivitiesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TaskActivity> taskActivities = taskActivityService.getTaskActivitiesByDate(date);
        Double totalHours = taskActivityService.getTotalHoursByDate(date);

        ApiResponse<List<TaskActivity>> response = ApiResponse
                .success("Task activities for " + date + " retrieved successfully", taskActivities)
                .withCount(taskActivities.size()).withTotalHours(totalHours);

        return ResponseEntity.ok(response);
    }

    /**
     * Get task activities by date range
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/by-date-range")
    public ResponseEntity<ApiResponse<List<TaskActivity>>> getTaskActivitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<TaskActivity> taskActivities =
                taskActivityService.getTaskActivitiesInDateRange(startDate, endDate);

        ApiResponse<List<TaskActivity>> response = ApiResponse
                .success("Task activities from " + startDate + " to " + endDate
                        + " retrieved successfully", taskActivities)
                .withCount(taskActivities.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get task activities by client
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/by-client")
    public ResponseEntity<ApiResponse<List<TaskActivity>>> getTaskActivitiesByClient(
            @RequestParam String client) {

        List<TaskActivity> taskActivities = taskActivityService.getTaskActivitiesByClient(client);

        ApiResponse<List<TaskActivity>> response =
                ApiResponse
                        .success("Task activities for client '" + client
                                + "' retrieved successfully", taskActivities)
                        .withCount(taskActivities.size());

        return ResponseEntity.ok(response);
    }
}
