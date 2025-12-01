package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.repository.DropdownValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DropdownValueService - Manage dropdown data
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Service
@Transactional
public class DropdownValueService {

    // Main categories
    public static final String CATEGORY_TASK = "TASK";
    public static final String CATEGORY_EXPENSE = "EXPENSE";

    // Task subcategories
    public static final String SUBCATEGORY_CLIENT = "CLIENT";
    public static final String SUBCATEGORY_PROJECT = "PROJECT";
    public static final String SUBCATEGORY_PHASE = "PHASE";

    // Expense subcategories

    public static final String SUBCATEGORY_EXPENSE_TYPE = "EXPENSE_TYPE";
    public static final String SUBCATEGORY_PAYMENT_METHOD = "PAYMENT_METHOD";
    public static final String SUBCATEGORY_EXPENSE_STATUS = "EXPENSE_STATUS";
    public static final String SUBCATEGORY_VENDOR = "VENDOR";
    public static final String SUBCATEGORY_CURRENCY = "CURRENCY";
    public static final String SUBCATEGORY_RECEIPT_STATUS = "RECEIPT_STATUS";

    /**
     * Repository dependency for data access operations.
     */
    private final DropdownValueRepository dropdownValueRepository;

    public DropdownValueService(DropdownValueRepository dropdownValueRepository) {
        this.dropdownValueRepository = dropdownValueRepository;
    }

    /**
     * Return active dropdown values
     */
    @Transactional(readOnly = true)
    public List<String> getActiveValuesByCategory(String category) {
        return dropdownValueRepository
        		// Convert Stream for functional operations
                .findActiveByCategoryOrderByDisplayOrder(category.toUpperCase()).stream()                                                                                // List to
                .map(DropdownValue::getItemValue) // Transform each DropdownValue to its string
                                                  // value
                .toList(); // Collect results back to List<String>
    }

    /**
     * Return all dropdown values
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getAllValuesByCategory(String category) {
        return dropdownValueRepository.findByCategoryOrderByDisplayOrder(category.toUpperCase());
    }

    /**
     * Return all dropdown values across all categories, sorted for display
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getAllDropdownValues() {
        return dropdownValueRepository
                .findAllByOrderByCategoryAscSubcategoryAscDisplayOrderAscItemValueAsc();
    }

    /**
     * Return all distinct categories from database
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return dropdownValueRepository.findAll().stream().map(DropdownValue::getCategory).distinct()
                .sorted().toList();
    }

    /**
     * Add new dropdown value with subcategory
     */
    public DropdownValue createDropdownValue(String category, String subcategory, String value) {
        // Check for duplicate values within a category and subcategory
        if (dropdownValueRepository.existsByCategoryAndSubcategoryAndItemValueIgnoreCase(
                category.toUpperCase(), subcategory.toUpperCase(), value)) {
            throw new RuntimeException(
                    "Value '" + value + "' already exists for category " + category
                            + " and subcategory " + subcategory);
        }

        Integer maxOrder =
                dropdownValueRepository.findMaxDisplayOrderByCategory(category.toUpperCase());

        // Create and configure new entity
        DropdownValue dropdownValue = new DropdownValue();
        dropdownValue.setCategory(category.toUpperCase()); // Format category name
        dropdownValue.setSubcategory(subcategory.toUpperCase()); // Format subcategory name
        dropdownValue.setItemValue(value);
        dropdownValue.setDisplayOrder(maxOrder + 1);
        dropdownValue.setIsActive(true);

        return dropdownValueRepository.save(dropdownValue);
    }

    /**
     * Update dropdown value
     */
    public DropdownValue updateDropdownValue(Long id, String value, Integer displayOrder,
            Boolean isActive) {
        Optional<DropdownValue> existing = dropdownValueRepository.findById(id);
        if (existing.isPresent()) {
            DropdownValue dropdownValue = existing.get();

            // Check for duplicate values within a category and subcategory
            if (!dropdownValue.getItemValue().equalsIgnoreCase(value) && dropdownValueRepository
                    .existsByCategoryAndSubcategoryAndItemValueIgnoreCase(
                            dropdownValue.getCategory(), dropdownValue.getSubcategory(), value)) {
                throw new RuntimeException("Value '" + value + "' already exists for category "
                        + dropdownValue.getCategory() + " and subcategory "
                        + dropdownValue.getSubcategory());
            }

            dropdownValue.setItemValue(value);
            dropdownValue.setDisplayOrder(displayOrder);
            dropdownValue.setIsActive(isActive);

            return dropdownValueRepository.save(dropdownValue);
        } else {
            throw new RuntimeException("Dropdown value not found with ID: " + id);
        }
    }

    /**
     * Delete a dropdown value.
     */
    public void deleteDropdownValue(Long id) {
        if (dropdownValueRepository.existsById(id)) {
            dropdownValueRepository.deleteById(id);
        } else {
            throw new RuntimeException("Dropdown value not found with ID: " + id);
        }
    }

    /**
     * Toggle active status
     */
    public DropdownValue toggleActiveStatus(Long id) {
        Optional<DropdownValue> existing = dropdownValueRepository.findById(id);
        if (existing.isPresent()) {
            DropdownValue dropdownValue = existing.get();
            dropdownValue.setIsActive(!dropdownValue.getIsActive()); // Toggle boolean state
            return dropdownValueRepository.save(dropdownValue);
        } else {
            throw new RuntimeException("Dropdown value not found with ID: " + id);
        }
    }

    @Transactional(readOnly = true)
    public Optional<DropdownValue> getDropdownValueById(Long id) {
        return dropdownValueRepository.findById(id);
    }

    /**
     * Get active values by category and subcategory (new pattern)
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveValuesByCategoryAndSubcategory(String category,
            String subcategory) {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                category.toUpperCase(), subcategory.toUpperCase());
    }

    /**
     * Get all values by category and subcategory (new pattern)
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getAllValuesByCategoryAndSubcategory(String category,
            String subcategory) {
        return dropdownValueRepository.findByCategoryAndSubcategoryOrderByDisplayOrder(
                category.toUpperCase(), subcategory.toUpperCase());
    }

    /**
     * Convenience methods for TASK subcategories
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveClients() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_TASK, SUBCATEGORY_CLIENT);
    }

    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveProjects() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_TASK, SUBCATEGORY_PROJECT);
    }

    @Transactional(readOnly = true)
    public List<DropdownValue> getActivePhases() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_TASK, SUBCATEGORY_PHASE);
    }

    /**
     * Convenience methods for EXPENSE subcategories
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveExpenseTypes() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_EXPENSE, SUBCATEGORY_EXPENSE_TYPE);
    }

    @Transactional(readOnly = true)
    public List<DropdownValue> getActivePaymentMethods() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_EXPENSE, SUBCATEGORY_PAYMENT_METHOD);
    }

    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveExpenseStatuses() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_EXPENSE, SUBCATEGORY_EXPENSE_STATUS);
    }

    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveVendors() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_EXPENSE, SUBCATEGORY_VENDOR);
    }

    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveCurrencies() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_EXPENSE, SUBCATEGORY_CURRENCY);
    }

    @Transactional(readOnly = true)
    public List<DropdownValue> getActiveReceiptStatuses() {
        return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                CATEGORY_EXPENSE, SUBCATEGORY_RECEIPT_STATUS);
    }
}
