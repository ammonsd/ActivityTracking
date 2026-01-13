package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.service.CsvImportService;
import com.ammons.taskactivity.service.CsvImportService.CsvImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for CSV data import operations. Provides endpoints for bulk importing
 * TaskActivity and Expense records.
 *
 * Author: Dean Ammons Date: January 2026
 */
@RestController
@RequestMapping("/api/import")
public class CsvImportController {

    private static final Logger logger = LoggerFactory.getLogger(CsvImportController.class);

    @Autowired
    private CsvImportService csvImportService;

    /**
     * Import TaskActivity records from CSV file.
     * 
     * Requires ADMIN or MANAGER role.
     * 
     * Expected CSV format: taskdate,client,project,phase,taskhours,details,username
     * 2026-01-15,ClientA,ProjectX,Development,8.00,Implementation work,john.doe
     * 
     * Supported date formats: ISO (2026-01-15), US (01/15/2026), etc.
     *
     * @param file CSV file to import
     * @return Import result with statistics and any errors
     */
    @PostMapping(value = "/taskactivities", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> importTaskActivities(
            @RequestParam("file") MultipartFile file) {

        logger.info("Received TaskActivity import request. File: {}, Size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(createErrorResponse("File must be a CSV file"));
        }

        try {
            CsvImportResult result = csvImportService.importTaskActivities(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import completed");
            response.put("totalProcessed", result.getProcessedCount());
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());

            if (result.hasErrors()) {
                response.put("errors", result.getErrors());
            }

            HttpStatus status = result.hasErrors() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).body(response);

        } catch (IOException e) {
            logger.error("IO error during TaskActivity import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to read file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during TaskActivity import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Import failed: " + e.getMessage()));
        }
    }

    /**
     * Import Expense records from CSV file.
     * 
     * Requires ADMIN or MANAGER role.
     * 
     * Expected CSV format:
     * username,client,project,expense_date,expense_type,description,amount,currency,payment_method,vendor,reference_number,expense_status
     * john.doe,ClientA,ProjectX,2026-01-15,Travel,Flight to NYC,450.00,USD,Corporate Card,United
     * Airlines,ABC123,Submitted
     * 
     * Optional fields: project, currency (defaults to USD), vendor, reference_number,
     * expense_status (defaults to Draft)
     *
     * @param file CSV file to import
     * @return Import result with statistics and any errors
     */
    @PostMapping(value = "/expenses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> importExpenses(
            @RequestParam("file") MultipartFile file) {

        logger.info("Received Expense import request. File: {}, Size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(createErrorResponse("File must be a CSV file"));
        }

        try {
            CsvImportResult result = csvImportService.importExpenses(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import completed");
            response.put("totalProcessed", result.getProcessedCount());
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());

            if (result.hasErrors()) {
                response.put("errors", result.getErrors());
            }

            HttpStatus status = result.hasErrors() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).body(response);

        } catch (IOException e) {
            logger.error("IO error during Expense import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to read file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during Expense import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Import failed: " + e.getMessage()));
        }
    }

    /**
     * Import DropdownValue records from CSV file.
     * 
     * Requires ADMIN or MANAGER role.
     * 
     * Expected CSV format: category,subcategory,itemvalue,displayorder,isactive CLIENT,General,Acme
     * Corp,1,true PROJECT,Development,Website Redesign,1,true
     * 
     * Duplicates (same category, subcategory, itemvalue) are silently skipped.
     *
     * @param file CSV file to import
     * @return Import result with statistics and any errors
     */
    @PostMapping(value = "/dropdownvalues", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> importDropdownValues(
            @RequestParam("file") MultipartFile file) {

        logger.info("Received DropdownValue import request. File: {}, Size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(createErrorResponse("File must be a CSV file"));
        }

        try {
            CsvImportResult result = csvImportService.importDropdownValues(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import completed");
            response.put("totalProcessed", result.getProcessedCount());
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());

            if (result.hasErrors()) {
                response.put("errors", result.getErrors());
            }

            HttpStatus status = result.hasErrors() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).body(response);

        } catch (IOException e) {
            logger.error("IO error during DropdownValue import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to read file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during DropdownValue import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Import failed: " + e.getMessage()));
        }
    }

    /**
     * Get CSV template information for TaskActivity imports.
     *
     * @return Template information and example
     */
    @GetMapping("/taskactivities/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<Map<String, Object>> getTaskActivityTemplate() {
        Map<String, Object> response = new HashMap<>();
        response.put("headers", new String[] {"taskdate", "client", "project", "phase", "taskhours",
                "details", "username"});
        response.put("example",
                "2026-01-15,ClientA,ProjectX,Development,8.00,Implementation work,john.doe");
        response.put("dateFormats", new String[] {"2026-01-15 (ISO format)",
                "01/15/2026 (US format)", "1/15/2026 (Short US format)", "15-Jan-2026"});
        response.put("notes", new String[] {"taskdate: Required, supports multiple date formats",
                "client: Required, max 255 characters", "project: Required, max 255 characters",
                "phase: Required, max 255 characters",
                "taskhours: Required, decimal between 0.01 and 24.00",
                "details: Optional, max 255 characters", "username: Required, max 50 characters"});
        return ResponseEntity.ok(response);
    }

    /**
     * Get CSV template information for Expense imports.
     *
     * @return Template information and example
     */
    @GetMapping("/expenses/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public ResponseEntity<Map<String, Object>> getExpenseTemplate() {
        Map<String, Object> response = new HashMap<>();
        response.put("headers",
                new String[] {"username", "client", "project", "expense_date", "expense_type",
                        "description", "amount", "currency", "payment_method", "vendor",
                        "reference_number", "expense_status"});
        response.put("example",
                "john.doe,ClientA,ProjectX,2026-01-15,Travel,Flight to NYC,450.00,USD,Corporate Card,United Airlines,ABC123,Submitted");
        response.put("dateFormats", new String[] {"2026-01-15 (ISO format)",
                "01/15/2026 (US format)", "1/15/2026 (Short US format)", "15-Jan-2026"});
        response.put("notes", new String[] {"username: Required, max 50 characters",
                "client: Required, max 50 characters", "project: Optional, max 50 characters",
                "expense_date: Required, supports multiple date formats",
                "expense_type: Required, max 50 characters",
                "description: Required, max 255 characters",
                "amount: Required, decimal greater than 0.01",
                "currency: Optional, defaults to USD, 3 characters",
                "payment_method: Required, max 50 characters",
                "vendor: Optional, max 100 characters",
                "reference_number: Optional, max 50 characters",
                "expense_status: Optional, defaults to Draft, max 50 characters"});
        return ResponseEntity.ok(response);
    }

    /**
     * Get CSV template information for DropdownValue imports.
     *
     * @return Template information and example
     */
    @GetMapping("/dropdownvalues/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getDropdownValueTemplate() {
        Map<String, Object> response = new HashMap<>();
        response.put("headers",
                new String[] {"category", "subcategory", "itemvalue", "displayorder", "isactive"});
        response.put("example", "TASK,CLIENT,Acme Corporation,1,true");
        response.put("notes",
                new String[] {"category: Required, max 50 characters (auto-converted to uppercase)",
                        "subcategory: Required, max 50 characters",
                        "itemvalue: Required, max 100 characters",
                        "displayorder: Optional, defaults to 0, integer",
                        "isactive: Optional, defaults to true, boolean (true/false)"});
        return ResponseEntity.ok(response);
    }

    /**
     * Create a standardized error response.
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
