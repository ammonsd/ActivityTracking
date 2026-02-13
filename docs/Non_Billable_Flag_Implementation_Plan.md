# Non-Billable Flag Implementation Plan

**Author:** Dean Ammons  
**Date:** February 2026  
**Status:** Planning

## Executive Summary

Replace the current "magic string" approach to billability tracking (checking if `project === "Non-Billable"`) with a flexible flag-based system. Add a `non_billable` boolean column to the `dropdownvalues` table, allowing any Client, Project, Phase, or Expense Type to be marked as non-billable.

### Business Justification

The current approach uses `"Non-Billable"` as a special project name to identify non-billable work. This doesn't align with the corporate time entry system where:

- PTO is tracked as `Corporate/General Administration`, not `Corporate/Non-Billable`
- Different client/project combinations may have their own billability rules
- The system should match real-world business processes, not work around them

### Key Benefits

1. **Business Alignment**: Matches actual corporate time system combinations
2. **Flexibility**: Mark billability at Client, Project, Phase, or Expense Type level
3. **Maintainability**: No hardcoded string comparisons in code
4. **Future-Proof**: Business rules change via database, not code deployments
5. **Consistency**: Same approach works for both tasks and expenses

### Scope Coverage

**Tasks:** Billability evaluated using Client + Project + Phase flags  
**Expenses:** Billability evaluated using Client + Project + Expense Type flags  
**Reports:** All reports updated to use flag-based evaluation, plus new filter for Weekly Timesheet/Expense Sheet

## Billability Evaluation Logic

### Precedence Rule: ANY Non-Billable = Non-Billable (Logical OR)

When evaluating if a task or expense is billable:

**For Tasks:**

```
isBillable = NOT (client.nonBillable OR project.nonBillable OR phase.nonBillable)
```

**For Expenses:**

```
isBillable = NOT (client.nonBillable OR project.nonBillable OR expenseType.nonBillable)
```

**Examples for Tasks:**

| Client                  | Project                 | Phase                  | Result           | Reason           |
| ----------------------- | ----------------------- | ---------------------- | ---------------- | ---------------- |
| Corporate (billable)    | Gen Admin (billable)    | PTO (non-billable)     | **Non-Billable** | Phase flag set   |
| Corporate (billable)    | Training (non-billable) | Development (billable) | **Non-Billable** | Project flag set |
| Internal (non-billable) | Product Dev (billable)  | Development (billable) | **Non-Billable** | Client flag set  |
| Acme Corp (billable)    | Consulting (billable)   | Development (billable) | **Billable**     | No flags set     |

**Examples for Expenses:**

| Client               | Project                 | Expense Type                          | Result           | Reason           |
| -------------------- | ----------------------- | ------------------------------------- | ---------------- | ---------------- |
| Acme Corp (billable) | Consulting (billable)   | Travel - Airfare (billable)           | **Billable**     | No flags set     |
| Corporate (billable) | Training (non-billable) | Travel - Hotel (billable)             | **Non-Billable** | Project flag set |
| Acme Corp (billable) | Consulting (billable)   | Home Office - Internet (non-billable) | **Non-Billable** | Type flag set    |

**Rationale:** Conservative approach prevents accidentally billing non-billable work. Easier to explain and audit.

---

## Phase 0: Database Schema and Migration (PREREQUISITE)

**Status:** Not Started  
**Dependencies:** None  
**Duration:** 1-2 hours  
**Risk:** Low

### 0.1 Database Schema Changes

**File:** `src/main/resources/db/migration/V{next}_add_non_billable_flag.sql`

```sql
-- Add non_billable column to dropdownvalues table
ALTER TABLE public.dropdownvalues
ADD COLUMN non_billable BOOLEAN NOT NULL DEFAULT FALSE;

-- Add index for performance on billability queries
CREATE INDEX idx_dropdownvalues_non_billable
ON public.dropdownvalues(non_billable)
WHERE non_billable = TRUE;

-- Update existing "Non-Billable" project entries
UPDATE public.dropdownvalues
SET non_billable = TRUE
WHERE category = 'TASK'
  AND subcategory = 'PROJECT'
  AND itemvalue = 'Non-Billable';

UPDATE public.dropdownvalues
SET non_billable = TRUE
WHERE category = 'EXPENSE'
  AND subcategory = 'PROJECT'
  AND itemvalue = 'Non-Billable';

-- Add comment to document the column
COMMENT ON COLUMN public.dropdownvalues.non_billable IS
'Flag indicating if this dropdown value represents non-billable work. Uses OR logic: if any component (client/project/phase) is non-billable, the entire entry is non-billable.';
```

### 0.2 Update Seed Data

**File:** `src/main/resources/data.sql`

Update INSERT statements to include `non_billable` column:

```sql
INSERT INTO public.dropdownvalues
(category, subcategory, itemvalue, displayorder, isactive, non_billable)
VALUES
  -- Mark specific phases as non-billable
  ('TASK', 'PHASE', 'PTO', 6, true, true),
  ('TASK', 'PHASE', 'Holiday', 2, true, true),

  -- Non-Billable project for backward compatibility
  ('TASK', 'PROJECT', 'Non-Billable', 2, true, true),
  ('EXPENSE', 'PROJECT', 'Non-Billable', 2, true, true),

  -- All other entries default to false (billable)
  ('TASK', 'PHASE', 'Development', 1, true, false),
  -- ... etc
```

