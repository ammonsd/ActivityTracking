package com.ammons.taskactivity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Configuration class for Task Activity list sorting options.
 * 
 * This class provides configurable sorting behavior for the task activity list, allowing
 * administrators to control the default sort order through application properties.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "app.task-activity.list.sort")
public class TaskListSortConfig {

    /**
     * Sort direction for task date column. Valid values: "ASC" or "DESC" Default: "DESC" (newest
     * first)
     */
    private String dateDirection = "DESC";

    /**
     * Sort direction for client column. Valid values: "ASC" or "DESC" Default: "ASC" (alphabetical)
     */
    private String clientDirection = "ASC";

    /**
     * Sort direction for project column. Valid values: "ASC" or "DESC" Default: "ASC"
     * (alphabetical)
     */
    private String projectDirection = "ASC";

    // Getters and Setters
    public String getDateDirection() {
        return dateDirection;
    }

    public void setDateDirection(String dateDirection) {
        this.dateDirection = dateDirection;
    }

    public String getClientDirection() {
        return clientDirection;
    }

    public void setClientDirection(String clientDirection) {
        this.clientDirection = clientDirection;
    }

    public String getProjectDirection() {
        return projectDirection;
    }

    public void setProjectDirection(String projectDirection) {
        this.projectDirection = projectDirection;
    }

    // Utility methods to convert string values to Sort.Direction

    /**
     * Get the sort direction for task date as Spring Data Sort.Direction enum
     * 
     * @return Sort.Direction.ASC or Sort.Direction.DESC
     */
    public Sort.Direction getDateSortDirection() {
        return "ASC".equalsIgnoreCase(dateDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * Get the sort direction for client as Spring Data Sort.Direction enum
     * 
     * @return Sort.Direction.ASC or Sort.Direction.DESC
     */
    public Sort.Direction getClientSortDirection() {
        return "ASC".equalsIgnoreCase(clientDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * Get the sort direction for project as Spring Data Sort.Direction enum
     * 
     * @return Sort.Direction.ASC or Sort.Direction.DESC
     */
    public Sort.Direction getProjectSortDirection() {
        return "ASC".equalsIgnoreCase(projectDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /**
     * Create a complete Sort object based on the configured directions
     * 
     * @return Sort object with configured directions for date, client, and project
     */
    public Sort createSort() {
        return Sort.by(getDateSortDirection(), "taskDate")
                .and(Sort.by(getClientSortDirection(), "client"))
                .and(Sort.by(getProjectSortDirection(), "project"));
    }
}
