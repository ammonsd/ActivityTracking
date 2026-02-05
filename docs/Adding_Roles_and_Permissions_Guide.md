# Adding Roles and Permissions Guide

**Last Updated:** February 5, 2026  
**Status:** Current

## Overview

This guide explains how to add new roles and permissions when introducing new features or UI components to the Task Activity Tracker application. The application uses a database-driven authorization system that separates roles from permissions, allowing flexible access control.

---

## Architecture Overview

### Key Components

1. **Roles** (`roles` table) - User roles (ADMIN, USER, GUEST, EXPENSE_ADMIN, etc.)
2. **Permissions** (`permissions` table) - Granular access rights defined by resource and action
3. **Role-Permission Mapping** (`role_permissions` table) - Many-to-many relationship
4. **@RequirePermission Annotation** - Controller method-level authorization
5. **PermissionAspect** - Spring AOP interceptor that enforces permissions

### Permission Structure

Each permission consists of:

- **resource**: The feature/module name (e.g., `TASK_ACTIVITY`, `EXPENSE`, `USER_MANAGEMENT`)
- **action**: The operation type (e.g., `CREATE`, `READ`, `UPDATE`, `DELETE`, `READ_ALL`)
- **description**: Human-readable explanation

---

## Step-by-Step Guide

### Step 1: Identify Required Permissions

Before adding permissions, determine:

1. **What is the resource?** (e.g., `REPORTS`, `ANALYTICS`, `INVOICES`)
2. **What actions are needed?** (e.g., `CREATE`, `READ`, `UPDATE`, `DELETE`, `APPROVE`, `EXPORT`)
3. **Who should have access?** (which roles need which permissions)

#### Common Permission Patterns

| Pattern                | Actions                                       | Use Case                   |
| ---------------------- | --------------------------------------------- | -------------------------- |
| **Full CRUD**          | CREATE, READ, UPDATE, DELETE                  | Standard entity management |
| **Read-Only**          | READ                                          | View-only access           |
| **Admin Full Control** | CREATE, READ, UPDATE, DELETE, READ_ALL        | Admin capabilities         |
| **Approval Workflow**  | CREATE, READ, UPDATE, SUBMIT, APPROVE, REJECT | Multi-stage processes      |
| **Report Access**      | VIEW, EXPORT                                  | Reporting features         |

---

### Step 2: Add Permissions to Database

#### Location: `src/main/resources/data.sql`

Add your new permissions using INSERT statements:

```sql
-- Define permissions for your new feature
INSERT INTO permissions (resource, action, description) VALUES
    ('YOUR_RESOURCE', 'CREATE', 'Create new your_resource items'),
    ('YOUR_RESOURCE', 'READ', 'View own your_resource items'),
    ('YOUR_RESOURCE', 'READ_ALL', 'View all users your_resource items (admin)'),
    ('YOUR_RESOURCE', 'UPDATE', 'Modify your_resource items'),
    ('YOUR_RESOURCE', 'DELETE', 'Delete your_resource items'),
    ('YOUR_RESOURCE', 'APPROVE', 'Approve your_resource submissions'),
    ('YOUR_RESOURCE', 'EXPORT', 'Export your_resource data')
ON CONFLICT (resource, action) DO NOTHING;
```

**Best Practices:**

- Use uppercase, underscore-separated names (e.g., `INVOICE_MANAGEMENT`)
- Be consistent with existing naming conventions
- Use descriptive action names that clearly indicate the operation
- Always include a description field

---

### Step 3: Assign Permissions to Roles

#### Location: `src/main/resources/data.sql`

Assign the new permissions to appropriate roles:

```sql
-- Give ADMIN full access to new feature
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'YOUR_RESOURCE'
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Give USER basic access (create, read, update, delete own)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'YOUR_RESOURCE'
    AND p.action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
WHERE r.name = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Give GUEST read-only access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'YOUR_RESOURCE'
    AND p.action = 'READ'
WHERE r.name = 'GUEST'
ON CONFLICT (role_id, permission_id) DO NOTHING;
```

**Role Assignment Guidelines:**

| Role              | Typical Access Level                                          |
| ----------------- | ------------------------------------------------------------- |
| **ADMIN**         | Full access to all resources and actions                      |
| **USER**          | CRUD access to own data, READ access to shared data           |
| **GUEST**         | READ-only access to limited data                              |
| **EXPENSE_ADMIN** | Full access to expense-related features, READ access to tasks |
| **Custom Roles**  | Specific combinations based on business requirements          |

---

### Step 4: Protect Controller Endpoints