### 0.3 Testing

**Verification Steps:**

1. Run migration on test database
2. Verify column added with correct default
3. Verify index created
4. Verify existing "Non-Billable" entries updated
5. Test rollback script
6. Verify no data loss or corruption

**Rollback Script:**

```sql
-- Remove index
DROP INDEX IF EXISTS idx_dropdownvalues_non_billable;

-- Remove column
ALTER TABLE public.dropdownvalues DROP COLUMN IF EXISTS non_billable;
```

### 0.4 Deliverables

- [x] Migration script created
- [x] Seed data updated
- [x] Rollback script created
- [x] Migration tested on local database
- [x] Documentation updated

---

## Phase 1: Spring Boot Backend Implementation

**Status:** Not Started  
**Dependencies:** Phase 0 complete  
**Duration:** 4-6 hours  
**Risk:** Medium

### 1.1 Entity Model Update

**File:** `src/main/java/com/ammons/taskactivity/entity/DropdownValue.java`

Add `nonBillable` field:

```java
@Column(name = "non_billable", nullable = false)
private Boolean nonBillable = false;

public Boolean getNonBillable() {
    return nonBillable;
}

public void setNonBillable(Boolean nonBillable) {
    this.nonBillable = nonBillable;
}
```

Update `toString()`, constructor, and test fixtures.

### 1.2 DTO Updates

Update DTOs that expose dropdown values to clients:

**Files:**

- Any DTOs that serialize `DropdownValue` entities
- REST response objects for dropdown endpoints

Add `nonBillable` field to match entity.

### 1.3 Repository Methods

**File:** `src/main/java/com/ammons/taskactivity/repository/DropdownValueRepository.java`

Add query methods:

```java
/**
 * Find all non-billable dropdown values for a specific category and subcategory.
 */
List<DropdownValue> findByCategoryAndSubcategoryAndNonBillableTrue(
    String category, String subcategory);

/**
 * Find a specific dropdown value with its billability status.
 */
Optional<DropdownValue> findByCategoryAndSubcategoryAndItemValue(
    String category, String subcategory, String itemValue);
```

### 1.4 Service Layer - Billability Evaluation

**New File:** `src/main/java/com/ammons/taskactivity/service/BillabilityService.java`

```java
@Service
public class BillabilityService {

    private final DropdownValueRepository dropdownRepo;

    /**
     * Evaluates if a task is billable based on client, project, and phase.
     * Uses OR logic: if ANY component is marked non-billable, returns false.
     */
    public boolean isTaskBillable(String client, String project, String phase) {
        return !isNonBillable("TASK", "CLIENT", client)
            && !isNonBillable("TASK", "PROJECT", project)
            && !isNonBillable("TASK", "PHASE", phase);
    }

    /**
     * Evaluates if an expense is billable based on client, project, and type.
     */
    public boolean isExpenseBillable(String client, String project, String type) {
        return !isNonBillable("EXPENSE", "CLIENT", client)
            && !isNonBillable("EXPENSE", "PROJECT", project)
            && !isNonBillable("EXPENSE", "EXPENSE_TYPE", type);
    }

    private boolean isNonBillable(String category, String subcategory, String value) {
        return dropdownRepo.findByCategoryAndSubcategoryAndItemValue(
            category, subcategory, value)
            .map(DropdownValue::getNonBillable)
            .orElse(false);
    }
}
```

### 1.5 REST Controller Updates

**Files:**

- `DropdownValueRestController.java` - Include `nonBillable` in responses
- Any controllers that return task/expense summaries with billability info

Ensure `nonBillable` field is included in JSON responses.

### 1.6 Testing

**New Test File:** `src/test/java/com/ammons/taskactivity/service/BillabilityServiceTest.java`

```java
@SpringBootTest
class BillabilityServiceTest {

    @Test
    void shouldReturnNonBillableWhenClientFlagSet() {
        // Given: client marked as non-billable
        // When: check task billability
        // Then: returns false (non-billable)
    }

    @Test
    void shouldReturnNonBillableWhenProjectFlagSet() {
        // Similar test for project flag
    }

    @Test
    void shouldReturnNonBillableWhenPhaseFlagSet() {
        // Similar test for phase flag
    }

    @Test
    void shouldReturnBillableWhenNoFlagsSet() {
        // Given: all components billable
        // When: check task billability
        // Then: returns true (billable)
    }

    @Test
    void shouldReturnNonBillableWhenAnyFlagSet() {
        // Test OR logic: any flag = non-billable
    }

    // Expense-specific tests
    @Test
    void shouldReturnNonBillableForExpenseWhenClientFlagSet() {
        // Given: expense client marked as non-billable
        // When: check expense billability
        // Then: returns false (non-billable)
    }

    @Test
    void shouldReturnNonBillableForExpenseWhenTypeFlagSet() {
        // Given: expense type marked as non-billable
        // When: check expense billability
        // Then: returns false (non-billable)
    }

    @Test
    void shouldReturnBillableForExpenseWhenNoFlagsSet() {
        // Given: all expense components billable
        // When: check expense billability
        // Then: returns true (billable)
    }
}
```

