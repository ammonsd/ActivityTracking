package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.DropdownValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DropdownValueRepository - Data access layer for DropdownValue entities.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Repository
public interface DropdownValueRepository extends JpaRepository<DropdownValue, Long> {

    /**
     * Finds active values for a category
     */
    @Query("SELECT dropdownValue FROM DropdownValue dropdownValue WHERE dropdownValue.category = :category AND dropdownValue.isActive = true ORDER BY dropdownValue.displayOrder, dropdownValue.itemValue")
    public List<DropdownValue> findActiveByCategoryOrderByDisplayOrder(
            @Param("category") String category);

    /**
     * Finds ALL values for a category
     */
    @Query("SELECT dropdownValue FROM DropdownValue dropdownValue WHERE dropdownValue.category = :category ORDER BY dropdownValue.displayOrder, dropdownValue.itemValue")
    public List<DropdownValue> findByCategoryOrderByDisplayOrder(
            @Param("category") String category);

    /**
     * Checks if a value already exists for a category.
     * 
     * @deprecated Use existsByCategoryAndSubcategoryAndItemValueIgnoreCase instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public boolean existsByCategoryAndItemValueIgnoreCase(String category, String itemValue);

    /**
     * Checks if a value already exists for a category and subcategory.
     */
    public boolean existsByCategoryAndSubcategoryAndItemValueIgnoreCase(String category,
                    String subcategory, String itemValue);

    /**
     * Finds the highest display order for a category. Used to next display order number for a new
     * value
     */
    @Query("SELECT COALESCE(MAX(dropdownValue.displayOrder), 0) FROM DropdownValue dropdownValue WHERE dropdownValue.category = :category")
    public Integer findMaxDisplayOrderByCategory(@Param("category") String category);

    /**
     * Finds all dropdown values sorted by category, displayOrder, and itemValue
     */
    public List<DropdownValue> findAllByOrderByCategoryAscDisplayOrderAscItemValueAsc();
}
