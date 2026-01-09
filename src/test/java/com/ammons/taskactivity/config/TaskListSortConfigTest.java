package com.ammons.taskactivity.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TaskListSortConfig configuration properties
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TaskListSortConfig Tests")
class TaskListSortConfigTest {

    @Autowired
    private TaskListSortConfig taskListSortConfig;

    @Test
    @DisplayName("Should load configuration properties from application properties")
    void shouldLoadConfigurationProperties() {
        // Verify the configuration is loaded and not null (regardless of specific values)
        assertThat(taskListSortConfig).isNotNull();
        assertThat(taskListSortConfig.getDateDirection()).isNotNull().isNotEmpty();
        assertThat(taskListSortConfig.getClientDirection()).isNotNull().isNotEmpty();
        assertThat(taskListSortConfig.getProjectDirection()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should convert string directions to Sort.Direction enums correctly")
    void shouldConvertStringDirectionsToSortDirection() {
        // Test that any valid configuration converts to proper Sort.Direction enums
        Sort.Direction dateDirection = taskListSortConfig.getDateSortDirection();
        Sort.Direction clientDirection = taskListSortConfig.getClientSortDirection();
        Sort.Direction projectDirection = taskListSortConfig.getProjectSortDirection();

        // Verify all directions are valid Sort.Direction enums
        // Verify all three directions are valid Sort.Direction values
        assertThat(dateDirection).isIn(Sort.Direction.ASC, Sort.Direction.DESC);
        assertThat(clientDirection).isIn(Sort.Direction.ASC, Sort.Direction.DESC);
        assertThat(projectDirection).isIn(Sort.Direction.ASC, Sort.Direction.DESC);
    }

    @Test
    @DisplayName("Should create complete Sort object with configured directions")
    void shouldCreateCompleteSortObject() {
        Sort sort = taskListSortConfig.createSort();

        assertThat(sort).isNotNull();

        // Verify the sort contains the expected fields with valid directions
        String sortString = sort.toString();
        assertThat(sortString).contains("taskDate:").contains("client:").contains("project:")
                .containsPattern("taskDate: (ASC|DESC)").containsPattern("client: (ASC|DESC)")
                .containsPattern("project: (ASC|DESC)");
    }

    @Test
    @DisplayName("Should handle case-insensitive direction values")
    void shouldHandleCaseInsensitiveDirectionValues() {
        // Store original values
        String originalDateDirection = taskListSortConfig.getDateDirection();
        String originalClientDirection = taskListSortConfig.getClientDirection();
        String originalProjectDirection = taskListSortConfig.getProjectDirection();

        try {
            // Set mixed case values
            taskListSortConfig.setDateDirection("desc");
            taskListSortConfig.setClientDirection("Asc");
            taskListSortConfig.setProjectDirection("ASC");

            // Verify conversion still works correctly
            assertThat(taskListSortConfig.getDateSortDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(taskListSortConfig.getClientSortDirection()).isEqualTo(Sort.Direction.ASC);
            assertThat(taskListSortConfig.getProjectSortDirection()).isEqualTo(Sort.Direction.ASC);
        } finally {
            // Restore original values
            taskListSortConfig.setDateDirection(originalDateDirection);
            taskListSortConfig.setClientDirection(originalClientDirection);
            taskListSortConfig.setProjectDirection(originalProjectDirection);
        }
    }

    @Test
    @DisplayName("Should default to DESC for invalid direction values")
    void shouldDefaultToDescForInvalidDirectionValues() {
        // Store original values
        String originalDateDirection = taskListSortConfig.getDateDirection();
        String originalClientDirection = taskListSortConfig.getClientDirection();
        String originalProjectDirection = taskListSortConfig.getProjectDirection();

        try {
            // Set invalid values
            taskListSortConfig.setDateDirection("invalid");
            taskListSortConfig.setClientDirection("");
            taskListSortConfig.setProjectDirection(null);

            // Should default to DESC for non-ASC values
            assertThat(taskListSortConfig.getDateSortDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(taskListSortConfig.getClientSortDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(taskListSortConfig.getProjectSortDirection()).isEqualTo(Sort.Direction.DESC);
        } finally {
            // Restore original values
            taskListSortConfig.setDateDirection(originalDateDirection);
            taskListSortConfig.setClientDirection(originalClientDirection);
            taskListSortConfig.setProjectDirection(originalProjectDirection);
        }
    }
}
