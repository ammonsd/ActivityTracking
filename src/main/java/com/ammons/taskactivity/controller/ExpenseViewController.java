package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.service.ExpenseService;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.service.ReceiptStorageService;
import com.ammons.taskactivity.service.BillabilityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.ammons.taskactivity.security.RequirePermission;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MVC Controller for Expense web views and forms. Handles expense management UI including forms,
 * lists, approval workflow, and receipt handling. Implements role-based access control to ensure
 * users can only manage their own expenses unless they have admin privileges.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Controller
@RequestMapping("/expenses")
public class ExpenseViewController {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(ExpenseViewController.class);

    // View names
    private static final String EXPENSE_LIST_VIEW = "expense-list";
    private static final String EXPENSE_FORM_VIEW = "expense-form";
    private static final String EXPENSE_DETAIL_VIEW = "expense-detail";
    private static final String EXPENSE_SHEET_VIEW = "expense-sheet";
    private static final String APPROVAL_QUEUE_VIEW = "admin/expense-approval-queue";
    private static final String REDIRECT_EXPENSE_LIST = "redirect:/expenses/list";

    // Model attributes
    private static final String ERROR_MESSAGE_ATTR = "errorMessage";
    private static final String SUCCESS_MESSAGE_ATTR = "successMessage";
    private static final String EXPENSE_DTO_ATTR = "expenseDto";
    private static final String EXPENSE_ID_ATTR = "expenseId";
    private static final String IS_EDIT_ATTR = "isEdit";

    // Common messages
    private static final String EXPENSE_NOT_FOUND = "Expense not found";
    private static final String STATUS_DRAFT = "Draft";
    private static final String EMAIL_REQUIRED_MESSAGE =
            "Email address is required to access expense features. Please update your profile with a valid email address.";

    private final ExpenseService expenseService;
    private final DropdownValueService dropdownValueService;
    private final UserService userService;
    private final ReceiptStorageService storageService;
    private final BillabilityService billabilityService;

    @Value("${app.upload.max-file-size}")
    private long maxFileSize;

    public ExpenseViewController(ExpenseService expenseService,
            DropdownValueService dropdownValueService, UserService userService,
            ReceiptStorageService storageService, BillabilityService billabilityService) {
        this.expenseService = expenseService;
        this.dropdownValueService = dropdownValueService;
        this.userService = userService;
        this.storageService = storageService;
        this.billabilityService = billabilityService;
    }