**Additional Tests:**

- Update `DropdownValueRepositoryTest` with `nonBillable` scenarios
- Update integration tests for REST endpoints
- Test DTO serialization includes new field
- Add expense-specific test cases in `ExpenseRepositoryTest`

### 1.7 Deliverables

- [x] Entity updated with `nonBillable` field
- [x] DTOs updated
- [x] Repository methods added
- [x] `BillabilityService` created and tested
- [x] REST controllers updated
- [x] Unit tests written (>80% coverage)
- [x] Integration tests pass
- [x] No compilation errors
- [x] API documentation updated (if using Swagger/OpenAPI)

### 1.8 Validation Criteria

```bash
# Compile and test
.\mvnw.cmd clean test -DskipFrontend

# Verify all tests pass
# Verify no regression in existing functionality
# Start app and verify REST endpoints return nonBillable field
.\mvnw.cmd spring-boot:run
```

**Manual API Testing:**

```bash
# Get dropdown values, verify nonBillable field present
curl http://localhost:8080/api/dropdowns/TASK/PROJECT

# Verify response includes: "nonBillable": false/true
```

---

## Phase 2: React Dashboard Implementation

**Status:** Not Started  
**Dependencies:** Phase 1 complete  
**Duration:** 3-4 hours  
**Risk:** Low

### 2.1 Type Definitions

**File:** `frontend-react/src/types/dropdown.types.ts` (create if needed)

```typescript
export interface DropdownValue {
    id: number;
    category: string;
    subcategory: string;
    itemValue: string;
    displayOrder: number;
    isActive: boolean;
    nonBillable: boolean; // NEW FIELD
}
```

### 2.2 Dropdown Maintenance Component

**File:** `frontend-react/src/components/admin/DropdownMaintenance.tsx` (or similar)

**Changes:**

1. Add "Non-Billable" checkbox/toggle to edit form
2. Display non-billable indicator in dropdown list (badge/icon)
3. Update form validation and submission handlers

**UI Mockup:**

```
┌─ Edit Dropdown Value ──────────────┐
│ Category: [TASK          ▼]        │
│ Subcategory: [PHASE      ▼]        │
│ Value: [PTO              ]         │
│ Display Order: [6        ]         │
│                                     │
│ ☑ Active                            │
│ ☑ Non-Billable  (NEW)               │
│                                     │
│ [Cancel]  [Save]                   │
└─────────────────────────────────────┘
```

### 2.3 Dropdown List Display

Add visual indicator for non-billable items:

```tsx
<TableRow>
    <TableCell>{item.itemValue}</TableCell>
    <TableCell>
        {item.nonBillable && (
            <Chip
                label="Non-Billable"
                size="small"
                color="warning"
                icon={<BlockIcon />}
            />
        )}
    </TableCell>
    <TableCell>{item.isActive ? "✓" : "✗"}</TableCell>
    <TableCell>
        <IconButton onClick={() => handleEdit(item)}>
            <EditIcon />
        </IconButton>
    </TableCell>
</TableRow>
```

### 2.4 API Service Updates

**File:** `frontend-react/src/services/api.service.ts`

Ensure API calls include `nonBillable` in request/response bodies.

### 2.5 Testing

**Manual Testing Checklist:**

- [x] View dropdown list - non-billable items show indicator
- [x] Create new dropdown with non-billable flag
- [x] Edit existing dropdown - toggle flag on/off
- [x] Save changes - verify persisted to database
- [x] Verify API requests include nonBillable field
- [x] Test validation (all combinations)

**Component Tests (if using Jest/React Testing Library):**

```typescript
describe("DropdownMaintenance", () => {
    it("should display non-billable indicator for flagged items", () => {
        // Test rendering
    });

    it("should allow toggling non-billable flag", () => {
        // Test interaction
    });

    it("should save non-billable flag correctly", () => {
        // Test submission
    });
});
```

### 2.6 Deliverables

- [x] Type definitions updated
- [x] Edit form includes non-billable checkbox
- [x] List view shows non-billable indicator
- [x] API service updated
- [x] Manual testing complete
- [x] No console errors or warnings
- [x] Responsive design maintained

---

## Phase 3: Angular Dashboard Implementation

**Status:** Not Started  
**Dependencies:** Phase 1 complete  
**Duration:** 6-8 hours  
**Risk:** Medium-High (most complex changes, includes new filters)

### 3.1 Model/Interface Updates

**File:** `frontend/src/app/models/dropdown.model.ts`

```typescript
export interface DropdownValue {
    id: number;
    category: string;
    subcategory: string;
    itemValue: string;
    displayOrder: number;
    isActive: boolean;
    nonBillable: boolean; // NEW FIELD
}
```

### 3.2 Reports Service - Replace Magic Strings

**File:** `frontend/src/app/services/reports.service.ts`

**Current Code (lines 607-650):**

