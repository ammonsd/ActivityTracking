package com.ammons.taskactivity.service;

import com.ammons.taskactivity.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Local file system implementation of ReceiptStorageService Used for development and testing
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class LocalFileStorageService implements ReceiptStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${storage.local.path}") String storagePath) {
        this.rootLocation = Paths.get(storagePath);
        try {
            Files.createDirectories(this.rootLocation);
            logger.info("Local storage initialized at: {}", this.rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String storeReceipt(MultipartFile file, String username, Long expenseId)
            throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        // Create directory structure: username/YYYY/MM/
        LocalDate now = LocalDate.now();
        String userDir = username;
        String yearDir = String.valueOf(now.getYear());
        String monthDir = String.format("%02d", now.getMonthValue());

        Path targetDir = rootLocation.resolve(userDir).resolve(yearDir).resolve(monthDir);
        Files.createDirectories(targetDir);

        // Generate unique filename: receipt_expenseId_uuid.ext
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = String.format("receipt_%d_%s%s", expenseId, UUID.randomUUID(), extension);

        Path targetFile = targetDir.resolve(filename);

        // Copy file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return relative path: username/YYYY/MM/filename
        Path relativePath = Paths.get(userDir).resolve(yearDir).resolve(monthDir).resolve(filename);
        logger.info("Stored receipt locally: {}", relativePath);

        return relativePath.toString();
    }

    @Override
    public InputStream getReceipt(String receiptPath) throws IOException {
        Path file = rootLocation.resolve(receiptPath);
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Receipt not found: " + receiptPath);
        }
        return Files.newInputStream(file);
    }

    @Override
    public void deleteReceipt(String receiptPath) throws IOException {
        Path file = rootLocation.resolve(receiptPath);
        Files.deleteIfExists(file);
        logger.info("Deleted receipt: {}", receiptPath);
    }

    @Override
    public boolean receiptExists(String receiptPath) {
        Path file = rootLocation.resolve(receiptPath);
        return Files.exists(file);
    }

    @Override
    public String getContentType(String receiptPath) {
        try {
            Path file = rootLocation.resolve(receiptPath);
            return Files.probeContentType(file);
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }
}
