package com.ammons.taskactivity.service;

import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.dto.ExpenseFilterDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.exception.ExpenseNotFoundException;
import com.ammons.taskactivity.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExpenseServiceTest - Unit tests for ExpenseService Tests CRUD operations, status transitions,
 * aggregates, and business logic
 *
 * @author Dean Ammons
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Tests")
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private Expense testExpense;
    private ExpenseDto testExpenseDto;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 11, 15);

        testExpense = new Expense();
        testExpense.setId(1L);
        testExpense.setUsername("testuser");
        testExpense.setClient("Test Client");
        testExpense.setProject("Test Project");
        testExpense.setExpenseDate(testDate);
        testExpense.setExpenseType("Travel - Airfare");
        testExpense.setDescription("Flight to client site");
        testExpense.setAmount(new BigDecimal("500.00"));
        testExpense.setCurrency("USD");
        testExpense.setPaymentMethod("Corporate Credit Card");
        testExpense.setExpenseStatus("Draft");

        testExpenseDto = new ExpenseDto();
        testExpenseDto.setUsername("testuser");
        testExpenseDto.setClient("Test Client");
        testExpenseDto.setProject("Test Project");
        testExpenseDto.setExpenseDate(testDate);
        testExpenseDto.setExpenseType("Travel - Airfare");
        testExpenseDto.setDescription("Flight to client site");
        testExpenseDto.setAmount(new BigDecimal("500.00"));
        testExpenseDto.setCurrency("USD");
        testExpenseDto.setPaymentMethod("Corporate Credit Card");
        testExpenseDto.setExpenseStatus("Draft");
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should create expense successfully")
        void shouldCreateExpenseSuccessfully() {
            // Given
            when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

            // When
            Expense result = expenseService.createExpense(testExpenseDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(result.getExpenseStatus()).isEqualTo("Draft");
            verify(expenseRepository, times(1)).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should get all expenses")
        void shouldGetAllExpenses() {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            when(expenseRepository.findAll()).thenReturn(expenses);

            // When
            List<Expense> result = expenseService.getAllExpenses();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(testExpense);
            verify(expenseRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should get expenses by username")
        void shouldGetExpensesByUsername() {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            when(expenseRepository.findByUsername("testuser")).thenReturn(expenses);

            // When
            List<Expense> result = expenseService.getExpensesByUsername("testuser");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("testuser");
            verify(expenseRepository, times(1)).findByUsername("testuser");
        }

        @Test
        @DisplayName("Should get expense by ID")
        void shouldGetExpenseById() {
            // Given
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

            // When
            Optional<Expense> result = expenseService.getExpenseById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result).contains(testExpense);
            verify(expenseRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should update expense successfully")
        void shouldUpdateExpenseSuccessfully() {
            // Given
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
            when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

            testExpenseDto.setDescription("Updated description");

            // When
            Expense result = expenseService.updateExpense(1L, testExpenseDto);

            // Then
            assertThat(result).isNotNull();
            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, times(1)).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent expense")
        void shouldThrowExceptionWhenUpdatingNonExistentExpense() {
            // Given
            when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> expenseService.updateExpense(999L, testExpenseDto))
                    .isInstanceOf(ExpenseNotFoundException.class).hasMessageContaining("999");

            verify(expenseRepository, times(1)).findById(999L);
            verify(expenseRepository, never()).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should delete expense successfully")
        void shouldDeleteExpenseSuccessfully() {
            // Given
            when(expenseRepository.existsById(1L)).thenReturn(true);
            doNothing().when(expenseRepository).deleteById(1L);

            // When
            expenseService.deleteExpense(1L);

            // Then
            verify(expenseRepository, times(1)).existsById(1L);
            verify(expenseRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent expense")
        void shouldThrowExceptionWhenDeletingNonExistentExpense() {
            // Given
            when(expenseRepository.existsById(999L)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> expenseService.deleteExpense(999L))
                    .isInstanceOf(ExpenseNotFoundException.class).hasMessageContaining("999");

            verify(expenseRepository, times(1)).existsById(999L);
            verify(expenseRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Status Management Tests")
    class StatusManagementTests {

        @Test
        @DisplayName("Should submit expense for approval")
        void shouldSubmitExpenseForApproval() {
            // Given
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
            when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

            // When
            Expense result = expenseService.submitForApproval(1L, "testuser");

            // Then
            assertThat(result).isNotNull();
            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, times(1)).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should throw exception when submitting expense of another user")
        void shouldThrowExceptionWhenSubmittingExpenseOfAnotherUser() {
            // Given
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            assertThatThrownBy(() -> expenseService.submitForApproval(1L, "otheruser"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("another user");

            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, never()).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should approve expense")
        void shouldApproveExpense() {
            // Given
            testExpense.setExpenseStatus("Submitted");
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
            when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

            // When
            Expense result = expenseService.approveExpense(1L, "admin", "Approved");

            // Then
            assertThat(result).isNotNull();
            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, times(1)).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should throw exception when approving non-submitted expense")
        void shouldThrowExceptionWhenApprovingNonSubmittedExpense() {
            // Given - expense is in Draft status
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            assertThatThrownBy(() -> expenseService.approveExpense(1L, "admin", "Approved"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Submitted or Resubmitted");

            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, never()).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should reject expense")
        void shouldRejectExpense() {
            // Given
            testExpense.setExpenseStatus("Submitted");
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
            when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

            // When
            Expense result = expenseService.rejectExpense(1L, "admin", "Needs more info");

            // Then
            assertThat(result).isNotNull();
            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, times(1)).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should mark expense as reimbursed")
        void shouldMarkExpenseAsReimbursed() {
            // Given
            testExpense.setExpenseStatus("Approved");
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));
            when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

            // When
            Expense result = expenseService.markAsReimbursed(1L, "admin", new BigDecimal("100.00"),
                    "Reimbursed");

            // Then
            assertThat(result).isNotNull();
            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, times(1)).save(any(Expense.class));
        }

        @Test
        @DisplayName("Should throw exception when marking non-approved expense as reimbursed")
        void shouldThrowExceptionWhenMarkingNonApprovedExpenseAsReimbursed() {
            // Given - expense is in Draft status
            when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

            // When/Then
            assertThatThrownBy(() -> expenseService.markAsReimbursed(1L, "admin",
                    new BigDecimal("100.00"), "Reimbursed"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Approved");

            verify(expenseRepository, times(1)).findById(1L);
            verify(expenseRepository, never()).save(any(Expense.class));
        }
    }

    @Nested
    @DisplayName("Query and Filter Tests")
    class QueryAndFilterTests {

        @Test
        @DisplayName("Should get expenses by date")
        void shouldGetExpensesByDate() {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            when(expenseRepository.findExpensesByDate(testDate)).thenReturn(expenses);

            // When
            List<Expense> result = expenseService.getExpensesByDate(testDate);

            // Then
            assertThat(result).hasSize(1);
            verify(expenseRepository, times(1)).findExpensesByDate(testDate);
        }

        @Test
        @DisplayName("Should get expenses in date range")
        void shouldGetExpensesInDateRange() {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            LocalDate startDate = testDate;
            LocalDate endDate = testDate.plusDays(7);
            when(expenseRepository.findExpensesInDateRange(startDate, endDate))
                    .thenReturn(expenses);

            // When
            List<Expense> result = expenseService.getExpensesInDateRange(startDate, endDate);

            // Then
            assertThat(result).hasSize(1);
            verify(expenseRepository, times(1)).findExpensesInDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("Should get expenses by client")
        void shouldGetExpensesByClient() {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            when(expenseRepository.findByClientIgnoreCase("Test Client")).thenReturn(expenses);

            // When
            List<Expense> result = expenseService.getExpensesByClient("Test Client");

            // Then
            assertThat(result).hasSize(1);
            verify(expenseRepository, times(1)).findByClientIgnoreCase("Test Client");
        }

        @Test
        @DisplayName("Should get expenses with pagination")
        void shouldGetExpensesWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Expense> page = new PageImpl<>(Arrays.asList(testExpense));
            when(expenseRepository.findAll(pageable)).thenReturn(page);

            // When
            Page<Expense> result = expenseService.getAllExpenses(pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(expenseRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("Should get expenses with filters")
        void shouldGetExpensesWithFilters() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<Expense> page = new PageImpl<>(Arrays.asList(testExpense));
            when(expenseRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(),
                    any(), any(Pageable.class))).thenReturn(page);

            // When
            ExpenseFilterDto filter = new ExpenseFilterDto("testuser", "Test Client", null, null,
                    "Draft", null, null, null);
            Page<Expense> result = expenseService.getExpensesByFilters(filter, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(expenseRepository, times(1)).findByFilters(any(), any(), any(), any(), any(),
                    any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should get pending approvals")
        void shouldGetPendingApprovals() {
            // Given
            List<Expense> expenses = Arrays.asList(testExpense);
            when(expenseRepository.findPendingApprovals()).thenReturn(expenses);

            // When
            List<Expense> result = expenseService.getPendingApprovals();

            // Then
            assertThat(result).hasSize(1);
            verify(expenseRepository, times(1)).findPendingApprovals();
        }
    }

    @Nested
    @DisplayName("Aggregate Tests")
    class AggregateTests {

        @Test
        @DisplayName("Should get total amount by date")
        void shouldGetTotalAmountByDate() {
            // Given
            when(expenseRepository.getTotalAmountByDate(testDate)).thenReturn(500.00);

            // When
            Double result = expenseService.getTotalAmountByDate(testDate);

            // Then
            assertThat(result).isEqualTo(500.00);
            verify(expenseRepository, times(1)).getTotalAmountByDate(testDate);
        }

        @Test
        @DisplayName("Should return zero when total is null")
        void shouldReturnZeroWhenTotalIsNull() {
            // Given
            when(expenseRepository.getTotalAmountByDate(testDate)).thenReturn(null);

            // When
            Double result = expenseService.getTotalAmountByDate(testDate);

            // Then
            assertThat(result).isEqualTo(0.0);
            verify(expenseRepository, times(1)).getTotalAmountByDate(testDate);
        }

        @Test
        @DisplayName("Should get total amount by user and date range")
        void shouldGetTotalAmountByUserAndDateRange() {
            // Given
            LocalDate startDate = testDate;
            LocalDate endDate = testDate.plusDays(7);
            when(expenseRepository.getTotalAmountByUserAndDateRange("testuser", startDate, endDate))
                    .thenReturn(1500.00);

            // When
            Double result =
                    expenseService.getTotalAmountByUserAndDateRange("testuser", startDate, endDate);

            // Then
            assertThat(result).isEqualTo(1500.00);
            verify(expenseRepository, times(1)).getTotalAmountByUserAndDateRange("testuser",
                    startDate, endDate);
        }

        @Test
        @DisplayName("Should get total amount by user and status")
        void shouldGetTotalAmountByUserAndStatus() {
            // Given
            when(expenseRepository.getTotalAmountByUserAndStatus("testuser", "Draft"))
                    .thenReturn(500.00);

            // When
            Double result = expenseService.getTotalAmountByUserAndStatus("testuser", "Draft");

            // Then
            assertThat(result).isEqualTo(500.00);
            verify(expenseRepository, times(1)).getTotalAmountByUserAndStatus("testuser", "Draft");
        }
    }

    @Nested
    @DisplayName("Utility Tests")
    class UtilityTests {

        @Test
        @DisplayName("Should check if user has expenses")
        void shouldCheckIfUserHasExpenses() {
            // Given
            when(expenseRepository.existsByUsername("testuser")).thenReturn(true);
            when(expenseRepository.existsByUsername("newuser")).thenReturn(false);

            // When
            boolean hasExpenses = expenseService.userHasExpenses("testuser");
            boolean noExpenses = expenseService.userHasExpenses("newuser");

            // Then
            assertThat(hasExpenses).isTrue();
            assertThat(noExpenses).isFalse();
            verify(expenseRepository, times(1)).existsByUsername("testuser");
            verify(expenseRepository, times(1)).existsByUsername("newuser");
        }
    }
}
