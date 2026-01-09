package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QueryExecutionService
 * 
 * Service for executing SQL queries and returning results in CSV format. Enforces read-only queries
 * (SELECT only) for security.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Service
public class QueryExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(QueryExecutionService.class);
    private static final int MAX_ROWS = 10000; // Safety limit

    private final JdbcTemplate jdbcTemplate;

    public QueryExecutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Execute a SQL query and return results as CSV.
     * 
     * @param sql SQL query to execute (must be SELECT only)
     * @param username Username of the admin executing the query (for audit logging)
     * @return CSV formatted results
     * @throws IllegalArgumentException if query is not a SELECT statement
     */
    public String executeQueryAsCsv(String sql, String username) {
        // Validate query is read-only
        validateReadOnlyQuery(sql);

        // Audit log: Record query execution
        logger.info("[AUDIT] Admin query executed by user: {} | Query: {}", username,
                sql.substring(0, Math.min(200, sql.length())));

        logger.debug("Executing query: {}", sql.substring(0, Math.min(100, sql.length())));

        try {
            // Execute query
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);

            if (results.isEmpty()) {
                logger.info("Query returned no results");
                return ""; // Empty CSV
            }

            if (results.size() > MAX_ROWS) {
                logger.warn("Query returned {} rows, truncating to {}", results.size(), MAX_ROWS);
                results = results.subList(0, MAX_ROWS);
            }

            // Convert to CSV
            return convertToCsv(results);

        } catch (Exception e) {
            logger.error("Error executing query", e);
            throw new RuntimeException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    /**
     * Validate that the query is a SELECT statement only.
     */
    private void validateReadOnlyQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String trimmedSql = sql.trim().toUpperCase();

        // Check if query starts with SELECT (allowing for whitespace and comments)
        String sqlWithoutComments = removeComments(trimmedSql);

        if (!sqlWithoutComments.startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }

        // Additional safety checks
        if (containsDangerousKeywords(sqlWithoutComments)) {
            throw new IllegalArgumentException("Query contains disallowed keywords");
        }
    }

    /**
     * Remove SQL comments from query.
     */
    private String removeComments(String sql) {
        // Remove single-line comments (-- style)
        String withoutSingleLine = sql.replaceAll("--[^\\n]*", "");

        // Remove multi-line comments (/* */ style)
        String withoutMultiLine = withoutSingleLine.replaceAll("/\\*.*?\\*/", "");

        return withoutMultiLine.trim();
    }

    /**
     * Check for dangerous keywords that shouldn't appear in SELECT queries. Uses word boundaries to
     * avoid matching keywords within column names.
     */
    private boolean containsDangerousKeywords(String sql) {
        String[] dangerousKeywords = {"INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
                "TRUNCATE", "EXEC", "EXECUTE", "GRANT", "REVOKE"};

        for (String keyword : dangerousKeywords) {
            // Use word boundary regex to match whole words only
            String pattern = "\\b" + keyword + "\\b";
            if (sql.matches(".*" + pattern + ".*")) {
                logger.warn("Query contains dangerous keyword: {}", keyword);
                return true;
            }
        }

        return false;
    }

    /**
     * Convert query results to CSV format.
     */
    private String convertToCsv(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "";
        }

        StringBuilder csv = new StringBuilder();

        // Get column names from first row
        List<String> columnNames = results.get(0).keySet().stream().collect(Collectors.toList());

        // Add header row
        csv.append(columnNames.stream().map(this::escapeCsvValue).collect(Collectors.joining(",")));
        csv.append("\n");

        // Add data rows
        for (Map<String, Object> row : results) {
            csv.append(columnNames.stream().map(col -> {
                Object value = row.get(col);
                return value == null ? "" : escapeCsvValue(value.toString());
            }).collect(Collectors.joining(",")));
            csv.append("\n");
        }

        return csv.toString();
    }

    /**
     * Escape a value for CSV format.
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
