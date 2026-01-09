package com.ammons.taskactivity.exception;

/**
 * Custom exception for file storage operations
 *
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
