package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * DropdownAdminController - MVC Controller
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Controller
@RequestMapping("/admin/dropdowns")
public class DropdownAdminController {

    private static final String SUCCESS_MESSAGE_ATTR = "successMessage";
    private static final String ERROR_MESSAGE_ATTR = "errorMessage";
    private static final String REDIRECT_ADMIN_DROPDOWNS = "redirect:/admin/dropdowns";

    private final DropdownValueService dropdownValueService;
    private final UserService userService;

    public DropdownAdminController(DropdownValueService dropdownValueService,
            UserService userService) {
        this.dropdownValueService = dropdownValueService;
        this.userService = userService;
    }

    /**
     * Display Dropdown Management Page
     */
    @GetMapping
    public String showDropdownManagement(Model model, Authentication authentication) {
        // No need to fetch data since the combined screen only shows navigation and add form
        model.addAttribute("newDropdownValue", new DropdownValue()); // For form binding
        addUserDisplayInfo(model, authentication);

        return "admin/dropdown-management";
    }

    /**
     * Form Submission Handler
     */
    @PostMapping("/add")
    public String addDropdownValue(@RequestParam String category, @RequestParam String value,
            RedirectAttributes redirectAttributes) {
        try {
            // Delegate business logic to service layer
            dropdownValueService.createDropdownValue(category, value);

            // Success message via flash attribute (available for next request only)
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Successfully added '"
                    + value
                    + "' to " + category.toLowerCase() + " dropdown.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to add dropdown value: " + e.getMessage());
        }

        return REDIRECT_ADMIN_DROPDOWNS;
    }

    /**
     * Show edit form for a dropdown value
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes, Authentication authentication) {
        Optional<DropdownValue> dropdownValue = dropdownValueService.getDropdownValueById(id);

        if (dropdownValue.isPresent()) {
            model.addAttribute("dropdownValue", dropdownValue.get());
            addUserDisplayInfo(model, authentication);
            return "admin/dropdown-edit";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Dropdown value not found with ID: " + id);
            return REDIRECT_ADMIN_DROPDOWNS;
        }
    }

    /**
     * Update a dropdown value
     */
    @PostMapping("/update/{id}")
    public String updateDropdownValue(@PathVariable Long id, @RequestParam String value,
            @RequestParam Integer displayOrder,
            @RequestParam(defaultValue = "false") Boolean isActive,
            RedirectAttributes redirectAttributes) {
        try {
            DropdownValue updated =
                    dropdownValueService.updateDropdownValue(id, value, displayOrder, isActive);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Successfully updated dropdown value.");

            // Redirect to appropriate category-specific page
            return getRedirectUrlForCategory(updated.getCategory());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to update dropdown value: " + e.getMessage());
        }

        return REDIRECT_ADMIN_DROPDOWNS;
    }

    /**
     * Toggle active status
     */
    @PostMapping("/toggle/{id}")
    public String toggleActiveStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            DropdownValue updated = dropdownValueService.toggleActiveStatus(id);
            String status =
                    Boolean.TRUE.equals(updated.getIsActive()) ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Successfully " + status + " dropdown value: " + updated.getItemValue());

            // Redirect to appropriate category-specific page
            return getRedirectUrlForCategory(updated.getCategory());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to toggle dropdown value: " + e.getMessage());
        }

        return REDIRECT_ADMIN_DROPDOWNS;
    }

    /**
     * Delete a dropdown value
     */
    @PostMapping("/delete/{id}")
    public String deleteDropdownValue(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<DropdownValue> dropdownValue = dropdownValueService.getDropdownValueById(id);
            if (dropdownValue.isPresent()) {
                String deletedValue = dropdownValue.get().getItemValue();
                String category = dropdownValue.get().getCategory();
                dropdownValueService.deleteDropdownValue(id);
                redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                        "Successfully deleted dropdown value: " + deletedValue);

                // Redirect to appropriate category-specific page
                return getRedirectUrlForCategory(category);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to delete dropdown value: " + e.getMessage());
        }

        return REDIRECT_ADMIN_DROPDOWNS;
    }

    /**
     * Helper method to determine redirect URL based on category
     */
    private String getRedirectUrlForCategory(String category) {
        if (category == null)
            return REDIRECT_ADMIN_DROPDOWNS;

        switch (category) {
            case DropdownValueService.CATEGORY_CLIENT:
                return "redirect:/admin/dropdowns/clients";
            case DropdownValueService.CATEGORY_PROJECT:
                return "redirect:/admin/dropdowns/projects";
            case DropdownValueService.CATEGORY_PHASE:
                return "redirect:/admin/dropdowns/phases";
            default:
                return REDIRECT_ADMIN_DROPDOWNS;
        }
    }

    /**
     * Display Client Management Page
     */
    @GetMapping("/clients")
    public String showClientManagement(Model model, Authentication authentication) {
        List<DropdownValue> clients =
                dropdownValueService.getAllValuesByCategory(DropdownValueService.CATEGORY_CLIENT);
        model.addAttribute("clients", clients);
        addUserDisplayInfo(model, authentication);
        return "admin/client-management";
    }

    /**
     * Add new client
     */
    @PostMapping("/clients/add")
    public String addClient(@RequestParam String value, RedirectAttributes redirectAttributes) {
        try {
            dropdownValueService.createDropdownValue(DropdownValueService.CATEGORY_CLIENT, value);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Successfully added client '" + value + "'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to add client: " + e.getMessage());
        }
        return "redirect:/admin/dropdowns/clients";
    }

    /**
     * Display Project Management Page
     */
    @GetMapping("/projects")
    public String showProjectManagement(Model model, Authentication authentication) {
        List<DropdownValue> projects =
                dropdownValueService.getAllValuesByCategory(DropdownValueService.CATEGORY_PROJECT);
        model.addAttribute("projects", projects);
        addUserDisplayInfo(model, authentication);
        return "admin/project-management";
    }

    /**
     * Add new project
     */
    @PostMapping("/projects/add")
    public String addProject(@RequestParam String value, RedirectAttributes redirectAttributes) {
        try {
            dropdownValueService.createDropdownValue(DropdownValueService.CATEGORY_PROJECT, value);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Successfully added project '" + value + "'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to add project: " + e.getMessage());
        }
        return "redirect:/admin/dropdowns/projects";
    }

    /**
     * Display Phase Management Page
     */
    @GetMapping("/phases")
    public String showPhaseManagement(Model model, Authentication authentication) {
        List<DropdownValue> phases =
                dropdownValueService.getAllValuesByCategory(DropdownValueService.CATEGORY_PHASE);
        model.addAttribute("phases", phases);
        addUserDisplayInfo(model, authentication);
        return "admin/phase-management";
    }

    /**
     * Add new phase
     */
    @PostMapping("/phases/add")
    public String addPhase(@RequestParam String value, RedirectAttributes redirectAttributes) {
        try {
            dropdownValueService.createDropdownValue(DropdownValueService.CATEGORY_PHASE, value);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Successfully added phase '" + value + "'.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to add phase: " + e.getMessage());
        }
        return "redirect:/admin/dropdowns/phases";
    }

    /**
     * Add user display information to the model
     */
    private void addUserDisplayInfo(Model model, Authentication authentication) {
        String username = authentication.getName();
        userService.getUserByUsername(username).ifPresent(user -> {
            String firstname = user.getFirstname();
            String lastname = user.getLastname();
            String displayName = (firstname + " " + lastname + " (" + username + ")").trim()
                    .replaceAll("\\s+", " ");
            model.addAttribute("userDisplayName", displayName);
        });
    }
}
