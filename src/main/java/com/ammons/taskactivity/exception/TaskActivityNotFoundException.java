package com.ammons.taskactivity.exception;

/**
 * Exception thrown when a TaskActivity is not found.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
public class TaskActivityNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TaskActivityNotFoundException(String message) {
        super(message);
    }

    public TaskActivityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskActivityNotFoundException(Long id) {
        super("Task activity not found with ID: " + id);
    }
}
