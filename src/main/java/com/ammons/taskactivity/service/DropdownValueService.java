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

    public static final String CATEGORY_CLIENT = "CLIENT";
    public static final String CATEGORY_PROJECT = "PROJECT";
    public static final String CATEGORY_PHASE = "PHASE";

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
     * Return all dropdown values across all categories
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getAllDropdownValues() {
        return dropdownValueRepository.findAll();
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
     * Add new dropdown value
     */
    public DropdownValue createDropdownValue(String category, String value) {
        // Check for duplicate values within a category
        if (dropdownValueRepository.existsByCategoryAndItemValueIgnoreCase(category.toUpperCase(),
                value)) {
            throw new RuntimeException(
                    "Value '" + value + "' already exists for category " + category);
        }

        Integer maxOrder =
                dropdownValueRepository.findMaxDisplayOrderByCategory(category.toUpperCase());

        // Create and configure new entity
        DropdownValue dropdownValue = new DropdownValue();
        dropdownValue.setCategory(category.toUpperCase()); // Format category name
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

            // Check for duplicate values within a category
            if (!dropdownValue.getItemValue().equalsIgnoreCase(value) && dropdownValueRepository
                    .existsByCategoryAndItemValueIgnoreCase(dropdownValue.getCategory(), value)) {
                throw new RuntimeException("Value '" + value + "' already exists for category "
                        + dropdownValue.getCategory());
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
}
