package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.TaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * TaskActivityRepository - Data access layer for TaskActivity entities.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {

    // Finds task activities within a date range (inclusive)
    public List<TaskActivity> findByTaskDateBetween(LocalDate startDate, LocalDate endDate);

    // Finds task activities by client name (case-insensitive)
    public List<TaskActivity> findByClientIgnoreCase(String client);

    // Finds task activities by project name (case-insensitive)
    public List<TaskActivity> findByProjectIgnoreCase(String project);

    // Finds task activities by phase name (case-insensitive)
    public List<TaskActivity> findByPhaseIgnoreCase(String phase);

    // Finds task activities by username
    public List<TaskActivity> findByUsername(String username);

    // Check if any task activities exist for a username
    public boolean existsByUsername(String username);

    // Finds task activities by username within date range
    public List<TaskActivity> findByUsernameAndTaskDateBetween(String username, LocalDate startDate,
                    LocalDate endDate);

    // Finds task activities by client and project
    public List<TaskActivity> findByClientIgnoreCaseAndProjectIgnoreCase(String client,
                    String project);

    // Custom query to find task activities for a specific date with ordering
    @Query("SELECT taskActivity FROM TaskActivity taskActivity WHERE taskActivity.taskDate = :date ORDER BY taskActivity.client, taskActivity.project, taskActivity.phase")
    public List<TaskActivity> findTaskActivitiesByDate(@Param("date") LocalDate date);

    // Custom query to calculate total hours for a specific date
    @Query("SELECT SUM(taskActivity.hours) FROM TaskActivity taskActivity WHERE taskActivity.taskDate = :date")
    public Double getTotalHoursByDate(@Param("date") LocalDate date);

    // Custom query to find task activities for a requested date range
    @Query("SELECT taskActivity FROM TaskActivity taskActivity WHERE taskActivity.taskDate BETWEEN :startDate AND :endDate ORDER BY taskActivity.taskDate DESC, taskActivity.client, taskActivity.project")
    public List<TaskActivity> findTaskActivitiesInDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Pageable versions for pagination support
    public Page<TaskActivity> findAll(Pageable pageable);

    public Page<TaskActivity> findByUsername(String username, Pageable pageable);

    public Page<TaskActivity> findByUsernameAndTaskDateBetween(String username, LocalDate startDate,
                    LocalDate endDate, Pageable pageable);

    public Page<TaskActivity> findByUsernameAndTaskDate(String username, LocalDate date,
                    Pageable pageable);

    public Page<TaskActivity> findByUsernameAndClientIgnoreCase(String username, String client,
                    Pageable pageable);

    public Page<TaskActivity> findByUsernameAndProjectIgnoreCase(String username, String project,
                    Pageable pageable);

    public Page<TaskActivity> findByTaskDateBetween(LocalDate startDate, LocalDate endDate,
            Pageable pageable);

    public Page<TaskActivity> findByClientIgnoreCase(String client, Pageable pageable);

    public Page<TaskActivity> findByProjectIgnoreCase(String project, Pageable pageable);

    public Page<TaskActivity> findByPhaseIgnoreCase(String phase, Pageable pageable);

    @Query("SELECT taskActivity FROM TaskActivity taskActivity WHERE taskActivity.taskDate = :date")
    public Page<TaskActivity> findTaskActivitiesByDate(@Param("date") LocalDate date,
                    Pageable pageable);

    @Query("SELECT taskActivity FROM TaskActivity taskActivity WHERE taskActivity.taskDate BETWEEN :startDate AND :endDate")
    public Page<TaskActivity> findTaskActivitiesInDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, Pageable pageable);
}
