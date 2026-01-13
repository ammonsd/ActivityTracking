package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.repository.DropdownValueRepository;
import com.ammons.taskactivity.repository.ExpenseRepository;
import com.ammons.taskactivity.repository.TaskActivityRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.Locale;

/**
 * Service for importing CSV files into the database. Supports bulk import of TaskActivity and
 * Expense records with validation.
 * 
 * CSV Column Name Flexibility: The service accepts both database column names (lowercase with
 * underscores) and camelCase variations for flexibility. For example, both "taskdate" and
 * "taskDate", or "expense_date" and "expenseDate" will work correctly. However, it's recommended to
 * use the actual database column names (taskdate, taskhours, expense_date, etc.) for consistency.
 *
 * Author: Dean Ammons Date: January 2026
 */
@Service
public class CsvImportService {

    private static final Logger logger = LoggerFactory.getLogger(CsvImportService.class);

    // Supported date formats
    private static final DateTimeFormatter[] DATE_FORMATTERS = {DateTimeFormatter.ISO_LOCAL_DATE, // 2026-01-15
            DateTimeFormatter.ofPattern("MM/dd/yyyy"), // 01/15/2026
            DateTimeFormatter.ofPattern("M/d/yyyy"), // 1/15/2026
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), // 2026-01-15
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH) // 11-Jun-2018
    };

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private DropdownValueRepository dropdownValueRepository;

    @Autowired
    private Validator validator;

    /**
     * Import TaskActivity records from CSV file.
     * 
     * Expected CSV format (with header): taskDate,client,project,phase,hours,details,username
     * 2026-01-15,ClientA,ProjectX,Development,8.00,Implementation work,john.doe
     *
     * @param file CSV file to import
     * @return Import result with statistics
     * @throws IOException if file reading fails
     */
    public CsvImportResult importTaskActivities(MultipartFile file) throws IOException {
        logger.info("Starting TaskActivity CSV import from file: {}", file.getOriginalFilename());

        CsvImportResult result = new CsvImportResult();
        int skippedDuplicates = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // First non-empty line is the header
                if (headers == null) {
                    headers = parseCsvLine(line);
                    continue;
                }

                try {
                    String[] values = parseCsvLine(line);
                    TaskActivity activity = parseTaskActivity(headers, values);

                    // Validate entity
                    Set<ConstraintViolation<TaskActivity>> violations =
                            validator.validate(activity);
                    if (!violations.isEmpty()) {
                        String errors = violations.stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .reduce((a, b) -> a + "; " + b).orElse("Unknown validation error");
                        result.addError(lineNumber, "Validation failed: " + errors);
                        continue;
                    }

                    result.incrementProcessed();

                    // Save record individually with duplicate handling
                    try {
                        taskActivityRepository.save(activity);
                        result.addSuccess(1);
                    } catch (DataIntegrityViolationException e) {
                        // Duplicate record - skip it silently
                        skippedDuplicates++;
                        logger.debug("Skipping duplicate TaskActivity record at line {}: {}",
                                lineNumber, e.getMessage());
                    }

                } catch (Exception e) {
                    result.addError(lineNumber, "Parse error: " + e.getMessage());
                    logger.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        if (skippedDuplicates > 0) {
            logger.info("Skipped {} duplicate TaskActivity records", skippedDuplicates);
        }
        logger.info("TaskActivity import completed. Success: {}, Errors: {}",
                result.getSuccessCount(), result.getErrorCount());
        return result;
    }

    /**
     * Import Expense records from CSV file.
     * 
     * Expected CSV format (with header):
     * username,client,project,expenseDate,expenseType,description,amount,currency,paymentMethod,vendor,referenceNumber,expenseStatus
     * john.doe,ClientA,ProjectX,2026-01-15,Travel,Flight to NYC,450.00,USD,Corporate Card,United
     * Airlines,ABC123,Submitted
     *
     * @param file CSV file to import
     * @return Import result with statistics
     * @throws IOException if file reading fails
     */
    public CsvImportResult importExpenses(MultipartFile file) throws IOException {
        logger.info("Starting Expense CSV import from file: {}", file.getOriginalFilename());

        CsvImportResult result = new CsvImportResult();
        int skippedDuplicates = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // First non-empty line is the header
                if (headers == null) {
                    headers = parseCsvLine(line);
                    continue;
                }

                try {
                    String[] values = parseCsvLine(line);
                    Expense expense = parseExpense(headers, values);

                    // Validate entity
                    Set<ConstraintViolation<Expense>> violations = validator.validate(expense);
                    if (!violations.isEmpty()) {
                        String errors = violations.stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .reduce((a, b) -> a + "; " + b).orElse("Unknown validation error");
                        result.addError(lineNumber, "Validation failed: " + errors);
                        continue;
                    }

                    result.incrementProcessed();

                    // Save record individually with duplicate handling
                    try {
                        expenseRepository.save(expense);
                        result.addSuccess(1);
                    } catch (DataIntegrityViolationException e) {
                        // Duplicate record - skip it silently
                        skippedDuplicates++;
                        logger.debug("Skipping duplicate Expense record at line {}: {}", lineNumber,
                                e.getMessage());
                    }

                } catch (Exception e) {
                    result.addError(lineNumber, "Parse error: " + e.getMessage());
                    logger.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        if (skippedDuplicates > 0) {
            logger.info("Skipped {} duplicate Expense records", skippedDuplicates);
        }
        logger.info("Expense import completed. Success: {}, Errors: {}", result.getSuccessCount(),
                result.getErrorCount());
        return result;
    }

    /**
     * Import DropdownValue records from CSV file.
     * 
     * Expected CSV format (with header): category,subcategory,itemvalue,displayorder,isactive
     * CLIENT,General,Acme Corp,1,true PROJECT,Development,Website Redesign,1,true
     * 
     * Note: Duplicates (same category, subcategory, itemvalue) are silently skipped. The unique
     * constraint is on (category, subcategory, itemvalue).
     *
     * @param file CSV file to import
     * @return Import result with statistics
     * @throws IOException if file reading fails
     */
    public CsvImportResult importDropdownValues(MultipartFile file) throws IOException {
        logger.info("Starting DropdownValue CSV import from file: {}", file.getOriginalFilename());

        CsvImportResult result = new CsvImportResult();
        int skippedDuplicates = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // First non-empty line is the header
                if (headers == null) {
                    headers = parseCsvLine(line);
                    continue;
                }

                try {
                    String[] values = parseCsvLine(line);
                    DropdownValue dropdownValue = parseDropdownValue(headers, values);

                    // Validate entity
                    Set<ConstraintViolation<DropdownValue>> violations =
                            validator.validate(dropdownValue);
                    if (!violations.isEmpty()) {
                        String errors = violations.stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .reduce((a, b) -> a + "; " + b).orElse("Unknown validation error");
                        result.addError(lineNumber, "Validation failed: " + errors);
                        continue;
                    }

                    result.incrementProcessed();

                    // Save record individually with duplicate handling
                    try {
                        dropdownValueRepository.save(dropdownValue);
                        result.addSuccess(1);
                    } catch (DataIntegrityViolationException e) {
                        // Duplicate record - skip it silently
                        skippedDuplicates++;
                        logger.debug("Skipping duplicate DropdownValue record at line {}: {}",
                                lineNumber, e.getMessage());
                    }

                } catch (Exception e) {
                    result.addError(lineNumber, "Parse error: " + e.getMessage());
                    logger.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        if (skippedDuplicates > 0) {
            logger.info("Skipped {} duplicate DropdownValue records", skippedDuplicates);
        }
        logger.info("DropdownValue import completed. Success: {}, Errors: {}",
                result.getSuccessCount(), result.getErrorCount());
        return result;
    }

    /**
     * Parse DropdownValue from CSV row using header mapping.
     */
    private DropdownValue parseDropdownValue(String[] headers, String[] values) {
        Map<String, String> fieldMap = createFieldMap(headers, values);

        DropdownValue dropdownValue = new DropdownValue();

        // Category is required and converted to uppercase
        String category = fieldMap.get("category");
        if (category != null) {
            dropdownValue.setCategory(category.toUpperCase());
        } else {
            throw new IllegalArgumentException("Missing required field: category");
        }

        // Subcategory is required
        String subcategory = fieldMap.get("subcategory");
        if (subcategory == null) {
            throw new IllegalArgumentException("Missing required field: subcategory");
        }
        dropdownValue.setSubcategory(subcategory);

        // ItemValue is required
        String itemValue = fieldMap.get("itemvalue");
        if (itemValue == null) {
            throw new IllegalArgumentException("Missing required field: itemvalue");
        }
        dropdownValue.setItemValue(itemValue);

        // DisplayOrder is optional, defaults to 0
        String displayOrderStr = fieldMap.get("displayorder");
        if (displayOrderStr != null && !displayOrderStr.trim().isEmpty()) {
            try {
                dropdownValue.setDisplayOrder(Integer.parseInt(displayOrderStr));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid displayorder format: " + displayOrderStr);
            }
        } else {
            dropdownValue.setDisplayOrder(0);
        }

        // IsActive is optional, defaults to true
        String isActiveStr = fieldMap.get("isactive");
        if (isActiveStr != null && !isActiveStr.trim().isEmpty()) {
            dropdownValue.setIsActive(Boolean.parseBoolean(isActiveStr));
        } else {
            dropdownValue.setIsActive(true);
        }

        return dropdownValue;
    }

    /**
     * Parse a CSV line handling quoted fields and escaped commas.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }

        fields.add(currentField.toString().trim());
        return fields.toArray(new String[0]);
    }

    /**
     * Parse TaskActivity from CSV row using header mapping.
     */
    private TaskActivity parseTaskActivity(String[] headers, String[] values) {
        Map<String, String> fieldMap = createFieldMap(headers, values);

        TaskActivity activity = new TaskActivity();
        activity.setTaskDate(parseDate(fieldMap.get("taskdate"), fieldMap.get("taskDate")));
        activity.setClient(fieldMap.get("client"));
        activity.setProject(fieldMap.get("project"));
        activity.setPhase(fieldMap.get("phase"));
        activity.setHours(parseBigDecimal(fieldMap.get("hours"), fieldMap.get("taskhours")));
        activity.setDetails(fieldMap.get("details"));
        activity.setUsername(fieldMap.get("username"));

        return activity;
    }

    /**
     * Parse Expense from CSV row using header mapping.
     */
    private Expense parseExpense(String[] headers, String[] values) {
        Map<String, String> fieldMap = createFieldMap(headers, values);

        Expense expense = new Expense();
        expense.setUsername(fieldMap.get("username"));
        expense.setClient(fieldMap.get("client"));
        expense.setProject(fieldMap.get("project"));
        expense.setExpenseDate(parseDate(fieldMap.get("expensedate"), fieldMap.get("expense_date"),
                fieldMap.get("expenseDate")));
        expense.setExpenseType(fieldMap.get("expensetype"));
        expense.setDescription(fieldMap.get("description"));
        expense.setAmount(parseBigDecimal(fieldMap.get("amount")));
        expense.setCurrency(getOrDefault(fieldMap.get("currency"), "USD"));
        expense.setPaymentMethod(fieldMap.get("paymentmethod"));
        expense.setVendor(fieldMap.get("vendor"));
        expense.setReferenceNumber(fieldMap.get("referencenumber"));
        expense.setExpenseStatus(getOrDefault(fieldMap.get("expensestatus"), "Draft"));

        return expense;
    }

    /**
     * Create a case-insensitive field map from headers and values.
     */
    private Map<String, String> createFieldMap(String[] headers, String[] values) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < headers.length && i < values.length; i++) {
            String key = headers[i].toLowerCase().replaceAll("[_\\s-]", "");
            map.put(key, values[i].isEmpty() ? null : values[i]);
        }
        return map;
    }

    /**
     * Parse date from string, trying multiple formats.
     */
    private LocalDate parseDate(String... dateStrings) {
        for (String dateStr : dateStrings) {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                continue;
            }

            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(dateStr.trim(), formatter);
                } catch (DateTimeParseException ignored) {
                    // Try next formatter
                }
            }
        }

        throw new IllegalArgumentException("Unable to parse date from provided values");
    }

    /**
     * Parse BigDecimal from string.
     */
    private BigDecimal parseBigDecimal(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                try {
                    return new BigDecimal(value.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number format: " + value);
                }
            }
        }
        throw new IllegalArgumentException("No valid numeric value provided");
    }

    /**
     * Get value or default if null/empty.
     */
    private String getOrDefault(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }

    /**
     * Result object for CSV import operations.
     */
    public static class CsvImportResult {
        private int processedCount = 0;
        private int successCount = 0;
        private int errorCount = 0;
        private final List<String> errors = new ArrayList<>();

        public void incrementProcessed() {
            processedCount++;
        }

        public void addSuccess(int count) {
            successCount += count;
        }

        public void addError(int lineNumber, String message) {
            errorCount++;
            errors.add("Line " + lineNumber + ": " + message);
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return errorCount > 0;
        }
    }
}
