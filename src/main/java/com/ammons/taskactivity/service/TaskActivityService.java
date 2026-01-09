package com.ammons.taskactivity.service;

import com.ammons.taskactivity.dto.TaskActivityDto;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.exception.TaskActivityNotFoundException;
import com.ammons.taskactivity.repository.TaskActivityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TaskActivityService - Business logic layer for TaskActivity operations.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since November 2025
 */
@Service
@Transactional
public class TaskActivityService {

    /**
     * For thread safety, the field is set to "final" so the object cannot be changed after
     * construction. Not required, but a good practice.
     */
    private final TaskActivityRepository taskActivityRepository;

    public TaskActivityService(TaskActivityRepository taskActivityRepository) {
        this.taskActivityRepository = taskActivityRepository;
    }

    public TaskActivity createTaskActivity(TaskActivityDto taskActivityDto) {
        TaskActivity taskActivity = convertDtoToEntity(taskActivityDto);
        return taskActivityRepository.save(taskActivity);
    }

    @Transactional(readOnly = true)
    public List<TaskActivity> getAllTaskActivities() {
        return taskActivityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TaskActivity> getTaskActivitiesByUsername(String username) {
        return taskActivityRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<TaskActivity> getTaskActivityById(Long id) {
        return taskActivityRepository.findById(id);
    }

    public TaskActivity updateTaskActivity(Long id, TaskActivityDto taskActivityDto) {
        Optional<TaskActivity> existingTaskActivity = taskActivityRepository.findById(id);
        if (existingTaskActivity.isPresent()) {
            TaskActivity taskActivity = existingTaskActivity.get();
            updateEntityFromDto(taskActivity, taskActivityDto);
            return taskActivityRepository.save(taskActivity);
        }
        throw new TaskActivityNotFoundException(id);
    }

    public void deleteTaskActivity(Long id) {
        if (taskActivityRepository.existsById(id)) {
            taskActivityRepository.deleteById(id);
        } else {
            throw new TaskActivityNotFoundException(id);
        }
    }

    /**
     * Return task activities for a specific date
     */
    @Transactional(readOnly = true)
    public List<TaskActivity> getTaskActivitiesByDate(LocalDate date) {
        return taskActivityRepository.findTaskActivitiesByDate(date);
    }

    /**
     * Return task activities within a date range.
     */
    @Transactional(readOnly = true)
    public List<TaskActivity> getTaskActivitiesInDateRange(LocalDate startDate, LocalDate endDate) {
        return taskActivityRepository.findTaskActivitiesInDateRange(startDate, endDate);
    }

    /**
     * Return task activities within a date range for a specific user.
     */
    @Transactional(readOnly = true)
    public List<TaskActivity> getTaskActivitiesInDateRangeForUser(String username,
            LocalDate startDate, LocalDate endDate) {
        return taskActivityRepository.findByUsernameAndTaskDateBetween(username, startDate,
                endDate);
    }

    /**
     * Return task activities filtered by client name.
     */
    @Transactional(readOnly = true)
    public List<TaskActivity> getTaskActivitiesByClient(String client) {
        return taskActivityRepository.findByClientIgnoreCase(client);
    }

    /**
     * Return task activities filtered by project name.
     */
    @Transactional(readOnly = true)
    public List<TaskActivity> getTaskActivitiesByProject(String project) {
        return taskActivityRepository.findByProjectIgnoreCase(project);
    }

    /**
     * Return total hours for a specific date.
     */
    @Transactional(readOnly = true)
    public Double getTotalHoursByDate(LocalDate date) {
        Double total = taskActivityRepository.getTotalHoursByDate(date);
        return total != null ? total : 0.0;
    }

    // Pageable versions for pagination support
    @Transactional(readOnly = true)
    public Page<TaskActivity> getAllTaskActivities(Pageable pageable) {
        return taskActivityRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesByDate(LocalDate date, Pageable pageable) {
        return taskActivityRepository.findTaskActivitiesByDate(date, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesInDateRange(LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        return taskActivityRepository.findTaskActivitiesInDateRange(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesByClient(String client, Pageable pageable) {
        return taskActivityRepository.findByClientIgnoreCase(client, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesByProject(String project, Pageable pageable) {
        return taskActivityRepository.findByProjectIgnoreCase(project, pageable);
    }

    // Username-filtered versions for data isolation
    @Transactional(readOnly = true)
    public Page<TaskActivity> getAllTaskActivitiesForUser(String username, Pageable pageable) {
        return taskActivityRepository.findByUsername(username, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesByDateForUser(String username, LocalDate date,
            Pageable pageable) {
        return taskActivityRepository.findByUsernameAndTaskDate(username, date, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesInDateRangeForUser(String username,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return taskActivityRepository.findByUsernameAndTaskDateBetween(username, startDate, endDate,
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesByClientForUser(String username, String client,
            Pageable pageable) {
        return taskActivityRepository.findByUsernameAndClientIgnoreCase(username, client, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesByProjectForUser(String username, String project,
            Pageable pageable) {
        return taskActivityRepository.findByUsernameAndProjectIgnoreCase(username, project,
                pageable);
    }

    /**
     * Get task activities with flexible filters and pagination
     */
    @Transactional(readOnly = true)
    public Page<TaskActivity> getTaskActivitiesByFilters(String username, String client,
            String project, String phase, LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        return taskActivityRepository.findByFilters(username, client, project, phase, startDate,
                endDate, pageable);
    }

    /**
     * Check if a user has any task activities
     * 
     * @param username the username to check
     * @return true if the user has any task activities, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean userHasTaskActivities(String username) {
        return taskActivityRepository.existsByUsername(username);
    }

    /**
     * Convert DTO object to an Entity object
     */
    private TaskActivity convertDtoToEntity(TaskActivityDto dto) {
        TaskActivity entity = new TaskActivity();
        entity.setTaskDate(dto.getTaskDate());
        entity.setClient(dto.getClient());
        entity.setProject(dto.getProject());
        entity.setPhase(dto.getPhase());
        entity.setHours(dto.getHours());
        entity.setDetails(dto.getDetails());
        entity.setUsername(dto.getUsername());
        return entity;
    }

    /**
     * Populate entity object with DTO details Note: Username is NOT updated - it's immutable after
     * creation
     */
    private void updateEntityFromDto(TaskActivity entity, TaskActivityDto dto) {
        entity.setTaskDate(dto.getTaskDate());
        entity.setClient(dto.getClient());
        entity.setProject(dto.getProject());
        entity.setPhase(dto.getPhase());
        entity.setHours(dto.getHours());
        entity.setDetails(dto.getDetails());
        // Username is NOT updated - it remains the original creator
    }
}
