package com.ammons.taskactivity.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for receipt storage operations. Implementations can use local file system or cloud
 * storage (S3).
 *
 * @author Dean Ammons
 * @version 1.0
 */
public interface ReceiptStorageService {

    /**
     * Store a receipt file and return the storage path/key
     *
     * @param file The uploaded file
     * @param username The username of the expense owner
     * @param expenseId The expense ID
     * @return The storage path/key (relative path for local, S3 key for S3)
     * @throws IOException if storage fails
     */
    String storeReceipt(MultipartFile file, String username, Long expenseId) throws IOException;

    /**
     * Retrieve a receipt file as an InputStream
     *
     * @param receiptPath The storage path/key
     * @return InputStream of the file
     * @throws IOException if retrieval fails
     */
    InputStream getReceipt(String receiptPath) throws IOException;

    /**
     * Delete a receipt file
     *
     * @param receiptPath The storage path/key
     * @throws IOException if deletion fails
     */
    void deleteReceipt(String receiptPath) throws IOException;

    /**
     * Check if a receipt exists
     *
     * @param receiptPath The storage path/key
     * @return true if exists, false otherwise
     */
    boolean receiptExists(String receiptPath);

    /**
     * Get the content type of a receipt
     *
     * @param receiptPath The storage path/key
     * @return Content type (e.g., "image/jpeg")
     */
    String getContentType(String receiptPath);
}
