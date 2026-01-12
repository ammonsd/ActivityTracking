package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.repository.ExpenseRepository;
import com.ammons.taskactivity.repository.TaskActivityRepository;
import com.ammons.taskactivity.service.CsvImportService.CsvImportResult;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CSV import functionality.
 *
 * Author: Dean Ammons Date: January 2026
 */
@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private TaskActivityRepository taskActivityRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private CsvImportService csvImportService;

    @BeforeEach
    void setUp() {
        // Mock validator to return no violations by default
        when(validator.validate(any())).thenReturn(Collections.emptySet());
    }

    @Test
    @DisplayName("Should successfully import TaskActivity CSV with all valid records")
    void testImportTaskActivities_Success() throws Exception {
        // Given
        String csvContent = """
                taskDate,client,project,phase,hours,details,username
                2026-01-15,Acme Corp,Website Redesign,Development,8.00,Implementation work,john.doe
                2026-01-16,Tech Solutions,Mobile App,Testing,6.50,Regression testing,jane.smith
                """;

        MockMultipartFile file = new MockMultipartFile("file", "taskactivity-test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvImportResult result = csvImportService.importTaskActivities(file);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(0);
        assertThat(result.hasErrors()).isFalse();

        // Verify repository was called twice (once per record)
        ArgumentCaptor<TaskActivity> captor = ArgumentCaptor.forClass(TaskActivity.class);
        verify(taskActivityRepository, times(2)).save(captor.capture());

        List<TaskActivity> savedActivities = captor.getAllValues();
        assertThat(savedActivities).hasSize(2);

        // Verify first record
        TaskActivity first = savedActivities.get(0);
        assertThat(first.getTaskDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(first.getClient()).isEqualTo("Acme Corp");
        assertThat(first.getProject()).isEqualTo("Website Redesign");
        assertThat(first.getPhase()).isEqualTo("Development");
        assertThat(first.getHours()).isEqualTo(new BigDecimal("8.00"));
        assertThat(first.getDetails()).isEqualTo("Implementation work");
        assertThat(first.getUsername()).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("Should successfully import Expense CSV with all valid records")
    void testImportExpenses_Success() throws Exception {
        // Given
        String csvContent =
                """
                        username,client,project,expenseDate,expenseType,description,amount,currency,paymentMethod,vendor,referenceNumber,expenseStatus
                        john.doe,Acme Corp,Website,2026-01-15,Travel,Flight to client site,450.00,USD,Corporate Card,United Airlines,UA12345,Submitted
                        jane.smith,Tech Solutions,Mobile App,2026-01-16,Meals,Client dinner,125.50,USD,Personal Card,The Steakhouse,INV-001,Draft
                        """;

        MockMultipartFile file = new MockMultipartFile("file", "expense-test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvImportResult result = csvImportService.importExpenses(file);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getErrorCount()).isEqualTo(0);

        // Verify repository was called twice (once per record)
        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository, times(2)).save(captor.capture());

        List<Expense> savedExpenses = captor.getAllValues();
        assertThat(savedExpenses).hasSize(2);

        // Verify first record
        Expense first = savedExpenses.get(0);
        assertThat(first.getUsername()).isEqualTo("john.doe");
        assertThat(first.getClient()).isEqualTo("Acme Corp");
        assertThat(first.getProject()).isEqualTo("Website");
        assertThat(first.getExpenseDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(first.getExpenseType()).isEqualTo("Travel");
        assertThat(first.getDescription()).isEqualTo("Flight to client site");
        assertThat(first.getAmount()).isEqualTo(new BigDecimal("450.00"));
        assertThat(first.getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should handle CSV with multiple date formats")
    void testImportTaskActivities_MultipleDateFormats() throws Exception {
        // Given
        String csvContent = """
                taskDate,client,project,phase,hours,details,username
                2026-01-15,Client1,Project1,Phase1,8.00,ISO format,user1
                01/16/2026,Client2,Project2,Phase2,7.00,US format,user2
                1/17/2026,Client3,Project3,Phase3,6.00,Short US,user3
                11-Jun-2018,Client4,Project4,Phase4,5.00,Month abbrev,user4
                """;

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvImportResult result = csvImportService.importTaskActivities(file);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(4);
        assertThat(result.getSuccessCount()).isEqualTo(4);
        assertThat(result.getErrorCount()).isEqualTo(0);

        ArgumentCaptor<TaskActivity> captor = ArgumentCaptor.forClass(TaskActivity.class);
        verify(taskActivityRepository, times(4)).save(captor.capture());

        List<TaskActivity> savedActivities = captor.getAllValues();
        assertThat(savedActivities.get(0).getTaskDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(savedActivities.get(1).getTaskDate()).isEqualTo(LocalDate.of(2026, 1, 16));
        assertThat(savedActivities.get(2).getTaskDate()).isEqualTo(LocalDate.of(2026, 1, 17));
        assertThat(savedActivities.get(3).getTaskDate()).isEqualTo(LocalDate.of(2018, 6, 11));
    }

    @Test
    @DisplayName("Should skip empty lines in CSV")
    void testImportTaskActivities_SkipEmptyLines() throws Exception {
        // Given
        String csvContent = """
                taskDate,client,project,phase,hours,details,username
                2026-01-15,Client1,Project1,Phase1,8.00,Details,user1

                2026-01-16,Client2,Project2,Phase2,7.00,Details,user2

                """;

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvImportResult result = csvImportService.importTaskActivities(file);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle CSV with quoted fields containing commas")
    void testImportTaskActivities_QuotedFields() throws Exception {
        // Given
        String csvContent =
                """
                        taskDate,client,project,phase,hours,details,username
                        2026-01-15,Acme Corp,"Website, Mobile",Development,8.00,"Implementation, testing",john.doe
                        """;

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvImportResult result = csvImportService.importTaskActivities(file);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);

        ArgumentCaptor<TaskActivity> captor = ArgumentCaptor.forClass(TaskActivity.class);
        verify(taskActivityRepository).save(captor.capture());

        TaskActivity saved = captor.getValue();
        assertThat(saved.getProject()).isEqualTo("Website, Mobile");
        assertThat(saved.getDetails()).isEqualTo("Implementation, testing");
    }

    @Test
    @DisplayName("Should handle Expense CSV with optional fields missing")
    void testImportExpenses_OptionalFieldsMissing() throws Exception {
        // Given
        String csvContent =
                """
                        username,client,project,expenseDate,expenseType,description,amount,currency,paymentMethod,vendor,referenceNumber,expenseStatus
                        john.doe,Acme Corp,,2026-01-15,Travel,Flight,450.00,,Corporate Card,,,
                        """;

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvImportResult result = csvImportService.importExpenses(file);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());

        Expense saved = captor.getValue();
        assertThat(saved.getProject()).isNull();
        assertThat(saved.getCurrency()).isEqualTo("USD"); // Default
        assertThat(saved.getVendor()).isNull();
        assertThat(saved.getReferenceNumber()).isNull();
        assertThat(saved.getExpenseStatus()).isEqualTo("Draft"); // Default
    }

    @Test
    @DisplayName("Should handle batch processing for large imports")
    void testImportTaskActivities_BatchProcessing() throws Exception {
        // Given - Create CSV with 150 records (should trigger batch processing)
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("taskDate,client,project,phase,hours,details,username\n");

        for (int i = 1; i <= 150; i++) {
            csvContent.append(
                    String.format("2026-01-15,Client%d,Project%d,Phase%d,8.00,Details%d,user%d\n",
                            i, i, i, i, i));
        }

        MockMultipartFile file = new MockMultipartFile("file", "large-test.csv", "text/csv",
                csvContent.toString().getBytes(StandardCharsets.UTF_8));

        // When
        CsvImportResult result = csvImportService.importTaskActivities(file);

        // Then
        assertThat(result.getProcessedCount()).isEqualTo(150);
        assertThat(result.getSuccessCount()).isEqualTo(150);
        assertThat(result.getErrorCount()).isEqualTo(0);

        // Verify save was called 150 times (individual saves for duplicate handling)
        verify(taskActivityRepository, times(150)).save(any(TaskActivity.class));
    }
}
