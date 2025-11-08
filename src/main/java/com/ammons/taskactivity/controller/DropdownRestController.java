package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.service.DropdownValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Dropdown Management Used by Angular frontend
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/dropdowns")
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
public class DropdownRestController {

    private static final Logger logger = LoggerFactory.getLogger(DropdownRestController.class);

    private final DropdownValueService dropdownValueService;

    public DropdownRestController(DropdownValueService dropdownValueService) {
        this.dropdownValueService = dropdownValueService;
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
     * Get all dropdown values for a specific category (legacy endpoint)
     */
    @GetMapping("/{category}")
    public ResponseEntity<List<DropdownValue>> getDropdownsByCategory(
            @PathVariable String category) {
        logger.debug("REST API: Getting dropdown values for category: {}", category);
        List<DropdownValue> values = dropdownValueService.getAllValuesByCategory(category);
        return ResponseEntity.ok(values);
    }

    /**
     * Get clients
     */
    @GetMapping("/clients")
    public ResponseEntity<List<DropdownValue>> getClients() {
        logger.debug("REST API: Getting clients");
        List<DropdownValue> clients = dropdownValueService.getAllValuesByCategory("CLIENT");
        return ResponseEntity.ok(clients);
    }

    /**
     * Get projects
     */
    @GetMapping("/projects")
    public ResponseEntity<List<DropdownValue>> getProjects() {
        logger.debug("REST API: Getting projects");
        List<DropdownValue> projects = dropdownValueService.getAllValuesByCategory("PROJECT");
        return ResponseEntity.ok(projects);
    }

    /**
     * Get phases
     */
    @GetMapping("/phases")
    public ResponseEntity<List<DropdownValue>> getPhases() {
        logger.debug("REST API: Getting phases");
        List<DropdownValue> phases = dropdownValueService.getAllValuesByCategory("PHASE");
        return ResponseEntity.ok(phases);
    }

    /**
     * Add new dropdown value (ADMIN only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DropdownValue> addDropdownValue(
            @RequestBody DropdownValue dropdownValue) {
        logger.debug("REST API: Adding dropdown value: {} = {}", dropdownValue.getCategory(),
                dropdownValue.getItemValue());
        DropdownValue created = dropdownValueService
                .createDropdownValue(dropdownValue.getCategory(), dropdownValue.getItemValue());
        return ResponseEntity.ok(created);
    }

    /**
     * Update dropdown value (ADMIN only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
     * Delete dropdown value (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDropdownValue(@PathVariable Long id) {
        logger.debug("REST API: Deleting dropdown value with ID: {}", id);
        return dropdownValueService.getDropdownValueById(id).map(dropdown -> {
            dropdownValueService.deleteDropdownValue(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
