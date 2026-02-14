package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.repository.DropdownValueRepository;
import org.springframework.stereotype.Service;

/**
 * Service for evaluating billability of tasks and expenses based on dropdown flags. Uses OR logic:
 * if ANY component (client/project/phase or client/project/type) is marked non-billable, the entire
 * entry is considered non-billable.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@Service
public class BillabilityService {

    private final DropdownValueRepository dropdownRepo;

    public BillabilityService(DropdownValueRepository dropdownRepo) {
        this.dropdownRepo = dropdownRepo;
    }

    /**
     * Evaluates if a task is billable based on client, project, and phase. Uses OR logic: if ANY
     * component is marked non-billable, returns false.
     *
     * @param client Client name from task
     * @param project Project name from task
     * @param phase Phase name from task
     * @return true if all components are billable, false if any is non-billable
     */
    public boolean isTaskBillable(String client, String project, String phase) {
        return !isNonBillable("TASK", "CLIENT", client)
                && !isNonBillable("TASK", "PROJECT", project)
                && !isNonBillable("TASK", "PHASE", phase);
    }

    /**
     * Evaluates if an expense is billable based on client, project, and expense type. Uses OR
     * logic: if ANY component is marked non-billable, returns false.
     *
     * @param client Client name from expense
     * @param project Project name from expense
     * @param expenseType Expense type from expense
     * @return true if all components are billable, false if any is non-billable
     */
    public boolean isExpenseBillable(String client, String project, String expenseType) {
        return !isNonBillable("EXPENSE", "CLIENT", client)
                && !isNonBillable("EXPENSE", "PROJECT", project)
                && !isNonBillable("EXPENSE", "EXPENSE_TYPE", expenseType);
    }

    /**
     * Checks if a specific dropdown value is marked as non-billable.
     *
     * @param category Category (TASK or EXPENSE)
     * @param subcategory Subcategory (CLIENT, PROJECT, PHASE, EXPENSE_TYPE)
     * @param value The dropdown value to check
     * @return true if the value is marked non-billable, false otherwise
     */
    private boolean isNonBillable(String category, String subcategory, String value) {
        if (value == null || value.trim().isEmpty()) {
            return false; // Null/empty values are considered billable
        }

        DropdownValue dropdown =
                dropdownRepo.findByCategoryAndSubcategoryAndItemValue(category, subcategory, value);

        if (dropdown == null) {
            // If dropdown not found, assume billable (fail-safe approach)
            return false;
        }

        return dropdown.getNonBillable() != null && dropdown.getNonBillable();
    }

    /**
     * Checks if a client is marked as non-billable for tasks.
     *
     * @param client Client name
     * @return true if client is non-billable
     */
    public boolean isClientNonBillable(String client) {
        return isNonBillable("TASK", "CLIENT", client);
    }

    /**
     * Checks if a project is marked as non-billable for tasks.
     *
     * @param project Project name
     * @return true if project is non-billable
     */
    public boolean isProjectNonBillable(String project) {
        return isNonBillable("TASK", "PROJECT", project);
    }

    /**
     * Checks if a phase is marked as non-billable for tasks.
     *
     * @param phase Phase name
     * @return true if phase is non-billable
     */
    public boolean isPhaseNonBillable(String phase) {
        return isNonBillable("TASK", "PHASE", phase);
    }

    /**
     * Checks if an expense type is marked as non-billable.
     *
     * @param expenseType Expense type name
     * @return true if expense type is non-billable
     */
    public boolean isExpenseTypeNonBillable(String expenseType) {
        return isNonBillable("EXPENSE", "EXPENSE_TYPE", expenseType);
    }
}
