# Task Activity Management System - Administrator User Guide

## Welcome

This guide is for administrators of the Task Activity Management System. As an administrator, you have access to additional features for managing users, viewing all tasks, and configuring system settings.

## Table of Contents

1. [Accessing the Application](#accessing-the-application)
2. [Navigation](#navigation)
3. [React Admin Dashboard](#react-admin-dashboard)
   - [Accessing the Dashboard](#accessing-the-react-dashboard)
   - [Dashboard Features](#dashboard-features)
4. [Administrator Features](#administrator-features)
   - [User Roles Overview](#user-roles-overview)
   - [Managing Users](#managing-users)
   - [Managing Roles and Permissions](#managing-roles-and-permissions)
   - [Viewing All User Tasks](#viewing-all-user-tasks)
   - [User Self-Service Profile Management](#user-self-service-profile-management)
   - [Managing Task Activities](#managing-task-activities)
   - [Changing User Passwords](#changing-user-passwords)
   - [Managing User Dropdown Access](#managing-user-dropdown-access)
   - [Sending Profile Notification Emails](#sending-profile-notification-emails)
5. [Expense Management Administration](#expense-management-administration)
   - [Managing User Expenses](#managing-user-expenses)
   - [Expense Approval Process](#expense-approval-process)
   - [Email Notifications](#email-notifications-for-expense-submissions)
   - [Reimbursement Tracking](#reimbursement-tracking)
   - [Receipt Management](#receipt-management)
   - [Managing Dropdowns](#managing-dropdowns)
6. [Guest Activity Dashboard](#guest-activity-dashboard)
7. [User Analytics & Performance Monitoring](#user-analytics--performance-monitoring)
8. [Common User Support Scenarios](#common-user-support-scenarios)
9. [Security Features](#security-features)
   - [Account Lockout Policy](#account-lockout-policy)
   - [Session Security](#session-security)
10. [Database Query Tool (SQL to CSV Export)](#database-query-tool-sql-to-csv-export)
11. [Email Configuration Management](#email-configuration-management)
    - [Email Notification Types](#email-notification-types)
    - [Email Configuration Variables](#email-configuration-variables)
    - [Method 1: PowerShell Script](#method-1-update-via-powershell-script-recommended)
    - [Method 2: AWS Console](#method-2-update-via-aws-console)
    - [Testing Email Configuration](#testing-email-configuration)
    - [Troubleshooting Email Issues](#troubleshooting-email-issues)
12. [System Monitoring and Health Checks](#system-monitoring-and-health-checks)
    - [Health Check Endpoints](#health-check-endpoints)
    - [AWS CloudWatch Monitoring](#aws-cloudwatch-monitoring)
    - [Common Monitoring Tasks](#common-monitoring-tasks)
    - [Troubleshooting Common Issues](#troubleshooting-common-issues)
    - [Deployment and Rollback Procedures](#deployment-and-rollback-procedures)
    - [Backup and Disaster Recovery](#backup-and-disaster-recovery)

## Accessing the Application

You can access the application using any of the following URLs. If you bookmark your preferred entry point, you will be taken directly to that page after logging in.

| URL | Description |
|-----|-------------|
| https://taskactivitytracker.com/ | **Main Application** — Task list, timesheets, expenses, and all user-facing features |
| https://taskactivitytracker.com/app/dashboard | **User Dashboard** — Overview panels and quick-access links |
| https://taskactivitytracker.com/dashboard | **Admin Dashboard** — User Management, Role Management, and Dropdown Management |

All three interfaces share the same data and authentication session. Log in with your administrator username and password.

## Navigation

### Sidebar Menu (Thymeleaf UI)

The Task Activity List page includes a **floating sidebar menu** for quick access to administrative functions:

**To Access the Menu:**
1. Click the **☰** (hamburger menu) button in the upper-right corner
2. The sidebar menu slides in from the right
3. Click **✕** or the menu button again to close

**Menu Items (Admin View):**
- **🏠 Update Profile**: Access your profile to update your information and password
- **👥 Manage Users**: Create, edit, and manage user accounts
- **� Notify Users**: Send profile detail emails to active users with email addresses
- **�📊 Guest Activity**: View login activity reports for GUEST users
- **🔧 Manage Dropdowns**: Configure clients, projects, phases, and expense types
- **📋 Export CSV**: Export filtered task list to CSV format (auto-closes menu after selection)
- **🎯 Admin Dashboard**: Open the React Admin Dashboard in a new tab (ADMIN only)

**Role-Based Visibility:**
- **ADMIN**: All menu items enabled
- **GUEST**: Menu visible but all options disabled (read-only)
- **USER**: Update Profile and Export CSV available, administrative functions hidden

---

## React Admin Dashboard

The **React Admin Dashboard** is a modern, single-page application built with React, TypeScript, and Material-UI that provides advanced administrative functionality for ADMIN users. It features a responsive design and follows Material Design principles.

### Accessing the React Dashboard

**Prerequisites:**
- Must be logged in with **ADMIN** role
- Non-ADMIN users cannot access the dashboard

**Access:**
- Click **🎯 Admin Dashboard** from the sidebar menu in the main application
- Or navigate directly to: `https://taskactivitytracker.com/dashboard`
- Your login session carries over — no separate login is required

### Dashboard Features

The Admin Dashboard provides the following management modules:

- **User Management**: Create, view, edit, and delete user accounts. Filter users by username, role, or company. Reset passwords and manage account status.
  - **User Dropdown Access Management**: Assign which Clients and Projects each user sees in their task and expense dropdowns (independently controlled per tab)
- **Roles Management**: View, create, edit, and delete roles. Assign permissions to roles using an organized permission tree.
- **Dropdown Management**: Manage all dropdown values (clients, projects, phases, expense types, payment methods) from a single screen. Add, edit, deactivate, or delete values.
- **Analytics & Reports**: View comprehensive team performance analytics across 10 report tabs. Covers user performance rankings, billable vs. non-billable hours, project phase distribution, stale project detection, client billability, client timeline heatmap, day-of-week patterns, tracking compliance, task repetition, and period-over-period delta comparisons. Includes a Scope toggle (Active Only / Include Inactive) and multiselect filters for Users, Clients, and Projects.
- **Guest Activity Report**: View login history and metrics for guest accounts.
- **System Settings**: Coming soon.

---

## Administrator Features

### User Roles Overview

The system uses a role-based access control system with customizable permissions. Administrators can create custom roles and assign permissions through the web interface without requiring any code changes.

**Four Default Roles:**

The system provides four pre-configured roles with different permission levels:

**GUEST (Read-Only Access)**
- Can view task list and task details
- Can create, edit, and delete tasks
- No access to weekly timesheet, expenses, user management, or dropdown settings
- **Cannot change password** (password changes must be done by an administrator)
- **Password expiration warnings are suppressed** (GUEST users won't see expiration warnings)
- **Cannot log in if password has expired** (must contact administrator for password reset)
- Useful for stakeholders who need task tracking without expense access

**Important for GUEST Users:**
- When a GUEST user's password expires, they will be blocked from logging in with the message: "Password has expired. Contact system administrator."
- Administrators must reset GUEST passwords and update the expiration date when needed

**USER (Standard Access)**
- Can view, create, edit, and delete their own tasks and expenses
- Access to weekly timesheet and weekly expense sheet
- Can upload receipts for expenses
- Can submit expenses for approval
- Can manage dropdown values (clients, projects, phases)
- Can change their own password
- Cannot view other users' tasks or expenses (except when submitted for approval)
- Standard role for team members doing time and expense tracking

**ADMIN (Full Access)**
- All USER permissions plus administrative capabilities
- Can view and manage all users' tasks and expenses
- Can create, edit, and delete user accounts
- **Can manage roles and permissions** through web UI
- Can create custom roles tailored to organizational needs
- Can assign/revoke permissions without code changes
- Can manage dropdown values (clients, projects, phases, expense types, payment methods)
- Can change passwords for any user
- Has access to all expense approval functions (can also act as EXPENSE_ADMIN)

**EXPENSE_ADMIN (Expense Approval Authority)**
- All USER permissions for tasks
- Can view all submitted expenses
- Can approve or reject expenses from expense detail page
- Can mark expenses as reimbursed
- Can view expense approval history and notes
- Cannot manage users or system settings (unless also has ADMIN role)

**Custom Roles:**

Administrators can create additional roles as needed:
- Navigate to "Manage Roles" from the Task Activity List sidebar menu or "Manage Roles & Permissions" from the Role Management page header
- Click "Add Role" to create a new custom role
- Assign specific permissions based on organizational requirements
- Examples: "PROJECT_MANAGER", "FINANCE_VIEWER", "READ_ONLY_ADMIN"

For detailed information on managing roles and permissions, see the "Managing Roles and Permissions" section below.

### Managing Users

Administrators can create, edit, and delete user accounts:

1. **Access User Management**: 
   - **Spring Boot UI (Thymeleaf)**: Click **"☰"** to open the sidebar menu, then click **"👥 Manage Users"**
   - **React Dashboard**: Click **"User Management"** card or sidebar menu option
2. **View All Users**: See a list of all system users with their full names, company, role, and last login time
3. **Filter Users**: Use the filter section to find specific users:
    - **Username**: Filter by username (partial match)
    - **Role**: Filter by user role (shows all roles defined in the system)
    - **Company**: Filter by company name (partial match)
    - Click **"Search"** to apply filters or **"Reset filters"** to clear
4. **Add New User**: Click **"Add User"** button
    - Enter username (required)
    - Enter first name (optional)
    - Enter last name (required)
    - Enter company (optional, maximum 100 characters)
    - Set initial password — the password fields are **pre-filled with the default temporary password** (`P@ssword!123`) as a convenience; overwrite it if you want a different initial password
    - Assign role — the dropdown lists all roles currently defined in the system, including any custom roles created via Role Management
    - Enable/disable account
    - Optionally force password change on first login
5. **Edit Users**: Modify first name, last name, company, role, or account status
    - **Note**: Usernames are immutable and cannot be changed after account creation
    - If a username needs to be changed, deactivate the current account and create a new user with the desired username
    - All other user details (first name, last name, company, role, enabled status) can be edited at any time
    - **Account Lock Status**: View if an account is locked due to failed login attempts (🔒 indicator in user list)
    - **Unlock Accounts**: Administrators can unlock locked accounts by unchecking the "Account Locked" checkbox in the edit dialog
    - **Failed Login Attempts**: View the count of failed login attempts in the edit dialog
    - **Email Notifications**: When an account is locked due to excessive failed login attempts, an email notification is automatically sent to the administrator with details about the lockout (username, number of attempts, IP address, and timestamp)
6. **Delete Users**: Remove user accounts (with confirmation)
    - **Note**: The Delete button is disabled (grayed out) for:
        - Your own account (cannot delete yourself)
        - Users who have task entries in the system
    - Users with task entries cannot be deleted to maintain data integrity
    - To prevent a user from accessing the system, disable their account instead of deleting
7. **Manage Dropdown Access**: Control which Clients and Projects a user can see in their dropdowns
    - **Spring Boot UI**: Click the **🔐 Access** button next to any non-ADMIN user in the list
    - **React Admin Dashboard**: Click the **Manage Access** icon button (person-gear, green) next to any non-ADMIN user; the button is disabled for ADMIN users with an explanatory tooltip
    - See [Managing User Dropdown Access](#managing-user-dropdown-access) for full details

### Managing Roles and Permissions

The system features a **database-driven authorization system** that allows administrators to create custom roles and assign permissions without modifying code. This provides flexibility in tailoring access controls to your organization's specific needs.

#### Accessing Role Management

1. **Navigate to Role Management**: 
   - Option 1: Click **"☰"** to open the Task Activity List sidebar menu, then click **"🔐 Manage Roles"** (ADMIN only)
   - Option 2: From within the Role Management page, use the **"Task Activity List"** button in the header to navigate
2. **View All Roles**: See a list of all roles with their descriptions and assigned permissions

#### Understanding Permissions

Each role is made up of a set of permissions that control access to specific features. Permissions follow a **resource:action** pattern — for example, a permission might allow a user to view expenses but not approve them.

**Permission Resources:**
- `TASK_ACTIVITY` — Task management features
- `EXPENSE` — Expense management features
- `USER` — User account management
- `DROPDOWN` — Dropdown value management
- `ROLE` — Role and permission management

**Permission Actions:**
- `CREATE` — Create new records
- `READ` — View records
- `UPDATE` — Edit existing records
- `DELETE` — Remove records
- `MANAGE` — Full management access
- `APPROVE` — Approve submitted items (expenses)

#### Default Roles

Four default roles are provided:

1. **ADMIN**
   - All permissions across all resources
   - Can manage users, roles, permissions, dropdowns
   - Full access to tasks and expenses for all users
   - Can approve expenses

2. **USER**
   - Full CRUD access to own tasks (`TASK_ACTIVITY:CREATE/READ/UPDATE/DELETE`)
   - Full CRUD access to own expenses (`EXPENSE:CREATE/READ/UPDATE/DELETE`)
   - Can manage dropdown values (`DROPDOWN:MANAGE`)
   - Standard role for team members

3. **GUEST**
   - Full CRUD access to task activities (`TASK_ACTIVITY:CREATE/READ/UPDATE/DELETE`)
   - **No expense access** (no expense permissions)
   - Read-only for most features
   - Useful for contractors or temporary staff

4. **EXPENSE_ADMIN**
   - All USER permissions for tasks
   - All EXPENSE permissions including APPROVE
   - Can view and manage all users' expenses
   - Specialized role for expense approvers

#### Creating Custom Roles

1. **Access Role Management**: Click **"🔐 Manage Roles"** from the Task Activity List sidebar menu
2. **Click "Add Role"**: Opens the role creation form
3. **Enter Role Details**:
   - **Role Name**: Unique identifier (e.g., "PROJECT_MANAGER", "FINANCE_VIEWER")
   - **Description**: Explanation of the role's purpose. Follow the recommended format below for best results in notification emails.
4. **Assign Permissions**: Check the boxes for permissions this role should have
   - Permissions are organized by resource (TASK_ACTIVITY, EXPENSE, etc.)
   - Each resource shows available actions (CREATE, READ, UPDATE, DELETE, etc.)
5. **Save Role**: Click "Create Role" button
6. **Success**: Role is created and can be assigned to users immediately

**Best Practices for Custom Roles:**
- Use clear, descriptive names (e.g., "READ_ONLY_TASKS" instead of "RO_TASKS")
- Document the purpose in the description field using the recommended format (see below)
- Start with minimal permissions and add more as needed
- Test new roles with a test user account before production use

#### Role Description Format

The description field supports a two-part format that controls how the role is displayed in **Profile Notification emails** sent to users.

**Format:** `Short Name - Full detail`

When the system sends a profile notification email, it uses the text **before** the ` - ` separator as a concise role label. If no ` - ` is present, the full description is used. If the description is blank, the raw role name (e.g., `EXPENSE_ADMIN`) is used as a fallback.

| Description field value | Displayed in notification email |
|---|---|
| `System Administrator - Full permissions for all functions` | `System Administrator` |
| `Standard User - Task and expense access for team members` | `Standard User` |
| `Read-Only Guest` | `Read-Only Guest` |
| *(blank)* | `GUEST` (raw role name) |

**Recommendation:** Always include a short, human-readable label before the ` - ` so that users receiving notification emails see a clear, friendly role description rather than a technical role name.

#### Editing Role Permissions

1. **Access Role Management**: Navigate to the role management page
2. **Select Role to Edit**: Click "Edit" button next to the role
3. **View Current Permissions**: See which permissions are currently assigned
4. **Modify Permissions**:
   - **Check boxes** to add permissions
   - **Uncheck boxes** to remove permissions
   - Role name is read-only (cannot be changed)
   - Description can be updated — follow the `Short Name - Full detail` format (see [Role Description Format](#role-description-format)) so notification emails display a clean, readable role label
5. **Save Changes**: Click "Save Changes" button
6. **Immediate Effect**: Permission changes take effect immediately for all users with that role

**Important Notes:**
- Removing permissions from a role immediately affects all users with that role
- Users must log out and log back in to see permission changes reflected in the UI
- Built-in roles (ADMIN, USER, GUEST, EXPENSE_ADMIN) and any custom roles can be edited — use caution, as permission changes take effect immediately for all users assigned that role
- Always test permission changes in a non-production environment first

#### Assigning Roles to Users

Roles are assigned through the User Management interface:

1. **Navigate to User Management**: Click "Manage Users" from header
2. **Edit User**: Click "Edit" button next to the user
3. **Select Role**: Choose from the dropdown list of available roles
4. **Save**: User immediately receives permissions from the new role

For detailed information on adding new roles to the system, see **`docs/Adding_Roles_and_Permissions_Guide.md`**.

#### Troubleshooting Permission Issues

**User reports they can't access a feature:**
1. Check user's assigned role in User Management
2. Navigate to Role Management and view the role's permissions
3. Verify the required permission is assigned to the role
4. Add missing permission if necessary
5. Ask user to log out and log back in

**Role changes not taking effect:**
- Users must log out and log back in after role or permission changes

### Viewing All User Tasks

As an administrator, you have the ability to view and manage tasks for all users in the system:

1. **Access Task List**: Click **"View Tasks"** from the main navigation
2. **View All Tasks**: By default, you'll see tasks from all users
3. **Filter by Specific User**:
    - Use the **"Filter by User"** dropdown at the top of the task list
    - Select a specific user from the list
    - Click **"Apply Filters"** or the page will reload automatically
    - You'll now see only tasks created by that user
4. **Clear Filter**:
    - Select the empty option in the dropdown, or
    - Clear the filter to return to viewing all tasks

**CSV Export:** When an administrator exports the task list to CSV, an additional **Username** column is included in the export, identifying which user each task belongs to. Regular users' exports do not include this column.

**Note:** Regular users only see their own tasks and do not have access to this filtering feature.

### User Self-Service Profile Management

All users (except Guest users) can manage their own profile information without administrator intervention. This reduces administrative burden while maintaining security controls.

**What Users Can Edit:**

- First Name
- Last Name  
- Company
- Email Address

**What Users Cannot Edit:**

- Username (immutable)
- Role (only administrators can change roles)
- Account Status (enabled/disabled)
- Account Lock Status
- Password (self-service password change available via dialog in Angular Profile or dedicated page in Backend)

**Access Methods:**

Users can access their profile through the navigation menu or the side menu — both provide the same profile editing and password change capabilities.

**Password Management:**

- Users can change their own password via the **"🔒 Update Password"** button in their profile page
- The password change form includes current password verification, new password validation with real-time feedback, and a confirm password field
- Password changes log the user out of all active sessions immediately; they must log back in with the new password
- Passwords must meet security requirements:
  - Minimum 10 characters
  - At least 1 uppercase letter
  - At least 1 numeric digit
  - At least 1 special character (+&%$#@!~*)
  - Not contain more than 2 consecutive identical characters
  - Not contain the username (case-insensitive)
  - Not be the same as the current password
- Passwords expire every 90 days
- Account lockout after 5 failed login attempts (administrator must unlock)

**Email Requirement:**

Users must have an email address configured to access expense management features. If a user reports they cannot add expenses, verify their email is populated in their profile.

**When to Edit as Administrator:**

You should edit a user's profile as administrator only when:

- The user is unable to access their profile (account locked, disabled, or Guest role)
- Changing role assignments
- Unlocking locked accounts
- Enabling/disabling accounts
- Initial user setup before first login

### Managing Roles in the Admin Dashboard

The React Dashboard provides a modern, intuitive interface for managing roles and permissions, complementing the Spring Boot backend role management system.

#### Accessing Roles Management

1. **Navigate to React Dashboard**: Access at https://taskactivitytracker.com/dashboard
2. **Click "Roles Management"**: From the dashboard home screen or sidebar menu
3. **Required Permission**: USER_MANAGEMENT:READ or higher

#### Viewing Roles

The Roles Management page displays all system roles in a Material-UI table:

- **Role Name**: Unique identifier for the role
- **Description**: Brief explanation of the role's purpose
- **Permissions**: Grouped by resource for easy readability (e.g., "USER_MANAGEMENT: CREATE, READ, UPDATE")
- **Actions**: Edit and Delete buttons for each role (requires USER_MANAGEMENT:UPDATE/DELETE permissions)

**Permission Display Format:**

Permissions are grouped by resource to improve readability:
- ✅ Good: "USER_MANAGEMENT: CREATE, READ, UPDATE | TASK_MANAGEMENT: READ, UPDATE"
- ❌ Before: Long comma-separated list difficult to scan

#### Creating New Roles

1. **Click "Add New Role"** button (top-right corner)
2. **Role Form Dialog Opens**:
   - **Role Name**: Enter unique identifier (e.g., "PROJECT_MANAGER", "FINANCE_VIEWER")
     - Must be unique across all roles
     - Displayed in uppercase by convention
   - **Description**: Enter clear explanation of the role's purpose
   - **Permissions**: Select from hierarchical permission tree
3. **Select Permissions**:
   - Permissions are organized by resource (USER_MANAGEMENT, TASK_MANAGEMENT, etc.)
   - **Master Checkbox**: Check/uncheck all permissions for a resource
   - **Indeterminate State**: Shows when only some permissions are selected
   - **Individual Checkboxes**: Select specific actions (CREATE, READ, UPDATE, DELETE)
4. **Click "Create"**: Role is created and immediately available for user assignment
5. **Success Message**: Confirmation displayed, table refreshes automatically

**Permission Resources:**
- `USER_MANAGEMENT`: User account management
- `TASK_MANAGEMENT`: Task activity operations
- `ROLE_MANAGEMENT`: Role and permission administration
- `SYSTEM_CONFIGURATION`: System settings (future)

#### Editing Existing Roles

1. **Click "Edit" icon** (pencil) next to the role you want to modify
2. **Role Form Dialog Opens**:
   - **Role Name**: Displayed as read-only (cannot be changed)
   - **Description**: Can be updated
   - **Current Permissions**: Pre-selected based on existing assignments
3. **Modify Permissions**:
   - Check boxes to add permissions
   - Uncheck boxes to remove permissions
   - Use master checkboxes for resource-level changes
4. **Click "Save"**: Changes are saved and take effect immediately
5. **Impact**: All users with this role receive updated permissions (requires re-login to see UI changes)

**Best Practices for Editing:**
- **Built-in Roles**: Use caution when modifying ADMIN, USER, GUEST, EXPENSE_ADMIN
- **Production Changes**: Test permission changes in non-production environment first
- **Communication**: Notify affected users when removing permissions from their role
- **Documentation**: Update role descriptions when changing permission scope

#### Deleting Roles

1. **Click "Delete" icon** (trash can) next to the role
2. **Confirmation Dialog Opens**:
   - Warning message displayed
   - ⚠️ **Important**: "Roles currently assigned to users cannot be deleted"
   - Lists affected users if role is in use
3. **Confirm or Cancel**:
   - **Cancel**: No changes made
   - **Delete**: Role is permanently removed (if not assigned to users)
4. **Error Handling**:
   - If role is assigned to users, deletion fails with error message
   - Must reassign users to different roles before deletion
   - Built-in roles (ADMIN, USER, GUEST, EXPENSE_ADMIN) have additional protection

**Pre-Deletion Checklist:**
- [ ] Check if role is assigned to any users
- [ ] Reassign users to appropriate alternative roles
- [ ] Document reason for role removal
- [ ] Verify no automated systems depend on this role

#### Permission Grouping and Display

The React interface groups permissions by resource for improved usability:

**Hierarchical Display:**
```
□ USER_MANAGEMENT (Master Checkbox)
  □ CREATE - Create new user accounts
  □ READ - View user information
  □ UPDATE - Modify user details
  □ DELETE - Remove user accounts
  
□ TASK_MANAGEMENT (Master Checkbox)
  □ CREATE - Create new tasks
  □ READ - View tasks
  □ UPDATE - Edit existing tasks
  □ DELETE - Remove tasks
```

**Indeterminate State:**

When some (but not all) permissions for a resource are selected, the master checkbox displays an indeterminate state (dash icon), making it clear that the resource has partial permissions.

#### Real-Time Validation and Error Handling

- **Duplicate Names**: Cannot create roles with names that already exist
- **Required Fields**: Role name and description are mandatory
- **Permission Selection**: Must select at least one permission
- **Delete Constraints**: Cannot delete roles assigned to users
- **Network Errors**: Graceful error handling with user-friendly messages
- **Loading States**: Visual feedback during API operations

#### Integration with User Management

Roles created or modified in the Roles Management interface are immediately available in the User Management interface:

1. Navigate to User Management
2. Edit or create a user
3. Role dropdown includes all custom roles
4. Assign role to user
5. User receives permissions defined in the role

**Synchronization:**

Changes made in either the React Dashboard or Spring Boot UI are synchronized through the backend database:
- Create role in React → Available in Spring Boot UI immediately
- Edit permissions in Spring Boot UI → Reflected in React Dashboard on page refresh

#### Troubleshooting

**Role not appearing in user assignment:**
- Refresh the User Management page
- Verify role was created successfully (check Roles Management table)
- Check browser console for errors

**Permission changes not taking effect:**
- Users must log out and log back in after role changes
- Clear browser cache if UI appears inconsistent
- Verify role exists in database

**Cannot delete role:**
- Check if role is assigned to users (error message lists users)
- Reassign users to different roles first
- Some built-in roles may have additional protections

**Permissions not grouped correctly:**
- Verify permission format follows "RESOURCE:ACTION" pattern
- Check backend logs for permission loading errors
- Ensure permissions table is properly seeded

### Managing Task Activities

Both the Thymeleaf and Angular UIs provide comprehensive task management capabilities. This section covers common operations for administrators and users.

#### Adding New Tasks

**Adding New Tasks:**

1. Click **"Add Task Activity"** from the task list
2. Fill in the task date, client, project, phase, and hours (all required)
3. Optionally add Task ID, Task Name, and a description
4. Click **"Save"** to create the task

#### Cloning Existing Tasks

The **Clone** feature allows you to quickly duplicate an existing task with today's date, useful for recurring activities.

**Angular Dashboard:**

1. **Locate Task to Clone**: Find the task in the task list table
2. **Click Clone Button**: Click the **content_copy icon** (📋) in the Actions column
3. **Review Cloned Data**: A dialog opens with:
    - All original task data (client, project, phase, hours, Task ID, Task Name, details) preserved
    - **Task Date automatically set to today**
    - Dialog title shows "Add Task Activity"
4. **Modify if Needed**: Update any fields (e.g., adjust hours or details)
5. **Save Cloned Task**: Click "Save" to create the new task
6. **Confirmation**: "Task cloned successfully" message appears

**Thymeleaf UI:**

1. **View Task Details**: Navigate to the task you want to clone
2. **Click "Clone Task"**: Creates a duplicate with today's date
3. **Modify and Save**: Adjust any fields and save

**Clone Use Cases:**

- **Daily Recurring Tasks**: Clone yesterday's standup meeting task
- **Similar Projects**: Clone a task and change only the client/project
- **Template Tasks**: Create a template task and clone it for each use
- **Bulk Entry**: Clone a task multiple times with minor modifications

#### Editing Tasks

**Angular Dashboard:**

1. **Locate Task**: Find the task in the task list table
2. **Click Edit Button**: Click the **edit icon** (✏️) in the Actions column
3. **Modify Task Data**:
    - Update any fields except username (username is immutable)
    - Dialog title shows "Edit Task Activity"
4. **Save Changes**: Click "Save"
5. **Confirmation**: "Task updated successfully" message appears

**Thymeleaf UI:**

1. **View Task**: Click on a task in the list
2. **Click "Edit"**: Opens the edit form
3. **Make Changes**: Update any field except username
4. **Save**: Click "Save" to apply changes

**Note for Administrators:**
- Administrators can edit tasks created by any user
- Regular users can only edit their own tasks
- The username field is always read-only to maintain data integrity

#### Deleting Tasks

**Angular Dashboard:**

1. **Locate Task**: Find the task in the task list table
2. **Click Delete Button**: Click the **delete icon** (🗑️) in the Actions column
3. **Confirm Deletion**: A confirmation dialog appears
4. **Confirm**: Click "Delete" in the confirmation dialog
5. **Task Removed**: Task is permanently deleted and list refreshes

**Thymeleaf UI:**

1. **View Task**: Navigate to the task details
2. **Click "Delete"**: Confirmation dialog appears
3. **Confirm**: Click "Yes" to permanently delete

**Important:**
- Deletion is permanent and cannot be undone
- Consider marking tasks as inactive instead if historical data is important
- Administrators can delete any user's tasks; users can only delete their own

#### Actions Column (Angular Dashboard)

The Actions column in the Angular dashboard provides three action buttons for each task row:

| Icon | Action | Description |
|------|--------|-------------|
| ✏️ (edit) | **Edit** | Opens edit dialog to modify task data |
| 📋 (content_copy) | **Clone** | Creates duplicate task with today's date |
| 🗑️ (delete) | **Delete** | Permanently deletes task after confirmation |

All action buttons use Angular Material icon buttons with tooltips for better usability.

### Changing User Passwords

1. **Access User Management**: Navigate to **"Manage Users"**
2. **Find User**: Locate the user in the list
3. **Click "Change Password"**: Opens password change form
4. **Enter New Password**: The password fields are **pre-filled with the default temporary password** (`P@ssword!123`) when changing another user's account; overwrite to set a custom password
   - When changing your own password, fields are blank (no pre-fill for self-service)
5. **Show Passwords (Optional)**: Use eye icons or checkbox to view passwords
6. **Optional**: Check "Force password update on next login"
7. **Save**: Click **"Change Password"**

#### Password History Validation

**Implemented**: February 2026

The system prevents users from reusing recent passwords to enhance security:

**How It Works:**
- System stores the last 5 password hashes for each user
- When changing passwords (admin or self-service), the new password is checked against this history
- If the new password matches any of the last 5 passwords, the change is rejected
- Only password hashes are stored (using BCrypt), never plain text

**What This Means for Administrators:**

1. **Password Rejections**: Users may report that their password change was rejected even though it meets complexity requirements
   - This is working as designed if they're trying to reuse a recent password
   - Explain that they need to choose a password they haven't used recently

2. **Force Password Change**: Even when admins force a password change, history validation still applies
   - This ensures users can't cycle between a small set of favorite passwords
   - The new admin-set password must not match the user's last 5 passwords

3. **Password Requirements**: All password change forms now display:
   - "Cannot match any of your previous 5 passwords"
   - This requirement appears alongside other password rules

4. **New User Creation**: When creating a new user account:
   - The initial password is automatically saved to their password history
   - This prevents them from immediately "changing" back to the same password

**Configuration** (contact your system administrator to change these defaults):

- Password history is enabled by default and checks the last 5 previous passwords
- These settings require a system configuration change and application restart to update

**Benefits:**
- Reduces risk of compromised credentials remaining in use
- Enforces meaningful password changes
- Meets compliance requirements for password rotation
- Prevents users from cycling between favorite passwords

**FAQs:**

**Q: Can I temporarily disable password history for a specific user?**  
A: No, the feature applies system-wide. However, a system administrator can temporarily disable it in configuration if needed for troubleshooting.

**Q: What if a user forgets all their passwords and can't create a new one?**  
A: As an administrator, you can set any new password that meets complexity requirements. The new password just can't match their last 5 passwords.

**Q: How long does password history persist?**  
A: Password history persists indefinitely but is limited to the configured number of entries (default: 5 most recent). When a user changes their password for the 6th time, the oldest entry is automatically removed.

**Q: What happens to password history when a user is deleted?**  
A: Password history is automatically deleted along with the user account (CASCADE DELETE).

### Self-Service Password Reset Feature

**Overview**: Users can reset their own passwords without administrator intervention using the password reset feature on the login page.

**How It Works:**

1. **User Initiates Reset**: User clicks "Reset Password" link on login page
2. **Enters Email**: User enters their registered email address
3. **Token Generation**: System generates a unique, secure token valid for 15 minutes
4. **Email Sent**: Reset email with secure link sent to user's email address
5. **User Clicks Link**: User clicks link in email to access password change form
6. **Password Changed**: User sets new password without needing current password
7. **Confirmation**: User receives confirmation email and can log in with new password

**Security Notes:**

- Reset tokens expire after 15 minutes and can only be used once
- Only users with a registered email address on their profile can use self-service reset
- The system does not confirm whether an email address exists (prevents user enumeration)

**Administrator Considerations:**

- **Email Required**: Users must have valid email addresses in their profiles to use password reset
- **Email Configuration**: System must have email (SMTP or AWS SES) configured and enabled
- **Monitoring**: Check application logs for password reset activity if needed
- **User Support**: If users don't receive reset emails, verify:
  - User has correct email address in profile
  - Email service is configured and operational
  - Check spam/junk folders
  - Verify `app.base-url` configuration for correct reset link URLs

**Email Configuration Requirements:**

For password reset to work, ensure these environment variables are configured:

- `MAIL_ENABLED=true` - Enable email notifications
- `MAIL_HOST` - SMTP server hostname
- `MAIL_PORT` - SMTP server port (typically 587)
- `MAIL_USERNAME` - SMTP authentication username
- `MAIL_PASSWORD` - SMTP authentication password
- `app.base-url` - Base URL for reset links (e.g., https://taskactivitytracker.com)

See [Email Configuration Management](#email-configuration-management) section for detailed setup instructions.

**Troubleshooting:**

| Issue | Solution |
|-------|----------|
| User doesn't receive reset email | 1. Verify user has email in profile<br>2. Check email service configuration<br>3. Check spam/junk folders<br>4. Review application logs for email errors |
| Reset link expired | User can request new reset link (expires after 15 minutes) |
| Reset link doesn't work | Token may be consumed or expired; request new reset |
| User has no email | Administrator must manually reset password using "Change Password" feature |
| GUEST users | **GUEST role users cannot reset passwords** (restriction for demo/temporary accounts). The reset form will accept the request but no email will be sent. Check logs for "Password reset blocked for GUEST user" warnings. Administrators must manually reset GUEST passwords using "Change Password" feature. |

---

### Managing User Dropdown Access

**Implemented**: February 2026

Administrators can restrict which Clients and Projects appear in dropdown menus for each individual user. Task recording and expense recording are controlled **independently** via two separate tabs: the **📋 Task** tab governs which Clients and Projects appear when a user logs time; the **💰 Expense** tab governs which Clients and Projects appear when a user records an expense. Saving on one tab never affects the other tab's assignments.

> **Why separate tabs?** The Clients and Projects available for task time-tracking (`TASK` category in Dropdown Management) and for expense recording (`EXPENSE` category) are maintained as distinct sets. A user may be authorized to bill expenses to a client they do not actively record time against, or vice versa.

**Key Concepts:**

- **Restricted by default**: Non-ADMIN users only see Clients/Projects that are either explicitly assigned to them *or* flagged as visible to all users
- **All Users flag**: Any dropdown value marked "All Users" is automatically visible to everyone — no explicit assignment required
- **ADMIN bypass**: Users with the ADMIN role always see all active Clients and Projects, regardless of assignments
- **UI only**: Filtering is applied to the dropdown menus; the system does not block saving entries against unauthorized values
- **Independent tabs**: TASK and EXPENSE assignments are stored separately — changing one tab does not affect the other

Both the Spring Boot UI and the React Admin Dashboard provide access management. The underlying data is identical — use whichever interface is more convenient.

#### Using the Spring Boot UI

1. **Navigate to User Management**: Click **"☰"** → **"👥 Manage Users"**
2. **Locate the User**: Find the user in the list (use filters if needed)
3. **Click "🔐 Access"**: Opens the dropdown access assignment page for that user
4. **Choose the Tab**:
   - **📋 Task tab** — controls Clients and Projects visible when the user logs task time
   - **💰 Expense tab** — controls Clients and Projects visible when the user records an expense
5. **Review the Checkboxes**:
   - **Clients section** (left column): Lists all active Client values for the selected category
   - **Projects section** (right column): Lists all active Project values for the selected category
   - Items marked **All Users** are shown with a grey badge and a pre-checked, disabled checkbox — they are already visible to this user and cannot be removed via this screen
6. **Select Assignments**: Check the boxes next to the Clients and Projects this user should be able to see
   - Use the **"Select All"** / **"Clear All"** links at the top of each section for convenience
7. **Click "💾 Save Access"**: Assignments for the **active tab only** are saved; the other tab's assignments are untouched
8. **Switch Tabs**: Repeat steps 4–7 for the other tab if needed
9. **Confirmation**: You are redirected back to User Management with a success message

#### Using the React Admin Dashboard

1. **Navigate to User Management**: Click **"User Management"** from the React Admin Dashboard sidebar or card
2. **Locate the User**: Use the filter fields (username, role, company) if needed
3. **Click the Manage Access button**: Click the **person-gear (🔑) icon** in the Actions column — this button is green and appears between the Change Password and Delete buttons
   - The button is **disabled** for ADMIN users (they always have full access) and shows a tooltip explaining why
4. **The Access Dialog opens**:
   - Header shows: "Manage Dropdown Access" with the username displayed below
   - A loading spinner appears briefly while assignment data is fetched
5. **Choose the Tab**: Click **Task** or **Expense** in the toggle control at the top of the dialog
   - **Task** — governs Clients and Projects visible when the user logs task time
   - **Expense** — governs Clients and Projects visible when the user records an expense
   - A caption below the toggle reminds you that saving only updates the active tab
6. **Review and Update Checkboxes**:
   - **Left column** — Clients for the selected tab
   - **Right column** — Projects for the selected tab
   - Items with the "All Users" flag: checkbox is **checked and disabled** (already visible to everyone), "All Users" chip displayed in blue, and a **🔓 Restrict** button appears
   - Items without the flag: checkbox is editable; a **🌐 All** button appears
7. **Toggle the "All Users" flag** (optional): Click **🌐 All** to make a value visible to every user, or **🔓 Restrict** to remove that flag — the change is applied immediately without navigating away
8. **Click "Save"**: Saves assignments for the **active tab only**; the other tab is not affected
   - The dialog closes automatically on success
9. **Switch Tabs**: Reopen the dialog and switch to the other tab to manage that set of assignments

#### Marking a Dropdown Value as "All Users"

To make a Client or Project visible to all users without individual assignment:

1. Navigate to **Manage Dropdowns** ("☰" → "🔧 Manage Dropdowns")
2. Find the Client or Project value (remember: TASK Clients and EXPENSE Clients are separate entries)
3. Click the **Edit** button
4. Check the **"All Users"** checkbox
5. Save — the value is now visible to every user automatically

Alternatively, the **🌐 All** button shown next to each value on the Access page sets this flag inline without leaving the page. The **🔓 Restrict** button removes it.

This is useful for commonly-used overhead entries (e.g., "Internal", "Non-Billable") that every user should always see.

#### Revoking Access

- **Spring Boot UI**: Open the **🔐 Access** page for the user, choose the appropriate tab (Task or Expense), and **uncheck** any Clients or Projects they should no longer see; click **💾 Save Access**
- **React Admin Dashboard**: Open the **Manage Access** dialog for the user, choose the appropriate tab, **uncheck** the values to revoke, and click **Save**
- The user's dropdowns update immediately on their next page load
- Existing task and expense entries are **not affected** — historical data is preserved

#### Notes for Administrators

| Scenario | Behavior |
|----------|---------|
| User has no assignments and no "All Users" values exist | User sees an empty Client/Project dropdown |
| User is ADMIN role | Always sees all active values — assignments are ignored |
| A dropdown value is deactivated | It no longer appears in any dropdown, regardless of access assignments |
| User is deleted | All their dropdown access rows are automatically removed (CASCADE DELETE) |
| Task and Expense tabs saved separately | Saving the Task tab does not clear Expense assignments, and vice versa |

---

### Sending Profile Notification Emails

**Implemented**: February 2026

Administrators can send a personalized profile detail email to one or more active users at once. This is useful when onboarding new users, distributing updated credentials, or confirming a user's current access assignments without requiring them to log in.

**What the email contains:**
- Account information (username, full name)
- A temporary password section — displayed only when the user's account has **"Force Password Update on Next Login"** enabled (indicating they have a temporary password that needs to be changed)
- Task access assignments (explicit Clients and Projects in the Task category)
- Expense access assignments (explicit Clients and Projects in the Expense category)

**Prerequisites:**
- Email (`MAIL_ENABLED`) must be configured and enabled in the system environment variables
- Only users with a valid email address in their profile are eligible; users without an email address do not appear in the list
- Disabled user accounts are excluded

#### Accessing Notify Users

1. **Click "☰"** to open the sidebar menu from the Task Activity List **or** the Expense List
2. **Click "📧 Notify Users"** — available to ADMIN users only

#### Sending Profile Emails

1. **The Notify Users page loads** with a table of all active users who have email addresses configured
2. **Filter by Last Name** (optional): Type in the **Last Name Filter** field and click **Search** to narrow the list; click **Reset** to clear
3. **Select recipients**: Check the checkbox next to each user you want to notify
   - Use the **Select All** checkbox in the table header to select or deselect all visible users at once
4. **Click "Send Profile Emails"**: A confirmation dialog appears showing the count of selected users
5. **Confirm**: Click **Confirm** in the dialog to send; click **Cancel** to abort
6. **Result**: A success message displays the number of emails sent and how many were skipped (if any had delivery issues)

#### Tips and Notes

| Scenario | Behavior |
|----------|----------|
| User has no email address | Excluded from the list entirely — not shown |
| User account is disabled | Excluded from the list entirely |
| "Force Password Update" is **checked** on the user | Password section appears in the email body |
| "Force Password Update" is **not checked** | Password section is omitted from the email body |
| Email delivery fails for one user | That user is counted as "skipped"; remaining sends continue |
| `MAIL_ENABLED=false` | No emails are sent; a warning is logged |

**Tip**: To send a welcome email with a temporary password to a newly created user, ensure "Force Password Update on Next Login" is checked on their account before using Notify Users. The email will then include their temporary credentials.

---

## Expense Management Administration

### Managing User Expenses

Administrators with ADMIN or EXPENSE_ADMIN roles can view and manage all user expenses:

1. **Access Expense List**: Click **"💰 Expense List"** from the navigation header, or navigate to the Expenses section in the Admin Dashboard for full management capabilities
   
2. **Expense Management Capabilities**:
   - **Create Expenses**: Add new expenses with integrated receipt upload in the Add dialog
   - **Edit Expenses**: Modify draft expenses and upload/replace receipts directly in the Edit dialog
   - **Clone Expenses**: Duplicate expenses for similar entries (date and receipt must be updated)
   - **Delete Expenses**: Remove draft expenses
   - **Filter Options**: Client, Project, Type, Status, Payment Method, Date Range, Username (admin)
   - **Actions Column**: Edit, Clone, and Delete buttons for each expense row
   - **Currency Formatting**: Amounts display with comma separators for improved readability

3. **View All Expenses**: By default, administrators see expenses from all users
3. **Filter Expenses**: Use comprehensive filtering options:
    - **User**: Filter by specific username
    - **Client**: Filter by client name
    - **Project**: Filter by project name
    - **Expense Type**: Filter by expense category (Travel, Home Office, etc.)
    - **Status**: Filter by workflow status (Draft, Submitted, Approved, Rejected, Reimbursed)
    - **Payment Method**: Filter by payment method
    - **Date Range**: Filter by expense date (Start Date and End Date)
    - Click **"Apply Filters"** to see filtered results
    - Click **"Reset Filters"** to clear all filters

4. **Expense Actions**:
    - **View Details**: Click on any expense to see full details including receipt
    - **Edit**: Modify expense details (ADMIN only, not available after submission)
    - **Delete**: Remove expenses (ADMIN only, only for Draft status)
    - **Export CSV**: Export filtered expense list to CSV for reporting

### Expense Approval Process

ADMIN and EXPENSE_ADMIN users can review and approve submitted expenses:

1. **Access Expense List**: Navigate to the expense list from the navigation header
2. **Filter for Pending Approvals**: Use the Status filter to select "Submitted" to see expenses awaiting approval
3. **Review Expense Details**:
    - Click **"Edit"** or **"View"** button on any expense to see full details
    - Review expense date, client, project, type
    - Verify amount and currency
    - Check payment method, vendor, and reference number
    - Read description and notes
    - View/download receipt attachment

4. **Approve Expense**:
    - From the expense detail page, click **"Approve"** button
    - Enter approval notes (optional but recommended)
    - Expense status changes to "Approved"
    - User is automatically notified via email
    - Expense becomes available for reimbursement processing
    - You are returned to the expense list with filters preserved

5. **Reject Expense**:
    - From the expense detail page, click **"Reject"** button
    - Enter rejection reason (required)
    - Expense status changes to "Rejected"
    - User is automatically notified via email with rejection notes
    - You are returned to the expense list with filters preserved

**Tip**: After applying filters (e.g., Status = Submitted), the filter values are preserved when you edit an expense and return to the list, making it efficient to process multiple expenses in sequence.

### Email Notifications for Expense Submissions

When a user submits an expense (changes status from Draft to Submitted), the system automatically sends an email notification to all configured expense approvers.

**Who Receives Notifications**:
- All email addresses configured in the `EXPENSE_APPROVERS` environment variable
- Multiple approvers can be configured using a comma-separated list
- Default approvers are set during system deployment

**Email Content**:
- Submitter's full name and username
- Expense ID
- Expense description
- Amount and currency
- Expense date
- Submission timestamp
- Instructions to review in the Approval Queue

**Purpose**:
- Alerts approvers immediately when expenses need review
- Provides key expense details without requiring login
- Includes direct instructions for accessing the Approval Queue

**Configuration**:
- Contact your system administrator to modify the list of approvers
- The approver list is managed via environment variables
- No database changes required to add/remove approvers

### Email Notifications for Status Changes

When you approve, reject, or reimburse an expense, the system automatically sends an email notification to the expense owner.

**Email Content**:
- User's full name
- Expense description  
- Amount and currency
- New status (Approved/Rejected/Reimbursed)
- Your full name as the processor
- Date and time of the action
- Any notes you entered

**Status-Specific Messages**:
- **Approved**: "Your expense has been approved and is ready for reimbursement processing."
- **Rejected**: "Your expense has been rejected. Please review the notes below for details."
- **Reimbursed**: "Your expense has been reimbursed. The payment should be reflected in your account soon."

**Important**:
- Users without an email address cannot access expense features
- Email field is required in user profiles for expense access
- The system displays the processor's full name in emails (not username)
- Email notifications are automatic and cannot be disabled

### Reimbursement Tracking

After expenses are approved, ADMIN and EXPENSE_ADMIN users can track reimbursements:

1. **Access Approved Expenses**: Filter expense list by Status = "Approved"
2. **Process Reimbursement**:
    - Click on expense to view details
    - Click **"Mark as Reimbursed"** button
    - Enter reimbursement details:
        - **Reimbursed Amount**: Actual amount paid (may differ from requested amount)
        - **Reimbursement Date**: Date of payment
        - **Reimbursement Notes**: Payment method, check number, transaction ID, etc.
    - Expense status changes to "Reimbursed"
    - User is automatically notified via email
    - You are returned to the expense list

3. **View Reimbursement History**:
    - Filter expense list by Status = "Reimbursed"
    - Export to CSV for accounting reconciliation
    - View complete audit trail: submitted date, approved date, reimbursed date

**Email Requirement for Expense Access**:
- Users must have a valid email address to access expense features
- When creating or editing users, ensure email field is populated for expense access
- Users without email will not see expense tracking options (similar to GUEST users)
- Email validation ensures proper format (username@domain.com)
- Maximum email length: 100 characters

### Receipt Management

Administrators can view and manage receipt attachments:

1. **View Receipts**: Click on expense to see details, then click receipt thumbnail or "View Receipt"
2. **Download Receipts**: Download receipt images for archival or printing
3. **Delete Receipts**: ADMIN users can delete receipt files (use with caution)

**Receipt Storage Options**:
- **Local Storage**: Receipts stored in `src/main/resources/receipts/` directory
- **AWS S3 Storage**: (If configured) Receipts stored in S3 bucket for scalability

---

### Managing Dropdowns

Dropdown management has been consolidated into a single, dynamic interface that supports multiple categories from one screen.

1. **Access Dropdown Management**: 
   - Option 1: Click **"☰"** to open the sidebar menu, then click **"🔧 Manage Dropdowns"**
   - Option 2: Navigate directly from the Angular Dashboard

2. **Select Category**: Use the dropdown filter to choose which category to manage:
    - **All Categories**: View all dropdown values across all categories
    - **CLIENT**: Manage client list
    - **PROJECT**: Manage project names
    - **PHASE**: Manage work phases (with TASK subcategory)
    - **EXPENSE**: Manage expense-related dropdowns with subcategories:
        - **EXPENSE_TYPE**: Types of expenses (Travel - Airfare, Hotel, Meals, Home Office Equipment, etc.)
        - **PAYMENT_METHOD**: Payment methods (Personal Credit Card, Personal Cash, Company Credit Card, Direct Bill)
        - **RECEIPT_STATUS**: Receipt availability (Attached, Pending, Not Available)
        - **EXPENSE_STATUS**: Workflow status (Draft, Submitted, Approved, Rejected, Reimbursed)
    - **Note**: New categories added to the database automatically appear in this list

**All Users Flag:**

Each Client and Project dropdown value can be marked as **"All Users"** to make it automatically visible to every user without requiring an explicit access assignment:

- **When to use**: Common overhead entries that should always be available system-wide (e.g., "Internal", "Non-Billable", "Training")
- **Display in admin UI**: "All Users" values appear with a grey **All Users** badge in the dropdown access assignment page and are shown as pre-checked, disabled checkboxes
- **Setting the flag**: Check the **"All Users"** checkbox when adding or editing a Client or Project value
- **Removing the flag**: Uncheck **"All Users"** — the value reverts to access-controlled and will only appear for users with an explicit assignment

**Billability Configuration:**

Each dropdown value can be marked as "Non-Billable" to classify work that should not be invoiced to clients:

- **Non-Billable Checkbox**: When adding or editing any dropdown value, check the "Non-Billable" box for:
  - **Clients**: Internal clients or corporate overhead
  - **Projects**: Internal projects, training, meetings, administrative work
  - **Phases**: Non-billable work phases (proposals, research, internal meetings)
  - **Expense Types**: Non-reimbursable expense categories

- **How It Works**:
  - **Tasks**: A task is billable only if its client, project, AND phase are ALL billable
  - **Expenses**: An expense is billable only if its client, project, AND type are ALL billable
  - **Filtering**: Users can filter weekly timesheets and expense sheets by billability status
  - **Reports**: Analytics dashboards distinguish billable from non-billable hours

- **Visual Indicators**:
  - Non-billable dropdown values display a 🚫 badge in management interfaces
  - Billable/non-billable status appears in the "Billability" column when managing dropdowns

- **Example Configuration**:
  - Mark "Corporate" client as non-billable → All tasks for this client are non-billable
  - Mark "Training" project as non-billable → Training tasks are always non-billable
  - Mark "Internal Meeting" phase as non-billable → Meeting time is excluded from billable hours

3. **Filter by Subcategory** (Optional):
    - After selecting a category, use the subcategory dropdown to further narrow results
    - Shows only subcategories that exist within the selected category
    - Select "All Subcategories" to view all values for the selected category
    - When you change categories, the subcategory filter automatically resets
    - **Tip**: Use this to quickly find specific groups of values when managing large dropdown lists

4. **Filter-First Workflow**:
    - The form is disabled until you select a category (except "All Categories")
    - This prevents accidentally adding values to the wrong category
    - Select a specific category to enable the "Add" form

5. **Add New Category**:
    - Click the **"Add New Category"** button in the header (next to "Task Activity List")
    - A modal dialog opens with three required fields:
        - **Category**: Enter the new category name (automatically converted to uppercase)
        - **Subcategory**: Enter the subcategory (automatically converted to uppercase)
        - **Value**: Enter the first dropdown value for this category
    - All three fields are required
    - The system validates that the category name doesn't already exist
    - After successful creation, you're automatically redirected to the new category's view
    - **Example**: Create a new "LOCATION" category with subcategory "OFFICE" and value "New York HQ"
    - **Note**: This creates the category AND its first value in a single operation

6. **Add New Values**:
    - Select a category from the filter dropdown
    - Enter a subcategory (e.g., 'TASK' for PHASE entries, 'GENERAL' for CLIENT/PROJECT)
    - Enter new value name in the "Item Value" field
    - Click "Add" button
    - Value appears in the table immediately
    - **Note**: Display order is automatically assigned based on existing values
    - **Subcategory Purpose**: Allows for finer-grained categorization within main categories (e.g., different types of phases like 'TASK', 'MEETING', 'ADMIN')

7. **Edit Values**:
    - Click the "Edit" (pencil icon) button next to any value
    - Opens a modal dialog with the current details
    - Update the category, subcategory, item value, display order, or active status
    - Click "Update" to save changes
    - Changes are reflected immediately in the table

8. **Delete Values**:
    - Click the "Delete" (trash icon) button next to any value
    - A styled confirmation modal appears (no more ugly browser dialogs!)
    - Confirm deletion by clicking "Delete" in the modal
    - Click "Cancel" to abort
    - **Note**: Cannot delete values that are in use by existing tasks

9. **Category Display**:
    - When viewing "All Categories", both "Category" and "Subcategory" columns show the full classification
    - When filtering by a specific category, the category column is hidden since all values belong to the same category
    - Active/inactive status is displayed with color-coded badges
    - Values are sorted by category → displayOrder → itemValue for consistent display

10. **Active/Inactive Toggle**:
    - Values can be marked as active or inactive via the Edit dialog
    - **How Inactive Values Work**:
      - **Add Mode**: Inactive values do NOT appear in dropdown lists when creating new tasks or expenses
      - **Clone Mode**: Inactive values do NOT appear in dropdown lists, and any inactive values from the original task/expense are automatically cleared
      - **View Mode**: Inactive values ARE displayed if they were previously assigned to the task/expense (read-only)
      - **Edit Mode**: Inactive values ARE displayed if currently assigned, allowing you to see what was selected, but you cannot select other inactive values
    - **Best Practice**: Use the inactive status instead of deleting values that are still referenced by existing tasks or expenses
    - **Impact on Users**: When users clone a task/expense with inactive values, those fields will be empty and they must select from active values before saving
    - **Historical Data Preserved**: Marking a value inactive doesn't affect existing records - they still display the original value when viewed or edited

**Benefits of Consolidated Dropdown Management:**
- ✅ Single interface for all dropdown categories
- ✅ Automatically supports new categories added to the database
- ✅ No code changes required when adding new dropdown categories
- ✅ "Add New Category" button for creating categories without database access
- ✅ Category uniqueness validation prevents duplicate categories
- ✅ Auto-uppercase for consistent category naming
- ✅ Filter-first design prevents errors
- ✅ Subcategory filtering for easier management of large value lists
- ✅ Cascading filters (subcategory auto-resets when category changes)
- ✅ Professional styled confirmation modals
- ✅ Consistent user experience across Spring Boot and Angular interfaces

---

### Guest Activity Dashboard

Administrators have access to a dedicated dashboard for monitoring GUEST user login activity. This feature is available in both the Spring Boot backend UI and the React Admin Dashboard, providing security visibility and access tracking.

**Available Interfaces:**
- **Main Application**: Traditional server-rendered interface at `/admin/guest-activity`
- **React Admin Dashboard**: Modern interface — access the Guest Activity card at https://taskactivitytracker.com/dashboard

**Accessing the Dashboard:**
1. **Navigate (Spring Boot UI)**: 
   - Option 1: Click **"☰"** to open the sidebar menu, then click **"📊 Guest Activity"**
   - Option 2: Navigate from Manage Users page
2. **Navigate (React Dashboard)**:
   - Click **"🎯 Admin Dashboard"** from sidebar menu
   - Click the **"Guest Activity"** card
2. **View Statistics**: The dashboard displays key metrics at the top:
   - **Total Logins**: Count of all GUEST login attempts (successful and failed)
   - **Unique Locations**: Number of distinct IP addresses that accessed the account
   - **Last Login**: Most recent successful login timestamp
   - **Success Rate**: Percentage of successful vs. failed login attempts

**Login Audit Table:**
- Shows detailed login history with columns:
  - **Date/Time**: When the login attempt occurred
  - **IP Address**: Source IP address of the login attempt
  - **Location**: Geographic information (if available) or source identifier
  - **Status**: Success (green badge) or Failed (red badge)
- Sorted by most recent first
- Automatically updates with new login activity

**Exporting Guest Activity Data:**

Since the login audit data is stored in-memory and resets when the application restarts, administrators can export the data for record-keeping:

1. **Click "📥 Export CSV"**: Opens a modal dialog with CSV data
2. **Choose Export Method**:
   - **📋 Copy to Clipboard**: Copies CSV data for pasting into Excel or other applications
   - **💾 Download CSV**: Creates a timestamped CSV file (format: `guest_login_audit_YYYY-MM-DD.csv`)
   - **Close**: Closes the modal without exporting
3. **CSV Format**: Includes headers and quoted fields:
   ```
   Date/Time,IP Address,Location,Status
   "Nov 16, 2025, 07:28 AM","127.0.0.1","Web Login","Success"
   ```

**Important Notes:**
- Login tracking data is stored **in-memory only** and will be cleared when the application restarts or is redeployed
- Data persists for the duration of the application runtime
- Export to CSV regularly if you need to maintain historical records
- Maximum of 1,000 login attempts are retained in memory
- Only GUEST user logins are tracked on this dashboard
- Administrators can track their own and other user logins through CloudWatch logs

**Use Cases:**
- Security auditing: Monitor unusual access patterns or suspicious login attempts
- Usage tracking: Understand when and from where guests are accessing the system
- Troubleshooting: Identify failed login attempts and potential access issues
- Compliance: Maintain records of guest account access for audit purposes

---

## User Analytics & Performance Monitoring

As an ADMIN user, you have access to comprehensive team analytics through the **Analytics & Reports** page in the **React Admin Dashboard**. This page provides 10 report tabs covering user performance, project health, client analysis, and time-tracking patterns.

### Accessing Analytics & Reports

**Via Sidebar:**
1. Navigate to the React Admin Dashboard (`https://taskactivitytracker.com/dashboard`)
2. Click **Analytics & Reports** in the left sidebar (chart icon, red)

**Via Dashboard Home:**
- Click the red **Analytics & Reports** card on the Dashboard Home page

### Report Controls

All report controls appear in a single panel at the top of the page. Changes to date range, scope, or filters reload all 10 tabs simultaneously.

#### Date Range

All reports are scoped by the selected date range.

**Preset Buttons:**
- **Current Week** — Monday through today
- **This Month** — 1st of the current month through today
- **Last Month** — full prior calendar month
- **Last 3 Months** — rolling 90-day window
- **This Year** — January 1st through today
- **All Time** — entire database history (default)

**Custom Range:**
- Enter a start date and/or end date in the date fields
- Click **Apply** to load data for that range
- Click **Clear** to reset to All Time

> **Note:** Tabs 8 (Tracking Compliance) and 10 (Period Delta) require a specific date range to be selected. They display an informational message when All Time is active.

#### Scope

Controls whether reports include only active records or all records (including inactive ones). The toggle contains two buttons:

| Button | Behaviour |
|--------|-----------|
| **Active Only** *(default)* | Reports include only enabled user accounts, active clients, and active projects |
| **Include Inactive** | Reports include all historical data — disabled accounts, inactive clients, and inactive projects are shown |

Use **Include Inactive** when you need to analyse historical work logged against clients or projects that have since been deactivated, or to view output from employees who have left.

> **Always excluded regardless of scope:** GUEST demo accounts and any clients, projects, or phases marked "All Access" (e.g. PTO, Corporate overhead, Town Meetings, Sick Days) are never included in analytics reports.

#### Filters

Three multiselect dropdowns let you narrow all 10 reports to a specific subset of the loaded data without re-fetching:

| Filter | Description |
|--------|-------------|
| **Users** | Restrict reports to one or more specific team members |
| **Clients** | Restrict reports to one or more specific clients |
| **Projects** | Restrict reports to one or more specific projects |

- Each dropdown lists only values present in the currently loaded data set (respects the selected date range and scope)
- Selecting nothing in a dropdown means "all" for that dimension
- Multiple selections within a dropdown are combined with OR logic (e.g. selecting two users shows both)
- Filters across dropdowns are combined with AND logic (e.g. User A AND Client X)
- A **Clear filters** link appears below the dropdowns when any filter is active; click it to reset all three to "all" at once
- Filter changes recompute all tabs instantly — no server round-trip

### Summary Statistics Bar

After data loads, a statistics bar appears below the filters showing team-wide totals for the selected period:

| Stat | Description |
|------|-------------|
| **Active Users** | Number of distinct users with recorded tasks |
| **Total Hours** | Sum of all hours across all users |
| **Total Billable** | Hours from fully-billable tasks (client + project + phase all billable) |
| **Total Non-Billable** | Hours from any task with a non-billable component |
| **Overall Billability %** | Billable ÷ Total Hours × 100 |

### Report Tabs

The page has 10 tabs displayed in a horizontally scrollable strip. Select any tab to view that report.

---

#### Tab 1: User Summary

A ranked table showing all users' performance for the selected period.

**Columns:**
- **Rank** — Position by total hours (🏆 #1, 🥈 #2, 🥉 #3)
- **Username** — User's login identifier
- **Total Hours** — Cumulative hours in the period
- **Billable** — Hours on fully-billable tasks
- **Non-Billable** — Hours with any non-billable component
- **Billability %** — Colored progress bar (green = high, amber = medium, red = low)
- **Tasks** — Count of task activities submitted
- **Avg Billable/Day** — Average billable hours per working day (only days with billable work counted)
- **Top Client** — Client with the most hours (non-billable clients excluded)
- **Top Project** — Project with the most hours (non-billable projects excluded)
- **Last Activity** — Date of most recent task submission

Click any column header to sort the table.

---

#### Tab 2: Hours by User

A horizontal bar chart comparing total hours across team members.

- **Segmented bars**: Green = billable hours, Orange = non-billable hours
- **Percentage labels**: Each user's share of total team hours
- **Sorted by total hours**: Highest contributor shown first
- **Detail table**: Below the chart, exact hour values per user

---

#### Tab 3: Phase Distribution

Breaks down hours logged per **project**, further subdivided by **project phase**.

- Projects are ranked by total hours (highest first)
- Each row shows the project name, total hours, and a set of colored phase chips
- Each chip displays the phase name and its percentage of that project's hours
- Useful for understanding where effort is concentrated within a project lifecycle

---

#### Tab 4: Stale Projects

Identifies projects that have had no task activity since a configurable threshold.

**Staleness Slider:**
- Drag the slider to set the inactivity threshold (range: 7 to 180 days)
- The table recomputes instantly without re-fetching data from the server
- Default threshold: 90 days

**Columns:**
- **Project** — Project name
- **Last Activity** — Date of most recent task logged to this project
- **Days Since Activity** — Calendar days since last task
- **Staleness** — Color-coded chip: 🟢 Active (under threshold), 🟡 Stale (1–2× threshold), 🔴 Very Stale (>2× threshold)

Use this tab to identify projects that may need to be closed or followed up on.

---

#### Tab 5: Client Billability

Compares billable vs. non-billable hours at the **client** level.

- Each row represents one client
- Colored progress bars show the billable percentage visually
- Sorted by total hours (highest first)
- Useful for understanding which clients drive the most billable revenue

---

#### Tab 6: Client Timeline

A **monthly heatmap matrix** showing hours logged per client, broken down by calendar month.

- **Rows**: Clients (sorted by total hours)
- **Columns**: Calendar months within the selected date range
- **Cell color intensity**: Darker blue = more hours in that month/client combination
- **Sticky client column**: Client names stay visible when scrolling horizontally
- Useful for spotting seasonal patterns and identifying client activity gaps

---

#### Tab 7: Day of Week

Shows total and average hours broken down by day of the week (Monday through Sunday).

- **Bar chart**: Horizontal bars for each weekday
  - Blue bars: Monday–Friday (working days)
  - Grey bars: Saturday–Sunday (weekend)
- **Detail table**: Below the chart — total hours and average hours per occurrence for each day
- Useful for identifying whether the team works predominantly on certain days or logs weekend hours

---

#### Tab 8: Tracking Compliance *(requires date range)*

Measures how consistently each user logs time on **weekdays** (Monday–Friday) within the selected period.

- **Compliance %**: Days with at least one task ÷ total weekdays in range × 100
- **Sorted worst-first**: Users with lowest compliance appear at the top
- Color-coded: Green = high compliance, Amber = moderate, Red = low

> This tab requires a specific date range (not All Time). Select a preset or custom range to activate it.

Use this tab to identify team members who may be forgetting to log their time.

---

#### Tab 9: Task Repetition

Identifies the most frequently recurring task entries across the entire team.

- Displays the **top 50 task IDs** by total occurrence count
- Task IDs appear in monospace font for readability
- Cross-user view: counts occurrences regardless of which user logged the task
- Useful for identifying standard recurring tasks that might benefit from templates or automation

---

#### Tab 10: Period Delta *(requires date range)*

Compares the **current selected period** against the **prior period** of identical duration.

- **Prior period**: Automatically computed as the same number of days immediately before the selected start date (no manual input required)
- **Two sub-tabs**: By User | By Client
- **Columns**: Current Hours, Prior Hours, Change (hours), Change %
- **Trend icons**: ▲ green (increase), ▼ red (decrease), — (no change)
- Sorted by current-period hours descending

> This tab requires a specific date range (not All Time). Select a preset or custom range to activate it.

Use this tab to compare team or client output across consecutive periods (e.g., this month vs. last month).

---

### Billable vs. Non-Billable Tracking

The system uses a flexible flag-based approach to distinguish billable from non-billable hours:

- **Flag-Based System**: Each dropdown value (client, project, phase, expense type) has a "Non-Billable" flag
- **Task Billability**: A task is billable only if its client, project, AND phase are ALL marked as billable
- **Expense Billability**: An expense is billable only if its client, project, AND type are ALL marked as billable
- **ANY Non-Billable Component**: If any component is flagged as non-billable, the entire entry is non-billable
- **Visual Indicators**: Billable hours appear in green, non-billable in orange
- **Average Calculation**: Avg Billable/Day only includes days where billable work was performed
- **Top Client/Project**: These fields exclude non-billable clients and projects to show actual client work
- **Configuration**: Administrators set billability flags when creating or editing dropdown values

**Filtering by Billability (weekly timesheet/expense sheet):**

Users can filter their own weekly views by billability status:
- **All**: Shows all tasks/expenses (default view)
- **Billable**: Shows only entries where all components are billable
- **Non-Billable**: Shows entries with any non-billable component

### Using Analytics for Management

**Performance Reviews:**
- Use Tab 1 (User Summary) for trophy rankings and average hours per day
- Check Last Activity date to identify inactive accounts
- Use Tab 10 (Period Delta) to compare individual user output period-over-period

**Resource Allocation:**
- Tab 5 (Client Billability) shows which clients drive the most billable work
- Tab 6 (Client Timeline) reveals seasonal patterns and client engagement gaps
- Tab 3 (Phase Distribution) shows where effort is concentrated within projects

**Project Health:**
- Tab 4 (Stale Projects) flags projects with no recent activity — adjust the slider for your team's expected cadence
- Tab 9 (Task Repetition) identifies recurring tasks that may benefit from streamlining

**Team Behaviour Patterns:**
- Tab 7 (Day of Week) shows whether weekend work is occurring
- Tab 8 (Tracking Compliance) identifies users who are not logging time consistently

**Client Management:**
- Tab 5 and Tab 6 together give a full picture of client activity over time
- Tab 10 (Period Delta by Client) highlights which clients saw increased or decreased activity

### Best Practices

- **Review weekly**: Open Analytics & Reports at least once per week to stay informed about team activity
- **Use presets for speed**: The "Current Week" and "This Month" presets load the most common views instantly
- **Tune the stale threshold**: Set the Stale Projects slider to match your project cadence (e.g., 30 days for active sprints, 90 days for long-running engagements)
- **Use Scope for historical analysis**: Switch to **Include Inactive** when you need to review work logged against deactivated clients, projects, or former employees
- **Use Filters to focus**: Select one or more Users, Clients, or Projects to zero in on a specific area without changing the date range — all 10 tabs update instantly
- **Combine tabs**: Use Tab 8 (Compliance) to find who isn't logging, then Tab 1 (User Summary) to assess the business impact
- **Privacy considerations**: Use data responsibly and communicate analytics practices to team members
- **Data-driven decisions**: Base resource allocation on actual hours worked, not estimates

### Important Notes

- Only ADMIN users can access the Analytics & Reports page
- All data is computed client-side from task records — no separate analytics database required
- Tabs 8 (Tracking Compliance) and 10 (Period Delta) require a date range to be selected; they display an informational message when All Time is active
- The Stale Projects slider (Tab 4) recalculates the table instantly without a server round-trip
- Data reflects all task activities in the database matching the selected date range across all users

---

## Common User Support Scenarios

Quick reference for the most frequent support requests administrators receive.

### "I can't log in" — Account Locked

**Symptom**: User receives "Account locked due to too many failed login attempts"

**Solution**: Navigate to **Manage Users** → find the user (look for the 🔒 icon) → **Edit** → uncheck **Account Locked** → **Save**. Consider resetting the password if you suspect unauthorized access.

### "I forgot my password" — Self-Service Reset

**Solution**: Direct the user to the **"Reset Password"** link on the login page. They need a valid email address on their profile. If they have no email, or are a Guest user, you must reset it manually: **Manage Users** → **Change Password**.

### "I can't see my clients or projects in the dropdown"

**Solution**: Navigate to **Manage Users** → click the 🔐 **Access** button for that user → assign the appropriate Clients and Projects on the Task or Expense tab → **Save**.

### "I can't add expenses" / Expense buttons are missing

**Cause**: User has no email address on their profile, or has the Guest role.

**Solution**: Edit the user's profile and add their email address. If they have the Guest role, upgrading their role to User will also enable expense features.

### "My password expired and I can't reset it myself"

**Note**: Guest users cannot reset their own passwords.

**Solution**: Go to **Manage Users** → find the user → **Change Password** → set a new password and provide it to the user directly.

### "I can't see a client or project that I expect to see"

**Solution**: Either (1) the value has been marked inactive in **Manage Dropdowns** (look for the inactive badge), or (2) the value has not been assigned to that user. Check both **Manage Dropdowns** and **Manage Users → Access** for that user.

### "I submitted an expense but my approver didn't get a notification email"

**Solution**: Verify the expense approver email address is correctly configured under **Email Configuration Management**. Also verify the submitting user has a valid email on their profile.

### "A user needs to be removed from the system"

**Important**: Users who have existing task or expense entries cannot be deleted — disabling their account is the correct approach.

**Solution**: **Edit** the user → uncheck **Enabled** → **Save**. Only delete accounts that have no task or expense history.

---

## Security Features

> **📘 Comprehensive Security Documentation**  
> For detailed information about all security measures, controls, and best practices implemented in this application, please refer to the **[Security Measures and Best Practices](Security_Measures_and_Best_Practices.md)** document.
> 
> **Recent Security Enhancements (January 2026):**
> - Admin endpoint access control with defense-in-depth security
> - JWT token type differentiation (access vs refresh tokens)
> - Enhanced account status enforcement in authentication filter
> - Receipt XSS prevention via Content-Disposition headers
> - MIME sniffing protection with X-Content-Type-Options
> - Refresh token validation to prevent token misuse
> - **Server-side JWT token revocation and blacklisting**
> - **Magic number validation for file uploads (prevents malicious files)**
> - **Password hash removal from debug logs**
>
> This section provides a quick overview of key security features relevant to administrators.

### Account Lockout Policy

The system includes automatic account lockout protection to prevent unauthorized access:

**How It Works:**
- Users are allowed **5 failed login attempts**
- After 5 failed attempts, the account is automatically locked
- The lockout applies to all user roles (GUEST, USER, and ADMIN)
- Locked accounts cannot log in until unlocked by an administrator

**Administrator Notifications:**
- When an account is locked, an **email notification is automatically sent** to the administrator
- The email includes:
  - Username of the locked account
  - Number of failed login attempts
  - IP address of the last failed attempt
  - Timestamp of the lockout
- Configure the administrator email address in application.properties: `app.mail.admin-email`

**Unlocking Accounts:**
1. Navigate to **"👥 Manage Users"**
2. Identify locked accounts by the 🔒 indicator in the user list
3. Click **"Edit"** on the locked user
4. Uncheck the **"Account Locked"** checkbox
5. Optionally reset the **"Failed Login Attempts"** counter to 0
6. Click **"Save"**
7. The user can now log in again

**Best Practices:**
- Review lockout notification emails promptly to identify potential security threats
- Investigate suspicious lockout patterns (multiple attempts from same IP, repeated lockouts)
- Consider resetting the user's password when unlocking if you suspect unauthorized access
- Educate users about password policies to reduce accidental lockouts
- Monitor the IP addresses in lockout notifications for unusual geographic locations

**Email Configuration:**
Email notifications require proper SMTP configuration. See the Developer Guide for details on configuring email settings for local development and AWS deployments.

### Session Security

When a user logs out or changes their password, their session is immediately invalidated. This means that if an account is suspected to be compromised, you can force the user to change their password (via Manage Users) to immediately cut off any unauthorized access. Expired sessions are automatically cleaned up by the system each night.

---

## Administrative Processes and One-Off Tasks

As an administrator, you have access to various maintenance and administrative tasks that can be run independently of the main application.

### Database Schema Initialization

The application includes automatic database initialization for new deployments:

**DatabaseInitializer (AWS Profile)**
- **Purpose**: Initializes database schema and seed data on first deployment
- **Trigger**: Runs automatically on AWS startup via `CommandLineRunner`
- **Location**: `DatabaseInitializer.java`
- **Function**: Executes `schema.sql` and `data.sql` if tables don't exist
- **Safety**: Checks for existing tables before running to prevent data loss

**DataInitializer (All Profiles)**
- **Purpose**: Creates initial admin user if none exists
- **Trigger**: Runs automatically on startup via `@PostConstruct`
- **Location**: `DataInitializer.java`
- **Profiles**: `local`, `docker`, `aws`
- **Configuration**: Set admin password via `APP_ADMIN_INITIAL_PASSWORD` environment variable

### Running One-Off Administrative Tasks

#### 1. Database Schema Updates

**Via SQL Scripts:**
```bash
# Connect to PostgreSQL database
psql -h <database-host> -U <username> -d AmmoP1DB

# Run migration script
\i /path/to/migration.sql
```

**Via Spring Boot Profile:**
```bash
# Create a custom admin profile
# Add to application-admin.properties:
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:migrations/V1_add_column.sql

# Run with admin profile
java -jar taskactivity.jar --spring.profiles.active=admin
```

#### 2. User Management Scripts

**Reset User Password (PowerShell):**
```powershell
# Located at: scripts/reset-export_user-passwords.ps1
.\scripts\reset-export_user-passwords.ps1
```

**Export User Data:**
```powershell
# Export all users to CSV
.\scripts\export-users-csv.ps1

# Export dropdown values
.\scripts\export-dropdowns-csv.ps1

# Export task activities
.\scripts\export-tasks-csv.ps1
```

#### 3. Database Backup and Restore

**Backup Database:**
```bash
# PostgreSQL backup
pg_dump -h <host> -U <username> -d AmmoP1DB -F c -f backup_$(date +%Y%m%d).dump

# Or using AWS RDS
aws rds create-db-snapshot \
  --db-instance-identifier taskactivity-db \
  --db-snapshot-identifier taskactivity-snapshot-$(date +%Y%m%d)
```

**Restore Database:**
```bash
# PostgreSQL restore
pg_restore -h <host> -U <username> -d AmmoP1DB backup.dump

# Or using AWS RDS snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier taskactivity-db-restored \
  --db-snapshot-identifier taskactivity-snapshot-20250101
```

#### 4. Secrets Management

**Backup Docker Secrets:**
```bash
# Located at: scripts/backup-secrets.sh
./scripts/backup-secrets.sh

# Options:
#   backup    - Create encrypted backup of all secrets
#   restore   - Restore secrets from backup
#   list      - List available backups
#   verify    - Verify backup integrity
```

**Rotate Secrets:**
```bash
# Located at: scripts/rotate-secrets.sh
./scripts/rotate-secrets.sh

# Automatically rotates:
# - Database passwords
# - Admin credentials
# - JWT secrets
```

#### 5. Production Environment Setup

**Initial Production Setup:**
```bash
# Located at: scripts/setup-production.sh
./scripts/setup-production.sh

# Interactive setup wizard for:
# - Docker Swarm initialization
# - Secrets configuration
# - SSL certificate setup
# - Database initialization
```

#### 6. Health Monitoring

**Check Application Health:**
```bash
# Located at: scripts/monitor-health.sh
./scripts/monitor-health.sh

# Monitors:
# - Application health endpoints
# - Database connectivity
# - Resource usage (CPU, memory)
# - Response times
```

#### 7. Infrastructure and Deployment Tasks

Infrastructure-level tasks such as Docker deployments, Kubernetes jobs, and AWS ECS operations are managed by your platform/DevOps team. If you need assistance with a deployment-level issue, contact your system administrator or refer to the AWS deployment documentation in the `aws/` folder.

---

## Database Query Tool (SQL to CSV Export)

### Overview

Administrators can execute custom SQL queries against the database and export results to CSV format using a PowerShell script that calls a secure REST API endpoint. This tool provides a safe way to extract data for reporting and analysis without requiring direct database access.

### Security Features

- **Authentication Required**: Must authenticate with admin credentials
- **Admin-Only Access**: Only users with ADMIN role can execute queries
- **SELECT-Only Queries**: Only SELECT statements are allowed (INSERT, UPDATE, DELETE, DROP, etc. are blocked)
- **Keyword Validation**: Dangerous SQL keywords are detected using word boundary matching
- **Row Limit**: Results are limited to 10,000 rows to prevent memory issues
- **No Direct Database Access**: Queries execute through the Spring Boot application API

### Prerequisites

- **For Local Testing**: Spring Boot application running locally (`.\start-local.ps1`)
- **For AWS/Production**: Application deployed and accessible via Cloudflare Tunnel (https://taskactivitytracker.com)
- **Admin Credentials**: Valid admin username and password configured in `.env` or `.env.local` file
- **PowerShell**: Script requires PowerShell 5.1 or higher

### Configuration Files

**`.env` (AWS/Production)**
```ini
# API endpoint for AWS deployment (Cloudflare Tunnel)
API_URL=https://taskactivitytracker.com

# Admin credentials for API authentication
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your-admin-password
```

**`.env.local` (Local Development Only — not for production use)**
```ini
# API endpoint for local testing
API_URL=http://localhost:8080

# Admin credentials for local API authentication
ADMIN_USERNAME=admin
ADMIN_PASSWORD=Admin123!

# Database Configuration for local Spring Boot
DB_HOST=localhost
DB_PORT=5432
DB_NAME=AmmoP1DB
DB_USERNAME=postgres
DB_PASSWORD=your-db-password
```

### Usage

**1. Create a SQL query file** (e.g., `my-query.sql`):
```sql
SELECT id, username, email, userrole, created_date 
FROM users 
WHERE userrole = 'ADMIN' 
ORDER BY created_date DESC
LIMIT 10;
```

**2. Execute the script:**

```powershell
# Local testing (uses .env.local if it exists, otherwise .env)
.\scripts\execute-sql-to-csv-api.ps1 -SqlFile "my-query.sql" -OutputCsv "results.csv"

# Explicitly specify .env file
.\scripts\execute-sql-to-csv-api.ps1 -SqlFile "my-query.sql" -OutputCsv "results.csv" -EnvFile ".env"

# Override API URL for testing
.\scripts\execute-sql-to-csv-api.ps1 -SqlFile "my-query.sql" -OutputCsv "results.csv" -ApiUrl "http://localhost:8080"
```

**3. Results:**
- CSV file is created with query results
- File size and row count are displayed
- CSV includes proper escaping for commas, quotes, and newlines

### Example Queries

**Get all active users:**
```sql
SELECT id, username, firstname, lastname, userrole, company, enabled, created_date
FROM users 
WHERE enabled = true
ORDER BY created_date DESC;
```

**Task hours by user and client:**
```sql
SELECT 
    t.username,
    t.client,
    COUNT(*) as task_count,
    SUM(t.hours) as total_hours
FROM tasks t
WHERE t.taskdate >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY t.username, t.client
ORDER BY total_hours DESC;
```

**Recent expense submissions:**
```sql
SELECT 
    e.username,
    e.expense_date,
    e.expense_type,
    e.amount,
    e.currency,
    e.expense_status,
    e.created_date
FROM expenses e
WHERE e.created_date >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY e.created_date DESC;
```

### Table and Column Reference

**Common Tables:**
- `users` - User accounts (username, firstname, lastname, email, userrole, enabled, created_date)
- `tasks` - Task activities (username, taskdate, client, project, phase, hours, description)
- `expenses` - Expense entries (username, expense_date, expense_type, amount, currency, expense_status)
- `dropdownvalues` - Dropdown configuration (category, subcategory, itemvalue, displayorder, isactive)

**Important Notes:**
- Column names use lowercase (e.g., `userrole`, `created_date`, not `role`, `created_at`)
- All tables are in the `public` schema
- Date columns are `created_date`, `taskdate`, `expense_date` (not `created_at`)

### CSV Format

The exported CSV files follow RFC 4180 standard:
- **Headers**: Column names from SELECT statement
- **Comma-Separated**: Fields separated by commas
- **Quoted Fields**: Values containing commas, quotes, or newlines are wrapped in double quotes
- **Escaped Quotes**: Double quotes within values are escaped by doubling them (`""`)
- **UTF-8 Encoding**: Supports international characters

### Error Messages

**Authentication Failed:**
```
[ERROR] Authentication failed: (401) Unauthorized
```
→ Check admin username and password in `.env` file

**Query Contains Disallowed Keywords:**
```
[ERROR] Query execution failed: (400) Bad Request
Error: Query contains disallowed keywords
```
→ Remove INSERT, UPDATE, DELETE, DROP, CREATE, ALTER, TRUNCATE, EXEC, EXECUTE, GRANT, or REVOKE from query

**SQL Grammar Error:**
```
[ERROR] Query execution failed: (500) Internal Server Error
Error executing query: bad SQL grammar
```
→ Check table/column names (remember: `userrole` not `role`, `created_date` not `created_at`)

**Connection Refused:**
```
[ERROR] Query execution failed: Unable to connect to the remote server
```
→ Ensure Spring Boot application is running (local) or accessible (AWS)

### API Endpoint Details

**Endpoint:** `POST /api/admin/query/execute`  
**Authentication:** Bearer token (JWT)  
**Authorization:** `ADMIN` role required  
**Request Body:**
```json
{
  "query": "SELECT * FROM users LIMIT 10;"
}
```

**Response:** CSV content as plain text with `text/csv` content type

### Best Practices

1. **Test queries locally first** before running against production
2. **Use LIMIT clauses** to restrict result sizes for large tables
3. **Filter by date ranges** when querying time-series data
4. **Save frequently-used queries** as `.sql` files for reuse
5. **Review column names** from schema.sql to ensure correct syntax
6. **Keep sensitive data secure** - don't commit `.env` files with passwords to git
7. **Use descriptive output filenames** including date/purpose (e.g., `user-report-2025-12.csv`)

### Troubleshooting

**Script can't find .env file:**
```powershell
# Check current directory
Get-Location

# Script looks for .env in workspace root (parent of scripts directory)
# Verify .env exists:
Test-Path .\.env
```

**Password prompts every time:**
```powershell
# Make sure ADMIN_PASSWORD is set in .env file
# Script will prompt if password is empty or missing
```

**Wrong database being queried:**
```powershell
# Check which .env file is being used (script shows API URL at startup)
# Local: http://localhost:8080 → local database
# AWS: https://taskactivitytracker.com → RDS database
```

---

## Email Configuration Management

The system sends email notifications for various events including expense submissions, approvals, Jenkins builds/deployments, and account lockouts. This section explains how to configure and manage email addresses for all notification types.

### Email Notification Types

The application sends two categories of email notifications:

**1. Jenkins CI/CD Notifications**
- **Build Success/Failure**: Sent when Jenkins builds complete
- **Deploy Success/Failure**: Sent when deployments to AWS complete
- Recipients configured via: `JENKINS_BUILD_NOTIFICATION_EMAIL` and `JENKINS_DEPLOY_NOTIFICATION_EMAIL`

**2. Application Notifications**
- **Expense Submissions**: Sent to approvers when users submit expenses
- **Expense Status Changes**: Sent to users when expenses are approved/rejected/reimbursed
- **Password Expiration Warnings**: Sent to users before password expires
- **Account Lockout Alerts**: Sent to administrators when accounts are locked
- **Contact System Admin Requests**: Sent to the administrator when a user submits a message via the sidebar menu
- Recipients configured via: `ADMIN_EMAIL` and `EXPENSE_APPROVERS`

**Note:** User notification emails (expense confirmations, password expiration) are sent to the email address configured in each user's profile. Only the administrator and approver notification addresses are configured at the system level.

### Email-Dependent Features

Several user-facing features are **automatically hidden** when `MAIL_ENABLED=false` to prevent misleading confirmations:

| Feature | Where | Behavior when `MAIL_ENABLED=false` |
|---------|-------|-------------------------------------|
| **Contact System Admin** | Task List and Expense List sidebar menus | Menu option is hidden entirely |
| **Password Reset** | Login screen | Request form is visible but no email is sent; GUEST users are always blocked regardless of mail setting |

> **Administrator Note:** If users report that the **Contact System Admin** option is missing from the sidebar, verify that `MAIL_ENABLED=true` is set in the environment configuration. The option will not appear until email is enabled.

### Email Configuration Variables

All email addresses are configured via environment variables:

| Variable | Purpose | Example |
|----------|---------|---------|
| `MAIL_FROM` | Sender address for all outgoing emails | `noreply@taskactivitytracker.com` |
| `ADMIN_EMAIL` | Administrator notifications (account lockouts, security alerts) | `admin@example.com` |
| `EXPENSE_APPROVERS` | Expense submission notifications | `approver1@example.com,approver2@example.com` |
| `JENKINS_BUILD_NOTIFICATION_EMAIL` | Jenkins build success/failure notifications | `dev-team@example.com` |
| `JENKINS_DEPLOY_NOTIFICATION_EMAIL` | Jenkins deployment notifications | `ops-team@example.com,manager@example.com` |

**Multiple Recipients:**
- Use **commas** (`,`) or **semicolons** (`;`) to separate multiple email addresses
- Example: `user1@example.com,user2@example.com` or `user1@example.com;user2@example.com`
- Both separators are supported by AWS SES

### Method 1: Update via PowerShell Script (Recommended)

The `update-ecs-variables.ps1` script automates updating email addresses and other environment variables from your `.env` file to the AWS ECS task definition.

**Prerequisites:**
- `.env` file in project root with email configuration
- AWS CLI installed and configured (only if deploying to AWS)
- PowerShell 5.1 or higher

**Step 1: Edit Email Addresses in `.env` File**

```ini
# Email Application Settings
MAIL_FROM=noreply@taskactivitytracker.com
ADMIN_EMAIL=admin@example.com
EXPENSE_APPROVERS=approver1@example.com,approver2@example.com
JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@example.com
JENKINS_DEPLOY_NOTIFICATION_EMAIL=ops-team@example.com,manager@example.com
```

**Step 2: Run the Update Script**

```powershell
# Update task definition JSON only (no AWS deployment)
.\aws\update-ecs-variables.ps1

# Update task definition AND deploy to AWS ECS
.\aws\update-ecs-variables.ps1 -DeployToAws
```

**What the Script Does:**
1. ✅ Loads email values from `.env` file
2. ✅ Updates `taskactivity-task-definition.json` with new addresses
3. ✅ Creates automatic backup before making changes
4. ✅ Validates JSON format
5. ✅ Shows comparison of old vs new values
6. ✅ Optionally registers new task definition with AWS ECS
7. ✅ Updates ECS service to use new configuration

**Script Output Example:**

```
========================================
ECS Task Definition Email Update Script
========================================

Loading environment variables from .env file...
Exported MAIL_FROM=noreply@taskactivitytracker.com
Exported ADMIN_EMAIL=admin@example.com
Exported EXPENSE_APPROVERS=approver1@example.com,approver2@example.com
Exported JENKINS_BUILD_NOTIFICATION_EMAIL=dev-team@example.com
Exported JENKINS_DEPLOY_NOTIFICATION_EMAIL=ops-team@example.com,manager@example.com

Email Configuration from .env file:
  MAIL_FROM: noreply@taskactivitytracker.com
  ADMIN_EMAIL: admin@example.com
  EXPENSE_APPROVERS: approver1@example.com,approver2@example.com
  JENKINS_BUILD_NOTIFICATION_EMAIL: dev-team@example.com
  JENKINS_DEPLOY_NOTIFICATION_EMAIL: ops-team@example.com,manager@example.com

Creating backup of task definition...
  Backup saved to: C:\...\aws\taskactivity-task-definition.json.backup

Updating task definition JSON file...

Comparison Results:

  Unchanged (already current):
    ✓ MAIL_FROM: noreply@taskactivitytracker.com
    ✓ ADMIN_EMAIL: admin@example.com

  Updated 2 email configuration(s):
    EXPENSE_APPROVERS:
      Old: old-approver@example.com
      New: approver1@example.com,approver2@example.com
    JENKINS_DEPLOY_NOTIFICATION_EMAIL:
      Old: single@example.com
      New: ops-team@example.com,manager@example.com

Task definition JSON file updated successfully
  File: C:\...\aws\taskactivity-task-definition.json
```

**Deployment Timeline:**

After running with `-DeployToAws`, changes take effect in **3-5 minutes**:
1. New ECS task starts with updated environment variables (~30 seconds)
2. Health check grace period (60 seconds)
3. Health checks validate new task (1-2 minutes)
4. Old task drains and stops (~30 seconds)

**Monitor deployment progress:**
- AWS Console: https://console.aws.amazon.com/ecs/v2/clusters/taskactivity-cluster/services/taskactivity-service
- AWS CLI: `aws ecs describe-services --cluster taskactivity-cluster --services taskactivity-service`

### Method 2: Update via AWS Console

If you prefer using the AWS Console instead of the script:

**Step 1: Navigate to ECS Task Definitions**
1. Open AWS Console: https://console.aws.amazon.com/ecs
2. Select **Task Definitions** from left sidebar
3. Click on **taskactivity** task definition family
4. Click **Create new revision** button

**Step 2: Update Environment Variables**
1. Scroll to **Container definitions**
2. Click on the **taskactivity** container
3. Scroll to **Environment variables** section
4. Locate and update these variables:
   - `MAIL_FROM`
   - `ADMIN_EMAIL`
   - `EXPENSE_APPROVERS`
   - `JENKINS_BUILD_NOTIFICATION_EMAIL`
   - `JENKINS_DEPLOY_NOTIFICATION_EMAIL`
5. Use commas or semicolons to separate multiple emails
6. Click **Update** button

**Step 3: Create New Revision**
1. Scroll to bottom of page
2. Click **Create** button
3. New revision number will be displayed (e.g., `taskactivity:5`)

**Step 4: Update ECS Service**
1. Navigate to **Clusters** → **taskactivity-cluster**
2. Click **Services** tab
3. Select **taskactivity-service**
4. Click **Update** button
5. In **Revision** dropdown, select the new revision number
6. Click **Update** button at bottom
7. Deployment will begin automatically

**Step 5: Monitor Deployment**
1. Stay on the service details page
2. Click **Deployments** tab
3. Watch for:
   - ✅ **Running count** matches desired count
   - ✅ **Status** shows "PRIMARY" deployment "COMPLETED"
   - ✅ New task is **HEALTHY**

### Method 3: Update via AWS CLI

For command-line enthusiasts or automation:

```bash
# Get current task definition
aws ecs describe-task-definition \
  --task-definition taskactivity \
  --query taskDefinition \
  --region us-east-1 > temp-task-def.json

# Edit temp-task-def.json to update email environment variables
# Then register new task definition
aws ecs register-task-definition \
  --cli-input-json file://temp-task-def.json \
  --region us-east-1

# Update service to use new task definition
aws ecs update-service \
  --cluster taskactivity-cluster \
  --service taskactivity-service \
  --task-definition taskactivity \
  --region us-east-1
```

### Testing Email Configuration

After updating email addresses, verify they work correctly:

**Test Jenkins Notifications:**
1. Trigger a Jenkins build manually
2. Wait for build completion (success or failure)
3. Check configured email addresses for notification

**Test Expense Notifications:**
1. Create and submit a test expense as a regular user
2. Check expense approver email addresses
3. Approve or reject the expense as ADMIN/EXPENSE_ADMIN
4. Check submitter's email for status notification

**Test Account Lockout Notifications:**
1. Attempt to log in with wrong password 5 times
2. Account will be locked
3. Check admin email address for lockout notification

### Troubleshooting Email Issues

**Emails Not Being Sent:**

Check application logs for email-related errors:
```bash
# AWS CloudWatch Logs
aws logs tail /ecs/taskactivity --follow --region us-east-1 | grep -i "email\|mail"

# Or via AWS Console
# CloudWatch → Log Groups → /ecs/taskactivity → Latest log stream
```

**Common Issues:**

1. **`MAIL_ENABLED` not set to `true`**
   - Check task definition: `MAIL_ENABLED` should be `true`
   - Restart service after updating

2. **AWS SES not configured**
   - Verify `MAIL_USE_AWS_SDK=true` in task definition
   - Ensure ECS task role has SES permissions
   - Check AWS SES console for verified email addresses

3. **Invalid email format**
   - Emails must follow format: `username@domain.com`
   - Check for typos or extra spaces
   - Verify separator is comma or semicolon (not both mixed randomly)

4. **Changes not taking effect**
   - Verify new task is running (check ECS service deployments)
   - Old task must fully stop before new config is active
   - Wait 3-5 minutes for complete deployment

**View Current Email Configuration:**

```bash
# Get running task's environment variables
aws ecs describe-tasks \
  --cluster taskactivity-cluster \
  --tasks $(aws ecs list-tasks --cluster taskactivity-cluster --service-name taskactivity-service --query 'taskArns[0]' --output text) \
  --query 'tasks[0].overrides.containerOverrides[0].environment' \
  --region us-east-1
```

### Email Security Considerations

- **Use AWS SES (recommended for production)**: More reliable than SMTP, uses IAM role credentials
- **Sender Address Verification**: Ensure `MAIL_FROM` address is verified in AWS SES
- **No Credentials Needed**: AWS SES SDK uses ECS task role, no username/password required
- **Secure Environment Variables**: Email addresses in ECS task definition are not encrypted (not sensitive)
- **Rate Limiting**: AWS SES has sending limits (check SES console for your account limits)

### Related Documentation

- **AWS SES Setup**: See `aws/AWS_SES_Setup_Guide.md` for detailed SES configuration
- **Email Service Code**: See `src/main/java/com/ammons/taskactivity/service/EmailService.java`
- **Jenkins Configuration**: See `Jenkinsfile` for how Jenkins calls the notification API

---

## System Monitoring and Health Checks

Administrators need to monitor the application's health, performance, and availability. This section covers monitoring tools, health checks, and troubleshooting procedures.

### Health Check Endpoints

The application exposes several health check endpoints for monitoring:

| Endpoint | Purpose | Authentication Required |
|----------|---------|------------------------|
| `/api/health` | Comprehensive health status (database, uptime) | No |
| `/api/health/simple` | Simple "OK" response for load balancer health checks | No |
| `/api/health/startup` | Startup time and uptime information | No |

**Example Health Check Response:**

```json
{
    "status": "UP",
    "database": "connected",
    "timestamp": "2026-01-19T15:30:00Z",
    "uptime": "3 days, 8 hours, 34 minutes"
}
```

**Using Health Checks:**

```bash
# Check application health via API
curl https://taskactivitytracker.com/api/health

# Check via AWS health check
aws ecs describe-services \
  --cluster taskactivity-cluster \
  --services taskactivity-service \
  --region us-east-1 \
  --query 'services[0].healthCheckGracePeriodSeconds'
```

### Health Monitoring Script

A comprehensive health monitoring script is available at `scripts/monitor-health.sh` (for Docker deployments).

**Features:**
- Monitors application health endpoints
- Checks database connectivity
- Tracks system resources (CPU, memory, disk)
- Analyzes application logs for errors
- Sends alerts via webhooks (Slack/Teams)
- Generates detailed health reports

**Usage:**

```bash
# Single health check
./scripts/monitor-health.sh check

# Continuous monitoring (every 60 seconds)
./scripts/monitor-health.sh monitor 60

# Show current status
./scripts/monitor-health.sh status

# Generate health report
./scripts/monitor-health.sh report
```

**Sample Output:**

```
[2026-01-19 15:30:00] ✓ Swarm cluster is healthy
[2026-01-19 15:30:01] ✓ All required secrets are present
[2026-01-19 15:30:02] ✓ All services are healthy
[2026-01-19 15:30:03] ✓ Application is responsive (245ms)
[2026-01-19 15:30:04] ✓ Database service is running
[2026-01-19 15:30:05] ✓ System resources are within normal ranges (CPU: 15%, Memory: 42%, Disk: 35%)
[2026-01-19 15:30:06] ✓ No critical errors found in recent logs

=== All health checks passed ===
```

### AWS CloudWatch Monitoring

For AWS deployments, application logs and metrics are sent to CloudWatch.

**Access CloudWatch Logs:**
1. Open AWS Console: https://console.aws.amazon.com/cloudwatch
2. Select **Log groups** from left sidebar
3. Click on `/ecs/taskactivity` log group
4. Select latest log stream to view real-time logs

**Command Line Access:**

```bash
# Tail application logs
aws logs tail /ecs/taskactivity --follow --region us-east-1

# Search for specific errors
aws logs tail /ecs/taskactivity --follow --region us-east-1 | grep -i "error\|exception"

# View logs for specific time range
aws logs filter-log-events \
  --log-group-name /ecs/taskactivity \
  --start-time 1706534400000 \
  --end-time 1706537000000 \
  --region us-east-1
```

**Key Metrics to Monitor:**

| Metric | Description | Alert Threshold |
|--------|-------------|----------------|
| **CPUUtilization** | ECS task CPU usage | > 80% sustained |
| **MemoryUtilization** | ECS task memory usage | > 85% sustained |
| **DatabaseConnections** | Active RDS connections | > 90% of max |
| **FreeStorageSpace** | RDS disk space | < 10% remaining |
| **ReadLatency / WriteLatency** | RDS query performance | > 100ms average |
| **4xx / 5xx Errors** | Application error rates | > 5% of requests |

**Create CloudWatch Alarms:**

```bash
# Example: Alert on high CPU usage
aws cloudwatch put-metric-alarm \
  --alarm-name taskactivity-high-cpu \
  --alarm-description "Alert when ECS CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=ServiceName,Value=taskactivity-service Name=ClusterName,Value=taskactivity-cluster \
  --region us-east-1
```

### Application Performance Monitoring

**Monitor Active Sessions:**
- Check **Analytics & Reports** in the React Admin Dashboard (Admin only)
- View concurrent user activity
- Track login/logout patterns

**Database Performance:**
```sql
-- Check active database connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'AmmoP1DB';

-- Check slow queries
SELECT pid, now() - query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active' AND now() - query_start > interval '5 seconds'
ORDER BY duration DESC;

-- Check database size
SELECT pg_size_pretty(pg_database_size('AmmoP1DB'));
```

**Application Response Times:**
- Monitor health check endpoint response times
- Track JWT token generation/validation times
- Watch for slow database queries in logs

### Common Monitoring Tasks

**Daily:**
- ✅ Check ECS task running status
- ✅ Review application logs for errors
- ✅ Verify RDS database status
- ✅ Monitor expense notification delivery

**Weekly:**
- ✅ Review CloudWatch metrics trends
- ✅ Check billing dashboard for cost anomalies
- ✅ Verify S3 receipt uploads are working
- ✅ Review account lockout alerts

**Monthly:**
- ✅ Analyze cost trends and optimize resources
- ✅ Review database backup retention
- ✅ Check for security updates (Spring Boot, dependencies)
- ✅ Review user access patterns and inactive accounts
- ✅ Verify JWT token revocation list size

### Troubleshooting Common Issues

**Issue: Application Not Responding**

**Symptoms:** Health check endpoint returns 503 or times out

**Steps to Diagnose:**
1. Check ECS service status:
   ```bash
   aws ecs describe-services --cluster taskactivity-cluster --services taskactivity-service
   ```
2. Check task health:
   ```bash
   aws ecs list-tasks --cluster taskactivity-cluster --service-name taskactivity-service
   aws ecs describe-tasks --cluster taskactivity-cluster --tasks <task-arn>
   ```
3. Review CloudWatch logs for startup errors
4. Check database connectivity

**Common Causes:**
- Database connection pool exhausted
- Out of memory (check MemoryUtilization metric)
- Database credentials incorrect
- Network connectivity issues

**Issue: High CPU/Memory Usage**

**Symptoms:** ECS tasks showing CPU > 80% or Memory > 85%

**Steps to Diagnose:**
1. Check current resource utilization:
   ```bash
   aws cloudwatch get-metric-statistics \
     --namespace AWS/ECS \
     --metric-name CPUUtilization \
     --dimensions Name=ServiceName,Value=taskactivity-service Name=ClusterName,Value=taskactivity-cluster \
     --start-time 2026-01-19T12:00:00Z \
     --end-time 2026-01-19T15:00:00Z \
     --period 300 \
     --statistics Average
   ```
2. Review application logs for heavy operations
3. Check for database query performance issues

**Solutions:**
- Scale up ECS task count (increase desired tasks)
- Optimize slow database queries
- Increase task CPU/memory limits in task definition

**Issue: Database Connectivity Errors**

**Symptoms:** Logs show `SQLException`, `ConnectionException`, or database timeouts

**Steps to Diagnose:**
1. Check RDS instance status:
   ```bash
   aws rds describe-db-instances --db-instance-identifier taskactivity-db
   ```
2. Verify security group allows ECS tasks to connect
3. Check database credentials in Secrets Manager
4. Review RDS CloudWatch metrics (DatabaseConnections)

**Common Causes:**
- Database password changed but not updated in Secrets Manager
- RDS instance stopped or restarting
- Security group rules blocking connection
- Connection pool exhausted (too many active connections)

**Issue: Emails Not Being Sent**

See [Troubleshooting Email Issues](#troubleshooting-email-issues) section above.

### Deployment and Rollback Procedures

**Deploy New Version:**

Via AWS Console:
1. Update ECS task definition with new image tag
2. Create new revision
3. Update ECS service to use new revision
4. Monitor deployment progress (3-5 minutes)

Via AWS CLI:
```bash
# Register new task definition
aws ecs register-task-definition --cli-input-json file://taskactivity-task-definition.json

# Update service
aws ecs update-service \
  --cluster taskactivity-cluster \
  --service taskactivity-service \
  --task-definition taskactivity
```

**Rollback to Previous Version:**

Via AWS Console:
1. Navigate to **ECS** → **Clusters** → **taskactivity-cluster**
2. Select **taskactivity-service**
3. Click **Update**
4. Select previous task definition revision from dropdown
5. Click **Update**

Via AWS CLI:
```bash
# List recent task definitions
aws ecs list-task-definitions \
  --family-prefix taskactivity \
  --status ACTIVE \
  --sort DESC \
  --max-items 5

# Rollback to previous version
aws ecs update-service \
  --cluster taskactivity-cluster \
  --service taskactivity-service \
  --task-definition taskactivity:<previous-revision-number>
```

**Monitor Deployment:**
```bash
# Watch deployment status
aws ecs describe-services \
  --cluster taskactivity-cluster \
  --services taskactivity-service \
  --query 'services[0].deployments'

# Check running tasks
aws ecs list-tasks \
  --cluster taskactivity-cluster \
  --service-name taskactivity-service \
  --desired-status RUNNING
```

### Backup and Disaster Recovery

**Database Backups:**

AWS RDS automated backups are enabled with 7-day retention.

**Manual Backup:**
```bash
# Create RDS snapshot
aws rds create-db-snapshot \
  --db-instance-identifier taskactivity-db \
  --db-snapshot-identifier taskactivity-manual-backup-$(date +%Y%m%d)

# Verify snapshot created
aws rds describe-db-snapshots \
  --db-instance-identifier taskactivity-db
```

**Database Restore from Snapshot:**
```bash
# Restore to new RDS instance
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier taskactivity-db-restored \
  --db-snapshot-identifier taskactivity-manual-backup-20260119

# Update ECS environment variables to point to new database
# Update JWT_SECRET_NAME to new database endpoint in task definition
```

**Application Configuration Backups:**

Important files to backup regularly:
- `taskactivity-task-definition.json` (ECS configuration)
- `.env` file (local development settings)
- CloudFormation templates (if using IaC)
- AWS Secrets Manager values (database credentials, JWT secret)

**Receipt File Backups:**

S3 bucket has lifecycle policy for automatic archiving:
- Receipts older than 90 days → moved to Glacier
- Receipts older than 1 year → moved to Deep Archive

**Verify S3 backups:**
```bash
# List recent receipts
aws s3 ls s3://taskactivity-receipts/receipts/ --recursive --human-readable

# Check lifecycle policy
aws s3api get-bucket-lifecycle-configuration --bucket taskactivity-receipts
```

### Performance Optimization Tips

**Database:**
- Review slow query logs monthly
- Add indexes for frequently queried columns
- Archive old task activities (> 2 years)
- Consider read replicas for reporting queries

**Application:**
- Enable HTTP caching for static resources
- Use CDN for Angular frontend assets
- Monitor JWT revocation list size (clean up old entries)
- Configure appropriate connection pool sizes

**Infrastructure:**
- Right-size ECS tasks (CPU/memory)
- Use Application Auto Scaling for variable load
- Optimize RDS instance class based on usage
- Enable Multi-AZ for production availability
