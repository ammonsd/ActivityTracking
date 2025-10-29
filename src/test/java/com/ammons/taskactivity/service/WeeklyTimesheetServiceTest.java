package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.service.WeeklyTimesheetService.WeeklyTimesheetData;
import com.ammons.taskactivity.service.WeeklyTimesheetService.DailyTimesheetData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyTimesheetService Tests")
class WeeklyTimesheetServiceTest {

    @Mock
    private TaskActivityService taskActivityService;

    @InjectMocks
    private WeeklyTimesheetService weeklyTimesheetService;

    private TaskActivity mondayTask;
    private TaskActivity tuesdayTask;
    private TaskActivity wednesdayTask;

    @BeforeEach
    void setUp() {
        // Create test tasks for different days of the week
        mondayTask = createTaskActivity(1L, LocalDate.of(2025, 9, 15), "Client A", "Project 1",
                "Development", new BigDecimal("8.00"), "Monday work");
        tuesdayTask = createTaskActivity(2L, LocalDate.of(2025, 9, 16), "Client B", "Project 2",
                "Testing", new BigDecimal("4.00"), "Tuesday work");
        wednesdayTask = createTaskActivity(3L, LocalDate.of(2025, 9, 17), "Client A", "Project 1",
                "Development", new BigDecimal("6.50"), "Wednesday work");
    }

    private TaskActivity createTaskActivity(Long id, LocalDate date, String client, String project,
            String phase, BigDecimal hours, String details) {
        TaskActivity task = new TaskActivity();
        task.setId(id);
        task.setTaskDate(date);
        task.setClient(client);
        task.setProject(project);
        task.setPhase(phase);
        task.setHours(hours);
        task.setDetails(details);
        return task;
    }

    @Nested
    @DisplayName("Current Week Timesheet Tests")
    class CurrentWeekTimesheetTests {

