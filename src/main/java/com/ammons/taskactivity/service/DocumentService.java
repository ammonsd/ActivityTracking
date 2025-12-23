package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import jakarta.annotation.PostConstruct;

/**
 * Service for retrieving public documents from S3
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Value("${storage.docs.bucket:taskactivity-docs}")
    private String docsBucketName;

    @Value("${storage.s3.region:us-east-1}")
    private String region;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder().region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create()).build();
        logger.info("DocumentService initialized for bucket: {} in region: {}", docsBucketName,
                region);
    }

    /**
     * Retrieve document from S3
     * 
     * @param documentPath Path to the document in S3 (e.g., "user-guide.html")
     * @return ResponseInputStream containing the document
     * @throws DocumentNotFoundException if document doesn't exist
     * @throws DocumentServiceException if S3 error occurs
     */
    public ResponseInputStream<GetObjectResponse> getDocument(String documentPath)
            throws DocumentNotFoundException, DocumentServiceException {
        try {
            GetObjectRequest getRequest =
                    GetObjectRequest.builder().bucket(docsBucketName).key(documentPath).build();

            logger.info("Retrieving document from S3: s3://{}/{}", docsBucketName, documentPath);
            return s3Client.getObject(getRequest);

        } catch (NoSuchKeyException e) {
            logger.warn("Document not found: {}", documentPath);
            throw new DocumentNotFoundException("Document not found: " + documentPath);
        } catch (S3Exception e) {
            logger.error("S3 error retrieving document {}: {}", documentPath,
                    e.awsErrorDetails().errorMessage(), e);
            throw new DocumentServiceException(
                    "Failed to retrieve document: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Get content type of document
     * 
     * @param documentPath Path to the document in S3
     * @return Content type (e.g., "text/html")
     */
    public String getContentType(String documentPath) {
        try {
            HeadObjectRequest headRequest =
                    HeadObjectRequest.builder().bucket(docsBucketName).key(documentPath).build();

            return s3Client.headObject(headRequest).contentType();

        } catch (S3Exception e) {
            logger.warn("Error getting content type for {}: {}", documentPath, e.getMessage());
            // Return default based on file extension
            if (documentPath.endsWith(".html"))
                return "text/html; charset=UTF-8";
            if (documentPath.endsWith(".pdf"))
                return "application/pdf";
            if (documentPath.endsWith(".docx"))
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if (documentPath.endsWith(".doc"))
                return "application/msword";
            if (documentPath.endsWith(".xlsx"))
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            if (documentPath.endsWith(".xls"))
                return "application/vnd.ms-excel";
            if (documentPath.endsWith(".csv"))
                return "text/csv; charset=UTF-8";
            if (documentPath.endsWith(".ods"))
                return "application/vnd.oasis.opendocument.spreadsheet";
            if (documentPath.endsWith(".txt"))
                return "text/plain; charset=UTF-8";
            if (documentPath.endsWith(".css"))
                return "text/css; charset=UTF-8";
            if (documentPath.endsWith(".js"))
                return "application/javascript; charset=UTF-8";
            return "application/octet-stream";
        }
    }

    /**
     * Get content length of document
     * 
     * @param documentPath Path to the document in S3
     * @return Content length in bytes, or -1 if unknown
     */
    public long getContentLength(String documentPath) {
        try {
            HeadObjectRequest headRequest =
                    HeadObjectRequest.builder().bucket(docsBucketName).key(documentPath).build();

            return s3Client.headObject(headRequest).contentLength();

        } catch (S3Exception e) {
            logger.warn("Error getting content length for {}: {}", documentPath, e.getMessage());
            return -1;
        }
    }

    /**
     * Check if document exists
     * 
     * @param documentPath Path to check
     * @return true if document exists
     */
    public boolean documentExists(String documentPath) {
        try {
            HeadObjectRequest headRequest =
                    HeadObjectRequest.builder().bucket(docsBucketName).key(documentPath).build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Error checking document existence {}: {}", documentPath, e.getMessage());
            return false;
        }
    }

    public static class DocumentNotFoundException extends Exception {
        public DocumentNotFoundException(String message) {
            super(message);
        }
    }

    public static class DocumentServiceException extends Exception {
        public DocumentServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
