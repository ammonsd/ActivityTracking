package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.QueryExecutionRequestDto;
import com.ammons.taskactivity.service.QueryExecutionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AdminQueryController
 * 
 * REST API endpoint for executing read-only SQL queries and returning results as CSV. This endpoint
 * is restricted to users with ADMIN role for security purposes.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/query")
public class AdminQueryController {

    private static final Logger logger = LoggerFactory.getLogger(AdminQueryController.class);

    private final QueryExecutionService queryExecutionService;

    public AdminQueryController(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    /**
     * Execute a SQL query and return results as CSV.
     * 
     * Only SELECT queries are allowed for security.
     * 
     * @param request Query execution request containing the SQL query
     * @return CSV formatted results
     */
    @PostMapping("/execute")
    public ResponseEntity<String> executeQuery(
            @Valid @RequestBody QueryExecutionRequestDto request) {
        logger.info("Executing query for admin user");

        try {
            String csvResult = queryExecutionService.executeQueryAsCsv(request.getQuery());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "csv"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"query-results.csv\"");

            logger.info("Query executed successfully, returned {} bytes", csvResult.length());

            return ResponseEntity.ok().headers(headers).body(csvResult);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid query request: {}", e.getMessage());
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN)
                    .body("Error: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Error executing query", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Error executing query: " + e.getMessage());
        }
    }
}
