package com.ammons.taskactivity.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * File Type Validator using magic numbers (file signatures).
 * 
 * SECURITY: This validator checks the actual file content (magic numbers) to verify file type,
 * preventing attacks where malicious files are uploaded with fake extensions or content-types.
 * 
 * This addresses Issue #10 (Magic Number Validation) from security audit.
 * 
 * Supported file types: - JPEG/JPG: FF D8 FF - PNG: 89 50 4E 47 0D 0A 1A 0A - PDF: 25 50 44 46
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Component
public class FileTypeValidator {

    private static final Logger logger = LoggerFactory.getLogger(FileTypeValidator.class);

    // Magic numbers for supported file types
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] PDF_SIGNATURE = {0x25, 0x50, 0x44, 0x46}; // %PDF

    // Map of file signatures to MIME types
    private static final Map<String, byte[]> FILE_SIGNATURES = new HashMap<>();
    static {
        FILE_SIGNATURES.put("image/jpeg", JPEG_SIGNATURE);
        FILE_SIGNATURES.put("image/jpg", JPEG_SIGNATURE); // Same as JPEG
        FILE_SIGNATURES.put("image/png", PNG_SIGNATURE);
        FILE_SIGNATURES.put("application/pdf", PDF_SIGNATURE);
    }

    /**
     * Validate file type by checking magic numbers.
     * 
     * @param file MultipartFile to validate
     * @param expectedContentType Expected Content-Type (from client)
     * @return ValidationResult containing success status and error message if any
     */
    public ValidationResult validateFileType(MultipartFile file, String expectedContentType) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("File is empty or null");
        }

        if (expectedContentType == null || expectedContentType.trim().isEmpty()) {
            return ValidationResult.error("Content-Type header is missing");
        }

        // Normalize content type (handle variations like "image/jpg" vs "image/jpeg")
        String normalizedType = normalizeContentType(expectedContentType);

        // Check if content type is supported
        if (!FILE_SIGNATURES.containsKey(normalizedType)) {
            logger.warn("Unsupported file type: {}", expectedContentType);
            return ValidationResult
                    .error(String.format("Unsupported file type: %s. Allowed types: JPEG, PNG, PDF",
                            expectedContentType));
        }

        // Read file signature (magic numbers)
        byte[] expectedSignature = FILE_SIGNATURES.get(normalizedType);
        byte[] actualSignature = new byte[expectedSignature.length];

        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(actualSignature);
            if (bytesRead < expectedSignature.length) {
                logger.warn("File too small to determine type: {} bytes", bytesRead);
                return ValidationResult.error("File is too small or corrupted");
            }

            // Compare signatures
            if (!Arrays.equals(actualSignature, expectedSignature)) {
                logger.warn(
                        "Magic number mismatch for file '{}'. Expected type: {}, Actual signature: {}",
                        file.getOriginalFilename(), normalizedType, bytesToHex(actualSignature));
                return ValidationResult.error(String.format(
                        "File content does not match declared type (%s). "
                                + "This may indicate a spoofed or corrupted file.",
                        expectedContentType));
            }

            logger.debug("File type validated successfully: {} - {}", file.getOriginalFilename(),
                    normalizedType);
            return ValidationResult.success();

        } catch (IOException e) {
            logger.error("Failed to read file for validation: {}", e.getMessage(), e);
            return ValidationResult.error("Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Normalize content type to handle variations.
     * 
     * @param contentType Original content type
     * @return Normalized content type
     */
    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        // Remove charset and other parameters
        String normalized = contentType.toLowerCase().split(";")[0].trim();

        // Handle image/jpg (should be image/jpeg)
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }

        return normalized;
    }

    /**
     * Convert byte array to hex string for logging.
     * 
     * @param bytes Byte array
     * @return Hex string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Validation result class.
     */
    public static class ValidationResult {
        private final boolean success;
        private final String errorMessage;

        private ValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
