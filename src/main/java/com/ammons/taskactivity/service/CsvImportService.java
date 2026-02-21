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
import org.springframework.data.jpa.repository.JpaRepository;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * Service for importing CSV files into the database. Supports bulk import of TaskActivity, Expense,
 * and DropdownValue records with validation.
 *
 * <p>
 * CSV Column Name Flexibility: The service accepts both database column names (lowercase with
 * underscores) and camelCase variations for flexibility. For example, both "taskdate" and
 * "taskDate", or "expense_date" and "expenseDate" will work correctly. However, it's recommended to
 * use the actual database column names (taskdate, taskhours, expense_date, etc.) for consistency.
 * </p>
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
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
     * <p>
     * Expected CSV format (with header):
     * taskdate,client,project,phase,taskhours,taskid,taskname,details,username
     * 2026-01-15,ClientA,ProjectX,Development,8.00,TA-001,My Task,Implementation work,john.doe
     * </p>
     *
     * @param file CSV file to import
     * @return Import result with statistics
     * @throws IOException if file reading fails
     */
    public CsvImportResult importTaskActivities(MultipartFile file) throws IOException {
        return importCsvGeneric(file, "TaskActivity", this::parseTaskActivity,
                taskActivityRepository);
    }

    /**
     * Import Expense records from CSV file.
     * 
     * <p>
     * Expected CSV format (with header):
     * username,client,project,expenseDate,expenseType,description,amount,currency,paymentMethod,vendor,referenceNumber,expenseStatus
     * john.doe,ClientA,ProjectX,2026-01-15,Travel,Flight to NYC,450.00,USD,Corporate Card,United
     * Airlines,ABC123,Submitted
     * </p>
     *
     * @param file CSV file to import
     * @return Import result with statistics
     * @throws IOException if file reading fails
     */
    public CsvImportResult importExpenses(MultipartFile file) throws IOException {
        return importCsvGeneric(file, "Expense", this::parseExpense, expenseRepository);
    }

    /**
     * Import DropdownValue records from CSV file.
     * 
     * <p>
     * Expected CSV format (with header): category,subcategory,itemvalue,displayorder,isactive
     * CLIENT,General,Acme Corp,1,true PROJECT,Development,Website Redesign,1,true
     * </p>
     * 
     * <p>
     * Note: Duplicates (same category, subcategory, itemvalue) are silently skipped. The unique
     * constraint is on (category, subcategory, itemvalue).
     * </p>
     *
     * @param file CSV file to import
     * @return Import result with statistics
     * @throws IOException if file reading fails
     */
    public CsvImportResult importDropdownValues(MultipartFile file) throws IOException {
        return importCsvGeneric(file, "DropdownValue", this::parseDropdownValue,
                dropdownValueRepository);
    }

    /**
     * Generic CSV import method supporting any entity type.
     * 
     * <p>
     * This method centralizes the CSV reading, validation, and saving logic, reducing code
     * duplication across all import methods.
     * </p>
     *
     * @param <T> the entity type
     * @param file CSV file to import
     * @param entityName display name for logging (e.g., "TaskActivity")
     * @param entityParser function to parse CSV row into entity
     * @param repository repository to save the entity
     * @return Import result with statistics
     * @throws IOException if file reading fails
     */
    private <T> CsvImportResult importCsvGeneric(MultipartFile file, String entityName,
            BiFunction<String[], String[], T> entityParser, Object repository) throws IOException {
        logger.info("Starting {} CSV import from file: {}", entityName, file.getOriginalFilename());

        CsvImportResult result = new CsvImportResult();
        AtomicInteger skippedDuplicates = new AtomicInteger(0);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNumber = 0;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                if (headers == null) {
                    headers = parseCsvLine(line);
                    continue;
                }

                processEntityImport(line, headers, lineNumber, entityName, entityParser, repository,
                        result, skippedDuplicates);
            }
        }

        logImportCompletion(entityName, result, skippedDuplicates.get());
        return result;
    }

    /**
     * Process import of a single CSV entity.
     */
    private <T> void processEntityImport(String line, String[] headers, int lineNumber,
            String entityName, BiFunction<String[], String[], T> entityParser, Object repository,
            CsvImportResult result, AtomicInteger skippedDuplicates) {
        try {
            String[] values = parseCsvLine(line);
            T entity = entityParser.apply(headers, values);

            if (!validateEntity(entity, lineNumber, result)) {
                return;
            }

            result.incrementProcessed();
            saveEntityWithDuplicateHandling(entity, repository, entityName, lineNumber,
                    skippedDuplicates, result);

        } catch (Exception e) {
            result.addError(lineNumber, "Parse error: " + e.getMessage());
            logger.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
        }
    }

    /**
     * Validate entity using Bean Validation.
     *
     * @param entity the entity to validate
     * @param lineNumber current line number for error reporting
     * @param result import result to add errors to
     * @return true if valid, false if validation errors exist
     */
    private <T> boolean validateEntity(T entity, int lineNumber, CsvImportResult result) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            String errors =
                    violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .reduce((a, b) -> a + "; " + b).orElse("Unknown validation error");
            result.addError(lineNumber, "Validation failed: " + errors);
            return false;
        }
        return true;
    }

    /**
     * Save entity to repository with duplicate handling.
     *
     * @param entity the entity to save
     * @param repository the repository to save to
     * @param entityName entity name for logging
     * @param lineNumber current line number for logging
     * @param skippedDuplicates counter for duplicate records
     * @param result import result to update
     */
    @SuppressWarnings("unchecked")
    private <T, ID> void saveEntityWithDuplicateHandling(T entity, Object repository,
            String entityName, int lineNumber, AtomicInteger skippedDuplicates,
            CsvImportResult result) {
        try {
            ((org.springframework.data.repository.CrudRepository<T, ID>) repository).save(entity);
            result.addSuccess(1);
        } catch (DataIntegrityViolationException e) {
            skippedDuplicates.incrementAndGet();
            logger.debug("Skipping duplicate {} record at line {}: {}", entityName, lineNumber,
                    e.getMessage());
        }
    }

    /**
     * Log final import completion statistics.
     *
     * @param entityName entity name for logging
     * @param result import result
     * @param skippedDuplicates number of skipped duplicate records
     */
    private void logImportCompletion(String entityName, CsvImportResult result,
            int skippedDuplicates) {
        if (skippedDuplicates > 0) {
            logger.info("Skipped {} duplicate {} records", skippedDuplicates, entityName);
        }
        logger.info("{} import completed. Success: {}, Errors: {}", entityName,
                result.getSuccessCount(), result.getErrorCount());
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

        // AllUsers is optional, defaults to false
        // Note: createFieldMap strips underscores, so "all_users" key becomes "allusers"
        String allUsersStr = fieldMap.get("allusers");
        if (allUsersStr != null && !allUsersStr.trim().isEmpty()) {
            dropdownValue.setAllUsers(Boolean.parseBoolean(allUsersStr));
        } else {
            dropdownValue.setAllUsers(false);
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
        activity.setTaskId(fieldMap.get("taskid"));
        activity.setTaskName(fieldMap.get("taskname"));
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
