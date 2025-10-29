package com.ammons.taskactivity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Generic API Response wrapper for consistent REST API responses. Provides a standardized structure
 * for all API endpoints.
 *
 * @param <T> The type of data contained in the response
 * @author Dean Ammons
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private Integer count;
    private Double totalHours;
    private LocalDateTime timestamp;

    /**
     * Private constructor - use static factory methods
     */
    private ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        return response;
    }

    /**
     * Create a successful response without data
     */
    public static <T> ApiResponse<T> success(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        return response;
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        return response;
    }

    /**
     * Create an error response with data (e.g., validation errors)
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.data = data;
        return response;
    }

    // Fluent setters for optional fields
    public ApiResponse<T> withCount(Integer count) {
        this.count = count;
        return this;
    }

    public ApiResponse<T> withTotalHours(Double totalHours) {
        this.totalHours = totalHours;
        return this;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Integer getCount() {
        return count;
    }

    public Double getTotalHours() {
        return totalHours;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Setters for Jackson deserialization
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public void setTotalHours(Double totalHours) {
        this.totalHours = totalHours;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
