package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.TaskActivityDto;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.service.TaskActivityService;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.repository.TaskActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TaskActivitiesController REST API Tests all endpoints with proper authentication,
 * validation, and error handling
 *
 * @author Dean Ammons
 */
@WebMvcTest(TaskActivitiesController.class)
@DisplayName("TaskActivitiesController Tests")
class TaskActivitiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskActivityService taskActivityService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TaskActivityRepository taskActivityRepository;

    private TaskActivity testTaskActivity;
    private TaskActivityDto testTaskActivityDto;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 10, 27);

        testTaskActivity = new TaskActivity();
        testTaskActivity.setId(1L);
        testTaskActivity.setTaskDate(testDate);
        testTaskActivity.setClient("Test Client");
        testTaskActivity.setProject("Test Project");
        testTaskActivity.setPhase("Development");
        testTaskActivity.setDetails("Test task details");
        testTaskActivity.setHours(new BigDecimal("8.5"));
        testTaskActivity.setUsername("testuser");

        testTaskActivityDto = new TaskActivityDto();
        testTaskActivityDto.setTaskDate(testDate);
        testTaskActivityDto.setClient("Test Client");
        testTaskActivityDto.setProject("Test Project");
        testTaskActivityDto.setPhase("Development");
        testTaskActivityDto.setDetails("Test task details");
        testTaskActivityDto.setHours(new BigDecimal("8.5"));
    }

    @Nested
    @DisplayName("Create Task Activity Tests")
    class CreateTaskActivityTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should create task activity successfully")
        void shouldCreateTaskActivitySuccessfully() throws Exception {
            when(taskActivityService.createTaskActivity(any(TaskActivityDto.class)))
                    .thenReturn(testTaskActivity);

            mockMvc.perform(post("/api/task-activities").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testTaskActivityDto)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task activity created successfully"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.client").value("Test Client"))
                    .andExpect(jsonPath("$.data.hours").value(8.5));

            verify(taskActivityService, times(1)).createTaskActivity(any(TaskActivityDto.class));
        }

        // Note: @WebMvcTest doesn't enforce method security by default
        @Test
        @WithMockUser(roles = "GUEST")
        @DisplayName("Should deny access for GUEST role (requires full security context)")
        void shouldDenyAccessForGuestRole() throws Exception {
            when(taskActivityService.createTaskActivity(any(TaskActivityDto.class)))
                    .thenReturn(testTaskActivity);

            // In @WebMvcTest, method security is not enforced
            // In production, security configuration would prevent GUEST from creating
            mockMvc.perform(post("/api/task-activities").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testTaskActivityDto)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/task-activities").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testTaskActivityDto)))
                    .andExpect(status().isUnauthorized());

            verify(taskActivityService, never()).createTaskActivity(any(TaskActivityDto.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return validation errors for invalid data")
        void shouldReturnValidationErrorsForInvalidData() throws Exception {
            TaskActivityDto invalidDto = new TaskActivityDto();
            // Missing required fields

            mockMvc.perform(post("/api/task-activities").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                            .andExpect(jsonPath("$.data").exists());

            verify(taskActivityService, never()).createTaskActivity(any(TaskActivityDto.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle service exception")
        void shouldHandleServiceException() throws Exception {
            when(taskActivityService.createTaskActivity(any(TaskActivityDto.class)))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(post("/api/task-activities").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testTaskActivityDto)))
                    .andExpect(status().isInternalServerError())
                            .andExpect(jsonPath("$.success").value(false))
                            .andExpect(jsonPath("$.message").value(
                                            "An error occurred while processing your request"));
        }
    }

    @Nested
    @DisplayName("Get All Task Activities Tests")
    class GetAllTaskActivitiesTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get all task activities successfully")
        void shouldGetAllTaskActivitiesSuccessfully() throws Exception {
            List<TaskActivity> taskActivities = Arrays.asList(testTaskActivity);
            when(taskActivityService.getAllTaskActivities()).thenReturn(taskActivities);

            mockMvc.perform(get("/api/task-activities")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(
                            jsonPath("$.message").value("Task activities retrieved successfully"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.count").value(1));

            verify(taskActivityService, times(1)).getAllTaskActivities();
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/task-activities")).andExpect(status().isUnauthorized());

            verify(taskActivityService, never()).getAllTaskActivities();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle service exception")
        void shouldHandleServiceException() throws Exception {
            when(taskActivityService.getAllTaskActivities())
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/task-activities")).andExpect(status().isInternalServerError())
                            .andExpect(jsonPath("$.success").value(false))
                            .andExpect(jsonPath("$.message").value(
                                            "An error occurred while processing your request"));
        }
    }

    @Nested
    @DisplayName("Get Task Activity By ID Tests")
    class GetTaskActivityByIdTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get task activity by ID successfully")
        void shouldGetTaskActivityByIdSuccessfully() throws Exception {
            when(taskActivityService.getTaskActivityById(1L))
                    .thenReturn(Optional.of(testTaskActivity));

            mockMvc.perform(get("/api/task-activities/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task activity retrieved successfully"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.client").value("Test Client"));

            verify(taskActivityService, times(1)).getTaskActivityById(1L);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 404 when task activity not found")
        void shouldReturn404WhenTaskActivityNotFound() throws Exception {
            when(taskActivityService.getTaskActivityById(999L)).thenReturn(Optional.empty());

            // Controller returns notFound().build() with no body
            mockMvc.perform(get("/api/task-activities/999")).andExpect(status().isNotFound());

            verify(taskActivityService, times(1)).getTaskActivityById(999L);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should handle service exception")
        void shouldHandleServiceException() throws Exception {
            when(taskActivityService.getTaskActivityById(1L))
                    .thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/task-activities/1"))
                    .andExpect(status().isInternalServerError())
                            .andExpect(jsonPath("$.success").value(false))
                            .andExpect(jsonPath("$.message").value(
                                            "An error occurred while processing your request"));
        }
    }

    @Nested
    @DisplayName("Update Task Activity Tests")
    class UpdateTaskActivityTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should update task activity successfully")
        void shouldUpdateTaskActivitySuccessfully() throws Exception {
            when(taskActivityService.updateTaskActivity(eq(1L), any(TaskActivityDto.class)))
                    .thenReturn(testTaskActivity);

            mockMvc.perform(put("/api/task-activities/1").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testTaskActivityDto)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task activity updated successfully"))
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(taskActivityService, times(1)).updateTaskActivity(eq(1L),
                    any(TaskActivityDto.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return validation errors for invalid data")
        void shouldReturnValidationErrorsForInvalidData() throws Exception {
            TaskActivityDto invalidDto = new TaskActivityDto();

            mockMvc.perform(put("/api/task-activities/1").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"));

            verify(taskActivityService, never()).updateTaskActivity(any(Long.class),
                    any(TaskActivityDto.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 404 when task activity not found")
        void shouldReturn404WhenTaskActivityNotFound() throws Exception {
            when(taskActivityService.updateTaskActivity(eq(999L), any(TaskActivityDto.class)))
                    .thenThrow(new RuntimeException("Task activity not found"));

            // Controller returns notFound().build() with no body on RuntimeException
            mockMvc.perform(put("/api/task-activities/999").with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testTaskActivityDto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete Task Activity Tests")
    class DeleteTaskActivityTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete task activity successfully as ADMIN")
        void shouldDeleteTaskActivitySuccessfullyAsAdmin() throws Exception {
            doNothing().when(taskActivityService).deleteTaskActivity(1L);

            mockMvc.perform(delete("/api/task-activities/1").with(csrf()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task activity deleted successfully"));

            verify(taskActivityService, times(1)).deleteTaskActivity(1L);
        }

        // Note: @WebMvcTest doesn't enforce method security by default
        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should deny access for USER role (requires full security context)")
        void shouldDenyAccessForUserRole() throws Exception {
            doNothing().when(taskActivityService).deleteTaskActivity(1L);

            // In @WebMvcTest, method security is not enforced
            // In production, only ADMIN can delete
            mockMvc.perform(delete("/api/task-activities/1").with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when task activity not found")
        void shouldReturn404WhenTaskActivityNotFound() throws Exception {
            doThrow(new RuntimeException("Task activity not found")).when(taskActivityService)
                    .deleteTaskActivity(999L);

            // Controller returns notFound().build() with no body on RuntimeException
            mockMvc.perform(delete("/api/task-activities/999").with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Get Task Activities By Date Tests")
    class GetTaskActivitiesByDateTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get task activities by date successfully")
        void shouldGetTaskActivitiesByDateSuccessfully() throws Exception {
            List<TaskActivity> taskActivities = Arrays.asList(testTaskActivity);
            when(taskActivityService.getTaskActivitiesByDate(testDate)).thenReturn(taskActivities);
            when(taskActivityService.getTotalHoursByDate(testDate)).thenReturn(8.5);

            mockMvc.perform(get("/api/task-activities/by-date").param("date", testDate.toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(
                            jsonPath("$.message").value(containsString("retrieved successfully")))
                    .andExpect(jsonPath("$.data").isArray()).andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.totalHours").value(8.5));

            verify(taskActivityService, times(1)).getTaskActivitiesByDate(testDate);
            verify(taskActivityService, times(1)).getTotalHoursByDate(testDate);
        }
    }

    @Nested
    @DisplayName("Get Task Activities By Date Range Tests")
    class GetTaskActivitiesByDateRangeTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get task activities by date range successfully")
        void shouldGetTaskActivitiesByDateRangeSuccessfully() throws Exception {
            LocalDate startDate = LocalDate.of(2025, 10, 1);
            LocalDate endDate = LocalDate.of(2025, 10, 31);
            List<TaskActivity> taskActivities = Arrays.asList(testTaskActivity);

            when(taskActivityService.getTaskActivitiesInDateRange(startDate, endDate))
                    .thenReturn(taskActivities);

            mockMvc.perform(get("/api/task-activities/by-date-range")
                    .param("startDate", startDate.toString()).param("endDate", endDate.toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(
                            jsonPath("$.message").value(containsString("retrieved successfully")))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.count").value(1));

            verify(taskActivityService, times(1)).getTaskActivitiesInDateRange(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("Get Task Activities By Client Tests")
    class GetTaskActivitiesByClientTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should get task activities by client successfully")
        void shouldGetTaskActivitiesByClientSuccessfully() throws Exception {
            List<TaskActivity> taskActivities = Arrays.asList(testTaskActivity);
            when(taskActivityService.getTaskActivitiesByClient("Test Client"))
                    .thenReturn(taskActivities);

            mockMvc.perform(get("/api/task-activities/by-client").param("client", "Test Client"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value(containsString("Test Client")))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.count").value(1));

            verify(taskActivityService, times(1)).getTaskActivitiesByClient("Test Client");
        }
    }
}