        @Test
        @DisplayName("Should get current week timesheet")
        void shouldGetCurrentWeekTimesheet() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate mondayOfWeek = today
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sundayOfWeek =
                    today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            List<TaskActivity> weekTasks = Arrays.asList(mondayTask);
            when(taskActivityService.getTaskActivitiesInDateRange(mondayOfWeek, sundayOfWeek))
                    .thenReturn(weekTasks);

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getCurrentWeekTimesheet();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(mondayOfWeek);
            assertThat(result.getWeekEndDate()).isEqualTo(sundayOfWeek);
            verify(taskActivityService).getTaskActivitiesInDateRange(mondayOfWeek, sundayOfWeek);
        }
    }

    @Nested
    @DisplayName("Weekly Timesheet Tests")
    class WeeklyTimesheetTests {

        @Test
        @DisplayName("Should get weekly timesheet for specific date")
        void shouldGetWeeklyTimesheetForSpecificDate() {
            // Given - Wednesday, September 17, 2025
            LocalDate testDate = LocalDate.of(2025, 9, 17);
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15); // Monday of that week
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21); // Sunday of that week

            List<TaskActivity> weekTasks = Arrays.asList(mondayTask, tuesdayTask, wednesdayTask);
            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(weekTasks);

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(expectedMonday);
            assertThat(result.getWeekEndDate()).isEqualTo(expectedSunday);
            assertThat(result.getWeekTotal()).isEqualByComparingTo(new BigDecimal("18.50")); // 8.00
                                                                                             // +
                                                                                             // 4.00
                                                                                             // +
                                                                                             // 6.50

            verify(taskActivityService).getTaskActivitiesInDateRange(expectedMonday,
                    expectedSunday);
        }

        @Test
        @DisplayName("Should get weekly timesheet for Monday")
        void shouldGetWeeklyTimesheetForMonday() {
            // Given - Monday, September 15, 2025
            LocalDate mondayDate = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            List<TaskActivity> weekTasks = Arrays.asList(mondayTask);
            when(taskActivityService.getTaskActivitiesInDateRange(mondayDate, expectedSunday))
                    .thenReturn(weekTasks);

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(mondayDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(mondayDate);
            assertThat(result.getWeekEndDate()).isEqualTo(expectedSunday);
        }

        @Test
        @DisplayName("Should get weekly timesheet for Sunday")
        void shouldGetWeeklyTimesheetForSunday() {
            // Given - Sunday, September 21, 2025
            LocalDate sundayDate = LocalDate.of(2025, 9, 21);
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15);

            List<TaskActivity> weekTasks = Arrays.asList();
            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, sundayDate))
                    .thenReturn(weekTasks);

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(sundayDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(expectedMonday);
            assertThat(result.getWeekEndDate()).isEqualTo(sundayDate);
        }

        @Test
        @DisplayName("Should handle empty week")
        void shouldHandleEmptyWeek() {
            // Given
            LocalDate testDate = LocalDate.of(2025, 9, 17);
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(Arrays.asList());

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(expectedMonday);
            assertThat(result.getWeekEndDate()).isEqualTo(expectedSunday);
            assertThat(result.getWeekTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getDailyData()).hasSize(7); // All 7 days should be present
            assertThat(result.getDaysWithTasks()).isEmpty(); // No days should have tasks

            // Check that all days have empty task lists
            for (DailyTimesheetData dailyData : result.getAllDays()) {
                assertThat(dailyData.getTasks()).isEmpty();
                assertThat(dailyData.getDayTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }
    }

    @Nested
    @DisplayName("WeeklyTimesheetData Tests")
    class WeeklyTimesheetDataTests {

        @Test
        @DisplayName("Should get days with tasks correctly")
        void shouldGetDaysWithTasksCorrectly() {
            // Given
            LocalDate testDate = LocalDate.of(2025, 9, 17);
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            List<TaskActivity> weekTasks = Arrays.asList(mondayTask, wednesdayTask); // Only Monday
                                                                                     // and
                                                                                     // Wednesday
            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(weekTasks);

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            List<DailyTimesheetData> daysWithTasks = result.getDaysWithTasks();
            assertThat(daysWithTasks).hasSize(2);
            assertThat(daysWithTasks).extracting(DailyTimesheetData::getDayOfWeek)
                    .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
        }

        @Test
        @DisplayName("Should get all days correctly")
        void shouldGetAllDaysCorrectly() {
            // Given
            LocalDate testDate = LocalDate.of(2025, 9, 17);
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(Arrays.asList());

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            List<DailyTimesheetData> allDays = result.getAllDays();
            assertThat(allDays).hasSize(7);
            assertThat(allDays).extracting(DailyTimesheetData::getDayOfWeek).containsExactly(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        }

        @Test
        @DisplayName("Should calculate correct date for each day")
        void shouldCalculateCorrectDateForEachDay() {
            // Given
            LocalDate testDate = LocalDate.of(2025, 9, 17); // Wednesday
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(Arrays.asList());

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            Map<DayOfWeek, DailyTimesheetData> dailyData = result.getDailyData();

            assertThat(dailyData.get(DayOfWeek.MONDAY).getDate())
                    .isEqualTo(LocalDate.of(2025, 9, 15));
            assertThat(dailyData.get(DayOfWeek.TUESDAY).getDate())
                    .isEqualTo(LocalDate.of(2025, 9, 16));
            assertThat(dailyData.get(DayOfWeek.WEDNESDAY).getDate())
                    .isEqualTo(LocalDate.of(2025, 9, 17));
            assertThat(dailyData.get(DayOfWeek.THURSDAY).getDate())
                    .isEqualTo(LocalDate.of(2025, 9, 18));
            assertThat(dailyData.get(DayOfWeek.FRIDAY).getDate())
                    .isEqualTo(LocalDate.of(2025, 9, 19));
            assertThat(dailyData.get(DayOfWeek.SATURDAY).getDate())
                    .isEqualTo(LocalDate.of(2025, 9, 20));
            assertThat(dailyData.get(DayOfWeek.SUNDAY).getDate())
                    .isEqualTo(LocalDate.of(2025, 9, 21));
        }
    }

    @Nested
    @DisplayName("DailyTimesheetData Tests")
    class DailyTimesheetDataTests {

        @Test
        @DisplayName("Should format day name correctly")
        void shouldFormatDayNameCorrectly() {
            // Given
            LocalDate testDate = LocalDate.of(2025, 9, 17);
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(Arrays.asList());

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            Map<DayOfWeek, DailyTimesheetData> dailyData = result.getDailyData();

            assertThat(dailyData.get(DayOfWeek.MONDAY).getDayName()).isEqualTo("Monday");
            assertThat(dailyData.get(DayOfWeek.TUESDAY).getDayName()).isEqualTo("Tuesday");
            assertThat(dailyData.get(DayOfWeek.WEDNESDAY).getDayName()).isEqualTo("Wednesday");
            assertThat(dailyData.get(DayOfWeek.THURSDAY).getDayName()).isEqualTo("Thursday");
            assertThat(dailyData.get(DayOfWeek.FRIDAY).getDayName()).isEqualTo("Friday");
            assertThat(dailyData.get(DayOfWeek.SATURDAY).getDayName()).isEqualTo("Saturday");
            assertThat(dailyData.get(DayOfWeek.SUNDAY).getDayName()).isEqualTo("Sunday");
        }

        @Test
        @DisplayName("Should correctly identify days with tasks")
        void shouldCorrectlyIdentifyDaysWithTasks() {
            // Given
            LocalDate testDate = LocalDate.of(2025, 9, 17);
            LocalDate expectedMonday = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            List<TaskActivity> weekTasks = Arrays.asList(mondayTask, wednesdayTask);
            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(weekTasks);

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            Map<DayOfWeek, DailyTimesheetData> dailyData = result.getDailyData();

            assertThat(dailyData.get(DayOfWeek.MONDAY).hasTasks()).isTrue();
            assertThat(dailyData.get(DayOfWeek.TUESDAY).hasTasks()).isFalse();
            assertThat(dailyData.get(DayOfWeek.WEDNESDAY).hasTasks()).isTrue();
            assertThat(dailyData.get(DayOfWeek.THURSDAY).hasTasks()).isFalse();
            assertThat(dailyData.get(DayOfWeek.FRIDAY).hasTasks()).isFalse();
            assertThat(dailyData.get(DayOfWeek.SATURDAY).hasTasks()).isFalse();
            assertThat(dailyData.get(DayOfWeek.SUNDAY).hasTasks()).isFalse();
        }

        @Test
        @DisplayName("Should correctly calculate daily totals")
        void shouldCorrectlyCalculateDailyTotals() {
            // Given
            LocalDate testDate = LocalDate.of(2025, 9, 15); // Monday
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            // Create multiple tasks for Monday
            TaskActivity mondayTask1 = createTaskActivity(1L, LocalDate.of(2025, 9, 15), "Client A",
                    "Project 1", "Development", new BigDecimal("4.00"), "Morning work");
            TaskActivity mondayTask2 = createTaskActivity(2L, LocalDate.of(2025, 9, 15), "Client B",
                    "Project 2", "Testing", new BigDecimal("3.50"), "Afternoon work");

            List<TaskActivity> weekTasks = Arrays.asList(mondayTask1, mondayTask2);
            when(taskActivityService.getTaskActivitiesInDateRange(testDate, expectedSunday))
                    .thenReturn(weekTasks);

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            DailyTimesheetData mondayData = result.getDailyData().get(DayOfWeek.MONDAY);
            assertThat(mondayData.getDayTotal()).isEqualByComparingTo(new BigDecimal("7.50")); // 4.00
                                                                                               // +
                                                                                               // 3.50
            assertThat(mondayData.getTasks()).hasSize(2);
            assertThat(result.getWeekTotal()).isEqualByComparingTo(new BigDecimal("7.50"));
        }

        @Test
        @DisplayName("Should handle null tasks list in hasTasks method")
        void shouldHandleNullTasksListInHasTasksMethod() {
            // Given
            WeeklyTimesheetService.DailyTimesheetData dailyData =
                    new WeeklyTimesheetService.DailyTimesheetData();
            dailyData.setTasks(null);

            // When/Then
            assertThat(dailyData.hasTasks()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle year boundary week")
        void shouldHandleYearBoundaryWeek() {
            // Given - New Year's Day 2026 (Wednesday)
            LocalDate newYearDate = LocalDate.of(2026, 1, 1);
            LocalDate expectedMonday = LocalDate.of(2025, 12, 29); // Previous year (actual Monday)
            LocalDate expectedSunday = LocalDate.of(2026, 1, 4); // Current year (actual Sunday)

            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(Arrays.asList());

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(newYearDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(expectedMonday);
            assertThat(result.getWeekEndDate()).isEqualTo(expectedSunday);
        }

        @Test
        @DisplayName("Should handle leap year week")
        void shouldHandleLeapYearWeek() {
            // Given - February 29, 2024 (leap year, Thursday)
            LocalDate leapYearDate = LocalDate.of(2024, 2, 29);
            LocalDate expectedMonday = LocalDate.of(2024, 2, 26);
            LocalDate expectedSunday = LocalDate.of(2024, 3, 3);

            when(taskActivityService.getTaskActivitiesInDateRange(expectedMonday, expectedSunday))
                    .thenReturn(Arrays.asList());

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(leapYearDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekStartDate()).isEqualTo(expectedMonday);
            assertThat(result.getWeekEndDate()).isEqualTo(expectedSunday);
        }

        @Test
        @DisplayName("Should handle tasks with zero hours")
        void shouldHandleTasksWithZeroHours() {
            // Given
            TaskActivity zeroHourTask = createTaskActivity(1L, LocalDate.of(2025, 9, 15),
                    "Client A", "Project 1", "Meeting", BigDecimal.ZERO, "Quick meeting");
            LocalDate testDate = LocalDate.of(2025, 9, 15);
            LocalDate expectedSunday = LocalDate.of(2025, 9, 21);

            when(taskActivityService.getTaskActivitiesInDateRange(testDate, expectedSunday))
                    .thenReturn(Arrays.asList(zeroHourTask));

            // When
            WeeklyTimesheetData result = weeklyTimesheetService.getWeeklyTimesheet(testDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWeekTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getDailyData().get(DayOfWeek.MONDAY).getDayTotal())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getDailyData().get(DayOfWeek.MONDAY).hasTasks()).isTrue();
        }
    }
}