#### Add @RequirePermission Annotation

Use the `@RequirePermission` annotation on controller methods to enforce authorization:

```java
@Controller
@RequestMapping("/your-resource")
public class YourResourceController {

    // View list - requires READ permission
    @GetMapping
    @RequirePermission(resource = "YOUR_RESOURCE", action = "READ")
    public String listYourResources(Model model) {
        // Implementation
    }

    // Create new item - requires CREATE permission
    @PostMapping("/add")
    @RequirePermission(resource = "YOUR_RESOURCE", action = "CREATE")
    public String createYourResource(@Valid @ModelAttribute YourResourceDto dto) {
        // Implementation
    }

    // Update existing item - requires UPDATE permission
    @PostMapping("/edit/{id}")
    @RequirePermission(resource = "YOUR_RESOURCE", action = "UPDATE")
    public String updateYourResource(@PathVariable Long id) {
        // Implementation
    }

    // Delete item - requires DELETE permission
    @PostMapping("/delete/{id}")
    @RequirePermission(resource = "YOUR_RESOURCE", action = "DELETE")
    public String deleteYourResource(@PathVariable Long id) {
        // Implementation
    }

    // Admin view all - requires READ_ALL permission
    @GetMapping("/admin/all")
    @RequirePermission(resource = "YOUR_RESOURCE", action = "READ_ALL")
    public String viewAllResources(Model model) {
        // Implementation
    }
}
```

**REST API Example:**

```java
@RestController
@RequestMapping("/api/your-resource")
public class YourResourceRestController {

    @GetMapping
    @RequirePermission(resource = "YOUR_RESOURCE", action = "READ")
    public ResponseEntity<List<YourResource>> getAll() {
        // Implementation
    }

    @PostMapping
    @RequirePermission(resource = "YOUR_RESOURCE", action = "CREATE")
    public ResponseEntity<YourResource> create(@RequestBody YourResourceDto dto) {
        // Implementation
    }
}
```

---

### Step 5: Update Test Data (Optional)

#### Location: `src/main/resources/test-data.sql`

For testing environments, add the same permissions and role assignments to `test-data.sql`:

```sql
-- Copy the same INSERT statements from data.sql
INSERT INTO permissions (resource, action, description) VALUES
    ('YOUR_RESOURCE', 'CREATE', 'Create new your_resource items'),
    -- ... etc
ON CONFLICT (resource, action) DO NOTHING;

-- Copy role assignments
INSERT INTO role_permissions (role_id, permission_id)
-- ... etc
```

---

### Step 6: Test Permission Enforcement

#### Manual Testing Checklist

1. **Test with ADMIN role:**
    - ✅ Should have full access to all features
    - ✅ Can perform all CRUD operations
    - ✅ Can access admin-only endpoints

2. **Test with USER role:**
    - ✅ Can access permitted features
    - ❌ Cannot access admin-only endpoints
    - ✅ Can manage own data
    - ❌ Cannot access other users' data (unless READ_ALL granted)

3. **Test with GUEST role:**
    - ✅ Can view permitted data
    - ❌ Cannot create, update, or delete
    - ❌ Cannot access restricted features

4. **Test authorization failures:**
    - ❌ User without permission gets 403 Forbidden
    - ❌ Unauthenticated user gets redirected to login

#### Automated Testing Example

```java
@Test
@WithMockUser(username = "testuser", roles = "USER")
void testUserCanAccessOwnResources() {
    // User has YOUR_RESOURCE:READ permission
    mockMvc.perform(get("/your-resource"))
           .andExpect(status().isOk());
}

@Test
@WithMockUser(username = "testuser", roles = "USER")
void testUserCannotAccessAdminEndpoint() {
    // User does NOT have YOUR_RESOURCE:READ_ALL permission
    mockMvc.perform(get("/your-resource/admin/all"))
           .andExpect(status().isForbidden());
}

@Test
@WithMockUser(username = "admin", roles = "ADMIN")
void testAdminCanAccessAllEndpoints() {
    // Admin has all permissions
    mockMvc.perform(get("/your-resource/admin/all"))
           .andExpect(status().isOk());
}
```

---

## Managing Permissions via UI

### Role & Permission Management Interface

Administrators can manage roles and permissions through the web UI:

1. **Access Role Management:**
    - Navigate to: `http://localhost:8080/task-activity/manage-roles`
    - Click "Manage Roles" from User Management page

2. **View Existing Roles:**
    - See all roles and their assigned permissions
    - View which users have which roles

