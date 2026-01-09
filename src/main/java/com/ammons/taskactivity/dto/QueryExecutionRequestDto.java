package com.ammons.taskactivity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * QueryExecutionRequestDto
 * 
 * Data transfer object for SQL query execution requests.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
public class QueryExecutionRequestDto {

    @NotBlank(message = "Query cannot be empty")
    private String query;

    public QueryExecutionRequestDto() {}

    public QueryExecutionRequestDto(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
