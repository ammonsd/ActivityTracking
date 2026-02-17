package com.ammons.taskactivity.service;

import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.dto.ExpenseFilterDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.exception.ExpenseNotFoundException;
import com.ammons.taskactivity.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * ExpenseService - Business logic layer for Expense operations.
 * 
 * Provides comprehensive expense management including CRUD operations, filtering, status tracking,
 * and approval workflow.
 *
 * @author Dean Ammons
 * @version 2.0
 * @since December 2025
 */
@Service
@Transactional
public class ExpenseService {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ExpenseService.class);

    // Status constants
    private static final String STATUS_DRAFT = "Draft";
    private static final String STATUS_SUBMITTED = "Submitted";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_RESUBMITTED = "Resubmitted";
    private static final String STATUS_REIMBURSED = "Reimbursed";

    private final ExpenseRepository expenseRepository;
    private final ReceiptStorageService storageService;
    private final UserService userService;
    private final EmailService emailService;

    public ExpenseService(ExpenseRepository expenseRepository,
            ReceiptStorageService storageService, UserService userService,
            EmailService emailService) {
        this.expenseRepository = expenseRepository;
        this.storageService = storageService;
        this.userService = userService;
        this.emailService = emailService;
    }

    // ========== CRUD Operations ==========

    /**
     * Create a new expense
     */
    public Expense createExpense(ExpenseDto expenseDto) {
        Expense expense = convertDtoToEntity(expenseDto);
        expense.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(expenseDto.getUsername());
        return expenseRepository.save(expense);
    }

    /**
     * Create a new expense with receipt file
     */
    public Expense createExpenseWithReceipt(ExpenseDto expenseDto, MultipartFile receiptFile)
            throws IOException {
        Expense expense = convertDtoToEntity(expenseDto);
        expense.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(expenseDto.getUsername());

        // Save the expense first to get an ID
        expense = expenseRepository.save(expense);

        // Handle receipt upload
        if (receiptFile != null && !receiptFile.isEmpty()) {
            // Store receipt using the configured storage service (S3 or local)
            String receiptPath = storageService.storeReceipt(receiptFile, expense.getUsername(),
                    expense.getId());

            // Update expense with receipt info
            expense.setReceiptPath(receiptPath);
            expense.setReceiptStatus("Receipt Attached");
            expense = expenseRepository.save(expense);
        }

        return expense;
    }

    /**
     * Get all expenses (admin only)
     */
    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    /**
     * Get expenses for a specific user
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByUsername(String username) {
        return expenseRepository.findByUsername(username);
    }

    /**
     * Get a single expense by ID
     */
    @Transactional(readOnly = true)
    public Optional<Expense> getExpenseById(Long id) {
        return expenseRepository.findById(id);
    }

    /**
     * Update an existing expense
     */
    public Expense updateExpense(Long id, ExpenseDto expenseDto) {
        Optional<Expense> existingExpense = expenseRepository.findById(id);
        if (existingExpense.isPresent()) {
            Expense expense = existingExpense.get();
            updateEntityFromDto(expense, expenseDto);
            expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
            expense.setLastModifiedBy(expenseDto.getUsername());
            return expenseRepository.save(expense);
        }
        throw new ExpenseNotFoundException(id);
    }

    /**
     * Update an existing expense with a new receipt file
     */
    public Expense updateExpenseWithReceipt(Long id, ExpenseDto expenseDto,
            MultipartFile receiptFile) throws IOException {
        Optional<Expense> existingExpense = expenseRepository.findById(id);
        if (existingExpense.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = existingExpense.get();

        // Save old receipt path before updating entity
        String oldReceiptPath = expense.getReceiptPath();

        updateEntityFromDto(expense, expenseDto);

        // Handle receipt upload
        if (receiptFile != null && !receiptFile.isEmpty()) {
            // Delete old receipt file if it exists
            if (oldReceiptPath != null && !oldReceiptPath.isEmpty()) {
                try {
                    storageService.deleteReceipt(oldReceiptPath);
                    logger.info("Deleted old receipt file: {}", oldReceiptPath);
                } catch (IOException e) {
                    logger.warn("Failed to delete old receipt file: {}", oldReceiptPath, e);
                    // Continue with upload even if deletion fails
                }
            }

            // Store receipt using the configured storage service (S3 or local)
            String receiptPath =
                    storageService.storeReceipt(receiptFile, expense.getUsername(), id);

            // Update expense with receipt info
            expense.setReceiptPath(receiptPath);
            expense.setReceiptStatus("Receipt Attached");
        }

        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(expenseDto.getUsername());
        return expenseRepository.save(expense);
    }

    /**
     * Save an expense entity directly
     */
    public Expense saveExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    /**
     * Delete an expense and its associated receipt file
     */
    public void deleteExpense(Long id) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Delete associated receipt file if it exists
        if (expense.getReceiptPath() != null && !expense.getReceiptPath().isEmpty()) {
            try {
                storageService.deleteReceipt(expense.getReceiptPath());
                logger.info("Deleted receipt file: {}", expense.getReceiptPath());
            } catch (IOException e) {
                logger.warn("Failed to delete receipt file: {}", expense.getReceiptPath(), e);
                // Continue with expense deletion even if receipt file deletion fails
            }
        }

        // Delete the expense record
        expenseRepository.deleteById(id);
    }

    // ========== Date-Based Queries ==========

    /**
     * Get expenses for a specific date
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByDate(LocalDate date) {
        return expenseRepository.findExpensesByDate(date);
    }

    /**
     * Get expenses within a date range
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesInDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findExpensesInDateRange(startDate, endDate);
    }

    /**
     * Get expenses within a date range for a specific user
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesInDateRangeForUser(String username, LocalDate startDate,
            LocalDate endDate) {
        return expenseRepository.findByUsernameAndExpenseDateBetween(username, startDate, endDate);
    }

    // ========== Filter Queries ==========

    /**
     * Get expenses by client
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByClient(String client) {
        return expenseRepository.findByClientIgnoreCase(client);
    }

    /**
     * Get expenses by project
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByProject(String project) {
        return expenseRepository.findByProjectIgnoreCase(project);
    }

    /**
     * Get expenses by type
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByType(String expenseType) {
        return expenseRepository.findByExpenseTypeIgnoreCase(expenseType);
    }

    /**
     * Get expenses by status
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByStatus(String status) {
        return expenseRepository.findByExpenseStatusIgnoreCase(status);
    }

    /**
     * Get expenses by username and status
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByUsernameAndStatus(String username, String status) {
        return expenseRepository.findByUsernameAndStatus(username, status);
    }

    /**
     * Get all pending approval expenses (admin/manager use)
     */
    @Transactional(readOnly = true)
    public List<Expense> getPendingApprovals() {
        return expenseRepository.findPendingApprovals();
    }

    // ========== Aggregate Queries ==========

    /**
     * Get total amount for a specific date
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByDate(LocalDate date) {
        Double total = expenseRepository.getTotalAmountByDate(date);
        return total != null ? total : 0.0;
    }

    /**
     * Get total amount for user within date range
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByUserAndDateRange(String username, LocalDate startDate,
            LocalDate endDate) {
        Double total =
                expenseRepository.getTotalAmountByUserAndDateRange(username, startDate, endDate);
        return total != null ? total : 0.0;
    }

    /**
     * Get total amount by user and status
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByUserAndStatus(String username, String status) {
        Double total = expenseRepository.getTotalAmountByUserAndStatus(username, status);
        return total != null ? total : 0.0;
    }

    /**
     * Get total amount by client within date range
     */
    @Transactional(readOnly = true)
    public Double getTotalAmountByClientAndDateRange(String client, LocalDate startDate,
            LocalDate endDate) {
        Double total =
                expenseRepository.getTotalAmountByClientAndDateRange(client, startDate, endDate);
        return total != null ? total : 0.0;
    }

    // ========== Pageable Queries ==========

    @Transactional(readOnly = true)
    public Page<Expense> getAllExpenses(Pageable pageable) {
        return expenseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsername(String username, Pageable pageable) {
        return expenseRepository.findByUsername(username, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndDate(String username, LocalDate date,
            Pageable pageable) {
        return expenseRepository.findByUsernameAndExpenseDate(username, date, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndDateRange(String username, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        return expenseRepository.findByUsernameAndExpenseDateBetween(username, startDate, endDate,
                pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndClient(String username, String client,
            Pageable pageable) {
        return expenseRepository.findByUsernameAndClientIgnoreCase(username, client, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndProject(String username, String project,
            Pageable pageable) {
        return expenseRepository.findByUsernameAndProjectIgnoreCase(username, project, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByUsernameAndStatus(String username, String status,
            Pageable pageable) {
        return expenseRepository.findByUsernameAndExpenseStatusIgnoreCase(username, status,
                pageable);
    }

    /**
     * Modified by: Dean Ammons - February 2026 Change: Added authenticatedUser parameter for draft
     * expense access control Reason: Enforce security policy that draft expenses are only visible
     * to their owner
     */
    /**
     * Get expenses with flexible filters and pagination. Draft expenses are only visible to the
     * authenticated user.
     * 
     * @param authenticatedUser the currently authenticated user (for draft filtering)
     * @param filter the filter criteria
     * @param pageable pagination information
     * @return page of expenses
     */
    @Transactional(readOnly = true)
    public Page<Expense> getExpensesByFilters(String authenticatedUser, ExpenseFilterDto filter,
            Pageable pageable) {
        return expenseRepository.findByFilters(authenticatedUser, filter.getUsername(),
                filter.getClient(), filter.getProject(), filter.getExpenseType(),
                filter.getStatus(), filter.getPaymentMethod(), filter.getStartDate(),
                filter.getEndDate(), pageable);
    }

    /**
     * Get all distinct usernames who have expenses.
     *
     * @return List of usernames
     */
    public List<String> getUsernamesWithExpenses() {
        return expenseRepository.findDistinctUsernames();
    }

    // ========== Status Management ==========

    /**
     * Submit expense for approval (changes status from Draft to Submitted)
     */
    public Expense submitForApproval(Long id, String username) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Validate user owns this expense
        if (!expense.getUsername().equals(username)) {
            throw new IllegalStateException("Cannot submit expense that belongs to another user");
        }

        // Validate current status allows submission
        if (!STATUS_DRAFT.equals(expense.getExpenseStatus())
                && !STATUS_REJECTED.equals(expense.getExpenseStatus())) {
            throw new IllegalStateException("Only Draft or Rejected expenses can be submitted");
        }

        expense.setExpenseStatus(STATUS_SUBMITTED);
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(username);

        return expenseRepository.save(expense);
    }

    /**
     * Submit an expense for approval (changes status from Draft to Submitted)
     */
    public Expense submitExpense(Long id) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Validate current status allows submission
        if (!STATUS_DRAFT.equals(expense.getExpenseStatus())
                && !"Rejected".equalsIgnoreCase(expense.getExpenseStatus())) {
            throw new IllegalStateException("Only Draft or Rejected expenses can be submitted");
        }

        // Determine if this is initial submission or resubmission
        if ("Rejected".equalsIgnoreCase(expense.getExpenseStatus())) {
            expense.setExpenseStatus(STATUS_RESUBMITTED);
        } else {
            expense.setExpenseStatus(STATUS_SUBMITTED);
        }

        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(expense.getUsername());

        Expense savedExpense = expenseRepository.save(expense);

        // Send email notification to expense approvers
        try {
            Optional<User> userOpt = userService.getUserByUsername(expense.getUsername());
            String fullName = "";
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                fullName = (user.getFirstname() != null ? user.getFirstname() : "") + " "
                        + (user.getLastname() != null ? user.getLastname() : "");
                fullName = fullName.trim();
            }

            emailService.sendExpenseSubmittedNotification(expense.getUsername(), fullName,
                    expense.getId(),
                    expense.getDescription() != null ? expense.getDescription() : "No description",
                    expense.getAmount() != null ? expense.getAmount().toString() : "0.00",
                    expense.getCurrency() != null ? expense.getCurrency() : "USD",
                    expense.getExpenseDate() != null ? expense.getExpenseDate().toString()
                            : "Unknown");

            logger.info("Expense submission notification sent for expense ID: {}", expense.getId());
        } catch (Exception e) {
            logger.error("Failed to send expense submission notification for expense {}: {}",
                    expense.getId(), e.getMessage(), e);
            // Don't fail the submission if email fails
        }

        return savedExpense;
    }

    /**
     * Approve an expense (admin/manager only)
     */
    public Expense approveExpense(Long id, String approverUsername, String approvalNotes) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Prevent users from approving their own expenses
        if (expense.getUsername().equals(approverUsername)) {
            throw new IllegalStateException("You cannot approve your own expense");
        }

        // Validate current status allows approval
        if (!STATUS_SUBMITTED.equals(expense.getExpenseStatus())
                && !STATUS_RESUBMITTED.equals(expense.getExpenseStatus())) {
            throw new IllegalStateException(
                    "Only Submitted or Resubmitted expenses can be approved");
        }

        expense.setExpenseStatus(STATUS_APPROVED);
        expense.setApprovedBy(approverUsername);
        expense.setApprovalDate(LocalDate.now());
        expense.setApprovalNotes(approvalNotes);
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(approverUsername);

        Expense savedExpense = expenseRepository.save(expense);

        // Send email notification to expense owner
        try {
            Optional<User> userOpt = userService.getUserByUsername(expense.getUsername());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String fullName = (user.getFirstname() != null ? user.getFirstname() : "") + " "
                            + (user.getLastname() != null ? user.getLastname() : "");
                    fullName = fullName.trim();

                    // Get processor's full name
                    String processedByName = approverUsername;
                    Optional<User> processorOpt = userService.getUserByUsername(approverUsername);
                    if (processorOpt.isPresent()) {
                        User processor = processorOpt.get();
                        String processorFullName =
                                (processor.getFirstname() != null ? processor.getFirstname() : "")
                                        + " "
                                        + (processor.getLastname() != null ? processor.getLastname()
                                                : "");
                        processorFullName = processorFullName.trim();
                        if (!processorFullName.isEmpty()) {
                            processedByName = processorFullName;
                        }
                    }

                    emailService.sendExpenseStatusNotification(user.getEmail(), user.getUsername(),
                            fullName, savedExpense.getId(), savedExpense.getDescription(),
                            savedExpense.getAmount().toString(), savedExpense.getCurrency(),
                            "Approved", approvalNotes, processedByName);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send approval email notification for expense {}: {}",
                    savedExpense.getId(), e.getMessage());
            // Don't fail the approval if email fails
        }

        return savedExpense;
    }

    /**
     * Reject an expense (admin/manager only)
     */
    public Expense rejectExpense(Long id, String approverUsername, String rejectionNotes) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Prevent users from rejecting their own expenses
        if (expense.getUsername().equals(approverUsername)) {
            throw new IllegalStateException("You cannot reject your own expense");
        }

        // Validate current status allows rejection
        if (!STATUS_SUBMITTED.equals(expense.getExpenseStatus())
                && !STATUS_RESUBMITTED.equals(expense.getExpenseStatus())) {
            throw new IllegalStateException(
                    "Only Submitted or Resubmitted expenses can be rejected");
        }

        expense.setExpenseStatus(STATUS_REJECTED);
        expense.setApprovedBy(approverUsername);
        expense.setApprovalDate(LocalDate.now());
        expense.setApprovalNotes(rejectionNotes);
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(approverUsername);

        Expense savedExpense = expenseRepository.save(expense);

        // Send email notification to expense owner
        try {
            Optional<User> userOpt = userService.getUserByUsername(expense.getUsername());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String fullName = (user.getFirstname() != null ? user.getFirstname() : "") + " "
                            + (user.getLastname() != null ? user.getLastname() : "");
                    fullName = fullName.trim();

                    // Get processor's full name
                    String processedByName = approverUsername;
                    Optional<User> processorOpt = userService.getUserByUsername(approverUsername);
                    if (processorOpt.isPresent()) {
                        User processor = processorOpt.get();
                        String processorFullName =
                                (processor.getFirstname() != null ? processor.getFirstname() : "")
                                        + " "
                                        + (processor.getLastname() != null ? processor.getLastname()
                                                : "");
                        processorFullName = processorFullName.trim();
                        if (!processorFullName.isEmpty()) {
                            processedByName = processorFullName;
                        }
                    }

                    emailService.sendExpenseStatusNotification(user.getEmail(), user.getUsername(),
                            fullName, savedExpense.getId(), savedExpense.getDescription(),
                            savedExpense.getAmount().toString(), savedExpense.getCurrency(),
                            "Rejected", rejectionNotes, processedByName);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send rejection email notification for expense {}: {}",
                    savedExpense.getId(), e.getMessage());
            // Don't fail the rejection if email fails
        }

        return savedExpense;
    }

    /**
     * Mark expense as reimbursed (admin only)
     */
    public Expense markAsReimbursed(Long id, String adminUsername, BigDecimal reimbursedAmount,
            String notes) {
        Optional<Expense> expenseOpt = expenseRepository.findById(id);
        if (expenseOpt.isEmpty()) {
            throw new ExpenseNotFoundException(id);
        }

        Expense expense = expenseOpt.get();

        // Validate current status allows reimbursement
        if (!STATUS_APPROVED.equals(expense.getExpenseStatus())) {
            throw new IllegalStateException("Only Approved expenses can be marked as reimbursed");
        }

        expense.setExpenseStatus(STATUS_REIMBURSED);
        expense.setReimbursedAmount(
                reimbursedAmount != null ? reimbursedAmount : expense.getAmount());
        expense.setReimbursementDate(LocalDate.now());
        expense.setReimbursementNotes(notes);
        expense.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        expense.setLastModifiedBy(adminUsername);

        Expense savedExpense = expenseRepository.save(expense);

        // Send email notification to expense owner
        try {
            Optional<User> userOpt = userService.getUserByUsername(expense.getUsername());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String fullName = (user.getFirstname() != null ? user.getFirstname() : "") + " "
                            + (user.getLastname() != null ? user.getLastname() : "");
                    fullName = fullName.trim();

                    // Get processor's full name
                    String processedByName = adminUsername;
                    Optional<User> processorOpt = userService.getUserByUsername(adminUsername);
                    if (processorOpt.isPresent()) {
                        User processor = processorOpt.get();
                        String processorFullName =
                                (processor.getFirstname() != null ? processor.getFirstname() : "")
                                        + " "
                                        + (processor.getLastname() != null ? processor.getLastname()
                                                : "");
                        processorFullName = processorFullName.trim();
                        if (!processorFullName.isEmpty()) {
                            processedByName = processorFullName;
                        }
                    }

                    BigDecimal finalAmount =
                            expense.getReimbursedAmount() != null ? expense.getReimbursedAmount()
                                    : expense.getAmount();

                    emailService.sendExpenseStatusNotification(user.getEmail(), user.getUsername(),
                            fullName, savedExpense.getId(), savedExpense.getDescription(),
                            finalAmount.toString(), savedExpense.getCurrency(), "Reimbursed", notes,
                            processedByName);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send reimbursement email notification for expense {}: {}",
                    savedExpense.getId(), e.getMessage());
            // Don't fail the reimbursement if email fails
        }

        return savedExpense;
    }

    // ========== Utility Methods ==========

    /**
     * Check if a user has any expenses
     */
    @Transactional(readOnly = true)
    public boolean userHasExpenses(String username) {
        return expenseRepository.existsByUsername(username);
    }

    // ========== DTO Conversion ==========

    /**
     * Convert DTO to Entity
     */
    private Expense convertDtoToEntity(ExpenseDto dto) {
        Expense entity = new Expense();
        entity.setUsername(dto.getUsername());
        entity.setClient(dto.getClient());
        entity.setProject(dto.getProject());
        entity.setExpenseDate(dto.getExpenseDate());
        entity.setExpenseType(dto.getExpenseType());
        entity.setDescription(dto.getDescription());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setVendor(dto.getVendor());
        entity.setReferenceNumber(dto.getReferenceNumber());
        // Modified by: Dean Ammons - February 2026
        // Change: Ignore client-provided receipt path/status on create
        // Reason: Prevent object key/path injection and enforce server-managed receipt metadata
        entity.setReceiptPath(null);
        entity.setReceiptStatus("Receipt Missing");
        entity.setExpenseStatus(
                dto.getExpenseStatus() != null ? dto.getExpenseStatus() : STATUS_DRAFT);
        entity.setApprovedBy(dto.getApprovedBy());
        entity.setApprovalDate(dto.getApprovalDate());
        entity.setApprovalNotes(dto.getApprovalNotes());
        entity.setReimbursedAmount(dto.getReimbursedAmount());
        entity.setReimbursementDate(dto.getReimbursementDate());
        entity.setReimbursementNotes(dto.getReimbursementNotes());
        entity.setNotes(dto.getNotes());
        return entity;
    }

    /**
     * Update Entity from DTO (username is immutable after creation)
     */
    private void updateEntityFromDto(Expense entity, ExpenseDto dto) {
        entity.setClient(dto.getClient());
        entity.setProject(dto.getProject());
        entity.setExpenseDate(dto.getExpenseDate());
        entity.setExpenseType(dto.getExpenseType());
        entity.setDescription(dto.getDescription());
        entity.setAmount(dto.getAmount());
        entity.setCurrency(dto.getCurrency());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setVendor(dto.getVendor());
        entity.setReferenceNumber(dto.getReferenceNumber());
        // Receipt fields are managed separately during file upload
        // entity.setReceiptPath(dto.getReceiptPath());
        // entity.setReceiptStatus(dto.getReceiptStatus());
        entity.setExpenseStatus(dto.getExpenseStatus());
        entity.setApprovedBy(dto.getApprovedBy());
        entity.setApprovalDate(dto.getApprovalDate());
        entity.setApprovalNotes(dto.getApprovalNotes());
        entity.setReimbursedAmount(dto.getReimbursedAmount());
        entity.setReimbursementDate(dto.getReimbursementDate());
        entity.setReimbursementNotes(dto.getReimbursementNotes());
        entity.setNotes(dto.getNotes());
        // Username is NOT updated - it remains the original creator
    }
}
