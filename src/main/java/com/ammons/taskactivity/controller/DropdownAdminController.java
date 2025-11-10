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
    private static final String REDIRECT_ADMIN_DROPDOWNS_CATEGORY =
            "redirect:/admin/dropdowns?category=";

    private final DropdownValueService dropdownValueService;
    private final UserService userService;

    public DropdownAdminController(DropdownValueService dropdownValueService,
            UserService userService) {
        this.dropdownValueService = dropdownValueService;
        this.userService = userService;
    }

    /**
     * Display Dropdown Management Page with optional category filtering
     */
    @GetMapping
    public String showDropdownManagement(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory, Model model,
            Authentication authentication) {

        List<DropdownValue> dropdownValues;
        List<String> allCategories;
        List<String> allSubcategories;

        // Get all distinct categories from database
        allCategories = dropdownValueService.getAllCategories();

        // If category filter is provided, fetch values for that category only
        // Otherwise, fetch all dropdown values
        if (category != null && !category.trim().isEmpty()) {
            String validCategory = category.toUpperCase();
            dropdownValues = dropdownValueService.getAllValuesByCategory(validCategory);
            model.addAttribute("selectedCategory", validCategory);
        } else {
            // Get all dropdown values across all categories
            dropdownValues = dropdownValueService.getAllDropdownValues();
            model.addAttribute("selectedCategory", "");
        }

        // Extract unique subcategories from the current dropdown values
        allSubcategories = dropdownValues.stream().map(DropdownValue::getSubcategory).distinct()
                .sorted().toList();

        // Apply subcategory filter if provided AND it exists in the current subcategories
        if (subcategory != null && !subcategory.trim().isEmpty()
                && allSubcategories.contains(subcategory)) {
            final String filterSubcategory = subcategory;
            dropdownValues = dropdownValues.stream()
                    .filter(dv -> filterSubcategory.equals(dv.getSubcategory())).toList();
            model.addAttribute("selectedSubcategory", subcategory);
        } else {
            model.addAttribute("selectedSubcategory", "");
        }

        // Add model attributes
        model.addAttribute("dropdownValues", dropdownValues);
        model.addAttribute("categories", allCategories);
        model.addAttribute("subcategories", allSubcategories);
        model.addAttribute("newDropdownValue", new DropdownValue());

        addUserDisplayInfo(model, authentication);

        return "admin/dropdown-management";
    }

    /**
     * Form Submission Handler
     */
    @PostMapping("/add")
    public String addDropdownValue(@RequestParam String category, @RequestParam String subcategory,
            @RequestParam String value,
            RedirectAttributes redirectAttributes) {
        try {
            // Delegate business logic to service layer
            dropdownValueService.createDropdownValue(category, subcategory, value);

            // Success message via flash attribute (available for next request only)
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR, "Successfully added '"
                    + value + "' to " + category.toLowerCase() + " / " + subcategory.toLowerCase()
                    + " dropdown.");

            // Redirect back to the same category
            return REDIRECT_ADMIN_DROPDOWNS_CATEGORY + category.toUpperCase();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to add dropdown value: " + e.getMessage());
        }

        return REDIRECT_ADMIN_DROPDOWNS;
    }

    /**
     * Add New Category with Initial Value
     */
    @PostMapping("/add-category")
    public String addNewCategory(@RequestParam String category, @RequestParam String subcategory,
            @RequestParam String value, RedirectAttributes redirectAttributes) {
        try {
            // Ensure category is uppercase
            String upperCategory = category.toUpperCase().trim();

            // Check if category already exists
            List<String> existingCategories = dropdownValueService.getAllCategories();
            if (existingCategories.contains(upperCategory)) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                        "Category '" + upperCategory + "' already exists.");
                return REDIRECT_ADMIN_DROPDOWNS;
            }

            // Create the first value for the new category
            dropdownValueService.createDropdownValue(upperCategory, subcategory, value);

            // Success message
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE_ATTR,
                    "Successfully created new category '" + upperCategory + "' with initial value '"
                            + value + "'.");

            // Redirect to the new category
            return REDIRECT_ADMIN_DROPDOWNS_CATEGORY + upperCategory;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to create category: " + e.getMessage());
            return REDIRECT_ADMIN_DROPDOWNS;
        }
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

            // Redirect back to the category page
            return REDIRECT_ADMIN_DROPDOWNS_CATEGORY + updated.getCategory();
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

            // Redirect back to the category page
            return REDIRECT_ADMIN_DROPDOWNS_CATEGORY + updated.getCategory();
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

                // Redirect back to the category page
                return REDIRECT_ADMIN_DROPDOWNS_CATEGORY + category;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR,
                    "Failed to delete dropdown value: " + e.getMessage());
        }

        return REDIRECT_ADMIN_DROPDOWNS;
    }

    /**
     * Backwards compatible redirect - Client Management Page
     */
    @GetMapping("/clients")
    public String redirectToClientManagement() {
        return "redirect:/admin/dropdowns?category=CLIENT";
    }

    /**
     * Backwards compatible redirect - Project Management Page
     */
    @GetMapping("/projects")
    public String redirectToProjectManagement() {
        return "redirect:/admin/dropdowns?category=PROJECT";
    }

    /**
     * Backwards compatible redirect - Phase Management Page
     */
    @GetMapping("/phases")
    public String redirectToPhaseManagement() {
        return "redirect:/admin/dropdowns?category=PHASE";
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

    /**
     * Export all dropdown values as CSV
     */
    @GetMapping("/export-csv")
    @ResponseBody
    public String exportDropdownValuesToCsv(@RequestParam(required = false) String category) {

        List<DropdownValue> dropdownValues;

        // Apply category filter if provided
        if (category != null && !category.trim().isEmpty()) {
            dropdownValues = dropdownValueService.getAllValuesByCategory(category);
        } else {
            // Get all dropdowns from all categories dynamically
            dropdownValues = dropdownValueService.getAllDropdownValues();
        }

        // Sort by category, then display order, then item value
        dropdownValues.sort((d1, d2) -> {
            int categoryCompare = d1.getCategory().compareToIgnoreCase(d2.getCategory());
            if (categoryCompare != 0)
                return categoryCompare;

            int orderCompare = Integer.compare(d1.getDisplayOrder(), d2.getDisplayOrder());
            if (orderCompare != 0)
                return orderCompare;

            return d1.getItemValue().compareToIgnoreCase(d2.getItemValue());
        });

        return generateDropdownCsv(dropdownValues);
    }

    private String generateDropdownCsv(List<DropdownValue> dropdownValues) {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append("Category,Subcategory,Item Value,Display Order,Active\n");

        // Data rows
        for (DropdownValue dv : dropdownValues) {
            csv.append(escapeCsvField(dv.getCategory())).append(",");
            csv.append(escapeCsvField(dv.getSubcategory())).append(",");
            csv.append(escapeCsvField(dv.getItemValue())).append(",");
            csv.append(dv.getDisplayOrder()).append(",");
            csv.append(dv.getIsActive());
            csv.append("\n");
        }

        return csv.toString();
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}

