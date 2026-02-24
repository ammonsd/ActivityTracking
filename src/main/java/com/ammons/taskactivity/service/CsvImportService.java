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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
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
 *
 *        Modified by: Dean Ammons - February 2026 Change: (1) Replaced line-by-line reading in
 *        importCsvGeneric with readCsvRecord() to accumulate physical lines until all quote pairs
 *        are balanced, producing one complete logical record. (2) Added charset auto-detection
 *        (UTF-8 with Windows-1252 fallback) so that files containing Windows-1252 high-ASCII
 *        characters such as the bullet (byte 0x95) import correctly instead of being replaced with
 *        the Unicode replacement character. Reason: (1) CSV fields with embedded newlines (e.g., a
 *        details column with bullet-point text) were split into separate rows because
 *        reader.readLine() returned each physical line independently, breaking the parser for
 *        RFC-4180 multi-line quoted fields.
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

        byte[] fileBytes = file.getInputStream().readAllBytes();
        Charset charset = detectCharset(fileBytes);
        logger.debug("Detected charset for {} import: {}", entityName, charset.name());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(fileBytes), charset))) {

            String csvRecord;
            int recordNumber = 0;
            String[] headers = null;

            while ((csvRecord = readCsvRecord(reader)) != null) {
                recordNumber++;

                if (csvRecord.trim().isEmpty()) {
                    continue;
                }

                if (headers == null) {
                    headers = parseCsvLine(csvRecord);
                    continue;
                }

                processEntityImport(csvRecord, headers, recordNumber, entityName, entityParser,
                        repository, result, skippedDuplicates);
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
     * Detects whether the given bytes are valid UTF-8. If not, falls back to Windows-1252, which
     * covers legacy high-ASCII characters such as the bullet character (byte 0x95 / decimal 149)
     * produced by applications like Microsoft Excel or Word.
     *
     * @param bytes raw file bytes
     * @return UTF-8 if the bytes decode without error, otherwise Windows-1252
     */
    private Charset detectCharset(byte[] bytes) {
        CharsetDecoder utf8Decoder =
                StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            utf8Decoder.decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            logger.debug("File bytes are not valid UTF-8, using Windows-1252: {}", e.getMessage());
            return Charset.forName("windows-1252");
        }
    }

    /**
     * Reads a complete logical CSV record from the reader, accumulating physical lines until all
     * double-quote pairs are balanced. This supports RFC-4180 multi-line quoted fields where a
     * single cell value spans multiple physical lines (e.g., a details field with embedded
     * newlines).
     *
     * @param reader the BufferedReader positioned at the start of a record
     * @return the full record string (may contain embedded newlines), or null at end of stream
     * @throws IOException if an I/O error occurs
     */
    private String readCsvRecord(BufferedReader reader) throws IOException {
        StringBuilder csvRecord = new StringBuilder();
        String line;
        int quoteCount = 0;

        while ((line = reader.readLine()) != null) {
            if (!csvRecord.isEmpty()) {
                csvRecord.append('\n');
            }
            csvRecord.append(line);
            quoteCount += countUnescapedQuotes(line);

            // Even quote count means all opened quote blocks are closed — record is complete
            if (quoteCount % 2 == 0) {
                return csvRecord.toString();
            }
        }

        // End of stream; return any accumulated partial data
        return csvRecord.isEmpty() ? null : csvRecord.toString();
    }

    /**
     * Counts unescaped double-quote characters in a line. Escaped quotes ("") are treated as a
     * single literal character and do not affect the open/close balance.
     *
     * @param line the line to scan
     * @return the number of unescaped double-quote characters
     */
    private int countUnescapedQuotes(String line) {
        int count = 0;
        int i = 0;
        while (i < line.length()) {
            if (line.charAt(i) == '"') {
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i += 2; // skip escaped quote pair
                } else {
                    count++;
                    i++;
                }
            } else {
                i++;
            }
        }
        return count;
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
