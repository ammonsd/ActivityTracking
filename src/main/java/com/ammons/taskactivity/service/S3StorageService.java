package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * AWS S3 implementation of ReceiptStorageService Used in production - leverages ECS task IAM role
 * for authentication
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class S3StorageService implements ReceiptStorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageService(@Value("${storage.s3.bucket}") String bucketName,
            @Value("${storage.s3.region}") String region) {
        this.bucketName = bucketName;
        // S3Client with explicit credentials provider - uses ECS task IAM role in production
        this.s3Client = S3Client.builder().region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create()).build();
        logger.info("S3 storage initialized for bucket: {} in region: {}", bucketName, region);
    }

    @Override
    public String storeReceipt(MultipartFile file, String username, Long expenseId)
            throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Cannot store empty file");
        }

        // Create S3 key structure: username/YYYY/MM/filename
        LocalDate now = LocalDate.now();
        String userDir = username;
        String yearDir = String.valueOf(now.getYear());
        String monthDir = String.format("%02d", now.getMonthValue());

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String filename = String.format("receipt_%d_%s%s", expenseId, UUID.randomUUID(), extension);

        String s3Key = userDir + "/" + yearDir + "/" + monthDir + "/" + filename;

        // Upload to S3
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder().bucket(bucketName).key(s3Key)
                    .contentType(file.getContentType()).contentLength(file.getSize()).build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            logger.info("Stored receipt in S3: s3://{}/{}", bucketName, s3Key);
            return s3Key;

        } catch (S3Exception e) {
            logger.error("Failed to store receipt in S3 bucket {}: {}", bucketName,
                    e.awsErrorDetails().errorMessage(), e);
            throw new IOException(
                    "Failed to store receipt in S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public InputStream getReceipt(String receiptPath) throws IOException {
        try {
            GetObjectRequest getRequest =
                    GetObjectRequest.builder().bucket(bucketName).key(receiptPath).build();

            return s3Client.getObject(getRequest);

        } catch (NoSuchKeyException e) {
            throw new IOException("Receipt not found: " + receiptPath, e);
        } catch (S3Exception e) {
            logger.error("Failed to retrieve receipt from S3 bucket {}, key {}: {}", bucketName,
                    receiptPath, e.awsErrorDetails().errorMessage(), e);
            throw new IOException(
                    "Failed to retrieve receipt from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void deleteReceipt(String receiptPath) throws IOException {
        try {
            DeleteObjectRequest deleteRequest =
                    DeleteObjectRequest.builder().bucket(bucketName).key(receiptPath).build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Deleted receipt from S3: {}", receiptPath);

        } catch (S3Exception e) {
            logger.error("Failed to delete receipt from S3 bucket {}, key {}: {}", bucketName,
                    receiptPath, e.awsErrorDetails().errorMessage(), e);
            throw new IOException(
                    "Failed to delete receipt from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public boolean receiptExists(String receiptPath) {
        try {
            HeadObjectRequest headRequest =
                    HeadObjectRequest.builder().bucket(bucketName).key(receiptPath).build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Error checking receipt existence in S3 bucket {}, key {}: {}", bucketName,
                    receiptPath, e.awsErrorDetails().errorMessage(), e);
            return false;
        }
    }

    @Override
    public String getContentType(String receiptPath) {
        try {
            HeadObjectRequest headRequest =
                    HeadObjectRequest.builder().bucket(bucketName).key(receiptPath).build();

            HeadObjectResponse response = s3Client.headObject(headRequest);
            return response.contentType();

        } catch (S3Exception e) {
            logger.error("Error getting content type from S3 bucket {}, key {}: {}", bucketName,
                    receiptPath, e.awsErrorDetails().errorMessage(), e);
            return "application/octet-stream";
        }
    }
}