```typescript
// BEFORE: Uses magic string comparison
const billableHours = userTasks
    .filter((t) => t.project !== "Non-Billable") // ❌ REMOVE
    .reduce((sum, t) => sum + t.hours, 0);
```

**New Code:**

```typescript
// AFTER: Uses flag-based evaluation
const billableHours = userTasks
    .filter((t) => this.isTaskBillable(t)) // ✅ NEW METHOD
    .reduce((sum, t) => sum + t.hours, 0);
```

**New Helper Methods:**

```typescript
/**
 * Evaluates if a task is billable based on dropdown flags.
 * Uses OR logic: if ANY component (client/project/phase) is non-billable, returns false.
 */
private isTaskBillable(task: TaskActivityDto): boolean {
  return this.isBillable(task.client, 'CLIENT', 'TASK')
      && this.isBillable(task.project, 'PROJECT', 'TASK')
      && this.isBillable(task.phase, 'PHASE', 'TASK');
}

/**
 * Evaluates if an expense is billable based on dropdown flags.
 * Uses OR logic: if ANY component (client/project/type) is non-billable, returns false.
 */
private isExpenseBillable(expense: ExpenseDto): boolean {
  return this.isBillable(expense.client, 'CLIENT', 'EXPENSE')
      && this.isBillable(expense.project, 'PROJECT', 'EXPENSE')
      && this.isBillable(expense.expenseType, 'EXPENSE_TYPE', 'EXPENSE');
}

/**
 * Checks if a specific dropdown value is billable.
 */
private isBillable(value: string, subcategory: string, category: string): boolean {
  const dropdown = this.dropdownCache
    .find(d => d.category === category && d.subcategory === subcategory && d.itemValue === value);
  return !(dropdown?.nonBillable ?? false);
}

/**
 * Loads dropdown values into cache for billability checks.
 */
private async loadDropdownsForBillability(): Promise<void> {
  if (this.dropdownCache.length === 0) {
    // Load all dropdowns once
    this.dropdownCache = await this.dropdownService.getAllDropdowns().toPromise();
  }
}
```

### 3.3 Update All Report Components

**Files to Update:**

1. **User Summary Report** (`reports/user-summary/`)
    - Lines 610, 613, 619, 628: Replace `project !== 'Non-Billable'` checks

2. **Time Breakdown Report** (if exists)
    - Replace any hardcoded billability checks

3. **Client Summary Report** (if exists)
    - Replace any hardcoded billability checks

4. **Weekly Timesheet Report** (`reports/weekly-timesheet/`) - **NEW FILTER**
    - Add billability filter: "Billable", "Non-Billable", "All" (default: "All")
    - Update filtering logic to use flag-based evaluation

5. **Weekly Expense Sheet Report** (`reports/weekly-expense-sheet/`) - **NEW FILTER**
    - Add billability filter: "Billable", "Non-Billable", "All" (default: "All")
    - Add `isExpenseBillable()` method for expenses using Client/Project/Type

6. **Other Expense Reports** (if any)
    - Add billability checking for expenses using Client/Project/Type

**Pattern to Replace:**

```typescript
// Find all instances of:
.filter((t) => t.project !== 'Non-Billable')
.filter((t) => t.project === 'Non-Billable')

// Replace with:
.filter((t) => this.isTaskBillable(t))
.filter((t) => !this.isTaskBillable(t))
```

### 3.4 Weekly Timesheet Filter Implementation

**File:** `frontend/src/app/components/reports/weekly-timesheet/weekly-timesheet.component.ts`

**New Filter Options:**

```typescript
export type BillabilityFilter = "All" | "Billable" | "Non-Billable";

export class WeeklyTimesheetComponent implements OnInit {
    billabilityFilter: BillabilityFilter = "All"; // NEW - Default to 'All'

    // Filter options for dropdown
    billabilityOptions: BillabilityFilter[] = [
        "All",
        "Billable",
        "Non-Billable",
    ];

    // Apply filter to tasks
    get filteredTasks(): TaskActivityDto[] {
        let tasks = this.allTasks;

        // Apply billability filter
        if (this.billabilityFilter === "Billable") {
            tasks = tasks.filter((t) => this.isTaskBillable(t));
        } else if (this.billabilityFilter === "Non-Billable") {
            tasks = tasks.filter((t) => !this.isTaskBillable(t));
        }
        // 'All' shows everything - no filtering needed

        return tasks;
    }

    onBillabilityFilterChange(filter: BillabilityFilter): void {
        this.billabilityFilter = filter;
        // Trigger view refresh
    }
}
```

**Template Update:**

```html
<!-- Add filter dropdown to template -->
<mat-form-field>
    <mat-label>Billability</mat-label>
    <mat-select
        [(value)]="billabilityFilter"
        (selectionChange)="onBillabilityFilterChange($event.value)"
    >
        <mat-option *ngFor="let option of billabilityOptions" [value]="option">
            {{ option }}
        </mat-option>
    </mat-select>
</mat-form-field>
```

### 3.5 Weekly Expense Sheet Filter Implementation

**File:** `frontend/src/app/components/reports/weekly-expense-sheet/weekly-expense-sheet.component.ts`

**Similar Implementation:**

