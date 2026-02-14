package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.repository.DropdownValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BillabilityService ensuring correct evaluation of billable vs non-billable tasks
 * and expenses using OR logic.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@ExtendWith(MockitoExtension.class)
class BillabilityServiceTest {

    @Mock
    private DropdownValueRepository dropdownRepo;

    @InjectMocks
    private BillabilityService billabilityService;

    private DropdownValue billableClient;
    private DropdownValue nonBillableClient;
    private DropdownValue billableProject;
    private DropdownValue nonBillableProject;
    private DropdownValue billablePhase;
    private DropdownValue nonBillablePhase;
    private DropdownValue billableExpenseType;
    private DropdownValue nonBillableExpenseType;

    @BeforeEach
    void setUp() {
        // Setup billable client
        billableClient = new DropdownValue("TASK", "CLIENT", "Acme Corp", 1, true, false);

        // Setup non-billable client (Corporate)
        nonBillableClient = new DropdownValue("TASK", "CLIENT", "Corporate", 2, true, true);

        // Setup billable project
        billableProject = new DropdownValue("TASK", "PROJECT", "Consulting", 1, true, false);

        // Setup non-billable project
        nonBillableProject = new DropdownValue("TASK", "PROJECT", "Training", 2, true, true);

        // Setup billable phase
        billablePhase = new DropdownValue("TASK", "PHASE", "Development", 1, true, false);

        // Setup non-billable phase
        nonBillablePhase = new DropdownValue("TASK", "PHASE", "PTO", 2, true, true);

        // Setup billable expense type
        billableExpenseType =
                new DropdownValue("EXPENSE", "EXPENSE_TYPE", "Travel - Airfare", 1, true, false);

        // Setup non-billable expense type
        nonBillableExpenseType = new DropdownValue("EXPENSE", "EXPENSE_TYPE",
                "Home Office - Internet", 2, true, true);
    }

    // ===== Task Billability Tests =====

    @Test
    void shouldReturnBillableWhenAllTaskComponentsAreBillable() {
        // Given: All components are billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "CLIENT", "Acme Corp"))
                .thenReturn(billableClient);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PROJECT", "Consulting"))
                .thenReturn(billableProject);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PHASE", "Development"))
                .thenReturn(billablePhase);

        // When: Check task billability
        boolean result =
                billabilityService.isTaskBillable("Acme Corp", "Consulting", "Development");

        // Then: Should be billable
        assertTrue(result, "Task should be billable when all components are billable");
    }

