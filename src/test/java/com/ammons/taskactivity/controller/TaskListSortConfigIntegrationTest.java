package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.config.TaskListSortConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify TaskListSortConfig is properly integrated in the controller
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("TaskListSortConfig Integration Test")
class TaskListSortConfigIntegrationTest {

    @Autowired
    private TaskActivityWebController taskActivityWebController;

    @Autowired
    private TaskListSortConfig taskListSortConfig;

    @Test
    @DisplayName("Should inject TaskListSortConfig into controller")
    void shouldInjectTaskListSortConfigIntoController() {
        assertThat(taskActivityWebController).isNotNull();
        assertThat(taskListSortConfig).isNotNull();
    }

    @Test
    @DisplayName("Should have configured sort order accessible")
    void shouldHaveConfiguredSortOrderAccessible() {
        Sort sort = taskListSortConfig.createSort();
        assertThat(sort).isNotNull();

        // Verify the current configuration creates a valid sort order
        String sortString = sort.toString();
        assertThat(sortString).matches(".*taskDate: (ASC|DESC).*").contains("client: ASC")
                .contains("project: ASC");
    }

    @Test
    @DisplayName("Should create sort with configurable direction")
    void shouldCreateSortWithConfigurableDirection() {
        // Test changing the date direction to ASC
        String originalDirection = taskListSortConfig.getDateDirection();
        taskListSortConfig.setDateDirection("ASC");

        Sort sort = taskListSortConfig.createSort();
        String sortString = sort.toString();

        assertThat(sortString).contains("taskDate: ASC").contains("client: ASC")
                .contains("project: ASC");

        // Reset to original for other tests
        taskListSortConfig.setDateDirection(originalDirection);
    }
}
