package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Controller for serving public documentation from S3 Allows access to docs via
 * https://taskactivitytracker.com/docs/filename.html
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/docs")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentService documentService;

    /**
     * Handle root /docs path - defaults to error.html
     * 
     * @return Document content for error.html
     */
    @GetMapping({"", "/"})
    public ResponseEntity<?> getDefaultDocument() {
        return getDocument("error.html");
    }

    /**
     * Serve document from S3
     * 
     * @param filename Document filename (e.g., "user-guide.html")
     * @return Document content with appropriate content type
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<?> getDocument(@PathVariable String filename) {
        try {
            // Validate filename - prevent directory traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                logger.warn("Invalid filename requested: {}", filename);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid filename");
            }

            // Get document from S3
            ResponseInputStream<GetObjectResponse> documentStream =
                    documentService.getDocument(filename);

            // Check if content is gzip-compressed
            String contentEncoding = documentStream.response().contentEncoding();
            boolean isGzipped = "gzip".equalsIgnoreCase(contentEncoding);

            // Read entire content into byte array to properly handle encoding
            byte[] documentBytes;
            try {
                byte[] rawBytes = documentStream.readAllBytes();

                // Decompress if gzipped
                if (isGzipped) {
                    logger.info("Decompressing gzipped document: {}", filename);
                    try (GZIPInputStream gzipStream =
                            new GZIPInputStream(new ByteArrayInputStream(rawBytes))) {
                        documentBytes = gzipStream.readAllBytes();
                    }
                } else {
                    documentBytes = rawBytes;
                }
            } catch (IOException e) {
                logger.error("Error reading document stream for {}: {}", filename, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN).body("Error reading document");
            } finally {
                try {
                    documentStream.close();
                } catch (IOException e) {
                    logger.warn("Error closing document stream: {}", e.getMessage());
                }
            }

            String contentType = documentService.getContentType(filename);

            // Force UTF-8 charset for HTML files to prevent encoding issues
            if (filename.toLowerCase().endsWith(".html") && !contentType.contains("charset")) {
                contentType = "text/html; charset=UTF-8";
            }

            logger.info("Serving document: {} ({}, {} bytes)", filename, contentType,
                    documentBytes.length);

            // Return document with appropriate headers
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .contentLength(documentBytes.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600") // Cache for 1 hour
                    .body(new ByteArrayResource(documentBytes));

        } catch (DocumentService.DocumentNotFoundException e) {
            logger.warn("Document not found: {}", filename);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN)
                    .body("Document not found: " + filename);

        } catch (DocumentService.DocumentServiceException e) {
            logger.error("Error serving document {}: {}", filename, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN).body("Error retrieving document");
        }
    }

    /**
     * Health check for document service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document service operational");
    }
}