    /**
     * Helper method to check if user has email for expense operations
     */
    private void validateUserHasEmail(String username, RedirectAttributes redirectAttributes) {
        if (!userService.userHasEmail(username)) {
            logger.warn("User {} attempted to access expense feature without email", username);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, EMAIL_REQUIRED_MESSAGE);
            throw new IllegalStateException(EMAIL_REQUIRED_MESSAGE);
        }
    }

    /**
     * Helper method to add file upload configuration to the model
     */
    private void addFileUploadConfig(Model model) {
        long maxFileSizeMB = maxFileSize / (1024 * 1024);
        model.addAttribute("maxFileSize", maxFileSize);
        model.addAttribute("maxFileSizeMB", maxFileSizeMB);
    }

    @GetMapping
    public String showMain(Authentication authentication) {
        return REDIRECT_EXPENSE_LIST;
    }

    @GetMapping("/list")
    public String showExpenseList(@RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page, Model model,
            Authentication authentication, RedirectAttributes redirectAttributes) {

        addUserInfo(model, authentication);

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "expense_date"));
        boolean canViewAllExpenses = canApproveExpenses(authentication);
        String currentUsername = canViewAllExpenses ? null : authentication.getName();
        String filterUsername =
                (canViewAllExpenses && username != null && !username.isEmpty()) ? username
                        : currentUsername;

        // Convert empty strings to null for proper SQL filtering
        String filterClient = emptyToNull(client);
        String filterProject = emptyToNull(project);
        String filterExpenseType = emptyToNull(expenseType);
        String filterStatus = emptyToNull(status);

        com.ammons.taskactivity.dto.ExpenseFilterDto filter =
                new com.ammons.taskactivity.dto.ExpenseFilterDto(filterUsername, filterClient,
                        filterProject, filterExpenseType, filterStatus, null, startDate, endDate);

        /**
         * Modified by: Dean Ammons - February 2026 Change: Pass authenticated username to service
         * for draft expense filtering Reason: Enforce that draft expenses are only visible to their
         * owner, preventing ADMIN/EXPENSE_ADMIN from accessing other users' draft expenses
         */
        // Always pass authenticated username for draft filtering
        // Draft expenses are ONLY visible to their owner (even for admins)
        String authenticatedUsername = authentication.getName();
        Page<Expense> expensesPage =
                expenseService.getExpensesByFilters(authenticatedUsername, filter, pageable);

        // Create a map to track which expenses can be edited
        java.util.Map<Long, Boolean> editableExpenses = new java.util.HashMap<>();
        for (Expense expense : expensesPage.getContent()) {
            editableExpenses.put(expense.getId(), canEditExpense(expense, authentication));
        }

        model.addAttribute("expenses", expensesPage.getContent());
        model.addAttribute("editableExpenses", editableExpenses);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", expensesPage.getTotalPages());
        model.addAttribute("totalItems", expensesPage.getTotalElements());

        addDropdownOptions(model);
        addFilterAttributes(model, client, project, expenseType, status, username, startDate,
                endDate);

        if (canViewAllExpenses) {
            // Get only users who have expenses
            List<String> usernamesWithExpenses = expenseService.getUsernamesWithExpenses();
            List<com.ammons.taskactivity.entity.User> usersWithExpenses = userService.getAllUsers()
                    .stream().filter(user -> usernamesWithExpenses.contains(user.getUsername()))
                    .toList();
            model.addAttribute("users", usersWithExpenses);
        }

        return EXPENSE_LIST_VIEW;
    }

    @GetMapping("/list/export-csv")
    @ResponseBody
    public String exportExpenseListToCsv(@RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        boolean canViewAllExpenses = canApproveExpenses(authentication);
        String currentUsername = canViewAllExpenses ? null : authentication.getName();
        String filterUsername =
                (canViewAllExpenses && username != null && !username.isEmpty()) ? username
                        : currentUsername;

        // Convert empty strings to null for proper SQL filtering
        String filterClient = emptyToNull(client);
        String filterProject = emptyToNull(project);
        String filterExpenseType = emptyToNull(expenseType);
        String filterStatus = emptyToNull(status);

        com.ammons.taskactivity.dto.ExpenseFilterDto filter =
                new com.ammons.taskactivity.dto.ExpenseFilterDto(filterUsername, filterClient,
                        filterProject, filterExpenseType, filterStatus, null, startDate, endDate);

        // Always pass authenticated username for draft filtering (even for CSV export)
        String authenticatedUsername = authentication.getName();

        // Get all expenses without pagination
        Page<Expense> allExpensesPage = expenseService.getExpensesByFilters(authenticatedUsername,
                filter, Pageable.unpaged());
        List<Expense> filteredExpenses = allExpensesPage.getContent();

        // Generate CSV
        return generateCsvFromExpenses(filteredExpenses, canViewAllExpenses);
    }

    private String generateCsvFromExpenses(List<Expense> expenses, boolean includeUsername) {
        StringBuilder csv = new StringBuilder();

        // Add header row
        if (includeUsername) {
            csv.append(
                    "Date,Client,Project,Expense Type,Amount,Currency,Status,Details,Username\n");
        } else {
            csv.append("Date,Client,Project,Expense Type,Amount,Currency,Status,Details\n");
        }

        // Sort expenses by date (newest first), then by client, then by project
        List<Expense> sortedExpenses = expenses.stream().sorted((e1, e2) -> {
            int dateCompare = e2.getExpenseDate().compareTo(e1.getExpenseDate());
            if (dateCompare != 0)
                return dateCompare;

            int clientCompare = e1.getClient().compareToIgnoreCase(e2.getClient());
            if (clientCompare != 0)
                return clientCompare;

            return e1.getProject().compareToIgnoreCase(e2.getProject());
        }).toList();

        // Add data rows
        for (Expense expense : sortedExpenses) {
            csv.append(formatCsvField(expense.getExpenseDate().toString())).append(",");
            csv.append(escapeCsvField(expense.getClient())).append(",");
            csv.append(escapeCsvField(expense.getProject())).append(",");
            csv.append(escapeCsvField(expense.getExpenseType())).append(",");
            csv.append(expense.getAmount()).append(",");
            csv.append(escapeCsvField(expense.getCurrency())).append(",");
            csv.append(escapeCsvField(expense.getExpenseStatus())).append(",");
            csv.append(escapeCsvField(
                    expense.getDescription() != null ? expense.getDescription() : ""));

            if (includeUsername) {
                csv.append(",").append(escapeCsvField(expense.getUsername()));
            }

            csv.append("\n");
        }

        return csv.toString();
    }

    private String formatCsvField(String dateStr) {
        // Convert ISO date format (yyyy-MM-dd) to MM/dd/yyyy
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return String.format("%02d/%02d/%04d", date.getMonthValue(), date.getDayOfMonth(),
                    date.getYear());
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    @GetMapping("/add")
    public String showForm(Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        validateUserHasEmail(authentication.getName(), redirectAttributes);
        addUserInfo(model, authentication);
        ExpenseDto expenseDto = new ExpenseDto();
        expenseDto.setExpenseDate(LocalDate.now());
        model.addAttribute(EXPENSE_DTO_ATTR, expenseDto);
        model.addAttribute(IS_EDIT_ATTR, false);
        addDropdownOptions(model);
        addFileUploadConfig(model);
        return EXPENSE_FORM_VIEW;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        addUserInfo(model, authentication);

        Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
        if (expenseOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, EXPENSE_NOT_FOUND);
            return REDIRECT_EXPENSE_LIST;
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = isAdmin(authentication);

        if (!isAdmin && !expense.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "You can only edit your own expenses");
            return REDIRECT_EXPENSE_LIST;
        }

        // Only allow editing of Draft expenses
        if (!STATUS_DRAFT.equalsIgnoreCase(expense.getExpenseStatus())) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Only expenses in Draft status can be edited");
            return REDIRECT_EXPENSE_LIST;
        }

        model.addAttribute(EXPENSE_DTO_ATTR, convertToDto(expense));
        model.addAttribute(EXPENSE_ID_ATTR, id);
        model.addAttribute(IS_EDIT_ATTR, true);
        addDropdownOptions(model);
        addFileUploadConfig(model);
        return EXPENSE_FORM_VIEW;
    }

    @GetMapping("/clone/{id}")
    public String cloneExpense(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Invalid expense ID provided.");
                return REDIRECT_EXPENSE_LIST;
            }

            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isPresent()) {
                ExpenseDto dto = convertToDto(expenseOpt.get());
                // Set today's date as default for cloned expense
                dto.setExpenseDate(LocalDate.now());
                // Reset status to Draft for cloned expense
                dto.setExpenseStatus(STATUS_DRAFT);
                // Clear approval/rejection fields
                dto.setApprovedBy(null);
                dto.setApprovalDate(null);
                dto.setApprovalNotes(null);
                // Clear reimbursement fields
                dto.setReimbursedAmount(null);
                dto.setReimbursementDate(null);
                dto.setReimbursementNotes(null);
                // Clear receipt (user needs to upload new one if needed)
                dto.setReceiptPath(null);
                dto.setReceiptStatus(null);

                // Clear out inactive dropdown values for clone (only active values should be
                // available)
                List<String> activeClients = dropdownValueService
                        .getActiveValuesByCategoryAndSubcategory("TASK", "CLIENT").stream()
                        .map(dv -> dv.getItemValue()).toList();
                List<String> activeProjects = dropdownValueService
                        .getActiveValuesByCategoryAndSubcategory("TASK", "PROJECT").stream()
                        .map(dv -> dv.getItemValue()).toList();
                List<String> activeExpenseTypes = dropdownValueService.getActiveExpenseTypes()
                        .stream().map(dv -> dv.getItemValue()).toList();
                List<String> activePaymentMethods = dropdownValueService.getActivePaymentMethods()
                        .stream().map(dv -> dv.getItemValue()).toList();
                List<String> activeVendors = dropdownValueService
                        .getActiveValuesByCategoryAndSubcategory("EXPENSE", "VENDOR").stream()
                        .map(dv -> dv.getItemValue()).toList();
                List<String> activeCurrencies = dropdownValueService
                        .getActiveValuesByCategoryAndSubcategory("EXPENSE", "CURRENCY").stream()
                        .map(dv -> dv.getItemValue()).toList();

                if (dto.getClient() != null && !activeClients.contains(dto.getClient())) {
                    dto.setClient(null);
                }
                if (dto.getProject() != null && !activeProjects.contains(dto.getProject())) {
                    dto.setProject(null);
                }
                if (dto.getExpenseType() != null
                        && !activeExpenseTypes.contains(dto.getExpenseType())) {
                    dto.setExpenseType(null);
                }
                if (dto.getPaymentMethod() != null
                        && !activePaymentMethods.contains(dto.getPaymentMethod())) {
                    dto.setPaymentMethod(null);
                }
                if (dto.getVendor() != null && !activeVendors.contains(dto.getVendor())) {
                    dto.setVendor(null);
                }
                if (dto.getCurrency() != null && !activeCurrencies.contains(dto.getCurrency())) {
                    dto.setCurrency(null);
                }

                addUserInfo(model, authentication);
                model.addAttribute(EXPENSE_DTO_ATTR, dto);
                model.addAttribute(IS_EDIT_ATTR, false);
                addDropdownOptions(model);
                addFileUploadConfig(model);
                return EXPENSE_FORM_VIEW;
            } else {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Expense not found with ID: " + id);
                return REDIRECT_EXPENSE_LIST;
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to clone expense: " + e.getMessage());
            return REDIRECT_EXPENSE_LIST;
        }
    }

    /**
     * Submit expense form (create or update). Validates the form data and creates/updates the
     * expense. Optionally handles receipt file upload. Sets the username from the authenticated
     * user to ensure ownership.
     * 
     * @param expenseDto the expense data transfer object
     * @param bindingResult validation results
     * @param receipt optional receipt file upload
     * @param model the model for view rendering
     * @param redirectAttributes for flash messages
     * @param authentication the authenticated user
     * @return redirect to expense list on success, or form view on validation errors
     */
    @RequirePermission(resource = "EXPENSE", action = "CREATE")
    @PostMapping("/submit")
    public String submitForm(@Valid @ModelAttribute ExpenseDto expenseDto,
            BindingResult bindingResult,
            @RequestParam(value = "receipt", required = false) MultipartFile receipt, Model model,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addUserInfo(model, authentication);
            model.addAttribute(IS_EDIT_ATTR, false);
            addDropdownOptions(model);
            addFileUploadConfig(model);
            return EXPENSE_FORM_VIEW;
        }

        try {
            expenseDto.setUsername(authentication.getName());

            // Handle file upload if provided
            if (receipt != null && !receipt.isEmpty()) {
                try {
                    expenseService.createExpenseWithReceipt(expenseDto, receipt);
                } catch (Exception e) {
                    logger.error("Error uploading receipt: {}", e.getMessage(), e);
                    model.addAttribute(ERROR_MESSAGE_ATTR,
                            "Expense created but receipt upload failed: " + e.getMessage());
                    addUserInfo(model, authentication);
                    addDropdownOptions(model);
                    addFileUploadConfig(model);
                    return EXPENSE_FORM_VIEW;
                }
            } else {
                expenseService.createExpense(expenseDto);
            }

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense created successfully");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            logger.error("Error creating expense: {}", e.getMessage(), e);
            model.addAttribute(ERROR_MESSAGE_ATTR, "Failed to create expense: " + e.getMessage());
            addUserInfo(model, authentication);
            addDropdownOptions(model);
            addFileUploadConfig(model);
            return EXPENSE_FORM_VIEW;
        }
    }

    @GetMapping("/detail/{id}")
    public String showExpenseDetail(@PathVariable Long id,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        addUserInfo(model, authentication);

        Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
        if (expenseOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, EXPENSE_NOT_FOUND);
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        }

        Expense expense = expenseOpt.get();
        boolean canViewAllExpenses = canApproveExpenses(authentication);

        if (!canViewAllExpenses && !expense.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "You can only view your own expenses");
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        }

        ExpenseDto dto = convertToDto(expense);
        model.addAttribute("expense", expense);
        model.addAttribute(EXPENSE_DTO_ATTR, dto);
        model.addAttribute(EXPENSE_ID_ATTR, id);
        addDropdownOptions(model, dto);
        addFileUploadConfig(model);

        // Pass filter parameters to the detail view so they can be used when going back
        addFilterAttributes(model, client, project, expenseType, status, username, startDate,
                endDate);

        return EXPENSE_DETAIL_VIEW;
    }

    @PostMapping("/update/{id}")
    public String updateExpense(@PathVariable Long id, @Valid @ModelAttribute ExpenseDto expenseDto,
            BindingResult bindingResult,
            @RequestParam(value = "receipt", required = false) MultipartFile receipt,
            @RequestParam(name = "filterClient", required = false) String client,
            @RequestParam(name = "filterProject", required = false) String project,
            @RequestParam(name = "filterExpenseType", required = false) String expenseType,
            @RequestParam(name = "filterStatus", required = false) String status,
            @RequestParam(name = "filterUsername", required = false) String username,
            @RequestParam(name = "filterStartDate", required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "filterEndDate", required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addUserInfo(model, authentication);
            model.addAttribute(EXPENSE_ID_ATTR, id);
            model.addAttribute(IS_EDIT_ATTR, true);
            addDropdownOptions(model, expenseDto);
            addFileUploadConfig(model);
            return EXPENSE_FORM_VIEW;
        }

        try {
            Optional<Expense> existingOpt = expenseService.getExpenseById(id);
            if (existingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, EXPENSE_NOT_FOUND);
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            Expense existing = existingOpt.get();
            boolean canEditAllExpenses = canApproveExpenses(authentication);

            if (!canEditAllExpenses && !existing.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "You can only update your own expenses");
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            // Only allow updating of Draft or Rejected expenses
            if (!STATUS_DRAFT.equalsIgnoreCase(existing.getExpenseStatus())
                    && !"Rejected".equalsIgnoreCase(existing.getExpenseStatus())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only expenses in Draft or Rejected status can be edited");
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            expenseDto.setUsername(existing.getUsername());

            // Handle file upload if provided
            if (receipt != null && !receipt.isEmpty()) {
                try {
                    expenseService.updateExpenseWithReceipt(id, expenseDto, receipt);
                } catch (Exception e) {
                    logger.error("Error uploading receipt: {}", e.getMessage(), e);
                    redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                            "Expense updated but receipt upload failed: " + e.getMessage());
                    return buildFilteredRedirect(client, project, expenseType, status, username,
                            startDate, endDate);
                }
            } else {
                expenseService.updateExpense(id, expenseDto);
            }

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense updated successfully");
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        } catch (Exception e) {
            logger.error("Error updating expense: {}", e.getMessage(), e);
            model.addAttribute(ERROR_MESSAGE_ATTR, "Failed to update expense: " + e.getMessage());
            addUserInfo(model, authentication);
            model.addAttribute(EXPENSE_ID_ATTR, id);
            model.addAttribute(IS_EDIT_ATTR, true);
            addDropdownOptions(model);
            addFileUploadConfig(model);
            return EXPENSE_FORM_VIEW;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteExpense(@PathVariable Long id,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, EXPENSE_NOT_FOUND);
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            Expense expense = expenseOpt.get();
            boolean canDeleteAllExpenses = canApproveExpenses(authentication);

            if (!canDeleteAllExpenses && !expense.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "You can only delete your own expenses");
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            // Only allow deleting of Draft expenses
            if (!STATUS_DRAFT.equalsIgnoreCase(expense.getExpenseStatus())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only expenses in Draft status can be deleted");
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            expenseService.deleteExpense(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense deleted successfully");
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        } catch (Exception e) {
            logger.error("Error deleting expense: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to delete expense: " + e.getMessage());
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        }
    }

    @PostMapping("/{id}/reimburse")
    public String markAsReimbursed(@PathVariable Long id, @RequestParam BigDecimal reimbursedAmount,
            @RequestParam(required = false) String reimbursementNotes,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, EXPENSE_NOT_FOUND);
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            Expense expense = expenseOpt.get();
            boolean canReimburse = canApproveExpenses(authentication);

            if (!canReimburse) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only administrators can mark expenses as reimbursed");
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            // Only allow reimbursement of Approved expenses
            if (!"Approved".equalsIgnoreCase(expense.getExpenseStatus())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only Approved expenses can be marked as reimbursed");
                return buildFilteredRedirect(client, project, expenseType, status, username,
                        startDate, endDate);
            }

            // Call service method to update and send notification
            expenseService.markAsReimbursed(id, authentication.getName(), reimbursedAmount,
                    reimbursementNotes);

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense marked as reimbursed successfully");
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        } catch (Exception e) {
            logger.error("Error marking expense as reimbursed: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to mark expense as reimbursed: " + e.getMessage());
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        }
    }

    @GetMapping("/weekly-sheet")
    public String showWeeklyExpenseSheet(
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "All") String billability,
            Model model, Authentication authentication) {
        addUserInfo(model, authentication);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDate startOfWeek =
                targetDate.minusDays((long) targetDate.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        String username = authentication.getName();
        List<Expense> expenses =
                expenseService.getExpensesInDateRangeForUser(username, startOfWeek, endOfWeek);

        // Apply billability filter if not "All"
        if (!"All".equals(billability)) {
            expenses = filterExpensesByBillability(expenses, billability);
        }

        // Build weekly data structure
        WeeklyExpenseData weeklyData = buildWeeklyExpenseData(startOfWeek, endOfWeek, expenses);

        model.addAttribute("weeklyData", weeklyData);
        model.addAttribute("startDate", startOfWeek);
        model.addAttribute("endDate", endOfWeek);
        model.addAttribute("targetDate", targetDate);
        model.addAttribute("billability", billability);

        return EXPENSE_SHEET_VIEW;
    }

    private WeeklyExpenseData buildWeeklyExpenseData(LocalDate startDate, LocalDate endDate,
            List<Expense> expenses) {
        WeeklyExpenseData weeklyData = new WeeklyExpenseData();
        weeklyData.setWeekStartDate(startDate);
        weeklyData.setWeekEndDate(endDate);

        // Initialize daily data for all days of the week
        Map<DayOfWeek, DailyExpenseData> dailyDataMap = new LinkedHashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DailyExpenseData dailyData = new DailyExpenseData();
            dailyData.setDate(currentDate);
            dailyData.setDayOfWeek(currentDate.getDayOfWeek());
            dailyData.setExpenses(new ArrayList<>());
            dailyDataMap.put(currentDate.getDayOfWeek(), dailyData);
            currentDate = currentDate.plusDays(1);
        }

        // Group expenses by date
        BigDecimal weekTotal = BigDecimal.ZERO;
        String weekCurrency = null;
        for (Expense expense : expenses) {
            DayOfWeek dayOfWeek = expense.getExpenseDate().getDayOfWeek();
            DailyExpenseData dailyData = dailyDataMap.get(dayOfWeek);
            if (dailyData != null) {
                dailyData.getExpenses().add(expense);
                dailyData.setDayTotal(dailyData.getDayTotal().add(expense.getAmount()));
                // Set currency symbol from first expense
                if (dailyData.getCurrencySymbol() == null && expense.getCurrency() != null) {
                    dailyData.setCurrencySymbol(expense.getCurrency());
                }
                if (weekCurrency == null && expense.getCurrency() != null) {
                    weekCurrency = expense.getCurrency();
                }
                weekTotal = weekTotal.add(expense.getAmount());
            }
        }

        weeklyData.setDailyData(dailyDataMap);
        weeklyData.setWeekTotal(weekTotal);
        weeklyData.setCurrencySymbol(weekCurrency != null ? weekCurrency : "USD");

        return weeklyData;
    }

    // Inner classes for weekly expense data structure
    private static class WeeklyExpenseData {
        private LocalDate weekStartDate;
        private LocalDate weekEndDate;
        private Map<DayOfWeek, DailyExpenseData> dailyData;
        private BigDecimal weekTotal;
        private String currencySymbol;

        public LocalDate getWeekStartDate() {
            return weekStartDate;
        }

        public void setWeekStartDate(LocalDate weekStartDate) {
            this.weekStartDate = weekStartDate;
        }

        public LocalDate getWeekEndDate() {
            return weekEndDate;
        }

        public void setWeekEndDate(LocalDate weekEndDate) {
            this.weekEndDate = weekEndDate;
        }

        public Map<DayOfWeek, DailyExpenseData> getDailyData() {
            return dailyData;
        }

        public void setDailyData(Map<DayOfWeek, DailyExpenseData> dailyData) {
            this.dailyData = dailyData;
        }

        public BigDecimal getWeekTotal() {
            return weekTotal;
        }

        public void setWeekTotal(BigDecimal weekTotal) {
            this.weekTotal = weekTotal;
        }

        public String getCurrencySymbol() {
            return currencySymbol;
        }

        public void setCurrencySymbol(String currencySymbol) {
            this.currencySymbol = currencySymbol;
        }

        public List<DailyExpenseData> getAllDays() {
            return dailyData.values().stream().toList();
        }
    }

    private static class DailyExpenseData {
        private DayOfWeek dayOfWeek;
        private LocalDate date;
        private List<Expense> expenses;
        private BigDecimal dayTotal = BigDecimal.ZERO;
        private String currencySymbol;

        public DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(DayOfWeek dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public List<Expense> getExpenses() {
            return expenses;
        }

        public void setExpenses(List<Expense> expenses) {
            this.expenses = expenses;
        }

        public BigDecimal getDayTotal() {
            return dayTotal;
        }

        public void setDayTotal(BigDecimal dayTotal) {
            this.dayTotal = dayTotal;
        }

        public String getCurrencySymbol() {
            return currencySymbol != null ? currencySymbol : "USD";
        }

        public void setCurrencySymbol(String currencySymbol) {
            this.currencySymbol = currencySymbol;
        }

        public String getDayName() {
            return dayOfWeek.toString();
        }

        public boolean hasExpenses() {
            return expenses != null && !expenses.isEmpty();
        }
    }

    @PostMapping("/{id}/submit")
    public String submitExpenseForApproval(@PathVariable Long id,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, EXPENSE_NOT_FOUND);
                return REDIRECT_EXPENSE_LIST;
            }

            Expense expense = expenseOpt.get();
            boolean isAdmin = isAdmin(authentication);

            if (!isAdmin && !expense.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "You can only submit your own expenses");
                return REDIRECT_EXPENSE_LIST;
            }

            // Only allow submitting Draft or Rejected expenses
            if (!STATUS_DRAFT.equalsIgnoreCase(expense.getExpenseStatus())
                    && !"Rejected".equalsIgnoreCase(expense.getExpenseStatus())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only Draft or Rejected expenses can be submitted");
                return REDIRECT_EXPENSE_LIST;
            }

            expenseService.submitExpense(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense submitted for approval");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            logger.error("Error submitting expense: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to submit expense: " + e.getMessage());
            return REDIRECT_EXPENSE_LIST;
        }
    }

    /**
     * Show the expense approval queue (admin only). Displays all expenses with "Submitted" or
     * "Resubmitted" status, excluding the current user's own expenses.
     * 
     * @param model the model for view rendering
     * @param authentication the authenticated admin user
     * @return the approval queue view
     */
    @RequirePermission(resource = "EXPENSE", action = "READ_ALL")
    @GetMapping("/admin/approval-queue")
    public String showApprovalQueue(Model model, Authentication authentication) {
        addUserInfo(model, authentication);
        String currentUsername = authentication.getName();

        // Debug logging
        logger.info("Approval queue accessed by user: {}", currentUsername);
        logger.info("User authorities: {}", authentication.getAuthorities());

        List<Expense> allPendingExpenses = expenseService.getPendingApprovals();
        logger.info("Total pending expenses found: {}", allPendingExpenses.size());

        // Filter out current user's own expenses
        List<Expense> pendingExpenses = allPendingExpenses.stream()
                .filter(expense -> !expense.getUsername().equals(currentUsername)).toList();
        logger.info("Pending expenses after filtering own: {}", pendingExpenses.size());

        // Get user details for display (first name, last name)
        Map<String, com.ammons.taskactivity.entity.User> userMap = new java.util.HashMap<>();
        for (Expense expense : pendingExpenses) {
            if (!userMap.containsKey(expense.getUsername())) {
                userService.getUserByUsername(expense.getUsername())
                        .ifPresent(user -> userMap.put(expense.getUsername(), user));
            }
        }

        model.addAttribute("pendingExpenses", pendingExpenses);
        model.addAttribute("userMap", userMap);
        return APPROVAL_QUEUE_VIEW;
    }

    /**
     * Approve an expense from the approval queue (admin only). Changes the expense status to
     * "Approved" and sends notification email.
     * 
     * @param id the expense ID to approve
     * @param notes optional approval notes
     * @param redirectAttributes for flash messages
     * @param authentication the authenticated admin user
     * @return redirect to expense list
     */
    @RequirePermission(resource = "EXPENSE", action = "APPROVE")
    @PostMapping("/admin/approve/{id}")
    public String approveExpense(@PathVariable Long id,
            @RequestParam(required = false) String notes, RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            expenseService.approveExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense approved");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to approve expense: " + e.getMessage());
            return REDIRECT_EXPENSE_LIST;
        }
    }

    /**
     * Reject an expense from the approval queue (admin only). Changes the expense status to
     * "Rejected" and sends notification email.
     * 
     * @param id the expense ID to reject
     * @param notes required rejection reason/notes
     * @param redirectAttributes for flash messages
     * @param authentication the authenticated admin user
     * @return redirect to expense list
     */
    @RequirePermission(resource = "EXPENSE", action = "REJECT")
    @PostMapping("/admin/reject/{id}")
    public String rejectExpense(@PathVariable Long id, @RequestParam String notes,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            expenseService.rejectExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense rejected");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to reject expense: " + e.getMessage());
            return REDIRECT_EXPENSE_LIST;
        }
    }

    /**
     * Approve an expense from the detail view (admin only). Preserves filter parameters when
     * redirecting back to the list view.
     * 
     * @param id the expense ID to approve
     * @param notes optional approval notes
     * @param client filter parameter to preserve
     * @param project filter parameter to preserve
     * @param expenseType filter parameter to preserve
     * @param status filter parameter to preserve
     * @param username filter parameter to preserve
     * @param startDate filter parameter to preserve
     * @param endDate filter parameter to preserve
     * @param redirectAttributes for flash messages
     * @param authentication the authenticated admin user
     * @return redirect to filtered expense list
     */
    @RequirePermission(resource = "EXPENSE", action = "APPROVE")
    @PostMapping("/{id}/approve")
    public String approveExpenseFromDetail(@PathVariable Long id,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            expenseService.approveExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense approved");
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to approve expense: " + e.getMessage());
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        }
    }

    /**
     * Reject an expense from the detail view (admin only). Preserves filter parameters when
     * redirecting back to the list view.
     * 
     * @param id the expense ID to reject
     * @param notes required rejection reason/notes
     * @param client filter parameter to preserve
     * @param project filter parameter to preserve
     * @param expenseType filter parameter to preserve
     * @param status filter parameter to preserve
     * @param username filter parameter to preserve
     * @param startDate filter parameter to preserve
     * @param endDate filter parameter to preserve
     * @param redirectAttributes for flash messages
     * @param authentication the authenticated admin user
     * @return redirect to filtered expense list
     */
    @RequirePermission(resource = "EXPENSE", action = "REJECT")
    @PostMapping("/{id}/reject")
    public String rejectExpenseFromDetail(@PathVariable Long id, @RequestParam String notes,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            expenseService.rejectExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense rejected");
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to reject expense: " + e.getMessage());
            return buildFilteredRedirect(client, project, expenseType, status, username, startDate,
                    endDate);
        }
    }

    // Helper methods
    private void addUserInfo(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute("username", username);
            model.addAttribute("isAdmin", isAdmin(authentication));
            model.addAttribute("canApproveExpenses", canApproveExpenses(authentication));
            model.addAttribute("isGuest", isGuest(authentication));

            // Check if user has email for expense access
            boolean hasEmail = userService.userHasEmail(username);
            model.addAttribute("userHasEmail", hasEmail);

            // Fetch user details to display full name
            userService.getUserByUsername(username).ifPresent(user -> {
                String firstname = user.getFirstname() != null ? user.getFirstname() : "";
                String lastname = user.getLastname() != null ? user.getLastname() : "";
                String displayName = (firstname + " " + lastname + " (" + username + ")").trim();
                // Remove extra spaces if firstname is empty
                displayName = displayName.replaceAll("\\s+", " ");
                model.addAttribute("userDisplayName", displayName);
            });
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean canApproveExpenses(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")
                        || auth.getAuthority().equals("ROLE_EXPENSE_ADMIN"));
    }

    private boolean isGuest(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST"));
    }

    /**
     * Determines if a user can edit a specific expense. Rules: - Regular users (USER role) can only
     * edit their own expenses in Draft or Rejected status - ADMIN/EXPENSE_ADMIN can edit any
     * expense, but cannot edit Reimbursed expenses
     */
    private boolean canEditExpense(Expense expense, Authentication authentication) {
        String status = expense.getExpenseStatus();
        boolean canEditAllExpenses = canApproveExpenses(authentication);
        boolean isOwnExpense = expense.getUsername().equals(authentication.getName());

        // ADMIN/EXPENSE_ADMIN: Can edit any expense except Reimbursed
        if (canEditAllExpenses) {
            return !"Reimbursed".equalsIgnoreCase(status);
        }

        // Regular USER: Can only edit their own Draft or Rejected expenses
        boolean isEditableStatus =
                STATUS_DRAFT.equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status);
        return isOwnExpense && isEditableStatus;
    }

    private void addDropdownOptions(Model model) {
        addDropdownOptions(model, null);
    }

    /**
     * Add dropdown options to model, including inactive values if they're currently selected
     * 
     * @param model the model to add attributes to
     * @param dto the current expense (null for new expenses)
     */
    private void addDropdownOptions(Model model, ExpenseDto dto) {
        List<String> clients =
                dropdownValueService.getActiveValuesByCategoryAndSubcategory("TASK", "CLIENT")
                        .stream().map(dv -> dv.getItemValue()).toList();
        List<String> projects =
                dropdownValueService.getActiveValuesByCategoryAndSubcategory("TASK", "PROJECT")
                        .stream().map(dv -> dv.getItemValue()).toList();
        List<String> expenseTypes = dropdownValueService.getActiveExpenseTypes().stream()
                .map(dv -> dv.getItemValue()).toList();
        List<String> paymentMethods = dropdownValueService.getActivePaymentMethods().stream()
                .map(dv -> dv.getItemValue()).toList();
        List<String> statuses = dropdownValueService.getActiveExpenseStatuses().stream()
                .map(dv -> dv.getItemValue()).toList();

        List<String> vendors = java.util.Collections.emptyList();
        List<String> currencies = java.util.Collections.emptyList();

        try {
            vendors = dropdownValueService
                    .getActiveValuesByCategoryAndSubcategory("EXPENSE", "VENDOR").stream()
                    .map(dv -> dv.getItemValue()).toList();
        } catch (Exception e) {
            logger.warn("Unable to load vendor dropdown values", e);
        }

        try {
            currencies = dropdownValueService
                    .getActiveValuesByCategoryAndSubcategory("EXPENSE", "CURRENCY").stream()
                    .map(dv -> dv.getItemValue()).toList();
        } catch (Exception e) {
            logger.warn("Unable to load currency dropdown values", e);
        }

        // When editing/viewing existing expense, include its values even if they're inactive
        if (dto != null) {
            if (dto.getClient() != null && !clients.contains(dto.getClient())) {
                clients = new java.util.ArrayList<>(clients);
                clients.add(dto.getClient());
                clients.sort(String::compareTo);
            }
            if (dto.getProject() != null && !projects.contains(dto.getProject())) {
                projects = new java.util.ArrayList<>(projects);
                projects.add(dto.getProject());
                projects.sort(String::compareTo);
            }
            if (dto.getExpenseType() != null && !expenseTypes.contains(dto.getExpenseType())) {
                expenseTypes = new java.util.ArrayList<>(expenseTypes);
                expenseTypes.add(dto.getExpenseType());
                expenseTypes.sort(String::compareTo);
            }
            if (dto.getPaymentMethod() != null
                    && !paymentMethods.contains(dto.getPaymentMethod())) {
                paymentMethods = new java.util.ArrayList<>(paymentMethods);
                paymentMethods.add(dto.getPaymentMethod());
                paymentMethods.sort(String::compareTo);
            }
            if (dto.getVendor() != null && !vendors.contains(dto.getVendor())) {
                vendors = new java.util.ArrayList<>(vendors);
                vendors.add(dto.getVendor());
                vendors.sort(String::compareTo);
            }
            if (dto.getCurrency() != null && !currencies.contains(dto.getCurrency())) {
                currencies = new java.util.ArrayList<>(currencies);
                currencies.add(dto.getCurrency());
                currencies.sort(String::compareTo);
            }
        }

        model.addAttribute("clients", clients);
        model.addAttribute("projects", projects);
        model.addAttribute("expenseTypes", expenseTypes);
        model.addAttribute("paymentMethods", paymentMethods);
        model.addAttribute("statuses", statuses);
        model.addAttribute("vendors", vendors);
        model.addAttribute("currencies", currencies);
    }

    private void addFilterAttributes(Model model, String client, String project, String expenseType,
            String status, String username, LocalDate startDate, LocalDate endDate) {
        model.addAttribute("selectedClient", client);
        model.addAttribute("selectedProject", project);
        model.addAttribute("selectedExpenseType", expenseType);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedUsername", username);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
    }

    /**
     * Download or view receipt file for an expense. Retrieves the receipt from S3 or local file
     * system based on configuration. Returns the file as an inline attachment for viewing in the
     * browser.
     * 
     * @param id the expense ID to retrieve the receipt from
     * @return ResponseEntity containing the receipt file stream
     */
    @RequirePermission(resource = "EXPENSE", action = "MANAGE_RECEIPTS")
    @GetMapping("/receipt/{id}")
    public ResponseEntity<Resource> getReceipt(@PathVariable Long id) {
        try {
            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isEmpty() || expenseOpt.get().getReceiptPath() == null) {
                return ResponseEntity.notFound().build();
            }

            String receiptPath = expenseOpt.get().getReceiptPath();

            // Use storage service to get receipt (works for both local and S3)
            InputStream receiptStream = storageService.getReceipt(receiptPath);
            String contentType = storageService.getContentType(receiptPath);

            // Extract filename from path
            String filename = receiptPath.substring(receiptPath.lastIndexOf('/') + 1);

            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(receiptStream));

        } catch (IOException e) {
            logger.error("Error retrieving receipt: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to convert empty strings to null for SQL filtering
     */
    /**
     * Build a redirect URL to expense list with filter parameters preserved
     */
    private String buildFilteredRedirect(String client, String project, String expenseType,
            String status, String username, LocalDate startDate, LocalDate endDate) {
        StringBuilder redirect = new StringBuilder(REDIRECT_EXPENSE_LIST);
        List<String> params = new ArrayList<>();

        if (client != null && !client.isEmpty()) {
            params.add("client=" + client);
        }
        if (project != null && !project.isEmpty()) {
            params.add("project=" + project);
        }
        if (expenseType != null && !expenseType.isEmpty()) {
            params.add("expenseType=" + expenseType);
        }
        if (status != null && !status.isEmpty()) {
            params.add("status=" + status);
        }
        if (username != null && !username.isEmpty()) {
            params.add("username=" + username);
        }
        if (startDate != null) {
            params.add("startDate=" + startDate.toString());
        }
        if (endDate != null) {
            params.add("endDate=" + endDate.toString());
        }

        if (!params.isEmpty()) {
            redirect.append("?").append(String.join("&", params));
        }

        return redirect.toString();
    }

    private String emptyToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value;
    }

    private ExpenseDto convertToDto(Expense expense) {
        ExpenseDto dto = new ExpenseDto();
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setClient(expense.getClient());
        dto.setProject(expense.getProject());
        dto.setExpenseType(expense.getExpenseType());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setCurrency(expense.getCurrency());
        dto.setPaymentMethod(expense.getPaymentMethod());
        dto.setVendor(expense.getVendor());
        dto.setReferenceNumber(expense.getReferenceNumber());
        dto.setReceiptPath(expense.getReceiptPath());
        dto.setReceiptStatus(expense.getReceiptStatus());
        dto.setExpenseStatus(expense.getExpenseStatus());
        dto.setNotes(expense.getNotes());
        dto.setUsername(expense.getUsername());
        return dto;
    }

    /**
     * Filters expenses list based on billability selection.
     * @param expenses The original expenses list
     * @param billability Filter value: "Billable" or "Non-Billable"
     * @return Filtered expenses list
     */
    private List<Expense> filterExpensesByBillability(List<Expense> expenses, String billability) {
        if (expenses == null || expenses.isEmpty()) {
            return expenses;
        }

        boolean showBillable = "Billable".equals(billability);

        return expenses.stream()
            .filter(expense -> {
                boolean isBillable = billabilityService.isExpenseBillable(
                    expense.getClient(), expense.getProject(), expense.getExpenseType());
                return showBillable ? isBillable : !isBillable;
            })
            .toList();
    }
}


