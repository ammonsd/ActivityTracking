package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * ExpenseRepository - Data access layer for Expense entities.
 *
 * @author Dean Ammons
 * @version 2.0
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // Basic finders
    List<Expense> findByUsername(String username);

    boolean existsByUsername(String username);

    // Date-based queries
    List<Expense> findByExpenseDateBetween(LocalDate startDate, LocalDate endDate);

    List<Expense> findByUsernameAndExpenseDateBetween(String username, LocalDate startDate,
            LocalDate endDate);

    // Filter queries
    List<Expense> findByClientIgnoreCase(String client);

    List<Expense> findByProjectIgnoreCase(String project);

    List<Expense> findByExpenseTypeIgnoreCase(String expenseType);

    List<Expense> findByExpenseStatusIgnoreCase(String status);

    List<Expense> findByPaymentMethodIgnoreCase(String paymentMethod);

    // Custom queries with ordering
    @Query("SELECT e FROM Expense e WHERE e.expenseDate = :date "
            + "ORDER BY e.client, e.project, e.expenseType")
    List<Expense> findExpensesByDate(@Param("date") LocalDate date);

    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate "
            + "ORDER BY e.expenseDate DESC, e.client, e.project")
    List<Expense> findExpensesInDateRange(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Status-based queries
    @Query("SELECT e FROM Expense e WHERE e.username = :username "
            + "AND e.expenseStatus = :status ORDER BY e.expenseDate DESC")
    List<Expense> findByUsernameAndStatus(@Param("username") String username,
            @Param("status") String status);

    @Query("SELECT e FROM Expense e WHERE e.expenseStatus IN ('Submitted', 'Pending Approval', 'Resubmitted') "
            + "ORDER BY e.expenseDate ASC")
    List<Expense> findPendingApprovals();

    // Aggregate queries
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.expenseDate = :date")
    Double getTotalAmountByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(e.amount) FROM Expense e "
            + "WHERE e.username = :username AND e.expenseDate BETWEEN :startDate AND :endDate")
    Double getTotalAmountByUserAndDateRange(@Param("username") String username,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(e.amount) FROM Expense e "
            + "WHERE e.username = :username AND e.expenseStatus = :status")
    Double getTotalAmountByUserAndStatus(@Param("username") String username,
            @Param("status") String status);

    // Client-based aggregates
    @Query("SELECT SUM(e.amount) FROM Expense e "
            + "WHERE e.client = :client AND e.expenseDate BETWEEN :startDate AND :endDate")
    Double getTotalAmountByClientAndDateRange(@Param("client") String client,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Pageable versions
    Page<Expense> findAll(Pageable pageable);

    Page<Expense> findByUsername(String username, Pageable pageable);

    Page<Expense> findByUsernameAndExpenseDateBetween(String username, LocalDate startDate,
            LocalDate endDate, Pageable pageable);

    Page<Expense> findByUsernameAndExpenseDate(String username, LocalDate date, Pageable pageable);

    Page<Expense> findByUsernameAndClientIgnoreCase(String username, String client,
            Pageable pageable);

    Page<Expense> findByUsernameAndProjectIgnoreCase(String username, String project,
            Pageable pageable);

    Page<Expense> findByUsernameAndExpenseStatusIgnoreCase(String username, String status,
            Pageable pageable);

    // Flexible filter query (matches TaskActivity pattern)
    @Query(value = "SELECT * FROM expenses e WHERE "
            + "(CAST(:username AS text) IS NULL OR e.username = :username) AND "
            + "(CAST(:client AS text) IS NULL OR e.client = :client) AND "
            + "(CAST(:project AS text) IS NULL OR e.project = :project) AND "
            + "(CAST(:expenseType AS text) IS NULL OR e.expense_type = :expenseType) AND "
            + "(CAST(:status AS text) IS NULL OR e.expense_status = :status) AND "
            + "(CAST(:paymentMethod AS text) IS NULL OR e.payment_method = :paymentMethod) AND "
            + "(CAST(:startDate AS date) IS NULL OR e.expense_date >= CAST(:startDate AS date)) AND "
            + "(CAST(:endDate AS date) IS NULL OR e.expense_date <= CAST(:endDate AS date))",
            nativeQuery = true)
    Page<Expense> findByFilters(@Param("username") String username, @Param("client") String client,
            @Param("project") String project, @Param("expenseType") String expenseType,
            @Param("status") String status, @Param("paymentMethod") String paymentMethod,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // Get distinct usernames who have expenses
    @Query("SELECT DISTINCT e.username FROM Expense e ORDER BY e.username")
    List<String> findDistinctUsernames();
}
