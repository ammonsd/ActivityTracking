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
 * WeeklyTimesheetService - Weekly timesheet supporting configurable week start day. Supports
 * Monday-Sunday (default) and Saturday-Friday ranges.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 *
 *        Modified by: Dean Ammons - March 2026 Change: Added support for configurable week start
 *        day (MONDAY or SATURDAY) Reason: Some clients use Saturday-Friday as their standard
 *        billing week
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

    /**
     * Resolves a week-start-day string preference (e.g. "MONDAY", "SATURDAY") to a DayOfWeek.
     * Returns MONDAY for any unrecognised value.
     *
     * @param weekStartDayStr the stored preference string
     * @return the corresponding DayOfWeek, defaulting to MONDAY
     */
    public static DayOfWeek resolveWeekStartDay(String weekStartDayStr) {
        if ("SATURDAY".equalsIgnoreCase(weekStartDayStr)) {
            return DayOfWeek.SATURDAY;
        }
        return DayOfWeek.MONDAY;
    }

    public WeeklyTimesheetData getCurrentWeekTimesheet() {
        return getWeeklyTimesheet(LocalDate.now(), DayOfWeek.MONDAY);
    }

    /**
     * Get data for the current week for the given user using their preferred start day.
     */
    public WeeklyTimesheetData getCurrentWeekTimesheet(String username, DayOfWeek weekStartDay) {
        return getWeeklyTimesheet(LocalDate.now(), username, weekStartDay);
    }

    /**
     * Get data for the week containing requested date (for current user), Monday start.
     */
    public WeeklyTimesheetData getCurrentWeekTimesheet(String username) {
        return getCurrentWeekTimesheet(username, DayOfWeek.MONDAY);
    }

    /**
     * Get data for the week containing requested date, using Monday as the start day.
     */
    public WeeklyTimesheetData getWeeklyTimesheet(LocalDate date) {
        return getWeeklyTimesheet(date, DayOfWeek.MONDAY);
    }

    /**
     * Get data for the week containing the requested date using the specified start day.
     */
    public WeeklyTimesheetData getWeeklyTimesheet(LocalDate date, DayOfWeek weekStartDay) {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(weekStartDay));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<TaskActivity> weekTasks =
                taskActivityService.getTaskActivitiesInDateRange(weekStart, weekEnd);
        return buildWeeklyTimesheetData(weekStart, weekEnd, weekTasks);
    }

    /**
     * Get data for the week containing requested date for a specific user, Monday start.
     */
    public WeeklyTimesheetData getWeeklyTimesheet(LocalDate date, String username) {
        return getWeeklyTimesheet(date, username, DayOfWeek.MONDAY);
    }

    /**
     * Get data for the week containing the requested date for a specific user, using the specified
     * start day.
     */
    public WeeklyTimesheetData getWeeklyTimesheet(LocalDate date, String username,
            DayOfWeek weekStartDay) {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(weekStartDay));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<TaskActivity> weekTasks = taskActivityService
                .getTaskActivitiesInDateRangeForUser(username, weekStart, weekEnd);
        return buildWeeklyTimesheetData(weekStart, weekEnd, weekTasks);
    }

    /**
     * Build the weekly timesheet data structure for any week start day. Days are ordered from
     * weekStartDate through weekEndDate.
     */
    private WeeklyTimesheetData buildWeeklyTimesheetData(LocalDate weekStartDate,
            LocalDate weekEndDate, List<TaskActivity> weekTasks) {

        // Group tasks and accumulate totals by day-of-week
        Map<DayOfWeek, List<TaskActivity>> tasksByDay = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, BigDecimal> totalsByDay = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            tasksByDay.put(d, new ArrayList<>());
            totalsByDay.put(d, BigDecimal.ZERO);
        }

        BigDecimal weekTotal = BigDecimal.ZERO;
        for (TaskActivity task : weekTasks) {
            DayOfWeek dayOfWeek = task.getTaskDate().getDayOfWeek();
            tasksByDay.get(dayOfWeek).add(task);
            totalsByDay.put(dayOfWeek, totalsByDay.get(dayOfWeek).add(task.getHours()));
            weekTotal = weekTotal.add(task.getHours());
        }

        // Build ordered daily buckets matching the actual week order (supports any start day)
        Map<DayOfWeek, DailyTimesheetData> dailyData = new LinkedHashMap<>();
        LocalDate current = weekStartDate;
        while (!current.isAfter(weekEndDate)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            DailyTimesheetData dailyTimesheet = new DailyTimesheetData();
            dailyTimesheet.setDayOfWeek(dayOfWeek);
            dailyTimesheet.setDate(current);
            dailyTimesheet.setTasks(tasksByDay.get(dayOfWeek));
            dailyTimesheet.setDayTotal(totalsByDay.get(dayOfWeek));
            dailyData.put(dayOfWeek, dailyTimesheet);
            current = current.plusDays(1);
        }

        WeeklyTimesheetData weeklyData = new WeeklyTimesheetData();
        weeklyData.setWeekStartDate(weekStartDate);
        weeklyData.setWeekEndDate(weekEndDate);
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
