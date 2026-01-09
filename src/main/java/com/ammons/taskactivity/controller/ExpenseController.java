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
import com.ammons.taskactivity.security.RequirePermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * @since December 2025
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseController.class);
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String EXPENSE_NOT_FOUND = "Expense not found";
    private static final String EMAIL_REQUIRED_MESSAGE =
                    "Email address is required to access expense features. Please update your profile with a valid email address.";

    private final ExpenseService expenseService;
    private final com.ammons.taskactivity.service.UserService userService;

    public ExpenseController(ExpenseService expenseService,
                    com.ammons.taskactivity.service.UserService userService) {
        this.expenseService = expenseService;
        this.userService = userService;
}

/**
 * Helper method to check if user has email for expense operations
 */
private void validateUserHasEmail(String username) {
        if (!userService.userHasEmail(username)) {
                logger.warn("User {} attempted to access expense feature without email", username);
                throw new IllegalStateException(EMAIL_REQUIRED_MESSAGE);
        }
}

/**
 * Check if current user can access expense features
 */
@RequirePermission(resource = "EXPENSE", action = "READ")
@GetMapping("/can-access")
public ResponseEntity<ApiResponse<Boolean>> canAccessExpenses(Authentication authentication) {
        String username = authentication.getName();
        boolean hasEmail = userService.userHasEmail(username);

        String message = hasEmail ? "User can access expense features"
                        : "Email address required to access expense features";

        return ResponseEntity.ok(ApiResponse.success(message, hasEmail));
    }

    // ========== CREATE ==========

    /**
     * Create a new expense. Requires the user to have a valid email address. The username is
     * automatically set from the authenticated user.
     * 
     * @param expenseDto the expense data transfer object containing expense details
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the created expense
     */
    @RequirePermission(resource = "EXPENSE", action = "CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(
            @Valid @RequestBody ExpenseDto expenseDto, Authentication authentication) {

        String username = authentication.getName();
        logger.info("User {} creating expense", username);

        // Check if user has email address
        validateUserHasEmail(username);

        // Set the username from the authenticated user (security measure)
        expenseDto.setUsername(username);

        Expense createdExpense = expenseService.createExpense(expenseDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Expense created successfully", createdExpense));
    }

    // ========== READ ==========

    /**
     * Get all expenses with pagination and filtering. ADMIN and EXPENSE_ADMIN users can see all
     * expenses and filter by username. Regular users can only see their own expenses.
     * 
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @param client optional filter by client
     * @param project optional filter by project
     * @param expenseType optional filter by expense type
     * @param status optional filter by status
     * @param paymentMethod optional filter by payment method
     * @param startDate optional filter by start date
     * @param endDate optional filter by end date
     * @param username optional filter by username (admin only)
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing a paginated list of expenses
     */
    @RequirePermission(resource = "EXPENSE", action = "READ")
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
                    @RequestParam(required = false) String username,
            Authentication authentication) {

            String authenticatedUsername = authentication.getName();
            validateUserHasEmail(authenticatedUsername);

            boolean isAdminOrExpenseAdmin = authentication.getAuthorities().stream()
                            .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN)
                                            || auth.getAuthority().equals("ROLE_EXPENSE_ADMIN"));

        logger.info(
                        "User {} requesting expenses (admin={}, filters: client={}, project={}, type={}, status={}, username={})",
                        authenticatedUsername, isAdminOrExpenseAdmin, client, project, expenseType,
                        status, username);

        // Create pageable with sorting (newest first)
        // Use database column name for native query
        Pageable pageable =
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "expense_date"));

        // Determine the username filter:
        // - For ADMIN/EXPENSE_ADMIN: use provided username filter (can be null to see all)
        // - For non-admins: always filter to their own username
        String filterUsername;
        if (isAdminOrExpenseAdmin) {
                filterUsername = username; // Can be null to see all users
        } else {
                filterUsername = authenticatedUsername; // Always filter to own expenses
        }

        // Build filter DTO
        ExpenseFilterDto filter = new ExpenseFilterDto(filterUsername, client, project, expenseType,
                status, paymentMethod, startDate, endDate);

        Page<Expense> expenses = expenseService.getExpensesByFilters(filter, pageable);

        return ResponseEntity.ok(ApiResponse.success("Expenses retrieved successfully", expenses));
    }

    /**
     * Get a single expense by ID. Users can only view their own expenses unless they are admins.
     * 
     * @param id the expense ID
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the expense if found and authorized
     */
    @RequirePermission(resource = "EXPENSE", action = "READ")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> getExpenseById(@PathVariable Long id,
            Authentication authentication) {

        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_ADMIN));

        Optional<Expense> expense = expenseService.getExpenseById(id);

        if (expense.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(EXPENSE_NOT_FOUND, null));
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
     * Get expenses for the current week (convenience endpoint). Returns all expenses for the
     * authenticated user from Monday to Sunday of the current week.
     * 
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing list of current week expenses
     */
    @RequirePermission(resource = "EXPENSE", action = "READ")
    @GetMapping("/current-week")
    public ResponseEntity<ApiResponse<List<Expense>>> getCurrentWeekExpenses(
            Authentication authentication) {

        String username = authentication.getName();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays((long) today.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        List<Expense> expenses =
                expenseService.getExpensesInDateRangeForUser(username, startOfWeek, endOfWeek);

        return ResponseEntity.ok(ApiResponse.success("Current week expenses retrieved", expenses));
    }

    // ========== UPDATE ==========

    /**
     * Update an existing expense. Users can only update their own expenses and cannot modify
     * approval/reimbursement fields. Non-admins can only set status to Draft, Submitted, or
     * Resubmitted. Admins can update any expense and all fields.
     * 
     * @param id the expense ID to update
     * @param expenseDto the updated expense data
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the updated expense
     */
    @RequirePermission(resource = "EXPENSE", action = "UPDATE")
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
                            .body(ApiResponse.error(EXPENSE_NOT_FOUND, null));
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
     * Delete an expense. Users can only delete their own expenses. Admins can delete any expense.
     * 
     * @param id the expense ID to delete
     * @param authentication the authenticated user making the request
     * @return ResponseEntity with success message
     */
    @RequirePermission(resource = "EXPENSE", action = "DELETE")
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
                            .body(ApiResponse.error(EXPENSE_NOT_FOUND, null));
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
     * Submit an expense for approval. Changes the expense status to "Submitted" or "Resubmitted"
     * and sends notification email.
     * 
     * @param id the expense ID to submit
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the submitted expense
     */
    @RequirePermission(resource = "EXPENSE", action = "SUBMIT")
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
     * Approve an expense (admin only). Changes the expense status to "Approved" and records the
     * approver and date. Sends notification email to the expense owner.
     * 
     * @param id the expense ID to approve
     * @param notes optional approval notes
     * @param authentication the authenticated admin making the request
     * @return ResponseEntity containing the approved expense
     */
    @RequirePermission(resource = "EXPENSE", action = "APPROVE")
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
     * Reject an expense (admin only). Changes the expense status to "Rejected" and records the
     * reason. Sends notification email to the expense owner.
     * 
     * @param id the expense ID to reject
     * @param notes required rejection reason/notes
     * @param authentication the authenticated admin making the request
     * @return ResponseEntity containing the rejected expense
     */
    @RequirePermission(resource = "EXPENSE", action = "REJECT")
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
     * Mark an expense as reimbursed. Changes the expense status to "Reimbursed" and records the
     * reimbursement details. Sends notification email to the expense owner.
     * 
     * @param id the expense ID to mark as reimbursed
     * @param reimbursedAmount the amount reimbursed (optional, defaults to expense amount)
     * @param notes optional reimbursement notes
     * @param authentication the authenticated admin making the request
     * @return ResponseEntity containing the updated expense
     */
    @RequirePermission(resource = "EXPENSE", action = "MARK_REIMBURSED")
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
     * Get pending approval queue (admin only). Returns all expenses with "Submitted" or
     * "Resubmitted" status.
     * 
     * @return ResponseEntity containing list of expenses pending approval
     */
    @RequirePermission(resource = "EXPENSE", action = "READ_ALL")
    @GetMapping("/pending-approvals")
    public ResponseEntity<ApiResponse<List<Expense>>> getPendingApprovals() {

        logger.info("Retrieving pending approval queue");
        List<Expense> pendingExpenses = expenseService.getPendingApprovals();

        return ResponseEntity
                .ok(ApiResponse.success("Pending approvals retrieved", pendingExpenses));
    }

    // ========== AGGREGATE QUERIES ==========

    /**
     * Get total expenses for user in date range. Calculates the sum of all expense amounts for the
     * authenticated user within the specified date range.
     * 
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the total expense amount
     */
    @RequirePermission(resource = "EXPENSE", action = "READ")
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
     * Get total expenses by status for user. Calculates the sum of all expense amounts for the
     * authenticated user with the specified status.
     * 
     * @param status the expense status to filter by
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the total expense amount for the status
     */
    @RequirePermission(resource = "EXPENSE", action = "READ")
    @GetMapping("/total-by-status")
    public ResponseEntity<ApiResponse<Double>> getTotalByStatus(@RequestParam String status,
            Authentication authentication) {

        String username = authentication.getName();
        Double total = expenseService.getTotalAmountByUserAndStatus(username, status);

        return ResponseEntity.ok(ApiResponse.success("Total expenses by status calculated", total));
    }
}


