package com.ammons.taskactivity.service;

import com.ammons.taskactivity.dto.TaskActivityDto;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.repository.TaskActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskActivityService Tests")
class TaskActivityServiceTest {

    @Mock
    private TaskActivityRepository taskActivityRepository;

    @InjectMocks
    private TaskActivityService taskActivityService;

    private TaskActivity testEntity;
    private TaskActivityDto testDto;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 9, 17);

        testEntity = new TaskActivity();
        testEntity.setId(1L);
        testEntity.setTaskDate(testDate);
        testEntity.setClient("Test Client");
        testEntity.setProject("Test Project");
        testEntity.setPhase("Development");
        testEntity.setHours(new BigDecimal("8.00"));
        testEntity.setDetails("Test task details");

        testDto = new TaskActivityDto();
        testDto.setTaskDate(testDate);
        testDto.setClient("Test Client");
        testDto.setProject("Test Project");
        testDto.setPhase("Development");
        testDto.setHours(new BigDecimal("8.00"));
        testDto.setDetails("Test task details");
    }

    @Nested
    @DisplayName("Create Task Activity Tests")
    class CreateTaskActivityTests {

        @Test
        @DisplayName("Should create task activity successfully")
        void shouldCreateTaskActivitySuccessfully() {
            // Given
            when(taskActivityRepository.save(any(TaskActivity.class))).thenReturn(testEntity);

            // When
            TaskActivity result = taskActivityService.createTaskActivity(testDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTaskDate()).isEqualTo(testDate);
            assertThat(result.getClient()).isEqualTo("Test Client");
            assertThat(result.getProject()).isEqualTo("Test Project");
            assertThat(result.getPhase()).isEqualTo("Development");
            assertThat(result.getHours()).isEqualByComparingTo(new BigDecimal("8.00"));
            assertThat(result.getDetails()).isEqualTo("Test task details");

            verify(taskActivityRepository).save(any(TaskActivity.class));
        }

        @Test
        @DisplayName("Should handle null DTO gracefully")
        void shouldHandleNullDto() {
            // Given/When/Then
            assertThatThrownBy(() -> taskActivityService.createTaskActivity(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Read Task Activity Tests")
    class ReadTaskActivityTests {

        @Test
        @DisplayName("Should get all task activities")
        void shouldGetAllTaskActivities() {
            // Given
            List<TaskActivity> expectedActivities = Arrays.asList(testEntity);
            when(taskActivityRepository.findAll()).thenReturn(expectedActivities);

            // When
            List<TaskActivity> result = taskActivityService.getAllTaskActivities();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findAll();
        }

        @Test
        @DisplayName("Should get task activity by ID")
        void shouldGetTaskActivityById() {
            // Given
            when(taskActivityRepository.findById(1L)).thenReturn(Optional.of(testEntity));

            // When
            Optional<TaskActivity> result = taskActivityService.getTaskActivityById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testEntity);
            verify(taskActivityRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when task activity not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(taskActivityRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<TaskActivity> result = taskActivityService.getTaskActivityById(999L);

            // Then
            assertThat(result).isEmpty();
            verify(taskActivityRepository).findById(999L);
        }

        @Test
        @DisplayName("Should get task activities by date")
        void shouldGetTaskActivitiesByDate() {
            // Given
            List<TaskActivity> expectedActivities = Arrays.asList(testEntity);
            when(taskActivityRepository.findTaskActivitiesByDate(testDate))
                    .thenReturn(expectedActivities);

            // When
            List<TaskActivity> result = taskActivityService.getTaskActivitiesByDate(testDate);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findTaskActivitiesByDate(testDate);
        }

        @Test
        @DisplayName("Should get task activities in date range")
        void shouldGetTaskActivitiesInDateRange() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 9, 1);
            LocalDate endDate = LocalDate.of(2025, 9, 30);
            List<TaskActivity> expectedActivities = Arrays.asList(testEntity);
            when(taskActivityRepository.findTaskActivitiesInDateRange(startDate, endDate))
                    .thenReturn(expectedActivities);

            // When
            List<TaskActivity> result =
                    taskActivityService.getTaskActivitiesInDateRange(startDate, endDate);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findTaskActivitiesInDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("Should get task activities by client")
        void shouldGetTaskActivitiesByClient() {
            // Given
            String client = "Test Client";
            List<TaskActivity> expectedActivities = Arrays.asList(testEntity);
            when(taskActivityRepository.findByClientIgnoreCase(client))
                    .thenReturn(expectedActivities);

            // When
            List<TaskActivity> result = taskActivityService.getTaskActivitiesByClient(client);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findByClientIgnoreCase(client);
        }

        @Test
        @DisplayName("Should get task activities by project")
        void shouldGetTaskActivitiesByProject() {
            // Given
            String project = "Test Project";
            List<TaskActivity> expectedActivities = Arrays.asList(testEntity);
            when(taskActivityRepository.findByProjectIgnoreCase(project))
                    .thenReturn(expectedActivities);

            // When
            List<TaskActivity> result = taskActivityService.getTaskActivitiesByProject(project);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findByProjectIgnoreCase(project);
        }

        @Test
        @DisplayName("Should get total hours by date")
        void shouldGetTotalHoursByDate() {
            // Given
            Double expectedTotal = 16.5;
            when(taskActivityRepository.getTotalHoursByDate(testDate)).thenReturn(expectedTotal);

            // When
            Double result = taskActivityService.getTotalHoursByDate(testDate);

            // Then
            assertThat(result).isEqualTo(expectedTotal);
            verify(taskActivityRepository).getTotalHoursByDate(testDate);
        }

        @Test
        @DisplayName("Should return 0.0 when total hours is null")
        void shouldReturnZeroWhenTotalHoursIsNull() {
            // Given
            when(taskActivityRepository.getTotalHoursByDate(testDate)).thenReturn(null);

            // When
            Double result = taskActivityService.getTotalHoursByDate(testDate);

            // Then
            assertThat(result).isEqualTo(0.0);
            verify(taskActivityRepository).getTotalHoursByDate(testDate);
        }
    }

    @Nested
    @DisplayName("Update Task Activity Tests")
    class UpdateTaskActivityTests {

        @Test
        @DisplayName("Should update task activity successfully")
        void shouldUpdateTaskActivitySuccessfully() {
            // Given
            TaskActivityDto updateDto = new TaskActivityDto();
            updateDto.setTaskDate(LocalDate.of(2025, 9, 18));
            updateDto.setClient("Updated Client");
            updateDto.setProject("Updated Project");
            updateDto.setPhase("Testing");
            updateDto.setHours(new BigDecimal("4.00"));
            updateDto.setDetails("Updated details");

            TaskActivity updatedEntity = new TaskActivity();
            updatedEntity.setId(1L);
            updatedEntity.setTaskDate(updateDto.getTaskDate());
            updatedEntity.setClient(updateDto.getClient());
            updatedEntity.setProject(updateDto.getProject());
            updatedEntity.setPhase(updateDto.getPhase());
            updatedEntity.setHours(updateDto.getHours());
            updatedEntity.setDetails(updateDto.getDetails());

            when(taskActivityRepository.findById(1L)).thenReturn(Optional.of(testEntity));
            when(taskActivityRepository.save(any(TaskActivity.class))).thenReturn(updatedEntity);

            // When
            TaskActivity result = taskActivityService.updateTaskActivity(1L, updateDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTaskDate()).isEqualTo(updateDto.getTaskDate());
            assertThat(result.getClient()).isEqualTo(updateDto.getClient());
            assertThat(result.getProject()).isEqualTo(updateDto.getProject());
            assertThat(result.getPhase()).isEqualTo(updateDto.getPhase());
            assertThat(result.getHours()).isEqualByComparingTo(updateDto.getHours());
            assertThat(result.getDetails()).isEqualTo(updateDto.getDetails());

            verify(taskActivityRepository).findById(1L);
            verify(taskActivityRepository).save(any(TaskActivity.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent task activity")
        void shouldThrowExceptionWhenUpdatingNonExistentTaskActivity() {
            // Given
            when(taskActivityRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> taskActivityService.updateTaskActivity(999L, testDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Task activity not found with ID: 999");

            verify(taskActivityRepository).findById(999L);
            verify(taskActivityRepository, never()).save(any(TaskActivity.class));
        }
    }

    @Nested
    @DisplayName("Delete Task Activity Tests")
    class DeleteTaskActivityTests {

        @Test
        @DisplayName("Should delete task activity successfully")
        void shouldDeleteTaskActivitySuccessfully() {
            // Given
            when(taskActivityRepository.existsById(1L)).thenReturn(true);

            // When
            taskActivityService.deleteTaskActivity(1L);

            // Then
            verify(taskActivityRepository).existsById(1L);
            verify(taskActivityRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent task activity")
        void shouldThrowExceptionWhenDeletingNonExistentTaskActivity() {
            // Given
            when(taskActivityRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> taskActivityService.deleteTaskActivity(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Task activity not found with ID: 999");

            verify(taskActivityRepository).existsById(999L);
            verify(taskActivityRepository, never()).deleteById(999L);
        }
    }

    @Nested
    @DisplayName("Pageable Tests")
    class PageableTests {

        @Test
        @DisplayName("Should get all task activities with pagination")
        void shouldGetAllTaskActivitiesWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<TaskActivity> expectedPage = new PageImpl<>(Arrays.asList(testEntity));
            when(taskActivityRepository.findAll(pageable)).thenReturn(expectedPage);

            // When
            Page<TaskActivity> result = taskActivityService.getAllTaskActivities(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should get task activities by date with pagination")
        void shouldGetTaskActivitiesByDateWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<TaskActivity> expectedPage = new PageImpl<>(Arrays.asList(testEntity));
            when(taskActivityRepository.findTaskActivitiesByDate(testDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<TaskActivity> result =
                    taskActivityService.getTaskActivitiesByDate(testDate, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findTaskActivitiesByDate(testDate, pageable);
        }

        @Test
        @DisplayName("Should get task activities in date range with pagination")
        void shouldGetTaskActivitiesInDateRangeWithPagination() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 9, 1);
            LocalDate endDate = LocalDate.of(2025, 9, 30);
            Pageable pageable = PageRequest.of(0, 10);
            Page<TaskActivity> expectedPage = new PageImpl<>(Arrays.asList(testEntity));
            when(taskActivityRepository.findTaskActivitiesInDateRange(startDate, endDate, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<TaskActivity> result =
                    taskActivityService.getTaskActivitiesInDateRange(startDate, endDate, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findTaskActivitiesInDateRange(startDate, endDate,
                    pageable);
        }

        @Test
        @DisplayName("Should get task activities by client with pagination")
        void shouldGetTaskActivitiesByClientWithPagination() {
            // Given
            String client = "Test Client";
            Pageable pageable = PageRequest.of(0, 10);
            Page<TaskActivity> expectedPage = new PageImpl<>(Arrays.asList(testEntity));
            when(taskActivityRepository.findByClientIgnoreCase(client, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<TaskActivity> result =
                    taskActivityService.getTaskActivitiesByClient(client, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findByClientIgnoreCase(client, pageable);
        }

        @Test
        @DisplayName("Should get task activities by project with pagination")
        void shouldGetTaskActivitiesByProjectWithPagination() {
            // Given
            String project = "Test Project";
            Pageable pageable = PageRequest.of(0, 10);
            Page<TaskActivity> expectedPage = new PageImpl<>(Arrays.asList(testEntity));
            when(taskActivityRepository.findByProjectIgnoreCase(project, pageable))
                    .thenReturn(expectedPage);

            // When
            Page<TaskActivity> result =
                    taskActivityService.getTaskActivitiesByProject(project, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(testEntity);
            verify(taskActivityRepository).findByProjectIgnoreCase(project, pageable);
        }
    }
}