```typescript
export class WeeklyExpenseSheetComponent implements OnInit {
    billabilityFilter: BillabilityFilter = "All"; // NEW - Default to 'All'
    billabilityOptions: BillabilityFilter[] = [
        "All",
        "Billable",
        "Non-Billable",
    ];

    get filteredExpenses(): ExpenseDto[] {
        let expenses = this.allExpenses;

        // Apply billability filter
        if (this.billabilityFilter === "Billable") {
            expenses = expenses.filter((e) => this.isExpenseBillable(e));
        } else if (this.billabilityFilter === "Non-Billable") {
            expenses = expenses.filter((e) => !this.isExpenseBillable(e));
        }

        return expenses;
    }

    onBillabilityFilterChange(filter: BillabilityFilter): void {
        this.billabilityFilter = filter;
    }
}
```

**Template Update:** Same pattern as timesheet with mat-select dropdown

### 3.6 Dropdown Service Updates

**File:** `frontend/src/app/services/dropdown.service.ts`

Ensure service properly deserializes `nonBillable` field from API responses.

### 3.7 Testing

**Unit Tests:**

```typescript
describe("ReportsService - Billability Logic", () => {
    it("should identify task as non-billable when client flag set", () => {
        // Setup: client with nonBillable=true
        // Assert: isTaskBillable returns false
    });

    it("should identify task as non-billable when project flag set", () => {
        // Similar test
    });

    it("should identify task as non-billable when phase flag set", () => {
        // Similar test
    });

    it("should identify expense as non-billable when client flag set", () => {
        // Setup: expense client with nonBillable=true
        // Assert: isExpenseBillable returns false
    });

    it("should identify expense as non-billable when type flag set", () => {
        // Setup: expense type with nonBillable=true
        // Assert: isExpenseBillable returns false
    });

    it("should identify task as billable when no flags set", () => {
        // Setup: all components with nonBillable=false
        // Assert: isTaskBillable returns true
    });

    it("should use OR logic - any flag makes non-billable", () => {
        // Test combinations
    });
});
```

**Integration Tests:**

```typescript
describe("User Summary Report Integration", () => {
    it("should correctly calculate billable hours using flags", () => {
        // Mock backend with flag data
        // Verify calculations match expected values
    });

    it("should correctly calculate non-billable hours using flags", () => {
        // Similar test
    });
});

describe("Weekly Timesheet Filter", () => {
    it("should show all tasks when filter is 'All'", () => {
        // Verify no filtering applied
    });

    it("should show only billable tasks when filter is 'Billable'", () => {
        // Verify only tasks with all billable components shown
    });

    it("should show only non-billable tasks when filter is 'Non-Billable'", () => {
        // Verify only tasks with any non-billable component shown
    });
});

describe("Weekly Expense Sheet Filter", () => {
    it("should show all expenses when filter is 'All'", () => {
        // Verify no filtering applied
    });

    it("should show only billable expenses when filter is 'Billable'", () => {
        // Verify only expenses with all billable components shown
    });

    it("should show only non-billable expenses when filter is 'Non-Billable'", () => {
        // Verify only expenses with any non-billable component shown
    });
});
```

**Manual Testing:**

- [x] User Summary report shows correct billable/non-billable split
- [x] Top client/project calculations exclude non-billable correctly
- [x] Average hours per day calculated on billable hours only
- [x] Weekly Timesheet filter shows "All" by default
- [x] Weekly Timesheet filter correctly filters to "Billable" only
- [x] Weekly Timesheet filter correctly filters to "Non-Billable" only
- [x] Weekly Expense Sheet filter shows "All" by default
- [x] Weekly Expense Sheet filter correctly filters expenses by billability
- [x] Expense billability evaluation checks Client/Project/Type flags
- [x] Verify with test data covering all flag scenarios (tasks and expenses)

### 3.8 Remove Old Logic (Cleanup)

After confirming new logic works:

1. Search for any remaining `'Non-Billable'` string literals in TypeScript files
2. Remove or update appropriately
3. Update comments that reference the old approach

### 3.9 Deliverables

- [x] Model interfaces updated
- [x] `ReportsService` refactored with billability methods for tasks AND expenses
- [x] All report components updated
- [x] Weekly Timesheet filter implemented with "All", "Billable", "Non-Billable" options
- [x] Weekly Expense Sheet filter implemented with "All", "Billable", "Non-Billable" options
- [x] Expense billability evaluation uses Client/Project/Type flags
- [x] Magic string checks removed
- [x] Unit tests pass (>80% coverage)
- [x] Integration tests pass
- [x] Manual testing complete
- [x] No regression in existing reports
- [x] Performance acceptable (dropdown cache works)

---

## Phase 4: Documentation and Deployment

**Status:** Not Started  
**Dependencies:** Phases 1-3 complete  
**Duration:** 2-3 hours  
**Risk:** Low

### 4.1 User Documentation

**File:** `docs/User_Guide.md`

**Updates:**

1. **Section: Best Practices for Time Tracking**
    - Remove references to "Non-Billable" project as special case
    - Explain that billability is determined by dropdown flags
    - Show examples of non-billable configurations

2. **Section: Reports**
    - Update explanation of billable vs. non-billable calculations
    - Clarify that ANY non-billable component makes entire entry non-billable