3. **Edit Role Permissions:**
    - Click "Edit" next to any role
    - Check/uncheck permission checkboxes
    - Save changes

4. **Create New Roles:**
    - Click "Add New Role"
    - Enter role name and description
    - Select permissions to assign
    - Create role

5. **Assign Roles to Users:**
    - Navigate to User Management
    - Edit user
    - Select role from dropdown
    - Save changes

---

## Common Scenarios

### Scenario 1: Adding a Simple Read/Write Feature

**Example: Adding a "Documents" feature**

1. **Add Permissions:**

```sql
INSERT INTO permissions (resource, action, description) VALUES
    ('DOCUMENTS', 'CREATE', 'Upload new documents'),
    ('DOCUMENTS', 'READ', 'View documents'),
    ('DOCUMENTS', 'UPDATE', 'Modify document metadata'),
    ('DOCUMENTS', 'DELETE', 'Delete documents')
ON CONFLICT (resource, action) DO NOTHING;
```

2. **Assign to Roles:**

```sql
-- ADMIN: Full access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.resource = 'DOCUMENTS'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- USER: CRUD on own documents
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.resource = 'DOCUMENTS'
    AND p.action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;
```

3. **Protect Endpoints:**

```java
@GetMapping("/documents")
@RequirePermission(resource = "DOCUMENTS", action = "READ")
public String listDocuments() { ... }

@PostMapping("/documents/upload")
@RequirePermission(resource = "DOCUMENTS", action = "CREATE")
public String uploadDocument() { ... }
```

---

### Scenario 2: Adding Admin-Only Feature

**Example: Adding "System Settings" management**

1. **Add Permissions (Admin only):**

```sql
INSERT INTO permissions (resource, action, description) VALUES
    ('SYSTEM_SETTINGS', 'READ', 'View system settings'),
    ('SYSTEM_SETTINGS', 'UPDATE', 'Modify system settings')
ON CONFLICT (resource, action) DO NOTHING;

-- Only ADMIN gets access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.resource = 'SYSTEM_SETTINGS'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
```

2. **Protect Endpoints:**

```java
@GetMapping("/admin/settings")
@RequirePermission(resource = "SYSTEM_SETTINGS", action = "READ")
public String viewSettings() { ... }
```

---

### Scenario 3: Adding Approval Workflow

**Example: Adding "Timesheet Approval" feature**

1. **Add Permissions with Workflow Actions:**

```sql
INSERT INTO permissions (resource, action, description) VALUES
    ('TIMESHEET', 'CREATE', 'Create timesheets'),
    ('TIMESHEET', 'READ', 'View own timesheets'),
    ('TIMESHEET', 'UPDATE', 'Edit own timesheets'),
    ('TIMESHEET', 'SUBMIT', 'Submit timesheets for approval'),
    ('TIMESHEET', 'APPROVE', 'Approve timesheets'),
    ('TIMESHEET', 'REJECT', 'Reject timesheets'),
    ('TIMESHEET', 'READ_ALL', 'View all timesheets')
ON CONFLICT (resource, action) DO NOTHING;
```

2. **Assign to Different Roles:**

```sql
-- Regular users: Create and submit
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.resource = 'TIMESHEET'
    AND p.action IN ('CREATE', 'READ', 'UPDATE', 'SUBMIT')
WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;

-- Managers: Can approve/reject and view all
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.resource = 'TIMESHEET'
    AND p.action IN ('READ_ALL', 'APPROVE', 'REJECT')
WHERE r.name = 'MANAGER'
ON CONFLICT DO NOTHING;
```

---

## Troubleshooting

### Common Issues

#### 1. Permission Denied (403 Forbidden)

**Symptom:** User gets 403 error when accessing endpoint

**Solutions:**

- Verify role has required permission in database:
    ```sql
    SELECT r.name, p.resource, p.action
    FROM roles r
    JOIN role_permissions rp ON r.id = rp.role_id
    JOIN permissions p ON rp.permission_id = p.id
    WHERE r.name = 'YOUR_ROLE';
    ```
- Check @RequirePermission annotation matches database exactly (case-sensitive)
- Verify user is assigned correct role
- Clear browser cache/cookies and re-login

#### 2. Permission Not Working After Adding to Database

**Symptom:** Added permission but still getting access denied

**Solutions:**

- Restart application to reload Spring context
- Verify SQL INSERT statements executed successfully (check logs)
- Check for typos in resource/action names (case-sensitive)
- Ensure role_permissions mapping exists

#### 3. All Permissions Denied After Update

**Symptom:** Even ADMIN cannot access features

**Solutions:**

