package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.config.DropdownConfig;
import com.ammons.taskactivity.config.TaskListSortConfig;
import com.ammons.taskactivity.dto.TaskActivityDto;
import com.ammons.taskactivity.entity.TaskActivity;
import com.ammons.taskactivity.service.TaskActivityService;
import com.ammons.taskactivity.service.WeeklyTimesheetService;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.entity.DropdownValue;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TaskActivityWebController
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Controller
@RequestMapping("/task-activity")
public class TaskActivityWebController {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(TaskActivityWebController.class);

    // Constants for view names
    private static final String TASK_ACTIVITY_FORM_VIEW = "task-activity-form";
    private static final String TASK_LIST_VIEW = "task-list";
    private static final String TASK_DETAIL_VIEW = "task-detail";
    private static final String DROPDOWN_MANAGEMENT_SIMPLE_VIEW = "dropdown-management-simple";
    private static final String DROPDOWN_CATEGORY_MANAGEMENT_VIEW = "dropdown-category-management";
    private static final String WEEKLY_TIMESHEET_VIEW = "weekly-timesheet";

    // Constants for redirect URLs
    private static final String REDIRECT_TASK_LIST = "redirect:/task-activity/list";
    private static final String REDIRECT_DROPDOWN_MANAGEMENT =
            "redirect:/task-activity/manage-dropdowns";

    // Constants for model attributes
    private static final String TASK_ACTIVITY_DTO_ATTR = "taskActivityDto";
    private static final String TASK_ID_ATTR = "taskId";
    private static final String SUCCESS_MESSAGE_ATTR = "successMessage";
    private static final String ERROR_MESSAGE_ATTR = "errorMessage";
    private static final String DROPDOWN_VALUES_ATTR = "dropdownValues";
    private static final String CATEGORY_ATTR = "category";
    private static final String CATEGORY_DISPLAY_NAME_ATTR = "categoryDisplayName";
    private static final String USERNAME_ATTR = "username";
    private static final String AUTHORITIES_ATTR = "authorities";

    private final TaskActivityService taskActivityService;
    private final DropdownConfig dropdownConfig;
    private final DropdownValueService dropdownValueService;
    private final WeeklyTimesheetService weeklyTimesheetService;
    private final UserService userService;
    private final TaskListSortConfig taskListSortConfig;

    public TaskActivityWebController(TaskActivityService taskActivityService,
            DropdownConfig dropdownConfig, DropdownValueService dropdownValueService,
            WeeklyTimesheetService weeklyTimesheetService, UserService userService,
            TaskListSortConfig taskListSortConfig) {
        this.taskActivityService = taskActivityService;
        this.dropdownConfig = dropdownConfig;
        this.dropdownValueService = dropdownValueService;
        this.weeklyTimesheetService = weeklyTimesheetService;
        this.userService = userService;
        this.taskListSortConfig = taskListSortConfig;
    }

    @GetMapping
    public String showMain(Model model, Authentication authentication) {
        // Add user info to model
        addUserInfo(model, authentication);
        // Redirect to task list when accessing /task-activity directly
        return REDIRECT_TASK_LIST;
    }

    @GetMapping("/add")
    public String showForm(Model model, Authentication authentication) {
        addUserInfo(model, authentication);
        model.addAttribute(TASK_ACTIVITY_DTO_ATTR, new TaskActivityDto());
        addDropdownOptions(model);
        return TASK_ACTIVITY_FORM_VIEW;
    }

    @GetMapping("/clone/{id}")
    public String cloneTask(@PathVariable Long id, @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Invalid task ID provided.");
                return buildFilteredRedirect(client, project, phase, username, startDate, endDate);
            }

