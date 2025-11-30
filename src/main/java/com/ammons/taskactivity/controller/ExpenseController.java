package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.dto.ExpenseFilterDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.exception.ExpenseNotFoundException;
import com.ammons.taskactivity.service.ExpenseService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ExpenseController - REST API Controller for Expense operations
 * 
 * Provides endpoints for expense CRUD operations, filtering, status management, and approval
 * workflow. Implements role-based security to ensure users can only access their own expenses
 * unless they are admins.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // ========== CREATE ==========

    /**
     * Create a new expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(
            @Valid @RequestBody ExpenseDto expenseDto, Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} creating expense", username);

        // Set the username from the authenticated user (security measure)
        expenseDto.setUsername(username);

        Expense createdExpense = expenseService.createExpense(expenseDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", createdExpense));
    }

    // ========== READ ==========

    /**
     * Get all expenses with pagination and filtering - ADMIN: sees all expenses - USER/GUEST: sees
     * only their own expenses
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Expense>>> getAllExpenses(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        logger.info(
                "User {} requesting expenses (admin={}, filters: client={}, project={}, type={}, status={})",
                username, isAdmin, client, project, expenseType, status);

        // Create pageable with sorting (newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expenseDate"));

        // Non-admins can only see their own expenses
        String filterUsername = isAdmin ? null : username;

        // Build filter DTO
        ExpenseFilterDto filter = new ExpenseFilterDto(filterUsername, client, project, expenseType,
                status, paymentMethod, startDate, endDate);

        Page<Expense> expenses = expenseService.getExpensesByFilters(filter, pageable);

        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully", expenses));
    }

    /**
     * Get a single expense by ID - Users can only get their own expenses - Admins can get any
     * expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> getExpenseById(@PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        Optional<Expense> expense = expenseService.getExpenseById(id);

        if (expense.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        // Verify ownership unless admin
        if (!isAdmin && !expense.get().getUsername().equals(username)) {
            logger.warn("User {} attempted to access expense {} owned by {}", username, id,
                    expense.get().getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only view your own expenses", null));
        }

        return ResponseEntity
                .ok(ApiResponse.success("Expense retrieved successfully", expense.get()));
    }

    /**
     * Get expenses for the current week (convenience endpoint)
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/current-week")
    public ResponseEntity<ApiResponse<List<Expense>>> getCurrentWeekExpenses(
            Authentication authentication) {

        String username = authentication.getName();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<Expense> expenses =
                expenseService.getExpensesInDateRangeForUser(username, startOfWeek, endOfWeek);

        return ResponseEntity.ok(ApiResponse.success("Current week expenses retrieved", expenses));
    }

    // ========== UPDATE ==========

    /**
     * Update an existing expense - Users can only update their own expenses - Users cannot modify
     * approval/reimbursement fields - Admins can update any expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(@PathVariable Long id,
            @Valid @RequestBody ExpenseDto expenseDto, Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        logger.info("User {} updating expense {}", username, id);

        // Verify expense exists
        Optional<Expense> existingExpense = expenseService.getExpenseById(id);
        if (existingExpense.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        // Verify ownership unless admin
        if (!isAdmin && !existingExpense.get().getUsername().equals(username)) {
            logger.warn("User {} attempted to update expense {} owned by {}", username, id,
                    existingExpense.get().getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only update your own expenses", null));
        }

        // Non-admins cannot modify protected fields
        if (!isAdmin) {
            if (expenseDto.getApprovedBy() != null || expenseDto.getApprovalDate() != null
                    || expenseDto.getReimbursedAmount() != null
                    || expenseDto.getReimbursementDate() != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse
                        .error("You cannot modify approval or reimbursement fields", null));
            }

            // Non-admins can only set status to Draft, Submitted, or Resubmitted
            if (expenseDto.getExpenseStatus() != null
                    && !expenseDto.getExpenseStatus().equals("Draft")
                    && !expenseDto.getExpenseStatus().equals("Submitted")
                    && !expenseDto.getExpenseStatus().equals("Resubmitted")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(
                        "You can only set status to Draft, Submitted, or Resubmitted", null));
            }
        }

        // Set username from authentication (prevent changing owner)
        expenseDto.setUsername(username);

        Expense updatedExpense = expenseService.updateExpense(id, expenseDto);

        return ResponseEntity
                .ok(ApiResponse.success("Expense updated successfully", updatedExpense));
    }

    // ========== DELETE ==========

    /**
     * Delete an expense - Users can only delete their own expenses - Admins can delete any expense
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(@PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        logger.info("User {} deleting expense {}", username, id);

        // Verify expense exists
        Optional<Expense> expense = expenseService.getExpenseById(id);
        if (expense.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Expense not found", null));
        }

        // Verify ownership unless admin
        if (!isAdmin && !expense.get().getUsername().equals(username)) {
            logger.warn("User {} attempted to delete expense {} owned by {}", username, id,
                    expense.get().getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You can only delete your own expenses", null));
        }

        expenseService.deleteExpense(id);

        return ResponseEntity.ok(ApiResponse.success("Expense deleted successfully", null));
    }

    // ========== STATUS MANAGEMENT ==========

    /**
     * Submit expense for approval
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<Expense>> submitForApproval(@PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} submitting expense {} for approval", username, id);

        try {
            Expense submittedExpense = expenseService.submitForApproval(id, username);
            return ResponseEntity
                    .ok(ApiResponse.success("Expense submitted for approval", submittedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Approve an expense (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Expense>> approveExpense(@PathVariable Long id,
            @RequestParam(required = false) String notes, Authentication authentication) {

        String username = authentication.getName();
        logger.info("Admin {} approving expense {}", username, id);

        try {
            Expense approvedExpense = expenseService.approveExpense(id, username, notes);
            return ResponseEntity.ok(ApiResponse.success("Expense approved", approvedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Reject an expense (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Expense>> rejectExpense(@PathVariable Long id,
            @RequestParam String notes, Authentication authentication) {

        String username = authentication.getName();
        logger.info("Admin {} rejecting expense {}", username, id);

        try {
            Expense rejectedExpense = expenseService.rejectExpense(id, username, notes);
            return ResponseEntity.ok(ApiResponse.success("Expense rejected", rejectedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Mark expense as reimbursed (admin and expense admin)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_ADMIN')")
    @PostMapping("/{id}/reimburse")
    public ResponseEntity<ApiResponse<Expense>> markAsReimbursed(@PathVariable Long id,
                    @RequestParam(required = false) BigDecimal reimbursedAmount,
            @RequestParam(required = false) String notes, Authentication authentication) {

        String username = authentication.getName();
        logger.info("Admin {} marking expense {} as reimbursed", username, id);

        try {
                Expense reimbursedExpense = expenseService.markAsReimbursed(id, username,
                                reimbursedAmount, notes);
            return ResponseEntity
                    .ok(ApiResponse.success("Expense marked as reimbursed", reimbursedExpense));
        } catch (ExpenseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), null));
        }
    }

    /**
     * Get pending approval queue (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending-approvals")
    public ResponseEntity<ApiResponse<List<Expense>>> getPendingApprovals() {

        logger.info("Retrieving pending approval queue");
        List<Expense> pendingExpenses = expenseService.getPendingApprovals();

        return ResponseEntity
                .ok(ApiResponse.success("Pending approvals retrieved", pendingExpenses));
    }

    // ========== AGGREGATE QUERIES ==========

    /**
     * Get total expenses for user in date range
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<Double>> getTotalExpenses(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        String username = authentication.getName();
        Double total =
                expenseService.getTotalAmountByUserAndDateRange(username, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success("Total expenses calculated", total));
    }

    /**
     * Get total expenses by status for user
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/total-by-status")
    public ResponseEntity<ApiResponse<Double>> getTotalByStatus(@RequestParam String status,
            Authentication authentication) {

        String username = authentication.getName();
        Double total = expenseService.getTotalAmountByUserAndStatus(username, status);

        return ResponseEntity.ok(ApiResponse.success("Total expenses by status calculated", total));
    }
}
