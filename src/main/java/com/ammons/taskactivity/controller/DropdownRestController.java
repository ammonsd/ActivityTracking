package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.UserDropdownAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ammons.taskactivity.security.RequirePermission;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Dropdown Management Used by Angular frontend
 *
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@RestController
@RequestMapping("/api/dropdowns")
public class DropdownRestController {

    private static final Logger logger = LoggerFactory.getLogger(DropdownRestController.class);

    /**
     * Modified by: Dean Ammons - February 2026 Change: Added UserDropdownAccessService for
     * user-filtered client/project endpoints Reason: Restrict which clients and projects the
     * Angular/React frontend sees per user
     */
    private final DropdownValueService dropdownValueService;
    private final UserDropdownAccessService userDropdownAccessService;

    public DropdownRestController(DropdownValueService dropdownValueService,
            UserDropdownAccessService userDropdownAccessService) {
        this.dropdownValueService = dropdownValueService;
        this.userDropdownAccessService = userDropdownAccessService;
    }

    /**
     * Get all categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        logger.debug("REST API: Getting all categories");
        List<String> categories = dropdownValueService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get all dropdown values across all categories
     */
    @GetMapping("/all")
    public ResponseEntity<List<DropdownValue>> getAllDropdownValues() {
        logger.debug("REST API: Getting all dropdown values");
        List<DropdownValue> values = dropdownValueService.getAllDropdownValues();
        return ResponseEntity.ok(values);
    }

    /**
     * Get all dropdown values for a specific category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<DropdownValue>> getValuesByCategory(@PathVariable String category) {
        logger.debug("REST API: Getting dropdown values for category: {}", category);
        List<DropdownValue> values = dropdownValueService.getAllValuesByCategory(category);
        return ResponseEntity.ok(values);
    }

    /**
     * Get clients filtered by the authenticated user's access assignments.
     */
    @GetMapping("/clients")
    public ResponseEntity<List<DropdownValue>> getClients(Authentication authentication) {
        logger.debug("REST API: Getting clients for user {}", authentication.getName());
        List<DropdownValue> clients =
                userDropdownAccessService.getAccessibleClients(authentication);
        return ResponseEntity.ok(clients);
    }

    /**
     * Get projects filtered by the authenticated user's access assignments.
     */
    @GetMapping("/projects")
    public ResponseEntity<List<DropdownValue>> getProjects(Authentication authentication) {
        logger.debug("REST API: Getting projects for user {}", authentication.getName());
        List<DropdownValue> projects =
                userDropdownAccessService.getAccessibleProjects(authentication);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get phases (now from TASK/PHASE)
     */
    @GetMapping("/phases")
    public ResponseEntity<List<DropdownValue>> getPhases() {
        logger.debug("REST API: Getting phases");
        List<DropdownValue> phases = dropdownValueService.getActivePhases();
        return ResponseEntity.ok(phases);
    }

    /**
     * Get expense types (from EXPENSE/EXPENSE_TYPE)
     */
    @GetMapping("/expense-types")
    public ResponseEntity<List<DropdownValue>> getExpenseTypes() {
        logger.debug("REST API: Getting expense types");
        List<DropdownValue> expenseTypes = dropdownValueService.getActiveExpenseTypes();
        return ResponseEntity.ok(expenseTypes);
    }

    /**
     * Get payment methods (from EXPENSE/PAYMENT_METHOD)
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<List<DropdownValue>> getPaymentMethods() {
        logger.debug("REST API: Getting payment methods");
        List<DropdownValue> paymentMethods = dropdownValueService.getActivePaymentMethods();
        return ResponseEntity.ok(paymentMethods);
    }

    /**
     * Get currencies (from EXPENSE/CURRENCY)
     */
    @GetMapping("/currencies")
    public ResponseEntity<List<DropdownValue>> getCurrencies() {
        logger.debug("REST API: Getting currencies");
        List<DropdownValue> currencies = dropdownValueService.getActiveCurrencies();
        return ResponseEntity.ok(currencies);
    }

    /**
     * Get vendors (from EXPENSE/VENDOR)
     */
    @GetMapping("/vendors")
    public ResponseEntity<List<DropdownValue>> getVendors() {
        logger.debug("REST API: Getting vendors");
        List<DropdownValue> vendors = dropdownValueService.getActiveVendors();
        return ResponseEntity.ok(vendors);
    }

    /**
     * Add new dropdown value (ADMIN only). Creates a new dropdown option for the specified category
     * and subcategory.
     * 
     * @param dropdownValue the dropdown value to create
     * @return ResponseEntity containing the created dropdown value
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "CREATE")
    @PostMapping
    public ResponseEntity<DropdownValue> addDropdownValue(
            @RequestBody DropdownValue dropdownValue) {
        logger.debug("REST API: Adding dropdown value: {} / {} = {}", dropdownValue.getCategory(),
                dropdownValue.getSubcategory(), dropdownValue.getItemValue());
        DropdownValue created =
                dropdownValueService.createDropdownValue(dropdownValue.getCategory(),
                        dropdownValue.getSubcategory(),
                        dropdownValue.getItemValue());
        return ResponseEntity.ok(created);
    }

    /**
     * Update dropdown value (ADMIN only). Modifies the item value, display order, or active status
     * of an existing dropdown option.
     * 
     * @param id the dropdown value ID to update
     * @param dropdownValue the updated dropdown value data
     * @return ResponseEntity containing the updated dropdown value
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<DropdownValue> updateDropdownValue(@PathVariable Long id,
            @RequestBody DropdownValue dropdownValue) {
        logger.debug("REST API: Updating dropdown value with ID: {}", id);
        return dropdownValueService.getDropdownValueById(id).map(existing -> {
            DropdownValue updated =
                    dropdownValueService.updateDropdownValue(id, dropdownValue.getItemValue(),
                            dropdownValue.getDisplayOrder(), dropdownValue.getIsActive());
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete dropdown value (ADMIN only). Permanently removes a dropdown option from the database.
     * 
     * @param id the dropdown value ID to delete
     * @return ResponseEntity with no content on success
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDropdownValue(@PathVariable Long id) {
        logger.debug("REST API: Deleting dropdown value with ID: {}", id);
        return dropdownValueService.getDropdownValueById(id).map(dropdown -> {
            dropdownValueService.deleteDropdownValue(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
