package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExpenseRepositoryTest - Unit tests for ExpenseRepository Tests all query methods including basic
 * finders, date-based queries, filters, aggregates, and pagination.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@DataJpaTest
@DisplayName("ExpenseRepository Tests")
class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Expense testExpense1;
    private Expense testExpense2;
    private Expense testExpense3;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2025, 11, 15);

        // Test expense 1 - testuser, Draft
        testExpense1 = new Expense();
        testExpense1.setUsername("testuser");
        testExpense1.setClient("Client A");
        testExpense1.setProject("Project 1");
        testExpense1.setExpenseDate(testDate);
        testExpense1.setExpenseType("Travel - Airfare");
        testExpense1.setDescription("Flight to client site");
        testExpense1.setAmount(new BigDecimal("500.00"));
        testExpense1.setCurrency("USD");
        testExpense1.setPaymentMethod("Corporate Credit Card");
        testExpense1.setExpenseStatus("Draft");
        entityManager.persist(testExpense1);

        // Test expense 2 - testuser, Submitted
        testExpense2 = new Expense();
        testExpense2.setUsername("testuser");
        testExpense2.setClient("Client B");
        testExpense2.setProject("Project 2");
        testExpense2.setExpenseDate(testDate.plusDays(1));
        testExpense2.setExpenseType("Travel - Hotel");
        testExpense2.setDescription("Hotel stay");
        testExpense2.setAmount(new BigDecimal("250.00"));
        testExpense2.setCurrency("USD");
        testExpense2.setPaymentMethod("Personal Credit Card");
        testExpense2.setExpenseStatus("Submitted");
        entityManager.persist(testExpense2);

        // Test expense 3 - otheruser, Approved
        testExpense3 = new Expense();
        testExpense3.setUsername("otheruser");
        testExpense3.setClient("Client A");
        testExpense3.setProject("Project 1");
        testExpense3.setExpenseDate(testDate.plusDays(2));
        testExpense3.setExpenseType("Home Office - Internet");
        testExpense3.setDescription("Monthly internet");
        testExpense3.setAmount(new BigDecimal("75.00"));
        testExpense3.setCurrency("USD");
        testExpense3.setPaymentMethod("Cash");
        testExpense3.setExpenseStatus("Approved");
        entityManager.persist(testExpense3);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should find expenses by username")
    void shouldFindExpensesByUsername() {
        // When
        List<Expense> expenses = expenseRepository.findByUsername("testuser");

        // Then
        assertThat(expenses).hasSize(2);
        assertThat(expenses).extracting(Expense::getUsername).containsOnly("testuser");
    }

    @Test
    @DisplayName("Should check if user has expenses")
    void shouldCheckIfUserHasExpenses() {
        // When
        boolean hasExpenses = expenseRepository.existsByUsername("testuser");
        boolean noExpenses = expenseRepository.existsByUsername("nonexistent");

        // Then
        assertThat(hasExpenses).isTrue();
        assertThat(noExpenses).isFalse();
    }

    @Test
    @DisplayName("Should find expenses by date range")
    void shouldFindExpensesByDateRange() {
        // When
        List<Expense> expenses =
                expenseRepository.findByExpenseDateBetween(testDate, testDate.plusDays(1));

        // Then
        assertThat(expenses).hasSize(2);
        assertThat(expenses).extracting(Expense::getExpenseDate)
                .allMatch(date -> !date.isBefore(testDate) && !date.isAfter(testDate.plusDays(1)));
    }

    @Test
    @DisplayName("Should find expenses by username and date range")
    void shouldFindExpensesByUsernameAndDateRange() {
        // When
        List<Expense> expenses = expenseRepository.findByUsernameAndExpenseDateBetween("testuser",
                testDate, testDate.plusDays(5));

        // Then
        assertThat(expenses).hasSize(2);
        assertThat(expenses).extracting(Expense::getUsername).containsOnly("testuser");
    }

    @Test
    @DisplayName("Should find expenses by client")
    void shouldFindExpensesByClient() {
        // When
        List<Expense> expenses = expenseRepository.findByClientIgnoreCase("client a");

        // Then
        assertThat(expenses).hasSize(2);
        assertThat(expenses).extracting(Expense::getClient).containsOnly("Client A");
    }

    @Test
    @DisplayName("Should find expenses by project")
    void shouldFindExpensesByProject() {
        // When
        List<Expense> expenses = expenseRepository.findByProjectIgnoreCase("project 1");

        // Then
        assertThat(expenses).hasSize(2);
        assertThat(expenses).extracting(Expense::getProject).containsOnly("Project 1");
    }

    @Test
    @DisplayName("Should find expenses by expense type")
    void shouldFindExpensesByExpenseType() {
        // When
        List<Expense> expenses = expenseRepository.findByExpenseTypeIgnoreCase("travel - airfare");

        // Then
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).getExpenseType()).isEqualTo("Travel - Airfare");
    }

    @Test
    @DisplayName("Should find expenses by status")
    void shouldFindExpensesByStatus() {
        // When
        List<Expense> expenses = expenseRepository.findByExpenseStatusIgnoreCase("draft");

        // Then
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).getExpenseStatus()).isEqualTo("Draft");
    }

    @Test
    @DisplayName("Should find expenses by username and status")
    void shouldFindExpensesByUsernameAndStatus() {
        // When
        List<Expense> expenses = expenseRepository.findByUsernameAndStatus("testuser", "Submitted");

        // Then
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).getExpenseStatus()).isEqualTo("Submitted");
        assertThat(expenses.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should find pending approvals")
    void shouldFindPendingApprovals() {
        // Given - create a pending approval expense
        Expense pendingExpense = new Expense();
        pendingExpense.setUsername("testuser");
        pendingExpense.setClient("Client C");
        pendingExpense.setExpenseDate(testDate);
        pendingExpense.setExpenseType("Travel - Meals");
        pendingExpense.setDescription("Team lunch");
        pendingExpense.setAmount(new BigDecimal("100.00"));
        pendingExpense.setCurrency("USD");
        pendingExpense.setPaymentMethod("Cash");
        pendingExpense.setExpenseStatus("Pending Approval");
        entityManager.persist(pendingExpense);
        entityManager.flush();

        // When
        List<Expense> expenses = expenseRepository.findPendingApprovals();

        // Then
        assertThat(expenses).hasSize(1);
        assertThat(expenses.get(0).getExpenseStatus()).isEqualTo("Pending Approval");
    }

    @Test
    @DisplayName("Should calculate total amount by date")
    void shouldCalculateTotalAmountByDate() {
        // When
        Double total = expenseRepository.getTotalAmountByDate(testDate);

        // Then
        assertThat(total).isEqualTo(500.00);
    }

    @Test
    @DisplayName("Should calculate total amount by user and date range")
    void shouldCalculateTotalAmountByUserAndDateRange() {
        // When
        Double total = expenseRepository.getTotalAmountByUserAndDateRange("testuser", testDate,
                testDate.plusDays(5));

        // Then
        assertThat(total).isEqualTo(750.00); // 500 + 250
    }

    @Test
    @DisplayName("Should calculate total amount by user and status")
    void shouldCalculateTotalAmountByUserAndStatus() {
        // When
        Double total = expenseRepository.getTotalAmountByUserAndStatus("testuser", "Draft");

        // Then
        assertThat(total).isEqualTo(500.00);
    }

    @Test
    @DisplayName("Should calculate total amount by client and date range")
    void shouldCalculateTotalAmountByClientAndDateRange() {
        // When
        Double total = expenseRepository.getTotalAmountByClientAndDateRange("Client A", testDate,
                testDate.plusDays(5));

        // Then
        assertThat(total).isEqualTo(575.00); // 500 + 75
    }

    @Test
    @DisplayName("Should return null for aggregate queries with no results")
    void shouldReturnNullForAggregateQueriesWithNoResults() {
        // When
        Double total = expenseRepository.getTotalAmountByDate(LocalDate.of(2020, 1, 1));

        // Then
        assertThat(total).isNull();
    }

    @Test
    @DisplayName("Should find expenses with pagination")
    void shouldFindExpensesWithPagination() {
        // When
        Pageable pageable = PageRequest.of(0, 2);
        Page<Expense> page = expenseRepository.findAll(pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find expenses by username with pagination")
    void shouldFindExpensesByUsernameWithPagination() {
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = expenseRepository.findByUsername("testuser", pageable);

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find expenses with filters")
    void shouldFindExpensesWithFilters() {
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = expenseRepository.findByFilters("testuser", // username
                "Client A", // client
                null, // project
                null, // expenseType
                null, // status
                null, // paymentMethod
                testDate, // startDate
                testDate.plusDays(5), // endDate
                pageable);

        // Then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getClient()).isEqualTo("Client A");
        assertThat(page.getContent().get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should find all expenses when filters are null")
    void shouldFindAllExpensesWhenFiltersAreNull() {
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Expense> page = expenseRepository.findByFilters(null, null, null, null, null, null,
                null, null, pageable);

        // Then
        assertThat(page.getContent()).hasSize(3);
    }
}
