package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.entity.UserDropdownAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Description: Repository for managing UserDropdownAccess entries. Provides queries to determine
 * which dropdown values (clients and projects) a given user is permitted to see. Values with
 * allUsers=true on DropdownValue are returned without requiring a row here.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@Repository
public interface UserDropdownAccessRepository extends JpaRepository<UserDropdownAccess, Long> {

    /**
     * Returns all active DropdownValue entries visible to a user for a given category and
     * subcategory. Includes values where allUsers=true OR the user has an explicit access row.
     * Ordered by displayOrder then itemValue.
     */
    @Query("""
            SELECT dv FROM DropdownValue dv
            WHERE dv.category = :category
              AND dv.subcategory = :subcategory
              AND dv.isActive = true
              AND (dv.allUsers = true
                   OR EXISTS (
                       SELECT 1 FROM UserDropdownAccess a
                       WHERE a.dropdownValue = dv
                         AND a.username = :username
                   ))
            ORDER BY dv.displayOrder, dv.itemValue
            """)
    List<DropdownValue> findAccessibleByUsernameAndCategoryAndSubcategory(
            @Param("username") String username, @Param("category") String category,
            @Param("subcategory") String subcategory);

    /**
     * Returns only explicitly assigned (non-allUsers) DropdownValue entries for a user. Used when
     * building profile notification emails — allUsers values are excluded because they represent
     * universal access and should not appear in the per-user assignment list. Ordered by
     * displayOrder then itemValue.
     */
    @Query("""
                    SELECT dv FROM DropdownValue dv
                    JOIN UserDropdownAccess a ON a.dropdownValue = dv
                    WHERE a.username = :username
                      AND dv.category = :category
                      AND dv.subcategory = :subcategory
                      AND dv.isActive = true
                    ORDER BY dv.displayOrder, dv.itemValue
                    """)
    List<DropdownValue> findExplicitAssignmentsByUsernameAndCategoryAndSubcategory(
                    @Param("username") String username, @Param("category") String category,
                    @Param("subcategory") String subcategory);

    /**
     * Returns all access rows for a given username. Used by the admin UI to display current
     * assignments.
     */
    List<UserDropdownAccess> findByUsername(String username);

    /**
     * Returns the set of dropdown value IDs a user has explicit access to. Useful for rendering
     * checkboxes in the admin assignment UI.
     */
    @Query("SELECT a.dropdownValue.id FROM UserDropdownAccess a WHERE a.username = :username")
    Set<Long> findDropdownValueIdsByUsername(@Param("username") String username);

    /**
     * Removes all access rows for a username in a specific category+subcategory. Used when saving
     * the admin assignment UI to replace existing assignments cleanly.
     */
    @Modifying
    @Query("""
            DELETE FROM UserDropdownAccess a
            WHERE a.username = :username
              AND a.dropdownValue.category = :category
              AND a.dropdownValue.subcategory = :subcategory
            """)
    void deleteByUsernameAndCategoryAndSubcategory(@Param("username") String username,
            @Param("category") String category, @Param("subcategory") String subcategory);

    /**
     * Removes all access rows for a given username across all categories.
     */
    @Modifying
    void deleteByUsername(String username);

    /**
     * Returns true if an explicit access row exists for the user and dropdown value.
     */
    boolean existsByUsernameAndDropdownValue(String username, DropdownValue dropdownValue);
}