- Check if permissions were accidentally removed from ADMIN role
- Restore ADMIN permissions:
    ```sql
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id FROM roles r, permissions p
    WHERE r.name = 'ADMIN'
    ON CONFLICT DO NOTHING;
    ```
- Verify database connection and data.sql execution

---

## Best Practices

### 1. Permission Naming Conventions

- ✅ Use uppercase with underscores: `INVOICE_MANAGEMENT`
- ✅ Resource names should be nouns: `EXPENSE`, `REPORT`, `USER_MANAGEMENT`
- ✅ Action names should be verbs: `CREATE`, `READ`, `UPDATE`, `DELETE`, `APPROVE`
- ❌ Avoid: `invoiceManagement`, `Invoice-Management`, `invoice management`

### 2. Granularity

- ✅ Create separate permissions for distinct operations
- ✅ Use `READ` vs `READ_ALL` to distinguish own vs all users' data
- ❌ Don't create overly granular permissions (e.g., `READ_NAME`, `READ_EMAIL`)
- ❌ Don't combine unrelated operations in one permission

### 3. Role Design

- ✅ ADMIN should always have all permissions
- ✅ Create specialized roles for specific workflows (e.g., EXPENSE_ADMIN)
- ✅ Design roles around job functions, not individuals
- ❌ Don't hardcode roles in business logic (always check permissions)

### 4. Security

- ✅ Always protect controller endpoints with @RequirePermission
- ✅ Validate permissions on both read and write operations
- ✅ Check permissions in service layer for complex business logic
- ✅ Test unauthorized access attempts
- ❌ Never rely solely on UI hiding (always enforce server-side)

### 5. Documentation

- ✅ Document new permissions in this guide
- ✅ Update role assignment tables
- ✅ Include permission requirements in API documentation
- ✅ Document any special permission logic

---

## Migration Strategy

### Adding Permissions to Existing Application

When adding new permissions to a production system:

1. **Create Migration Script:**

```sql
-- migration-v2.0.sql
-- Add new feature permissions
INSERT INTO permissions (resource, action, description)
VALUES ('NEW_FEATURE', 'READ', 'Access new feature')
ON CONFLICT (resource, action) DO NOTHING;

-- Assign to existing roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.resource = 'NEW_FEATURE'
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;
```

2. **Test in Development:**
    - Apply migration to dev database
    - Test all affected roles
    - Verify no regressions

3. **Deploy to Production:**
    - Run migration during maintenance window
    - Verify permissions applied correctly
    - Test with real user accounts
    - Monitor logs for permission errors

4. **Rollback Plan:**

```sql
-- rollback-v2.0.sql
DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT id FROM permissions
    WHERE resource = 'NEW_FEATURE'
);

DELETE FROM permissions
WHERE resource = 'NEW_FEATURE';
```

---

## Reference

### Existing Permissions in System

| Resource          | Actions                                                                          | Used By            |
| ----------------- | -------------------------------------------------------------------------------- | ------------------ |
| `TASK_ACTIVITY`   | CREATE, READ, UPDATE, DELETE, READ_ALL                                           | Main task tracking |
| `USER_MANAGEMENT` | CREATE, READ, UPDATE, DELETE, MANAGE_ROLES                                       | User admin         |
| `REPORTS`         | VIEW, EXPORT                                                                     | Reporting module   |
| `EXPENSE`         | CREATE, READ, UPDATE, DELETE, SUBMIT, APPROVE, REJECT, MANAGE_RECEIPTS, READ_ALL | Expense tracking   |

### Related Files

- **Permission Definitions:** `src/main/resources/data.sql`
- **Test Data:** `src/main/resources/test-data.sql`
- **Permission Entity:** `src/main/java/com/ammons/taskactivity/entity/Permission.java`
- **Role Entity:** `src/main/java/com/ammons/taskactivity/entity/Roles.java`
- **Permission Aspect:** `src/main/java/com/ammons/taskactivity/security/PermissionAspect.java`
- **Annotation:** `src/main/java/com/ammons/taskactivity/security/RequirePermission.java`
- **Permission Service:** `src/main/java/com/ammons/taskactivity/service/PermissionService.java`
- **Role Management UI:** `src/main/resources/templates/admin/role-*.html`

---

## Support

For questions or issues with roles and permissions:

1. Check this guide for common scenarios
2. Review existing permission implementations in controllers
3. Check application logs for permission errors
4. Use Role Management UI to verify current role assignments
5. Run SQL queries to validate database state

**Version:** 1.0  
**Last Updated:** December 19, 2025