            Optional<TaskActivity> taskActivity = taskActivityService.getTaskActivityById(id);
            if (taskActivity.isPresent()) {
                TaskActivityDto dto = convertEntityToDto(taskActivity.get());
                // Set today's date as default for cloned task
                dto.setTaskDate(LocalDate.now());

                addUserInfo(model, authentication);
                model.addAttribute(TASK_ACTIVITY_DTO_ATTR, dto);
                addDropdownOptions(model);
                addFilterAttributes(model, client, project, phase, username, startDate, endDate);
                return TASK_ACTIVITY_FORM_VIEW;
            } else {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Task not found with ID: " + id);
                return buildFilteredRedirect(client, project, phase, username, startDate, endDate);
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to clone task: " + e.getMessage());
            return REDIRECT_TASK_LIST;
        }
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST', 'EXPENSE_ADMIN')")
    @PostMapping("/submit")
    public String submitForm(@Valid @ModelAttribute TaskActivityDto taskActivityDto,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes,
            Authentication authentication) {

        // Access the logged-in user
        String username = authentication.getName();
        logger.info("User {} submitting form", username);

        if (bindingResult.hasErrors()) {
            addDropdownOptions(model);
            return TASK_ACTIVITY_FORM_VIEW;
        }

        try {
            // Set the username from the authenticated user
            taskActivityDto.setUsername(username);
            taskActivityService.createTaskActivity(taskActivityDto);
            
            // Keep aLL fields populated after successful submission to allow quick entry of similar
            // tasks
            model.addAttribute(TASK_ACTIVITY_DTO_ATTR, taskActivityDto);
            model.addAttribute(SUCCESS_MESSAGE_ATTR, "Task activity saved successfully!");
            addDropdownOptions(model);
            return TASK_ACTIVITY_FORM_VIEW;

        } catch (DataIntegrityViolationException e) {
            // Handle duplicate task entry
            logger.warn("Duplicate task entry attempt by user {}: {}", username, e.getMessage());
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "A task with the same date, client, project, phase, and details already exists.");
            model.addAttribute(TASK_ACTIVITY_DTO_ATTR, taskActivityDto);
            addDropdownOptions(model);
            return TASK_ACTIVITY_FORM_VIEW;

        } catch (Exception e) {
            logger.error("Error saving task activity for user {}: {}", username, e.getMessage());
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to save task activity: " + e.getMessage());
            model.addAttribute(TASK_ACTIVITY_DTO_ATTR, taskActivityDto);
            addDropdownOptions(model);
            return TASK_ACTIVITY_FORM_VIEW;
        }
    }

    @GetMapping("/list")
    public String showTaskList(@RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            Model model, Authentication authentication) {

        addUserInfo(model, authentication);

        // Create pageable with 20 items per page, using configured sort order
        Pageable pageable = PageRequest.of(page, 20, taskListSortConfig.createSort());

        // Check if user is admin - admins see all tasks, regular users see only their own
        boolean isUserAdmin = isAdmin(authentication);
        String currentUsername = isUserAdmin ? null : getUsername(authentication);

        // Determine which username to filter by
        // If admin and username filter is provided, use it; otherwise use current user's username
        String filterUsername =
                (isUserAdmin && username != null && !username.trim().isEmpty()) ? username
                        : currentUsername;

        // Fetch tasks based on filters and user role
        Page<TaskActivity> tasksPage = fetchTasksPage(client, project, startDate, endDate, pageable,
                isUserAdmin, filterUsername, phase);

        // Add pagination and filtering attributes to model
        addPaginationAttributes(model, tasksPage, page, tasksPage.getContent());
        addFilterAttributes(model, client, project, phase, username, startDate, endDate);
        addDropdownOptions(model);

        // Add users list for admin filter (only if user is admin)
        if (isUserAdmin) {
            model.addAttribute("users", userService.getAllUsers());
        }

        return TASK_LIST_VIEW;
    }

    private Page<TaskActivity> fetchTasksPage(String client, String project, LocalDate startDate,
            LocalDate endDate, Pageable pageable, boolean isUserAdmin, String username,
            String phase) {
        // Convert empty strings to null for proper SQL NULL handling
        String clientFilter = (client != null && !client.trim().isEmpty()) ? client : null;
        String projectFilter = (project != null && !project.trim().isEmpty()) ? project : null;
        String phaseFilter = (phase != null && !phase.trim().isEmpty()) ? phase : null;

        // Use the comprehensive filter method that handles ALL filter combinations
        return taskActivityService.getTaskActivitiesByFilters(username, clientFilter, projectFilter,
                phaseFilter, startDate, endDate, pageable);
    }

    private void addPaginationAttributes(Model model, Page<TaskActivity> tasksPage, int page,
            List<TaskActivity> filteredTasks) {
        model.addAttribute("tasks", filteredTasks);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", tasksPage.getTotalPages());
        model.addAttribute("totalElements", tasksPage.getTotalElements());
        model.addAttribute("hasNext", tasksPage.hasNext());
        model.addAttribute("hasPrevious", tasksPage.hasPrevious());

        int startEntry = page * 20 + 1;
        int endEntry = Math.min((page + 1) * 20, (int) tasksPage.getTotalElements());
        model.addAttribute("startEntry", startEntry);
        model.addAttribute("endEntry", endEntry);
    }

    private void addFilterAttributes(Model model, String client, String project, String phase,
            String username, LocalDate startDate, LocalDate endDate) {
        model.addAttribute("selectedClient", client);
        model.addAttribute("selectedProject", project);
        model.addAttribute("selectedPhase", phase);
        model.addAttribute("selectedUsername", username);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
    }

    @GetMapping("/detail/{id}")
    public String showTaskDetail(@PathVariable Long id,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            // Validate ID
            if (id == null || id <= 0) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Invalid task ID provided.");
                return buildFilteredRedirect(client, project, phase, username, startDate, endDate);
            }

            Optional<TaskActivity> taskActivity = taskActivityService.getTaskActivityById(id);
            if (taskActivity.isPresent()) {
                // Security check: verify task belongs to logged-in user (unless admin)
                boolean isUserAdmin = isAdmin(authentication);
                if (!isUserAdmin) {
                    String currentUsername = getUsername(authentication);
                    if (!taskActivity.get().getUsername().equals(currentUsername)) {
                        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                                "Access denied: You can only view your own tasks.");
                        return buildFilteredRedirect(client, project, phase, username, startDate,
                                endDate);
                    }
                }

                TaskActivityDto dto = convertEntityToDto(taskActivity.get());
                addUserInfo(model, authentication);
                model.addAttribute(TASK_ACTIVITY_DTO_ATTR, dto);
                model.addAttribute(TASK_ID_ATTR, id);

                // Check if this is a read-only view (admin viewing another user's task)
                String currentUsername = getUsername(authentication);
                boolean isReadOnly = !taskActivity.get().getUsername().equals(currentUsername);
                model.addAttribute("isReadOnly", isReadOnly);

                addDropdownOptions(model);
                addFilterAttributes(model, client, project, phase, username, startDate, endDate);
                return TASK_DETAIL_VIEW;
            } else {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Task not found with ID: " + id);
                return buildFilteredRedirect(client, project, phase, username, startDate, endDate);
            }
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Invalid task ID format. Please provide a valid number.");
            return REDIRECT_TASK_LIST;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Error loading task: " + e.getMessage());
            return REDIRECT_TASK_LIST;
        }
    }

    @GetMapping("/detail")
    public String showTaskDetailNoId(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                "Please select a task from the list to edit.");
        return REDIRECT_TASK_LIST;
    }

    @PostMapping("/update/{id}")
    public String updateTask(@PathVariable Long id,
            @Valid @ModelAttribute TaskActivityDto taskActivityDto, BindingResult bindingResult,
            @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model, RedirectAttributes redirectAttributes, Authentication authentication) {

        if (bindingResult.hasErrors()) {
            model.addAttribute(TASK_ID_ATTR, id);
            addDropdownOptions(model);
            return TASK_DETAIL_VIEW;
        }

        try {
            // Security check: verify task belongs to logged-in user before updating (unless admin)
            boolean isUserAdmin = isAdmin(authentication);
            if (!isUserAdmin) {
                String currentUsername = getUsername(authentication);
                Optional<TaskActivity> existingTask = taskActivityService.getTaskActivityById(id);
                if (existingTask.isPresent()
                        && !existingTask.get().getUsername().equals(currentUsername)) {
                    redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                            "Access denied: You can only update your own tasks.");
                    return buildFilteredRedirect(client, project, phase, username, startDate,
                            endDate);
                }
            }

            taskActivityService.updateTaskActivity(id, taskActivityDto);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Task activity updated successfully!");
            return buildFilteredRedirect(client, project, phase, username, startDate, endDate);

        } catch (Exception e) {
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to update task activity: " + e.getMessage());
            model.addAttribute(TASK_ID_ATTR, id);
            addDropdownOptions(model);
            return TASK_DETAIL_VIEW;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTask(@PathVariable Long id, @RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        try {
            // Security check: verify task belongs to logged-in user before deleting (unless admin)
            boolean isUserAdmin = isAdmin(authentication);
            if (!isUserAdmin) {
                String currentUsername = getUsername(authentication);
                Optional<TaskActivity> existingTask = taskActivityService.getTaskActivityById(id);
                if (existingTask.isPresent()
                        && !existingTask.get().getUsername().equals(currentUsername)) {
                    redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                            "Access denied: You can only delete your own tasks.");
                    return buildFilteredRedirect(client, project, phase, username, startDate,
                            endDate);
                }
            }

            taskActivityService.deleteTaskActivity(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Task activity deleted successfully!");
            return buildFilteredRedirect(client, project, phase, username, startDate, endDate);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to delete task activity: " + e.getMessage());
            return REDIRECT_TASK_LIST;
        }
    }

    /**
     * Convert TaskActivity entity to DTO
     */
    private TaskActivityDto convertEntityToDto(TaskActivity entity) {
        TaskActivityDto dto = new TaskActivityDto();
        dto.setTaskDate(entity.getTaskDate());
        dto.setClient(entity.getClient());
        dto.setProject(entity.getProject());
        dto.setPhase(entity.getPhase());
        dto.setHours(entity.getHours());
        dto.setDetails(entity.getDetails());
        dto.setUsername(entity.getUsername());
        return dto;
    }

    private void addDropdownOptions(Model model) {
        model.addAttribute("clients", dropdownConfig.getClientsList());
        model.addAttribute("projects", dropdownConfig.getProjectsList());
        model.addAttribute("phases", dropdownConfig.getPhasesList());
    }

    /**
     * Add user authentication information to the model for display in templates
     */
    private void addUserInfo(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute(USERNAME_ATTR, username);
            model.addAttribute(AUTHORITIES_ATTR, authentication.getAuthorities());

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

    /**
     * Check if the current user has ADMIN role
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Get username from authentication, with null safety
     */
    private String getUsername(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("User must be authenticated");
        }
        return authentication.getName();
    }

    @ExceptionHandler(NumberFormatException.class)
    public String handleNumberFormatException(NumberFormatException e,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                "Invalid task ID format. Please provide a valid task ID.");
        return REDIRECT_TASK_LIST;
    }

    @GetMapping("/manage-dropdowns")
    public String showDropdownManagement(Model model, Authentication authentication) {
        try {
            addUserInfo(model, authentication);
            // Get all dropdown values by category and subcategory
            List<DropdownValue> clients = dropdownValueService.getActiveClients();
            List<DropdownValue> projects = dropdownValueService.getActiveProjects();
            List<DropdownValue> phases = dropdownValueService.getActivePhases();

            model.addAttribute("clients", clients);
            model.addAttribute("projects", projects);
            model.addAttribute("phases", phases);

            return DROPDOWN_MANAGEMENT_SIMPLE_VIEW;
        } catch (Exception e) {
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "Error accessing dropdown management: " + e.getMessage());
            return TASK_LIST_VIEW;
        }
    }

    @GetMapping("/manage-clients")
    public String showClientManagement(Model model, Authentication authentication) {
        try {
            addUserInfo(model, authentication);
            List<DropdownValue> clients = dropdownValueService.getActiveClients();
            model.addAttribute(DROPDOWN_VALUES_ATTR, clients);
            model.addAttribute(CATEGORY_ATTR, DropdownValueService.SUBCATEGORY_CLIENT);
            model.addAttribute(CATEGORY_DISPLAY_NAME_ATTR, "Clients");
            return DROPDOWN_CATEGORY_MANAGEMENT_VIEW;
        } catch (Exception e) {
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "Error accessing client management: " + e.getMessage());
            return DROPDOWN_MANAGEMENT_SIMPLE_VIEW;
        }
    }

    @GetMapping("/manage-projects")
    public String showProjectManagement(Model model, Authentication authentication) {
        try {
            addUserInfo(model, authentication);
            List<DropdownValue> projects = dropdownValueService.getActiveProjects();
            model.addAttribute(DROPDOWN_VALUES_ATTR, projects);
            model.addAttribute(CATEGORY_ATTR, DropdownValueService.SUBCATEGORY_PROJECT);
            model.addAttribute(CATEGORY_DISPLAY_NAME_ATTR, "Projects");
            return DROPDOWN_CATEGORY_MANAGEMENT_VIEW;
        } catch (Exception e) {
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "Error accessing project management: " + e.getMessage());
            return DROPDOWN_MANAGEMENT_SIMPLE_VIEW;
        }
    }

    @GetMapping("/manage-phases")
    public String showPhaseManagement(Model model, Authentication authentication) {
        try {
            addUserInfo(model, authentication);
            List<DropdownValue> phases = dropdownValueService.getActivePhases();
            model.addAttribute(DROPDOWN_VALUES_ATTR, phases);
            model.addAttribute(CATEGORY_ATTR, DropdownValueService.SUBCATEGORY_PHASE);
            model.addAttribute(CATEGORY_DISPLAY_NAME_ATTR, "Phases");
            return DROPDOWN_CATEGORY_MANAGEMENT_VIEW;
        } catch (Exception e) {
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "Error accessing phase management: " + e.getMessage());
            return DROPDOWN_MANAGEMENT_SIMPLE_VIEW;
        }
    }

    @PostMapping("/add-dropdown")
    public String addDropdownValue(@RequestParam String category, @RequestParam String value,
            RedirectAttributes redirectAttributes) {
        try {
            dropdownValueService.createDropdownValue(category, "TASK", value);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Successfully added '"
                    + value
                    + "' to " + category.toLowerCase() + " dropdown.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to add dropdown value: " + e.getMessage());
        }

        return REDIRECT_DROPDOWN_MANAGEMENT;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/delete-dropdown/{id}")
    public String deleteDropdownValue(@PathVariable Long id,
            @RequestParam(required = false) String returnCategory,
            RedirectAttributes redirectAttributes, Authentication authentication) {

        String username = authentication.getName();
        logger.info("Admin {} deleting dropdown value with ID: {}", username, id);

        try {
            Optional<DropdownValue> dropdownValue = dropdownValueService.getDropdownValueById(id);
            if (dropdownValue.isPresent()) {
                String deletedValue = dropdownValue.get().getItemValue();
                String category = dropdownValue.get().getCategory();
                dropdownValueService.deleteDropdownValue(id);
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                        "Successfully deleted " + category.toLowerCase() + ": " + deletedValue);

                return REDIRECT_DROPDOWN_MANAGEMENT;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to delete dropdown value: " + e.getMessage());
        }

        return REDIRECT_DROPDOWN_MANAGEMENT;
    }

    @PostMapping("/toggle-dropdown/{id}")
    public String toggleDropdownValue(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            DropdownValue updated = dropdownValueService.toggleActiveStatus(id);
            String status =
                    Boolean.TRUE.equals(updated.getIsActive()) ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Successfully " + status
                    + " "
                    + updated.getCategory().toLowerCase() + ": " + updated.getItemValue());

            return REDIRECT_DROPDOWN_MANAGEMENT;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to toggle dropdown value: " + e.getMessage());
        }

        return REDIRECT_DROPDOWN_MANAGEMENT;
    }

    @GetMapping("/weekly-timesheet")
    public String showWeeklyTimesheet(@RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model,
            Authentication authentication) {
        try {
            addUserInfo(model, authentication);

            WeeklyTimesheetService.WeeklyTimesheetData weeklyData;

            // Check if user is admin - admins see all tasks, regular users see only their own
            boolean isUserAdmin = isAdmin(authentication);

            if (isUserAdmin) {
                // Admin sees all tasks
                if (date != null) {
                    weeklyData = weeklyTimesheetService.getWeeklyTimesheet(date);
                } else {
                    weeklyData = weeklyTimesheetService.getCurrentWeekTimesheet();
                }
            } else {
                // Regular users see only their own tasks
                String username = getUsername(authentication);
                if (date != null) {
                    weeklyData = weeklyTimesheetService.getWeeklyTimesheet(date, username);
                } else {
                    weeklyData = weeklyTimesheetService.getCurrentWeekTimesheet(username);
                }
            }

            model.addAttribute("weeklyData", weeklyData);
            return WEEKLY_TIMESHEET_VIEW;

        } catch (Exception e) {
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "Error loading weekly timesheet: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/list/export-csv")
    @ResponseBody
    public String exportTaskListToCsv(@RequestParam(required = false) String client,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String phase,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        // Check if user is admin - admins see all tasks, regular users see only their own
        boolean isUserAdmin = isAdmin(authentication);
        String currentUsername = isUserAdmin ? null : getUsername(authentication);

        // Determine which username to filter by
        String filterUsername =
                (isUserAdmin && username != null && !username.trim().isEmpty()) ? username
                        : currentUsername;

        // Convert empty strings to null for proper SQL NULL handling
        String clientFilter = (client != null && !client.trim().isEmpty()) ? client : null;
        String projectFilter = (project != null && !project.trim().isEmpty()) ? project : null;
        String phaseFilter = (phase != null && !phase.trim().isEmpty()) ? phase : null;

        // Fetch ALL tasks based on filters using the comprehensive filter method (no pagination)
        // Use Pageable.unpaged() to get all results without pagination
        Page<TaskActivity> allTasksPage =
                taskActivityService.getTaskActivitiesByFilters(filterUsername, clientFilter,
                        projectFilter, phaseFilter, startDate, endDate, Pageable.unpaged());

        List<TaskActivity> filteredTasks = allTasksPage.getContent();

        // Generate CSV
        return generateCsvFromTasks(filteredTasks, isUserAdmin);
    }

    private String generateCsvFromTasks(List<TaskActivity> tasks, boolean includeUsername) {
        StringBuilder csv = new StringBuilder();

        // Add header row
        if (includeUsername) {
            csv.append("Date,Client,Project,Phase,Hours,Details,Username\n");
        } else {
            csv.append("Date,Client,Project,Phase,Hours,Details\n");
        }

        // Sort tasks by date (newest first), then by client, then by project
        List<TaskActivity> sortedTasks = tasks.stream().sorted((t1, t2) -> {
            int dateCompare = t2.getTaskDate().compareTo(t1.getTaskDate());
            if (dateCompare != 0)
                return dateCompare;

            int clientCompare = t1.getClient().compareToIgnoreCase(t2.getClient());
            if (clientCompare != 0)
                return clientCompare;

            return t1.getProject().compareToIgnoreCase(t2.getProject());
        }).toList();

        // Add data rows
        for (TaskActivity task : sortedTasks) {
            csv.append(formatCsvField(task.getTaskDate().toString())).append(",");
            csv.append(escapeCsvField(task.getClient())).append(",");
            csv.append(escapeCsvField(task.getProject())).append(",");
            csv.append(escapeCsvField(task.getPhase())).append(",");
            csv.append(task.getHours()).append(",");
            csv.append(escapeCsvField(task.getDetails() != null ? task.getDetails() : ""));

            if (includeUsername) {
                csv.append(",").append(escapeCsvField(task.getUsername()));
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

    /**
     * Build a redirect URL to task list with filter parameters preserved
     */
    private String buildFilteredRedirect(String client, String project, String phase,
            String username, LocalDate startDate, LocalDate endDate) {
        StringBuilder redirect = new StringBuilder("redirect:/task-activity/list");
        java.util.List<String> params = new java.util.ArrayList<>();

        if (client != null && !client.isEmpty()) {
            params.add("client=" + client);
        }
        if (project != null && !project.isEmpty()) {
            params.add("project=" + project);
        }
        if (phase != null && !phase.isEmpty()) {
            params.add("phase=" + phase);
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
}

