package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.entity.UserDropdownAccess;
import com.ammons.taskactivity.repository.DropdownValueRepository;
import com.ammons.taskactivity.repository.UserDropdownAccessRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Description: Service for managing user access to Client and Project dropdown values. Controls
 * which clients and projects appear in dropdowns based on the authenticated user. ADMIN role
 * bypasses all restrictions and sees all active values. Values flagged allUsers=true on
 * DropdownValue are visible to every user without requiring an explicit access row.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@Service
@Transactional
public class UserDropdownAccessService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserDropdownAccessRepository userDropdownAccessRepository;
    private final DropdownValueRepository dropdownValueRepository;

    public UserDropdownAccessService(UserDropdownAccessRepository userDropdownAccessRepository,
            DropdownValueRepository dropdownValueRepository) {
        this.userDropdownAccessRepository = userDropdownAccessRepository;
        this.dropdownValueRepository = dropdownValueRepository;
    }

    /**
     * Returns the list of Client dropdown values accessible to the authenticated user. ADMIN role
     * receives all active clients. Other users receive only clients where allUsers=true or they
     * have an explicit access row.
     *
     * @param authentication the current user's authentication
     * @return accessible Client DropdownValue entries ordered by displayOrder, itemValue
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getAccessibleClients(Authentication authentication) {
        if (isAdmin(authentication)) {
            return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                    DropdownValueService.CATEGORY_TASK, DropdownValueService.SUBCATEGORY_CLIENT);
        }
        return userDropdownAccessRepository.findAccessibleByUsernameAndCategoryAndSubcategory(
                authentication.getName(), DropdownValueService.CATEGORY_TASK,
                DropdownValueService.SUBCATEGORY_CLIENT);
    }

    /**
     * Returns the list of Project dropdown values accessible to the authenticated user. ADMIN role
     * receives all active projects. Other users receive only projects where allUsers=true or they
     * have an explicit access row.
     *
     * @param authentication the current user's authentication
     * @return accessible Project DropdownValue entries ordered by displayOrder, itemValue
     */
    @Transactional(readOnly = true)
    public List<DropdownValue> getAccessibleProjects(Authentication authentication) {
        if (isAdmin(authentication)) {
            return dropdownValueRepository.findActiveByCategoryAndSubcategoryOrderByDisplayOrder(
                    DropdownValueService.CATEGORY_TASK, DropdownValueService.SUBCATEGORY_PROJECT);
        }
        return userDropdownAccessRepository.findAccessibleByUsernameAndCategoryAndSubcategory(
                authentication.getName(), DropdownValueService.CATEGORY_TASK,
                DropdownValueService.SUBCATEGORY_PROJECT);
    }

    /**
     * Returns the set of dropdown value IDs explicitly assigned to a username. Used by the admin
     * assignment UI to pre-populate checkboxes.
     *
     * @param username the username to look up
     * @return set of assigned dropdown value IDs
     */
    @Transactional(readOnly = true)
    public Set<Long> getAssignedDropdownValueIds(String username) {
        return userDropdownAccessRepository.findDropdownValueIdsByUsername(username);
    }

    /**
     * Replaces a user's client access assignments with the provided list of dropdown value IDs.
     * Deletes all existing client rows for the user then inserts the new ones.
     *
     * @param username the username to update
     * @param dropdownValueIds IDs of DropdownValue entries to assign
     */
    public void saveClientAssignments(String username, List<Long> dropdownValueIds) {
        saveAssignments(username, DropdownValueService.CATEGORY_TASK,
                DropdownValueService.SUBCATEGORY_CLIENT, dropdownValueIds);
    }

    /**
     * Replaces a user's project access assignments with the provided list of dropdown value IDs.
     * Deletes all existing project rows for the user then inserts the new ones.
     *
     * @param username the username to update
     * @param dropdownValueIds IDs of DropdownValue entries to assign
     */
    public void saveProjectAssignments(String username, List<Long> dropdownValueIds) {
        saveAssignments(username, DropdownValueService.CATEGORY_TASK,
                DropdownValueService.SUBCATEGORY_PROJECT, dropdownValueIds);
    }

    /**
     * Replaces a user's expense-category client access assignments.
     *
     * @param username the username to update
     * @param dropdownValueIds IDs of EXPENSE/CLIENT DropdownValue entries to assign
     */
    public void saveExpenseClientAssignments(String username, List<Long> dropdownValueIds) {
        saveAssignments(username, DropdownValueService.CATEGORY_EXPENSE,
                DropdownValueService.SUBCATEGORY_CLIENT, dropdownValueIds);
    }

    /**
     * Replaces a user's expense-category project access assignments.
     *
     * @param username the username to update
     * @param dropdownValueIds IDs of EXPENSE/PROJECT DropdownValue entries to assign
     */
    public void saveExpenseProjectAssignments(String username, List<Long> dropdownValueIds) {
        saveAssignments(username, DropdownValueService.CATEGORY_EXPENSE,
                DropdownValueService.SUBCATEGORY_PROJECT, dropdownValueIds);
    }

    /**
     * Replaces a user's expense type access assignments with the provided list of dropdown value
     * IDs.
     *
     * @param username the username to update
     * @param dropdownValueIds IDs of DropdownValue entries to assign
     */
    public void saveExpenseTypeAssignments(String username, List<Long> dropdownValueIds) {
        saveAssignments(username, DropdownValueService.CATEGORY_EXPENSE,
                DropdownValueService.SUBCATEGORY_EXPENSE_TYPE, dropdownValueIds);
    }

    /**
     * Replaces a user's payment method access assignments with the provided list of dropdown value
     * IDs.
     *
     * @param username the username to update
     * @param dropdownValueIds IDs of DropdownValue entries to assign
     */
    public void savePaymentMethodAssignments(String username, List<Long> dropdownValueIds) {
        saveAssignments(username, DropdownValueService.CATEGORY_EXPENSE,
                DropdownValueService.SUBCATEGORY_PAYMENT_METHOD, dropdownValueIds);
    }

    /**
     * Internal helper that deletes existing assignments for a category+subcategory then
     * bulk-inserts the new ones.
     */
    private void saveAssignments(String username, String category, String subcategory,
            List<Long> dropdownValueIds) {
        userDropdownAccessRepository.deleteByUsernameAndCategoryAndSubcategory(username, category,
                subcategory);

        if (dropdownValueIds != null && !dropdownValueIds.isEmpty()) {
            List<UserDropdownAccess> newRows = dropdownValueIds.stream().map(id -> {
                DropdownValue dv = dropdownValueRepository.getReferenceById(id);
                return new UserDropdownAccess(username, dv);
            }).toList();
            userDropdownAccessRepository.saveAll(newRows);
        }
    }

    /**
     * Removes all dropdown access rows for a user across all categories. Useful when deleting or
     * deactivating a user account.
     *
     * @param username the username whose access rows should be removed
     */
    public void removeAllAccessForUser(String username) {
        userDropdownAccessRepository.deleteByUsername(username);
    }

    /**
     * Returns true if the authenticated user has the ADMIN role.
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> ROLE_ADMIN.equals(auth.getAuthority()));
    }
}