**Example Update:**

```markdown
### Billable vs. Non-Billable Time

The system tracks billable and non-billable hours and expenses automatically based on
dropdown configurations:

- **Billability Flags**: Administrators can mark any Client, Project, Phase, or Expense Type
  as non-billable in the dropdown maintenance screen
- **Evaluation Logic**: If ANY component is marked non-billable, the entire entry is considered non-billable:
    - **Tasks**: Client OR Project OR Phase
    - **Expenses**: Client OR Project OR Expense Type
- **Examples**:
    - `Corporate` (billable) / `General Admin` (billable) / `PTO` (non-billable)
      → **Non-Billable**
    - `Acme Corp` (billable) / `Consulting` (billable) / `Development` (billable)
      → **Billable**

**Weekly Reports Filtering:**
The Weekly Timesheet and Weekly Expense Sheet reports include a billability filter
with three options: "All" (default), "Billable", or "Non-Billable".

Reports separate billable and non-billable hours automatically.
```

### 4.2 Administrator Documentation

**File:** `docs/Administrator_User_Guide.md`

**New Section: Configuring Billability**

```markdown
## Configuring Non-Billable Work

### Setting Billability Flags

1. Navigate to **Dropdown Maintenance** (React Dashboard)
2. Select the dropdown category (Client, Project, Phase, or Expense Type)
3. Edit the value you want to mark as non-billable
4. Check the **Non-Billable** checkbox
5. Save changes

### Billability Precedence

The system uses **OR logic** for billability:

**For Tasks:**

- If the CLIENT is marked non-billable → entry is non-billable
- If the PROJECT is marked non-billable → entry is non-billable
- If the PHASE is marked non-billable → entry is non-billable
- Entry is only billable if ALL three components are billable

**For Expenses:**

- If the CLIENT is marked non-billable → entry is non-billable
- If the PROJECT is marked non-billable → entry is non-billable
- If the EXPENSE TYPE is marked non-billable → entry is non-billable
- Entry is only billable if ALL three components are billable

### Common Configurations

**Tasks:**
| Scenario | Configuration | Result |
| ---------------- | -------------------------------- | ------------------------------------- |
| PTO time | Phase: PTO (non-billable) | All PTO entries are non-billable |
| Internal project | Project: Training (non-billable) | All training entries are non-billable |
| Company overhead | Client: Internal (non-billable) | All internal work is non-billable |

**Expenses:**
| Scenario | Configuration | Result |
| --------------------------- | ----------------------------------------------- | -------------------------------------------- |
| Home office expenses | Type: Home Office - Internet (non-billable) | All home office internet is non-billable |
| Internal training travel | Project: Training (non-billable) | All training-related expenses are non-billable |
| Company overhead expenses | Client: Internal (non-billable) | All internal expenses are non-billable |

### Weekly Report Filters

The **Weekly Timesheet** and **Weekly Expense Sheet** reports include a billability filter:

- **All** (default): Shows all entries regardless of billability
- **Billable**: Shows only entries where all components are billable
- **Non-Billable**: Shows only entries where any component is non-billable

### Migration from Old System

If you were using the "Non-Billable" project name convention:

1. Existing "Non-Billable" projects are automatically flagged
2. You can now create proper project names and mark them non-billable
3. Update existing tasks to use correct project names
4. Consider retiring the "Non-Billable" project name
```

### 4.3 Technical Documentation

**File:** `docs/Technical_Features_Summary.md`

**Update Section: Billable/Non-Billable Hours Tracking**

````markdown
### Billable/Non-Billable Hours Tracking

**Implementation:** Flag-based system using `dropdownvalues.non_billable` column

**Components:**

- Database: `non_billable` BOOLEAN column on `dropdownvalues` table
- Backend: `BillabilityService` evaluates client/project/phase/type flags
- Frontend: Angular and React components check flags instead of magic strings

**Evaluation Logic:**

```java
// For Tasks
isBillable = NOT (client.nonBillable OR project.nonBillable OR phase.nonBillable)

// For Expenses
isBillable = NOT (client.nonBillable OR project.nonBillable OR expenseType.nonBillable)
```
````

**Benefits:**

- Flexible: Any dropdown value can be marked non-billable
- Maintainable: No hardcoded business logic in code
- Aligned: Matches corporate time system combinations
- Extensible: Works for both tasks and expenses

**Report Features:**

- Weekly Timesheet includes billability filter: All, Billable, Non-Billable
- Weekly Expense Sheet includes billability filter: All, Billable, Non-Billable
- All reports use flag-based evaluation for accuracy

**API Changes:**

- `GET /api/dropdowns/*` includes `nonBillable` field
- `POST/PUT /api/dropdowns/*` accepts `nonBillable` field

````

### 4.4 API Documentation

If using Swagger/OpenAPI, update:

**File:** OpenAPI spec or Swagger annotations

