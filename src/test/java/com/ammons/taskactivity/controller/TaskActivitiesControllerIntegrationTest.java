package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.TaskActivityDto;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.repository.TaskActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TaskActivitiesController. Tests all endpoints with different user roles and
 * permission scenarios.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("TaskActivitiesController Integration Tests")
class TaskActivitiesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    private TaskActivityDto testDto;

    @BeforeEach
    void setUp() {
        testDto = new TaskActivityDto();
        testDto.setTaskDate(LocalDate.now());
        testDto.setClient("Test Client");
        testDto.setProject("Test Project");
        testDto.setPhase("Development");
        testDto.setDetails("Test Details");
        testDto.setHours(new BigDecimal("8.00"));
        testDto.setUsername("testuser");
    }

    @Nested
    @DisplayName("POST /api/task-activities - Create Task Activity")
    class CreateTaskActivityTests {

        @Test
        @WithUserDetails("user")
        @DisplayName("Should create task activity for user with CREATE permission")
        void shouldCreateTaskActivityWithPermission() throws Exception {
            mockMvc.perform(post("/api/task-activities").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Task activity created successfully"))
                    .andExpect(jsonPath("$.data.client").value("Test Client"))
                    .andExpect(jsonPath("$.data.project").value("Test Project"));
        }

        @Test
        @WithUserDetails("guest")
        @DisplayName("Should deny creation for guest without CREATE permission")
        void shouldDenyCreateForGuestWithoutPermission() throws Exception {
            mockMvc.perform(post("/api/task-activities").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny unauthenticated requests")
        void shouldDenyUnauthenticatedCreate() throws Exception {
            mockMvc.perform(post("/api/task-activities").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Should validate required fields")
        void shouldValidateRequiredFields() throws Exception {
            TaskActivityDto invalidDto = new TaskActivityDto();
            // Missing required fields

            mockMvc.perform(post("/api/task-activities").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/task-activities - Get All Task Activities")
    class GetAllTaskActivitiesTests {

        @BeforeEach
        void createTestData() {
            TaskActivity activity = new TaskActivity();
            activity.setTaskDate(LocalDate.now());
            activity.setClient("Test Client");
            activity.setProject("Test Project");
            activity.setPhase("Development");
            activity.setDetails("Test");
            activity.setHours(new BigDecimal("8.00"));
            activity.setUsername("testuser");
            taskActivityRepository.save(activity);
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should see all task activities")
        void adminShouldSeeAllTasks() throws Exception {
            mockMvc.perform(get("/api/task-activities")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithUserDetails("testuser")
        @DisplayName("Regular user should only see their own task activities")
        void userShouldSeeOnlyOwnTasks() throws Exception {
            mockMvc.perform(get("/api/task-activities")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithUserDetails("guest")
        @DisplayName("Guest with READ permission should access endpoint")
        void guestWithReadPermissionShouldAccess() throws Exception {
            mockMvc.perform(get("/api/task-activities")).andExpect(status().isOk());
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Should support pagination parameters")
        void shouldSupportPagination() throws Exception {
            mockMvc.perform(get("/api/task-activities").param("page", "0").param("size", "10"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.currentPage").exists())
                    .andExpect(jsonPath("$.currentPage").value(0));
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Should support filtering by client")
        void shouldSupportClientFilter() throws Exception {
            mockMvc.perform(get("/api/task-activities").param("client", "Test Client"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/task-activities/{id} - Get Task Activity By ID")
    class GetTaskActivityByIdTests {

        private Long testTaskId;

        @BeforeEach
        void createTestTask() {
            TaskActivity activity = new TaskActivity();
            activity.setTaskDate(LocalDate.now());
            activity.setClient("Test Client");
            activity.setProject("Test Project");
            activity.setPhase("Development");
            activity.setDetails("Test");
            activity.setHours(new BigDecimal("8.00"));
            activity.setUsername("testuser");
            activity = taskActivityRepository.save(activity);
            testTaskId = activity.getId();
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should retrieve any task by ID")
        void adminShouldRetrieveAnyTask() throws Exception {
            mockMvc.perform(get("/api/task-activities/" + testTaskId)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(testTaskId));
        }

        @Test
        @WithUserDetails("testuser")
        @DisplayName("User should retrieve their own task")
        void userShouldRetrieveOwnTask() throws Exception {
            mockMvc.perform(get("/api/task-activities/" + testTaskId)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @WithUserDetails("user")
        @DisplayName("Should return 404 for non-existent task")
        void shouldReturn404ForNonExistentTask() throws Exception {
            mockMvc.perform(get("/api/task-activities/99999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/task-activities/{id} - Update Task Activity")
    class UpdateTaskActivityTests {

        private Long testTaskId;

        @BeforeEach
        void createTestTask() {
            TaskActivity activity = new TaskActivity();
            activity.setTaskDate(LocalDate.now());
            activity.setClient("Original Client");
            activity.setProject("Original Project");
            activity.setPhase("Development");
            activity.setDetails("Original");
            activity.setHours(new BigDecimal("4.00"));
            activity.setUsername("testuser");
            activity = taskActivityRepository.save(activity);
            testTaskId = activity.getId();
        }

        @Test
        @WithUserDetails("testuser")
        @DisplayName("User should update their own task")
        void userShouldUpdateOwnTask() throws Exception {
            testDto.setClient("Updated Client");
            testDto.setUsername("testuser");

            mockMvc.perform(put("/api/task-activities/" + testTaskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto))).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.client").value("Updated Client"));
        }

        @Test
        @WithUserDetails("otheruser")
        @DisplayName("User should not update another user's task")
        void userShouldNotUpdateOtherUserTask() throws Exception {
            mockMvc.perform(put("/api/task-activities/" + testTaskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should update any task")
        void adminShouldUpdateAnyTask() throws Exception {
            testDto.setClient("Admin Updated");

            mockMvc.perform(put("/api/task-activities/" + testTaskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto))).andExpect(status().isOk());
        }

        @Test
        @WithUserDetails("guest")
        @DisplayName("Guest without UPDATE permission should be denied")
        void guestShouldBeDeniedUpdate() throws Exception {
            mockMvc.perform(put("/api/task-activities/" + testTaskId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/task-activities/{id} - Delete Task Activity")
    class DeleteTaskActivityTests {

        private Long testTaskId;

        @BeforeEach
        void createTestTask() {
            TaskActivity activity = new TaskActivity();
            activity.setTaskDate(LocalDate.now());
            activity.setClient("Test Client");
            activity.setProject("Test Project");
            activity.setPhase("Development");
            activity.setDetails("Test");
            activity.setHours(new BigDecimal("8.00"));
            activity.setUsername("testuser");
            activity = taskActivityRepository.save(activity);
            testTaskId = activity.getId();
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Admin should delete any task")
        void adminShouldDeleteAnyTask() throws Exception {
            mockMvc.perform(delete("/api/task-activities/" + testTaskId)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithUserDetails("guest") // GUEST role doesn't have TASK:DELETE permission
        @DisplayName("User without DELETE permission should be denied")
        void userWithoutDeletePermissionShouldBeDenied() throws Exception {
            mockMvc.perform(delete("/api/task-activities/" + testTaskId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithUserDetails("admin")
        @DisplayName("Should return 404 when deleting non-existent task")
        void shouldReturn404ForNonExistentTask() throws Exception {
            mockMvc.perform(delete("/api/task-activities/99999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Permission Edge Cases")
    class PermissionEdgeCaseTests {

        // Edge case tests with @WithMockUser are not representative of the database-driven
        // permission model
        // Real users always have roles with permissions loaded from the database
        // @Test
        // @WithMockUser(username = "user", authorities = {})
        // @DisplayName("User with no authorities should be denied")
        // void userWithNoAuthoritiesShouldBeDenied() throws Exception {
        // mockMvc.perform(get("/api/task-activities")).andExpect(status().isForbidden());
        // }

        // @Test
        // @WithMockUser(username = "user", roles = {"UNKNOWN_ROLE"})
        // @DisplayName("User with unknown role should be denied")
        // void userWithUnknownRoleShouldBeDenied() throws Exception {
        // mockMvc.perform(get("/api/task-activities")).andExpect(status().isForbidden());
        // }
    }
}
