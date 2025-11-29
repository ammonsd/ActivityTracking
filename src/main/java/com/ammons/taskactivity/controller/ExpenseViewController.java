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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
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

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
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
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes,
            Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addUserInfo(model, authentication);
            model.addAttribute("expenseId", id);
            addDropdownOptions(model);
            return EXPENSE_DETAIL_VIEW;
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

            expenseDto.setUsername(existing.getUsername());
            expenseService.updateExpense(id, expenseDto);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Expense updated successfully");
            return "redirect:/expenses/detail/" + id;
        } catch (Exception e) {
            logger.error("Error updating expense: {}", e.getMessage(), e);
            model.addAttribute(ERROR_MESSAGE_ATTR, "Failed to update expense: " + e.getMessage());
            addUserInfo(model, authentication);
            addDropdownOptions(model);
            return EXPENSE_DETAIL_VIEW;
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

        model.addAttribute("expenses", expenses);
        model.addAttribute("startDate", startOfWeek);
        model.addAttribute("endDate", endOfWeek);
        model.addAttribute("targetDate", targetDate);

        return EXPENSE_SHEET_VIEW;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_APPROVER')")
    @GetMapping("/admin/approval-queue")
    public String showApprovalQueue(Model model, Authentication authentication) {
        addUserInfo(model, authentication);
        List<Expense> pendingExpenses = expenseService.getPendingApprovals();
        model.addAttribute("expenses", pendingExpenses);
        return APPROVAL_QUEUE_VIEW;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_APPROVER')")
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

    @PreAuthorize("hasAnyRole('ADMIN', 'EXPENSE_APPROVER')")
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
                        || auth.getAuthority().equals("ROLE_EXPENSE_APPROVER"));
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("clients",
                dropdownValueService.getActiveValuesByCategoryAndSubcategory("EXPENSE", "CLIENT")
                        .stream().map(dv -> dv.getItemValue()).toList());
        model.addAttribute("projects",
                dropdownValueService.getActiveValuesByCategoryAndSubcategory("EXPENSE", "PROJECT")
                        .stream().map(dv -> dv.getItemValue()).toList());
        model.addAttribute("expenseTypes", dropdownValueService.getActiveExpenseTypes().stream()
                .map(dv -> dv.getItemValue()).toList());
        model.addAttribute("paymentMethods", dropdownValueService.getActivePaymentMethods().stream()
                .map(dv -> dv.getItemValue()).toList());
        model.addAttribute("statuses", dropdownValueService.getActiveExpenseStatuses().stream()
                .map(dv -> dv.getItemValue()).toList());
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