```yaml
DropdownValue:
  type: object
  properties:
    id:
      type: integer
    category:
      type: string
    subcategory:
      type: string
    itemValue:
      type: string
    displayOrder:
      type: integer
    isActive:
      type: boolean
    nonBillable:  # NEW
      type: boolean
      description: "Flag indicating if this value represents non-billable work"
      default: false
````

### 4.5 Deployment Checklist

**Pre-Deployment:**

- [x] All phases 0-3 complete and tested
- [x] Database migration tested on staging
- [x] Backend tests pass (all environments)
- [x] Frontend builds successful (Angular + React)
- [x] Integration tests pass
- [x] Documentation updated
- [x] Rollback plan documented

**Deployment Steps:**

1. **Backup Database**

    ```bash
    pg_dump taskactivity > backup_pre_nonbillable_$(date +%Y%m%d).sql
    ```

2. **Deploy Database Migration**

    ```bash
    # Run migration script
    psql -d taskactivity -f V{version}_add_non_billable_flag.sql

    # Verify migration
    psql -d taskactivity -c "\d dropdownvalues"
    psql -d taskactivity -c "SELECT * FROM dropdownvalues WHERE non_billable = TRUE"
    ```

3. **Deploy Backend**

    ```bash
    # Build and deploy Spring Boot application
    .\mvnw.cmd clean package -DskipTests
    # Deploy to server (method depends on infrastructure)
    ```

4. **Deploy Frontend(s)**

    ```bash
    # Build Angular
    cd frontend && ng build --configuration=production

    # Build React
    cd frontend-react && npm run build

    # Deploy to web server
    ```

5. **Post-Deployment Verification**
    - [x] Test dropdown CRUD with non-billable flag
    - [x] Verify reports calculate correctly
    - [x] Check existing "Non-Billable" projects still work
    - [x] Test task/expense entry with various flag combinations
    - [x] Monitor logs for errors

**Rollback Plan:**

If critical issues discovered:

```bash
# 1. Revert codebase to previous version
git revert <commit-hash>

# 2. Rollback database (if needed)
psql -d taskactivity -f rollback_non_billable_flag.sql

# 3. Redeploy previous versions
```

### 4.6 Deliverables

- [x] User guide updated
- [x] Administrator guide updated
- [x] Technical documentation updated
- [x] API documentation updated
- [x] Deployment checklist created
- [x] Rollback procedures documented
- [x] Change log entry added

---

## Testing Strategy

### Unit Testing

**Coverage Target:** >80% for new code

**Key Areas:**

- `BillabilityService` logic (all combinations)
- Repository methods
- DTO serialization/deserialization
- Angular service methods

### Integration Testing

**Scenarios:**

1. Create dropdown with non-billable flag via API
2. Retrieve dropdown and verify flag present
3. Submit task with non-billable component, verify reports
4. Update flag and verify immediate effect on reports

### User Acceptance Testing

**Test Scenarios:**

| Scenario                | Steps                                                           | Expected Result                            |
| ----------------------- | --------------------------------------------------------------- | ------------------------------------------ |
| Mark phase non-billable | 1. Edit PTO phase<br>2. Check non-billable<br>3. Save           | PTO tasks show as non-billable in reports  |
| Bi-billable client      | 1. Edit client as non-billable<br>2. Log task<br>3. View report | All tasks for that client are non-billable |
| Mixed configuration     | 1. Billable client<br>2. Non-billable project<br>3. Log task    | Task shows as non-billable (OR logic)      |
| Report accuracy         | 1. Log multiple tasks (mix)<br>2. View user summary             | Billable/non-billable split correct        |

### Performance Testing

**Verification:**

- Dropdown queries with flag filtering perform <100ms
- Reports with billability checks perform <2s for 1000 tasks
- Index on `non_billable` column used in queries

---

## Risk Assessment and Mitigation

### High Risks

| Risk                              | Impact | Probability | Mitigation                                                          |
| --------------------------------- | ------ | ----------- | ------------------------------------------------------------------- |
| **Report calculations incorrect** | High   | Medium      | Comprehensive testing, phase rollout, A/B comparison with old logic |
| **Breaking existing workflows**   | High   | Low         | Maintain "Non-Billable" project backward compatibility initially    |

### Medium Risks

| Risk                                | Impact | Probability | Mitigation                                                           |
| ----------------------------------- | ------ | ----------- | -------------------------------------------------------------------- |
| **Performance degradation**         | Medium | Low         | Index on flag column, cache dropdowns in memory, monitor query plans |
| **User confusion about precedence** | Medium | Medium      | Clear documentation, admin training, UI tooltips                     |

### Low Risks

| Risk                       | Impact | Probability | Mitigation                                                       |
| -------------------------- | ------ | ----------- | ---------------------------------------------------------------- |
| **Migration script fails** | Low    | Low         | Test thoroughly, backup before deployment, rollback script ready |
| **API compatibility**      | Low    | Low         | Additive change (new field), doesn't break existing clients      |

---

## Success Criteria

### Technical Success

- [x] All tests pass (unit, integration, UAT)
- [x] No performance regression (<5% slower queries)
- [x] Zero critical bugs in production for 2 weeks
- [x] Successful rollout to all environments

### Business Success

- [x] Reports accurately reflect billability per corporate rules
- [x] Administrators can configure billability without developer involvement
- [x] Users can track time using actual corporate project names
- [x] No confusion or support tickets about billability logic

### Quality Metrics

- [x] Code coverage >80% for new code
- [x] Zero hard-coded billability strings remaining
- [x] Documentation reviewed and approved
- [x] Stakeholder sign-off obtained

---

## Timeline Estimate

| Phase                      | Duration  | Dependencies   | Assignee        |
| -------------------------- | --------- | -------------- | --------------- |
| **Phase 0:** Database      | 1-2 hours | None           | Backend Dev     |
| **Phase 1:** Spring Boot   | 4-6 hours | Phase 0        | Backend Dev     |
| **Phase 2:** React         | 3-4 hours | Phase 1        | Frontend Dev    |
| **Phase 3:** Angular       | 6-8 hours | Phase 1        | Frontend Dev    |
| **Phase 4:** Documentation | 2-3 hours | Phases 1-3     | Tech Writer/Dev |
| **Testing & QA**           | 5-7 hours | All phases     | QA Team         |
| **Deployment**             | 2-3 hours | Testing passed | DevOps          |

**Total Estimated Effort:** 23-33 hours  
**Calendar Time (with contingency):** 6-8 business days

**Note:** Phase 3 duration increased to account for Weekly Timesheet and Weekly Expense Sheet filter implementation.

---

## Approval and Sign-Off

| Role                     | Name | Approval Date | Signature |
| ------------------------ | ---- | ------------- | --------- |
| **Project Lead**         |      |               |           |
| **Technical Lead**       |      |               |           |
| **QA Lead**              |      |               |           |
| **Business Stakeholder** |      |               |           |

---

## Appendix A: Database Schema Reference

### Current Schema

```sql
CREATE TABLE public.dropdownvalues (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50) NOT NULL,
    itemvalue VARCHAR(255) NOT NULL,
    displayorder INTEGER NOT NULL DEFAULT 0,
    isactive BOOLEAN NOT NULL DEFAULT TRUE
);
```

### New Schema (After Phase 0)

```sql
CREATE TABLE public.dropdownvalues (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    subcategory VARCHAR(50) NOT NULL,
    itemvalue VARCHAR(255) NOT NULL,
    displayorder INTEGER NOT NULL DEFAULT 0,
    isactive BOOLEAN NOT NULL DEFAULT TRUE,
    non_billable BOOLEAN NOT NULL DEFAULT FALSE  -- NEW COLUMN
);

