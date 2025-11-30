package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ExpenseDto;
import com.ammons.taskactivity.entity.Expense;
import com.ammons.taskactivity.service.ExpenseService;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.UserService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final ExpenseService expenseService;
    private final DropdownValueService dropdownValueService;
    private final UserService userService;

    public ExpenseViewController(ExpenseService expenseService,
            DropdownValueService dropdownValueService, UserService userService) {
        this.expenseService = expenseService;
        this.dropdownValueService = dropdownValueService;
        this.userService = userService;
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
            Authentication authentication) {

        addUserInfo(model, authentication);

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "expense_date"));
        boolean isAdmin = isAdmin(authentication);
        String currentUsername = isAdmin ? null : authentication.getName();
        String filterUsername = (isAdmin && username != null) ? username : currentUsername;

        com.ammons.taskactivity.dto.ExpenseFilterDto filter =
                new com.ammons.taskactivity.dto.ExpenseFilterDto(filterUsername, client, project,
                        expenseType, status, null, startDate, endDate);

        Page<Expense> expensesPage = expenseService.getExpensesByFilters(filter, pageable);

        model.addAttribute("expenses", expensesPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", expensesPage.getTotalPages());
        model.addAttribute("totalItems", expensesPage.getTotalElements());

        addDropdownOptions(model);
        addFilterAttributes(model, client, project, expenseType, status, username, startDate,
                endDate);

        if (isAdmin) {
            model.addAttribute("users", userService.getAllUsers());
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

        boolean isAdmin = isAdmin(authentication);
        String currentUsername = isAdmin ? null : authentication.getName();
        String filterUsername = (isAdmin && username != null) ? username : currentUsername;

        com.ammons.taskactivity.dto.ExpenseFilterDto filter =
                new com.ammons.taskactivity.dto.ExpenseFilterDto(filterUsername, client, project,
                        expenseType, status, null, startDate, endDate);

        // Get all expenses without pagination
        Page<Expense> allExpensesPage =
                expenseService.getExpensesByFilters(filter, Pageable.unpaged());
        List<Expense> filteredExpenses = allExpensesPage.getContent();

        // Generate CSV
        return generateCsvFromExpenses(filteredExpenses, isAdmin);
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
    public String showForm(Model model, Authentication authentication) {
        addUserInfo(model, authentication);
        ExpenseDto expenseDto = new ExpenseDto();
        expenseDto.setExpenseDate(LocalDate.now());
        model.addAttribute("expenseDto", expenseDto);
        model.addAttribute("isEdit", false);
        addDropdownOptions(model);
        return EXPENSE_FORM_VIEW;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        addUserInfo(model, authentication);

        Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
        if (expenseOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
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
        if (!"Draft".equalsIgnoreCase(expense.getExpenseStatus())) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Only expenses in Draft status can be edited");
            return REDIRECT_EXPENSE_LIST;
        }

        model.addAttribute("expenseDto", convertToDto(expense));
        model.addAttribute("expenseId", id);
        model.addAttribute("isEdit", true);
        addDropdownOptions(model);
        return EXPENSE_FORM_VIEW;
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'EXPENSE_ADMIN')")
    public String submitForm(@Valid @ModelAttribute ExpenseDto expenseDto,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addUserInfo(model, authentication);
            model.addAttribute("isEdit", false);
            addDropdownOptions(model);
            return EXPENSE_FORM_VIEW;
        }

        try {
            expenseDto.setUsername(authentication.getName());
            expenseService.createExpense(expenseDto);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense created successfully");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            logger.error("Error creating expense: {}", e.getMessage(), e);
            model.addAttribute(ERROR_MESSAGE_ATTR, "Failed to create expense: " + e.getMessage());
            addUserInfo(model, authentication);
            addDropdownOptions(model);
            return EXPENSE_FORM_VIEW;
        }
    }

    @GetMapping("/detail/{id}")
    public String showExpenseDetail(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        addUserInfo(model, authentication);

        Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
        if (expenseOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
            return REDIRECT_EXPENSE_LIST;
        }

        Expense expense = expenseOpt.get();
        boolean isAdmin = isAdmin(authentication);

        if (!isAdmin && !expense.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "You can only view your own expenses");
            return REDIRECT_EXPENSE_LIST;
        }

        model.addAttribute("expense", expense);
        model.addAttribute("expenseDto", convertToDto(expense));
        model.addAttribute("expenseId", id);
        addDropdownOptions(model);
        return EXPENSE_DETAIL_VIEW;
    }

    @PostMapping("/update/{id}")
    public String updateExpense(@PathVariable Long id, @Valid @ModelAttribute ExpenseDto expenseDto,
            BindingResult bindingResult,
            @RequestParam(value = "receipt", required = false) MultipartFile receipt, Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addUserInfo(model, authentication);
            model.addAttribute("expenseId", id);
            model.addAttribute("isEdit", true);
            addDropdownOptions(model);
            return EXPENSE_FORM_VIEW;
        }

        try {
            Optional<Expense> existingOpt = expenseService.getExpenseById(id);
            if (existingOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
                return REDIRECT_EXPENSE_LIST;
            }

            Expense existing = existingOpt.get();
            boolean isAdmin = isAdmin(authentication);

            if (!isAdmin && !existing.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "You can only update your own expenses");
                return REDIRECT_EXPENSE_LIST;
            }

            // Only allow updating of Draft expenses
            if (!"Draft".equalsIgnoreCase(existing.getExpenseStatus())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only expenses in Draft status can be edited");
                return REDIRECT_EXPENSE_LIST;
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
                    return REDIRECT_EXPENSE_LIST;
                }
            } else {
                expenseService.updateExpense(id, expenseDto);
            }

            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense updated successfully");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            logger.error("Error updating expense: {}", e.getMessage(), e);
            model.addAttribute(ERROR_MESSAGE_ATTR, "Failed to update expense: " + e.getMessage());
            addUserInfo(model, authentication);
            model.addAttribute("expenseId", id);
            model.addAttribute("isEdit", true);
            addDropdownOptions(model);
            return EXPENSE_FORM_VIEW;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteExpense(@PathVariable Long id, RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
                return REDIRECT_EXPENSE_LIST;
            }

            Expense expense = expenseOpt.get();
            boolean isAdmin = isAdmin(authentication);

            if (!isAdmin && !expense.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "You can only delete your own expenses");
                return REDIRECT_EXPENSE_LIST;
            }

            // Only allow deleting of Draft expenses
            if (!"Draft".equalsIgnoreCase(expense.getExpenseStatus())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only expenses in Draft status can be deleted");
                return REDIRECT_EXPENSE_LIST;
            }

            expenseService.deleteExpense(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense deleted successfully");
            return REDIRECT_EXPENSE_LIST;
        } catch (Exception e) {
            logger.error("Error deleting expense: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to delete expense: " + e.getMessage());
            return REDIRECT_EXPENSE_LIST;
        }
    }

    @GetMapping("/weekly-sheet")
    public String showWeeklyExpenseSheet(
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model, Authentication authentication) {
        addUserInfo(model, authentication);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDate startOfWeek = targetDate.minusDays(targetDate.getDayOfWeek().getValue() - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        String username = authentication.getName();
        List<Expense> expenses =
                expenseService.getExpensesInDateRangeForUser(username, startOfWeek, endOfWeek);

        // Build weekly data structure
        WeeklyExpenseData weeklyData = buildWeeklyExpenseData(startOfWeek, endOfWeek, expenses);

        model.addAttribute("weeklyData", weeklyData);
        model.addAttribute("startDate", startOfWeek);
        model.addAttribute("endDate", endOfWeek);
        model.addAttribute("targetDate", targetDate);

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
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, "Expense not found");
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
            if (!"Draft".equalsIgnoreCase(expense.getExpenseStatus())
                    && !"Rejected".equalsIgnoreCase(expense.getExpenseStatus())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Only Draft or Rejected expenses can be submitted");
                return REDIRECT_EXPENSE_LIST;
            }

            expenseService.submitExpense(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense submitted for approval");
            return "redirect:/expenses/detail/" + id;
        } catch (Exception e) {
            logger.error("Error submitting expense: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to submit expense: " + e.getMessage());
            return "redirect:/expenses/detail/" + id;
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_ADMIN')")
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

    @PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_ADMIN')")
    @PostMapping("/admin/approve/{id}")
    public String approveExpense(@PathVariable Long id,
            @RequestParam(required = false) String notes, RedirectAttributes redirectAttributes,
            Authentication authentication) {
        try {
            expenseService.approveExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense approved");
            return "redirect:/expenses/admin/approval-queue";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to approve expense: " + e.getMessage());
            return "redirect:/expenses/admin/approval-queue";
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_ADMIN')")
    @PostMapping("/admin/reject/{id}")
    public String rejectExpense(@PathVariable Long id, @RequestParam String notes,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            expenseService.rejectExpense(id, authentication.getName(), notes);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Expense rejected");
            return "redirect:/expenses/admin/approval-queue";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to reject expense: " + e.getMessage());
            return "redirect:/expenses/admin/approval-queue";
        }
    }

    // Helper methods
    private void addUserInfo(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute("username", username);
            model.addAttribute("isAdmin", isAdmin(authentication));
            model.addAttribute("canApproveExpenses", canApproveExpenses(authentication));

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

    private void addDropdownOptions(Model model) {
        model.addAttribute("clients",
                dropdownValueService.getActiveValuesByCategoryAndSubcategory("TASK", "CLIENT")
                        .stream().map(dv -> dv.getItemValue()).toList());
        model.addAttribute("projects",
                dropdownValueService.getActiveValuesByCategoryAndSubcategory("TASK", "PROJECT")
                        .stream().map(dv -> dv.getItemValue()).toList());
        model.addAttribute("expenseTypes", dropdownValueService.getActiveExpenseTypes().stream()
                .map(dv -> dv.getItemValue()).toList());
        model.addAttribute("paymentMethods", dropdownValueService.getActivePaymentMethods().stream()
                .map(dv -> dv.getItemValue()).toList());
        model.addAttribute("statuses", dropdownValueService.getActiveExpenseStatuses().stream()
                .map(dv -> dv.getItemValue()).toList());

        // Add vendors and currencies for form pages (wrapped in try-catch for safety)
        try {
            model.addAttribute("vendors",
                    dropdownValueService
                            .getActiveValuesByCategoryAndSubcategory("EXPENSE", "VENDOR").stream()
                            .map(dv -> dv.getItemValue()).toList());
        } catch (Exception e) {
            logger.warn("Unable to load vendor dropdown values", e);
            model.addAttribute("vendors", java.util.Collections.emptyList());
        }

        try {
            model.addAttribute("currencies",
                    dropdownValueService
                            .getActiveValuesByCategoryAndSubcategory("EXPENSE", "CURRENCY").stream()
                            .map(dv -> dv.getItemValue()).toList());
        } catch (Exception e) {
            logger.warn("Unable to load currency dropdown values", e);
            model.addAttribute("currencies", java.util.Collections.emptyList());
        }
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
     * Download/view receipt file
     */
    @GetMapping("/receipt/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'EXPENSE_ADMIN')")
    public ResponseEntity<Resource> getReceipt(@PathVariable Long id) {
        try {
            Optional<Expense> expenseOpt = expenseService.getExpenseById(id);
            if (expenseOpt.isEmpty() || expenseOpt.get().getReceiptPath() == null) {
                return ResponseEntity.notFound().build();
            }

            String receiptPath = expenseOpt.get().getReceiptPath();
            Path filePath = Paths.get(receiptPath);

            if (!Files.exists(filePath)) {
                logger.error("Receipt file not found: {}", receiptPath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String filename = filePath.getFileName().toString();
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (IOException e) {
            logger.error("Error retrieving receipt: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
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
}