    @Test
    void shouldReturnNonBillableWhenTaskClientFlagSet() {
        // Given: Client is non-billable (Corporate)
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "CLIENT", "Corporate"))
                .thenReturn(nonBillableClient);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PROJECT", "Consulting"))
                .thenReturn(billableProject);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PHASE", "Development"))
                .thenReturn(billablePhase);

        // When: Check task billability
        boolean result =
                billabilityService.isTaskBillable("Corporate", "Consulting", "Development");

        // Then: Should be non-billable due to client flag
        assertFalse(result, "Task should be non-billable when client flag is set");
    }

    @Test
    void shouldReturnNonBillableWhenTaskProjectFlagSet() {
        // Given: Project is non-billable (Training)
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "CLIENT", "Acme Corp"))
                .thenReturn(billableClient);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PROJECT", "Training"))
                .thenReturn(nonBillableProject);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PHASE", "Development"))
                .thenReturn(billablePhase);

        // When: Check task billability
        boolean result = billabilityService.isTaskBillable("Acme Corp", "Training", "Development");

        // Then: Should be non-billable due to project flag
        assertFalse(result, "Task should be non-billable when project flag is set");
    }

    @Test
    void shouldReturnNonBillableWhenTaskPhaseFlagSet() {
        // Given: Phase is non-billable (PTO)
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "CLIENT", "Acme Corp"))
                .thenReturn(billableClient);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PROJECT", "Consulting"))
                .thenReturn(billableProject);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PHASE", "PTO"))
                .thenReturn(nonBillablePhase);

        // When: Check task billability
        boolean result = billabilityService.isTaskBillable("Acme Corp", "Consulting", "PTO");

        // Then: Should be non-billable due to phase flag
        assertFalse(result, "Task should be non-billable when phase flag is set");
    }

    @Test
    void shouldReturnNonBillableWhenAnyTaskComponentFlagSet() {
        // Given: Multiple components are non-billable (testing OR logic)
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "CLIENT", "Corporate"))
                .thenReturn(nonBillableClient);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PROJECT", "Training"))
                .thenReturn(nonBillableProject);
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PHASE", "PTO"))
                .thenReturn(nonBillablePhase);

        // When: Check task billability
        boolean result = billabilityService.isTaskBillable("Corporate", "Training", "PTO");

        // Then: Should be non-billable (OR logic - any flag = non-billable)
        assertFalse(result,
                "Task should be non-billable when any component flag is set (OR logic)");
    }

    @Test
    void shouldReturnBillableWhenTaskDropdownNotFound() {
        // Given: Dropdown value not found (null)
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue(anyString(), anyString(),
                anyString())).thenReturn(null);

        // When: Check task billability
        boolean result = billabilityService.isTaskBillable("Unknown", "Unknown", "Unknown");

        // Then: Should default to billable (fail-safe)
        assertTrue(result, "Task should default to billable when dropdown not found");
    }

    @Test
    void shouldReturnBillableWhenTaskComponentIsNull() {
        // When: Check with null values
        boolean result = billabilityService.isTaskBillable(null, null, null);

        // Then: Should be billable (null values treated as billable)
        assertTrue(result, "Task should be billable when components are null");
    }

    // ===== Expense Billability Tests =====

    @Test
    void shouldReturnBillableWhenAllExpenseComponentsAreBillable() {
        // Given: All components are billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "CLIENT",
                "Acme Corp")).thenReturn(
                        new DropdownValue("EXPENSE", "CLIENT", "Acme Corp", 1, true, false));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "PROJECT",
                "Consulting")).thenReturn(
                        new DropdownValue("EXPENSE", "PROJECT", "Consulting", 1, true, false));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "EXPENSE_TYPE",
                "Travel - Airfare")).thenReturn(billableExpenseType);

        // When: Check expense billability
        boolean result =
                billabilityService.isExpenseBillable("Acme Corp", "Consulting", "Travel - Airfare");

        // Then: Should be billable
        assertTrue(result, "Expense should be billable when all components are billable");
    }

    @Test
    void shouldReturnNonBillableWhenExpenseClientFlagSet() {
        // Given: Client is non-billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "CLIENT",
                "Corporate")).thenReturn(
                        new DropdownValue("EXPENSE", "CLIENT", "Corporate", 1, true, true));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "PROJECT",
                "Consulting")).thenReturn(
                        new DropdownValue("EXPENSE", "PROJECT", "Consulting", 1, true, false));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "EXPENSE_TYPE",
                "Travel - Airfare")).thenReturn(billableExpenseType);

        // When: Check expense billability
        boolean result =
                billabilityService.isExpenseBillable("Corporate", "Consulting", "Travel - Airfare");

        // Then: Should be non-billable due to client flag
        assertFalse(result, "Expense should be non-billable when client flag is set");
    }

    @Test
    void shouldReturnNonBillableWhenExpenseProjectFlagSet() {
        // Given: Project is non-billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "CLIENT",
                "Acme Corp")).thenReturn(
                        new DropdownValue("EXPENSE", "CLIENT", "Acme Corp", 1, true, false));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "PROJECT",
                "Training")).thenReturn(
                        new DropdownValue("EXPENSE", "PROJECT", "Training", 1, true, true));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "EXPENSE_TYPE",
                "Travel - Airfare")).thenReturn(billableExpenseType);

        // When: Check expense billability
        boolean result =
                billabilityService.isExpenseBillable("Acme Corp", "Training", "Travel - Airfare");

        // Then: Should be non-billable due to project flag
        assertFalse(result, "Expense should be non-billable when project flag is set");
    }

    @Test
    void shouldReturnNonBillableWhenExpenseTypeFlagSet() {
        // Given: Expense type is non-billable (Home Office)
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "CLIENT",
                "Acme Corp")).thenReturn(
                        new DropdownValue("EXPENSE", "CLIENT", "Acme Corp", 1, true, false));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "PROJECT",
                "Consulting")).thenReturn(
                        new DropdownValue("EXPENSE", "PROJECT", "Consulting", 1, true, false));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "EXPENSE_TYPE",
                "Home Office - Internet")).thenReturn(nonBillableExpenseType);

        // When: Check expense billability
        boolean result = billabilityService.isExpenseBillable("Acme Corp", "Consulting",
                "Home Office - Internet");

        // Then: Should be non-billable due to expense type flag
        assertFalse(result, "Expense should be non-billable when expense type flag is set");
    }

    @Test
    void shouldReturnNonBillableWhenAnyExpenseComponentFlagSet() {
        // Given: All components are non-billable (testing OR logic)
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "CLIENT",
                "Corporate")).thenReturn(
                        new DropdownValue("EXPENSE", "CLIENT", "Corporate", 1, true, true));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "PROJECT",
                "Training")).thenReturn(
                        new DropdownValue("EXPENSE", "PROJECT", "Training", 1, true, true));
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "EXPENSE_TYPE",
                "Home Office - Internet")).thenReturn(nonBillableExpenseType);

        // When: Check expense billability
        boolean result = billabilityService.isExpenseBillable("Corporate", "Training",
                "Home Office - Internet");

        // Then: Should be non-billable (OR logic - any flag = non-billable)
        assertFalse(result,
                "Expense should be non-billable when any component flag is set (OR logic)");
    }

    // ===== Individual Component Check Tests =====

    @Test
    void shouldIdentifyNonBillableClient() {
        // Given: Client is non-billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "CLIENT", "Corporate"))
                .thenReturn(nonBillableClient);

        // When: Check if client is non-billable
        boolean result = billabilityService.isClientNonBillable("Corporate");

        // Then: Should return true
        assertTrue(result, "Should identify Corporate client as non-billable");
    }

    @Test
    void shouldIdentifyBillableClient() {
        // Given: Client is billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "CLIENT", "Acme Corp"))
                .thenReturn(billableClient);

        // When: Check if client is non-billable
        boolean result = billabilityService.isClientNonBillable("Acme Corp");

        // Then: Should return false
        assertFalse(result, "Should identify Acme Corp client as billable");
    }

    @Test
    void shouldIdentifyNonBillableProject() {
        // Given: Project is non-billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PROJECT", "Training"))
                .thenReturn(nonBillableProject);

        // When: Check if project is non-billable
        boolean result = billabilityService.isProjectNonBillable("Training");

        // Then: Should return true
        assertTrue(result, "Should identify Training project as non-billable");
    }

    @Test
    void shouldIdentifyNonBillablePhase() {
        // Given: Phase is non-billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("TASK", "PHASE", "PTO"))
                .thenReturn(nonBillablePhase);

        // When: Check if phase is non-billable
        boolean result = billabilityService.isPhaseNonBillable("PTO");

        // Then: Should return true
        assertTrue(result, "Should identify PTO phase as non-billable");
    }

    @Test
    void shouldIdentifyNonBillableExpenseType() {
        // Given: Expense type is non-billable
        when(dropdownRepo.findByCategoryAndSubcategoryAndItemValue("EXPENSE", "EXPENSE_TYPE",
                "Home Office - Internet")).thenReturn(nonBillableExpenseType);

        // When: Check if expense type is non-billable
        boolean result = billabilityService.isExpenseTypeNonBillable("Home Office - Internet");

        // Then: Should return true
        assertTrue(result, "Should identify Home Office - Internet as non-billable");
    }
}