CREATE INDEX idx_dropdownvalues_non_billable
ON public.dropdownvalues(non_billable)
WHERE non_billable = TRUE;
```

---

## Appendix B: Code Refactoring Examples

### Before: Magic String Approach

```typescript
// Angular reports.service.ts
const billableHours = userTasks
    .filter((t) => t.project !== "Non-Billable") // ❌ Fragile
    .reduce((sum, t) => sum + t.hours, 0);

const topClient = clientHours
    .filter((c) => c.project !== "Non-Billable") // ❌ Hardcoded
    .sort((a, b) => b.hours - a.hours)[0]?.client;
```

### After: Flag-Based Approach

```typescript
// Angular reports.service.ts
const billableHours = userTasks
  .filter((t) => this.isTaskBillable(t))  // ✅ Clear intent
  .reduce((sum, t) => sum + t.hours, 0);

const topClient = clientHours
  .filter(c => this.isTaskBillable(c))  // ✅ Maintainable
  .sort((a, b) => b.hours - a.hours)[0]?.client;

// Helper method with clear business logic
private isTaskBillable(task: TaskActivityDto): boolean {
  return this.isBillable(task.client, 'CLIENT')
      && this.isBillable(task.project, 'PROJECT')
      && this.isBillable(task.phase, 'PHASE');
}
```

---

## Appendix C: Sample Test Data

### Test Dropdown Values

```sql
-- Non-billable client
INSERT INTO dropdownvalues VALUES
(100, 'TASK', 'CLIENT', 'Internal', 1, true, true);

-- Non-billable project
INSERT INTO dropdownvalues VALUES
(101, 'TASK', 'PROJECT', 'Training', 2, true, true);

-- Non-billable phase
INSERT INTO dropdownvalues VALUES
(102, 'TASK', 'PHASE', 'PTO', 6, true, true);

-- Billable combinations
INSERT INTO dropdownvalues VALUES
(103, 'TASK', 'CLIENT', 'Acme Corp', 2, true, false),
(104, 'TASK', 'PROJECT', 'Consulting', 3, true, false),
(105, 'TASK', 'PHASE', 'Development', 1, true, false);
```

### Test Scenarios

| Client    | Project    | Phase       | Expected Billable? | Reason                              |
| --------- | ---------- | ----------- | ------------------ | ----------------------------------- |
| Internal  | Training   | Development | ❌ No              | Client flag set                     |
| Acme Corp | Training   | Development | ❌ No              | Project flag set                    |
| Acme Corp | Consulting | PTO         | ❌ No              | Phase flag set                      |
| Acme Corp | Consulting | Development | ✅ Yes             | No flags set                        |
| Internal  | Consulting | PTO         | ❌ No              | Multiple flags (any = non-billable) |

---

**END OF IMPLEMENTATION PLAN**
