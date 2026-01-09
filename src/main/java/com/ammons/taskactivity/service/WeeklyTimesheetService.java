package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.TaskActivity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * WeeklyTimesheetService - Weekly timesheet (Monday-Sunday)
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Service
@Transactional(readOnly = true)
public class WeeklyTimesheetService {

    private final TaskActivityService taskActivityService;

    /**
     * Constructor injection for service dependency.
     */
    public WeeklyTimesheetService(TaskActivityService taskActivityService) {
        this.taskActivityService = taskActivityService;
    }

    public WeeklyTimesheetData getCurrentWeekTimesheet() {
        LocalDate today = LocalDate.now();
        return getWeeklyTimesheet(today);
    }

    /**
     * Get data for the week containing requested date (for current user)
     */
    public WeeklyTimesheetData getCurrentWeekTimesheet(String username) {
        LocalDate today = LocalDate.now();
        return getWeeklyTimesheet(today, username);
    }

    /**
     * Get data for the week containing requested date Default is current or previous Monday.
     */
    public WeeklyTimesheetData getWeeklyTimesheet(LocalDate date) {
        LocalDate mondayOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sundayOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<TaskActivity> weekTasks =
                taskActivityService.getTaskActivitiesInDateRange(mondayOfWeek, sundayOfWeek);

        return buildWeeklyTimesheetData(mondayOfWeek, sundayOfWeek, weekTasks);
    }

    /**
     * Get data for the week containing requested date for specific user Default is current or
     * previous Monday.
     */
    public WeeklyTimesheetData getWeeklyTimesheet(LocalDate date, String username) {
        LocalDate mondayOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sundayOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<TaskActivity> weekTasks = taskActivityService
                .getTaskActivitiesInDateRangeForUser(username, mondayOfWeek, sundayOfWeek);

        return buildWeeklyTimesheetData(mondayOfWeek, sundayOfWeek, weekTasks);
    }

    /**
     * Build data structure
     */
    private WeeklyTimesheetData buildWeeklyTimesheetData(LocalDate mondayOfWeek,
            LocalDate sundayOfWeek, List<TaskActivity> weekTasks) {
        WeeklyTimesheetData weeklyData = new WeeklyTimesheetData();
        weeklyData.setWeekStartDate(mondayOfWeek);
        weeklyData.setWeekEndDate(sundayOfWeek);

        Map<DayOfWeek, List<TaskActivity>> tasksByDay = new LinkedHashMap<>();
        Map<DayOfWeek, BigDecimal> totalsByDay = new LinkedHashMap<>();

        // Initialize empty lists and zero totals for each day
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            tasksByDay.put(dayOfWeek, new ArrayList<>());
            totalsByDay.put(dayOfWeek, BigDecimal.ZERO);
        }

        // Group tasks by day of week
        BigDecimal weekTotal = BigDecimal.ZERO;
        for (TaskActivity task : weekTasks) {
            DayOfWeek dayOfWeek = task.getTaskDate().getDayOfWeek();
            tasksByDay.get(dayOfWeek).add(task);
            totalsByDay.put(dayOfWeek, totalsByDay.get(dayOfWeek).add(task.getHours()));
            weekTotal = weekTotal.add(task.getHours());
        }

        // Populate daily buckets
        Map<DayOfWeek, DailyTimesheetData> dailyData = new LinkedHashMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            DailyTimesheetData dailyTimesheet = new DailyTimesheetData();
            dailyTimesheet.setDayOfWeek(dayOfWeek);
            dailyTimesheet.setDate(mondayOfWeek.plusDays((long) dayOfWeek.getValue() - 1));
            dailyTimesheet.setTasks(tasksByDay.get(dayOfWeek));
            dailyTimesheet.setDayTotal(totalsByDay.get(dayOfWeek));
            dailyData.put(dayOfWeek, dailyTimesheet);
        }

        weeklyData.setDailyData(dailyData);
        weeklyData.setWeekTotal(weekTotal);

        return weeklyData;
    }

    /**
     * Weekly time sheet data class
     */
    public static class WeeklyTimesheetData {
        private LocalDate weekStartDate;
        private LocalDate weekEndDate;
        private Map<DayOfWeek, DailyTimesheetData> dailyData;
        private BigDecimal weekTotal;

        public LocalDate getWeekStartDate() {
            return weekStartDate;
        }

        public void setWeekStartDate(LocalDate weekStartDate) {
            this.weekStartDate = weekStartDate;
        }

        public LocalDate getWeekEndDate() {
            return weekEndDate;
        }

        public void setWeekEndDate(LocalDate weekEndDate) {
            this.weekEndDate = weekEndDate;
        }

        public Map<DayOfWeek, DailyTimesheetData> getDailyData() {
            return dailyData;
        }

        public void setDailyData(Map<DayOfWeek, DailyTimesheetData> dailyData) {
            this.dailyData = dailyData;
        }

        public BigDecimal getWeekTotal() {
            return weekTotal;
        }

        public void setWeekTotal(BigDecimal weekTotal) {
            this.weekTotal = weekTotal;
        }

        /**
         * Return days that have tasks
         */
        public List<DailyTimesheetData> getDaysWithTasks() {
            return dailyData.values().stream().filter(daily -> !daily.getTasks().isEmpty())
                    .toList();
        }

        /**
         * Return all days (Monday through Sunday)
         */
        public List<DailyTimesheetData> getAllDays() {
            return dailyData.values().stream().toList();
        }
    }

    /**
     * Daily timesheet data class
     */
    public static class DailyTimesheetData {
        private DayOfWeek dayOfWeek;
        private LocalDate date;
        private List<TaskActivity> tasks;
        private BigDecimal dayTotal;

        public DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public List<TaskActivity> getTasks() {
            return tasks;
        }

        public void setTasks(List<TaskActivity> tasks) {
            this.tasks = tasks;
        }

        public BigDecimal getDayTotal() {
            return dayTotal;
        }

        public void setDayTotal(BigDecimal dayTotal) {
            this.dayTotal = dayTotal;
        }

        /**
         * Return formatted day name
         */
        public String getDayName() {
            return dayOfWeek.name().substring(0, 1).toUpperCase()
                    + dayOfWeek.name().substring(1).toLowerCase();
        }

        /**
         * Verify day has tasks
         */
        public boolean hasTasks() {
            return tasks != null && !tasks.isEmpty();
        }
    }
}
