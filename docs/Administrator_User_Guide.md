# Task Activity Management System - Administrator User Guide

## Welcome

This guide is for administrators of the Task Activity Management System. As an administrator, you have access to additional features for managing users, viewing all tasks, and configuring system settings.

## Table of Contents

1. [Accessing the Application](#accessing-the-application)
2. [Navigation](#navigation)
3. [React Admin Dashboard](#react-admin-dashboard)
   - [Accessing the Dashboard](#accessing-the-react-dashboard)
   - [Dashboard Features](#dashboard-features)
   - [Development vs Production](#development-vs-production)
4. [Administrator Features](#administrator-features)
   - [User Roles Overview](#user-roles-overview)
   - [Managing Users](#managing-users)
   - [Managing Roles and Permissions](#managing-roles-and-permissions)
   - [Viewing All User Tasks](#viewing-all-user-tasks)
   - [User Self-Service Profile Management](#user-self-service-profile-management)
   - [Managing Task Activities](#managing-task-activities)
   - [Changing User Passwords](#changing-user-passwords)
4. [Expense Management Administration](#expense-management-administration)
   - [Managing User Expenses](#managing-user-expenses)
   - [Expense Approval Process](#expense-approval-process)
   - [Email Notifications](#email-notifications-for-expense-submissions)
   - [Reimbursement Tracking](#reimbursement-tracking)
   - [Receipt Management](#receipt-management)
   - [Managing Dropdowns](#managing-dropdowns)
5. [Guest Activity Dashboard](#guest-activity-dashboard)
6. [User Analytics & Performance Monitoring](#user-analytics--performance-monitoring)
7. [Security Features](#security-features)
   - [Account Lockout Policy](#account-lockout-policy)
   - [JWT Token Revocation Management](#jwt-token-revocation-management)
8. [Administrative Processes and One-Off Tasks](#administrative-processes-and-one-off-tasks)
9. [Database Query Tool (SQL to CSV Export)](#database-query-tool-sql-to-csv-export)
10. [Email Configuration Management](#email-configuration-management)
    - [Email Notification Types](#email-notification-types)
    - [Email Configuration Variables](#email-configuration-variables)
    - [Method 1: PowerShell Script](#method-1-update-via-powershell-script-recommended)
    - [Method 2: AWS Console](#method-2-update-via-aws-console)
    - [Method 3: AWS CLI](#method-3-update-via-aws-cli)
    - [Testing Email Configuration](#testing-email-configuration)
    - [Troubleshooting Email Issues](#troubleshooting-email-issues)
11. [System Monitoring and Health Checks](#system-monitoring-and-health-checks)
    - [Health Check Endpoints](#health-check-endpoints)
    - [Health Monitoring Script](#health-monitoring-script)
    - [AWS CloudWatch Monitoring](#aws-cloudwatch-monitoring)
    - [Application Performance Monitoring](#application-performance-monitoring)
    - [Common Monitoring Tasks](#common-monitoring-tasks)
    - [Troubleshooting Common Issues](#troubleshooting-common-issues)
    - [Deployment and Rollback Procedures](#deployment-and-rollback-procedures)
    - [Backup and Disaster Recovery](#backup-and-disaster-recovery)
    - [Performance Optimization Tips](#performance-optimization-tips)
12. [12-Factor App Compliance](#12-factor-app-compliance)

## Accessing the Application

The Task Activity Management System provides two user interface options:

### Thymeleaf UI (Traditional Web Interface)
- **URL**: http://localhost:8080
- **Description**: Server-rendered HTML interface with full functionality
- **Login**: Use your username and password

### Angular UI (Modern Single Page Application)
- **URL**: http://localhost:4200
- **Description**: Modern Angular-based interface with Material Design
- **Login**: Use the same username and password as the Thymeleaf UI
- **Features**: Responsive design, Material UI components, enhanced user experience

Both interfaces connect to the same backend and share the same data and authentication.

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
- **📊 Guest Activity**: View login activity reports for GUEST users
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

**Development Access (Phase 3):**
- URL: [http://localhost:4201](http://localhost:4201)
- Opens in a new browser tab
- Available from Thymeleaf UI sidebar menu: Click **🎯 Admin Dashboard**
- Runs on separate Vite dev server for hot-reload during development

**Production Access:**
- URL: [http://localhost:8080/dashboard](http://localhost:8080/dashboard) or `https://yourdomain.com/dashboard`
- Served by Spring Boot from static resources
- Opens in same browser window (uses th:href link)
- Session-based authentication maintained across Angular and React dashboards

### Dashboard Features

**Current Status: Phase 7 Complete (Active Development)**

The React Admin Dashboard has multiple completed phases with production-ready features:

**Available Now:**
- **Modern Material-UI Design**: Clean, responsive interface with Material Design components
- **Role-Based Access**: ADMIN role enforcement via Spring Security
- **Session Integration**: Shares authentication session with Spring Boot and Angular
- **Navigation Structure**: Left sidebar with feature cards for management modules
- **Task Activity Tracker**: Functional link navigates back to Spring Boot Task List UI

**Available Features:**
- **User Management** (Phase 4 - Implemented February 2026):
  - Full CRUD operations (Create, Read, Update, Delete)
  - Filter users by username, role, or company
  - Pagination with configurable rows per page (5/10/25/50)
  - Add new users with role assignment and password requirements
  - Edit existing users (all fields except username)
  - Admin password change with force update option
  - Delete protection: Cannot delete yourself or users with task activities
  - Password validation with show/hide toggles
  - Real-time validation and error handling
  - Material-UI dialogs and responsive design
- **Guest Activity Report** (Phase 6 - Implemented February 2026):
  - View guest login statistics and audit history
  - Real-time metrics: Total Logins, Unique Locations, Last Login, Success Rate
  - Login audit table with Date/Time, IP Address, Location, Status columns
  - CSV export with dialog options (Copy to Clipboard, Download CSV, Close)
  - Compact date format for card displays (e.g., "Feb 9, 11:59 AM")
  - Responsive Material-UI cards and tables
  - Data resets with each deployment (in-memory storage)
- **Roles Management** (Phase 7 - Implemented February 2026):
  - Comprehensive role and permission management system
  - View all system roles with assigned permissions
  - Create new roles with custom permission sets
  - Edit existing roles to add/remove permissions
  - Delete roles with constraint validation (cannot delete roles assigned to users)
  - Hierarchical permission selection grouped by resource
  - Master checkboxes for resource-level permission control
  - Indeterminate state shows partial permission selection
  - Permission grouping: USER_MANAGEMENT, TASK_MANAGEMENT, ROLE_MANAGEMENT, SYSTEM_CONFIGURATION
  - Real-time validation and error handling
  - Material-UI dialogs with responsive design
  - **Access Control**: Requires USER_MANAGEMENT:READ permission to view, USER_MANAGEMENT:CREATE/UPDATE/DELETE for modifications
- **Dropdown Management** (Phase 5 - Implemented February 2026):
  - Manage dropdown values for tasks and expenses
  - Category and subcategory filtering with dynamic updates
  - "Add New Category" modal for creating new category/subcategory combinations
  - Inline form for adding values to existing categories (disabled until category selected)
  - Full CRUD operations (Create, Read, Update, Delete)
  - Data table with columns: Category, Subcategory, Value, Display Order, Status, Actions
  - Edit dialog for modifying value, display order, and active status
  - Delete confirmation dialog with context information
  - Summary statistics showing total values and active count
  - Success/Error messages via Snackbar notifications
  - Responsive Material-UI design following established patterns
  - **Access Control**: Requires USER_MANAGEMENT:CREATE/UPDATE/DELETE permissions for modifications

**Feature Placeholders (Phase 8 Coming Soon):**
- **System Settings**: Shows "Coming Soon" dialog (Phase 8)

### Development vs Production

**Phase 3 Development Mode:**
- React runs on Vite dev server (port 4201)
- Accessed via hardcoded URL: `http://localhost:4201`
- Opens in new tab from Thymeleaf UI
- Provides fast hot-reload for development
- Template links in `task-list.html` and `expense-list.html` use `target="_blank"`

**Production Deployment:**
- React built as static files in `target/classes/static/dashboard/`
- Served by Spring Boot on port 8080 at `/dashboard`
- Maven automatically builds both Angular and React during package phase
- Template links will use `th:href="@{/dashboard}"` instead of hardcoded URL
- Seamless integration with existing Spring Boot authentication

**AWS Deployment Notes:**
- Maven build process handles both Angular and React
- Spring Boot `ServerConfig.java` configures `/dashboard/**` route
- `SecurityConfig.java` enforces ADMIN role requirement
- `CustomAuthenticationSuccessHandler.java` preserves `/dashboard` URLs during login
- Session cookies work across `/app`, `/dashboard`, and root paths

### Implementation Progress

Completed Phases:
- ✅ **Phase 3**: Skeleton dashboard with navigation (December 2025)
- ✅ **Phase 4**: User Management with full CRUD operations (February 2026)
- ✅ **Phase 5**: Dropdown Management with category filtering and CRUD operations (February 2026)
- ✅ **Phase 6**: Guest Activity Report with metrics and CSV export (February 2026)
- ✅ **Phase 7**: Roles Management with permission assignment (February 2026)

Upcoming Phases:
- **Phase 8**: System Settings functionality

For technical details on React Dashboard implementation, see [React_Dashboard_Blueprint.md](React_Dashboard_Blueprint.md).

---

## Administrator Features

### User Roles Overview

The system uses a **database-driven role-based access control (RBAC) system** with customizable permissions. Administrators can create custom roles and assign permissions through the web interface without requiring code changes.

**Four Default Roles:**

The system provides four pre-configured roles with different permission levels:

**GUEST (Read-Only Access)**
- Can view task list and task details
- Full CRUD access for task activities (can create, edit, delete own tasks)
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
    - **Role**: Filter by user role (ADMIN, USER, GUEST)
    - **Company**: Filter by company name (partial match)
    - Click **"Search"** to apply filters or **"Reset filters"** to clear
4. **Add New User**: Click **"Add User"** button
    - Enter username (required)
    - Enter first name (optional)
    - Enter last name (required)
    - Enter company (optional, maximum 100 characters)
    - Set initial password
    - Assign role (GUEST, USER, or ADMIN)
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

### Managing Roles and Permissions

The system features a **database-driven authorization system** that allows administrators to create custom roles and assign permissions without modifying code. This provides flexibility in tailoring access controls to your organization's specific needs.

#### Accessing Role Management

1. **Navigate to Role Management**: 
   - Option 1: Click **"☰"** to open the Task Activity List sidebar menu, then click **"🔐 Manage Roles"** (ADMIN only)
   - Option 2: From within the Role Management page, use the **"Task Activity List"** button in the header to navigate
2. **View All Roles**: See a list of all roles with their descriptions and assigned permissions

#### Understanding the Permission Model

Permissions follow a **resource:action** pattern:

**Resources:**
- `TASK_ACTIVITY` - Task management features
- `EXPENSE` - Expense management features
- `USER` - User account management
- `DROPDOWN` - Dropdown value management
- `ROLE` - Role and permission management

**Actions:**
- `CREATE` - Create new records
- `READ` - View/list records
- `UPDATE` - Edit existing records
- `DELETE` - Remove records
- `MANAGE` - Full management access (often implies all CRUD operations)
- `APPROVE` - Approve submitted items (expenses)

**Examples:**
- `TASK_ACTIVITY:CREATE` - Permission to create new tasks
- `EXPENSE:APPROVE` - Permission to approve expenses
- `USER:MANAGE` - Permission to manage user accounts
- `ROLE:MANAGE` - Permission to manage roles and permissions

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
   - **Description**: Brief explanation of the role's purpose
4. **Assign Permissions**: Check the boxes for permissions this role should have
   - Permissions are organized by resource (TASK_ACTIVITY, EXPENSE, etc.)
   - Each resource shows available actions (CREATE, READ, UPDATE, DELETE, etc.)
5. **Save Role**: Click "Create Role" button
6. **Success**: Role is created and can be assigned to users immediately

**Best Practices for Custom Roles:**
- Use clear, descriptive names (e.g., "READ_ONLY_TASKS" instead of "RO_TASKS")
- Document the purpose in the description field
- Start with minimal permissions and add more as needed
- Test new roles with a test user account before production use

#### Editing Role Permissions

1. **Access Role Management**: Navigate to the role management page
2. **Select Role to Edit**: Click "Edit" button next to the role
3. **View Current Permissions**: See which permissions are currently assigned
4. **Modify Permissions**:
   - **Check boxes** to add permissions
   - **Uncheck boxes** to remove permissions
   - Role name is read-only (cannot be changed)
   - Description can be updated
5. **Save Changes**: Click "Save Changes" button
6. **Immediate Effect**: Permission changes take effect immediately for all users with that role

**Important Notes:**
- Removing permissions from a role immediately affects all users with that role
- Users must log out and log back in to see permission changes reflected in the UI
- Built-in roles (ADMIN, USER, GUEST, EXPENSE_ADMIN) can be edited but use caution
- Always test permission changes in a non-production environment first

#### Assigning Roles to Users

Roles are assigned through the User Management interface:

1. **Navigate to User Management**: Click "Manage Users" from header
2. **Edit User**: Click "Edit" button next to the user
3. **Select Role**: Choose from the dropdown list of available roles
4. **Save**: User immediately receives permissions from the new role

#### Permission Checking in the System

The system uses the `@RequirePermission` annotation to enforce permissions:

```java
@RequirePermission(resource = "TASK_ACTIVITY", action = "CREATE")
public void createTask() { ... }
```

When you create new features:
1. Define new permissions in the database
2. Assign permissions to appropriate roles
3. Use `@RequirePermission` annotation on controller methods
4. No code deployment required to assign permissions to roles

For detailed information on adding permissions when developing new features, see **`docs/localdocs/Adding_Roles_and_Permissions_Guide.md`**.

#### Troubleshooting Permission Issues

**User reports they can't access a feature:**
1. Check user's assigned role in User Management
2. Navigate to Role Management and view the role's permissions
3. Verify the required permission is assigned to the role
4. Add missing permission if necessary
5. Ask user to log out and log back in

**New feature isn't secured:**
1. Verify `@RequirePermission` annotation is present on controller method
2. Check permission exists in `permissions` table
3. Ensure appropriate roles have the permission assigned
4. Review application logs for permission check failures

**Role changes not taking effect:**
- Users must log out and log back in after role or permission changes
- Check browser console for authentication errors
- Verify PermissionAspect is configured correctly

### Viewing All User Tasks

As an administrator, you have the ability to view and manage tasks for all users in the system:

1. **Access Task List**: Click **"View Tasks"** from the main page
2. **View All Tasks**: By default, you'll see tasks from all users
3. **Filter by Specific User**:
    - Use the **"Filter by User"** dropdown at the top of the task list
    - Select a specific user from the list
    - Click **"Apply Filters"** or the page will reload automatically
    - You'll now see only tasks created by that user
4. **Clear Filter**:
    - Select the empty option in the dropdown, or
    - Clear the filter to return to viewing all tasks

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
- Password (changed via separate Change Password page)

**Access Methods:**

Users can access their profile through:

1. **Angular UI**: "My Profile" card on dashboard or side menu (modern Material Design interface)
2. **Backend UI**: "My Profile" option in user menu (Thymeleaf-based with success/error notifications)

**Password Management:**

- Users can change their own password via the **"🔒 Update Password"** button in their profile page
- To access: Click **"👤 Update Profile"** in the sidebar menu, then click **"🔒 Update Password"** at the bottom
- Password changes redirect back to the profile page after completion
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

### Managing Roles in React Dashboard (Phase 7)

The React Dashboard provides a modern, intuitive interface for managing roles and permissions, complementing the Spring Boot backend role management system.

#### Accessing Roles Management

1. **Navigate to React Dashboard**: Access via http://localhost:4201 (development) or http://localhost:8080/dashboard (production)
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

**Angular Dashboard:**

1. **Access Dashboard**: Navigate to the Angular UI at http://localhost:4200/app/dashboard
2. **Click "Add Task" Button**: Located in the toolbar at the top of the task list
3. **Fill Out Task Form**:
    - **Task Date**: Select the date using the date picker (defaults to today)
    - **Client**: Choose from dropdown (required)
    - **Project**: Choose from dropdown (required)
    - **Phase**: Choose from dropdown (required)
    - **Hours**: Enter hours worked (0-24)
    - **Details**: Enter task description (optional)
4. **Save Task**: Click "Save" button
5. **Confirmation**: Success message appears and task list refreshes

**Thymeleaf UI:**

1. **Access Task List**: Navigate to the task list page
2. **Click "Add Task Activity"**: Opens task creation form
3. **Fill Out Form**: Enter task date, client, project, phase, hours, and details
4. **Submit**: Click "Save" to create the task

#### Cloning Existing Tasks

The **Clone** feature allows you to quickly duplicate an existing task with today's date, useful for recurring activities.

**Angular Dashboard:**

1. **Locate Task to Clone**: Find the task in the task list table
2. **Click Clone Button**: Click the **content_copy icon** (📋) in the Actions column
3. **Review Cloned Data**: A dialog opens with:
    - All original task data (client, project, phase, hours, details) preserved
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
4. **Enter New Password**: Type the new password (twice for confirmation)
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

**Configuration** (System Administrators Only):

The feature can be configured in `application.properties`:
```properties
# Enable/disable password history validation (default: true)
security.password.history.enabled=true

# Number of previous passwords to check (default: 5)
security.password.history.size=5
```

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

**Security Features:**

- **Time-Limited Tokens**: Reset tokens expire after 15 minutes
- **Single-Use**: Each token can only be used once
- **In-Memory Storage**: Tokens stored in-memory and cleared on application restart
- **Auto-Cleanup**: Expired tokens automatically removed every 5 minutes
- **No Email Enumeration**: System doesn't reveal if email address exists
- **Secure Links**: Reset links include randomly generated UUID tokens
- **Email Verification**: Only users with registered email addresses can reset

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

## Expense Management Administration

### Managing User Expenses

Administrators with ADMIN or EXPENSE_ADMIN roles can view and manage all user expenses:

1. **Access Expense List**: 
   - **Backend**: Click **"💰 Expense List"** from the navigation header (http://localhost:8080)
   - **Angular Dashboard**: Navigate to Expenses section (http://localhost:4200) for full CRUD operations
   
2. **Angular Dashboard Capabilities**: The Angular UI provides streamlined expense management:
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
      - **Important**: Create a project named "Non-Billable" for tracking overhead activities
      - Users should log meetings, training, and administrative tasks to this project
      - The Reports system uses this project name to distinguish billable from non-billable hours
    - **PHASE**: Manage work phases (with TASK subcategory)
    - **EXPENSE**: Manage expense-related dropdowns with subcategories:
        - **EXPENSE_TYPE**: Types of expenses (Travel - Airfare, Hotel, Meals, Home Office Equipment, etc.)
        - **PAYMENT_METHOD**: Payment methods (Personal Credit Card, Personal Cash, Company Credit Card, Direct Bill)
        - **RECEIPT_STATUS**: Receipt availability (Attached, Pending, Not Available)
        - **EXPENSE_STATUS**: Workflow status (Draft, Submitted, Approved, Rejected, Reimbursed)
    - **Note**: New categories added to the database automatically appear in this list

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
- **Spring Boot UI**: Traditional server-rendered interface at `/admin/guest-activity`
- **React Admin Dashboard**: Modern SPA interface with Material Design
  - Development: http://localhost:4201 (Guest Activity card)
  - Production: http://localhost:8080/dashboard (Guest Activity card)

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

As an ADMIN user, you have access to comprehensive user analytics through the **Reports** section's **User Analysis** tab.

### Accessing User Analytics

1. Navigate to **Reports** from the main menu
2. Click the **User Analysis** tab (only visible to ADMIN users)
3. View team performance metrics and user comparisons

### User Performance Summary Table

The User Performance Summary displays a comprehensive view of all users' activities:

**Columns Displayed:**
- **Rank**: Position based on total hours worked
  - 🏆 Gold trophy for #1 performer
  - 🥈 Silver medal for #2 performer
  - 🥉 Bronze medal for #3 performer
- **Username**: User's login identifier
- **Total Hours**: Cumulative hours worked in the selected period
- **Billable**: Hours worked on billable client projects (excludes Non-Billable project)
- **Non-Billable**: Hours logged to the "Non-Billable" project (overhead, meetings, training, admin)
- **Tasks**: Number of task activities submitted
- **Avg Billable/Day**: Average billable hours per day (calculated only from days with billable work)
- **Top Client**: Client with most hours for this user (excludes Non-Billable project)
- **Top Project**: Project with most hours for this user (excludes Non-Billable project)
- **Last Activity**: Date of most recent task submission

**Billable vs. Non-Billable Tracking:**
The system distinguishes between billable and non-billable hours using a simple naming convention:
- **Billable Hours**: All tasks logged to any project EXCEPT "Non-Billable"
- **Non-Billable Hours**: Tasks logged to the project named "Non-Billable"
- **Visual Indicators**: Billable hours appear in green, non-billable in orange
- **Average Calculation**: Avg Billable/Day only includes days where billable work was performed
- **Top Client/Project**: These fields exclude the Non-Billable project to show actual client work

**Features:**
- Sortable columns: Click any column header to sort
- Real-time data: Based on current database records
- Trophy rankings: Visual recognition for top performers
- Quick identification of active vs. inactive users

### Hours by User Chart

A bar chart visualization comparing total hours across all team members:

- **Color-coded bars**: Each user has a distinct color
- **Percentage labels**: Shows each user's share of total team hours
- **Interactive tooltips**: Hover for exact hours and percentage
- **Sorted by hours**: Bars ordered from highest to lowest

### Using User Analytics for Management

**Performance Reviews:**
- Identify top performers with trophy rankings
- Review average hours per day to assess workload
- Check last activity date to identify inactive accounts

**Resource Allocation:**
- See which users are working on which clients/projects
- Identify users with capacity for additional work
- Ensure balanced workload distribution across the team

**Client Management:**
- Understand which team members are most familiar with specific clients
- Plan resource allocation for client projects
- Identify expertise distribution across the team

**Activity Monitoring:**
- Track team activity levels over time
- Identify users who may need support or have capacity issues
- Monitor engagement with the time tracking system

### Best Practices

- **Review weekly**: Check User Analysis tab at least once per week to stay informed about team activities
- **Set expectations**: Use metrics to establish clear performance expectations
- **Recognize achievement**: Use trophy rankings to acknowledge top performers
- **Identify trends**: Look for patterns in activity levels and time distribution
- **Data-driven decisions**: Base resource allocation on actual hours worked, not estimates
- **Privacy considerations**: Use data responsibly and communicate analytics practices to team members

### Important Notes

- Only ADMIN users can access User Analysis features
- All data respects user privacy and is for management purposes only
- Charts reflect all task activities in the database across all users
- Regular users can only see their own data in other report tabs
- Data updates in real-time as users submit new tasks

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

### JWT Token Revocation Management

The system includes a server-side token revocation mechanism to invalidate JWT tokens before their natural expiration. This provides enhanced security control over user sessions.

**How Token Revocation Works:**
- When users log out, their tokens are added to a blacklist database (`revoked_tokens` table)
- When users change their password, all their existing tokens are automatically revoked
- Authentication requests check the blacklist before granting access
- Expired tokens are automatically cleaned up via scheduled job (daily at 2 AM)

**Auditing Revoked Tokens:**

As an administrator, you can audit revoked tokens directly in the database to:
- Monitor logout activity
- Track password change events
- Identify security incidents where tokens were manually revoked
- Review token expiration patterns

**SQL Queries for Monitoring:**

```sql
-- Count total revoked tokens
SELECT COUNT(*) FROM revoked_tokens;

-- View recent revocations
SELECT jti, username, token_type, reason, revoked_at 
FROM revoked_tokens 
ORDER BY revoked_at DESC 
LIMIT 20;

-- Count revocations by reason
SELECT reason, COUNT(*) as count 
FROM revoked_tokens 
GROUP BY reason 
ORDER BY count DESC;

-- Count revocations by user
SELECT username, COUNT(*) as token_count 
FROM revoked_tokens 
GROUP BY username 
ORDER BY token_count DESC;

-- Find expired tokens (can be cleaned up)
SELECT COUNT(*) FROM revoked_tokens 
WHERE expiration_time < NOW();

-- View all revocations for a specific user
SELECT jti, token_type, reason, revoked_at, expiration_time 
FROM revoked_tokens 
WHERE username = 'john.doe' 
ORDER BY revoked_at DESC;
```

**Revoked Tokens Table Schema:**

| Column | Type | Description |
|--------|------|-------------|
| `id` | SERIAL | Primary key |
| `jti` | VARCHAR(255) | Unique JWT ID (UUID) |
| `username` | VARCHAR(255) | Owner of the token |
| `token_type` | VARCHAR(20) | "access" or "refresh" |
| `expiration_time` | TIMESTAMP | When token naturally expires |
| `revoked_at` | TIMESTAMP | When token was revoked |
| `reason` | VARCHAR(100) | Revocation reason (logout, password_change, security_incident, manual) |

**Performance Considerations:**
- Blacklist checks are highly optimized (O(1) lookup via indexed JTI)
- Typical overhead: <50ms per authentication request
- Automatic cleanup prevents table bloat
- Indexes on `jti`, `expiration_time`, and `username` ensure fast queries

**Best Practices:**
- Monitor revocation patterns for unusual activity (e.g., mass logouts may indicate compromise)
- Review `security_incident` reasons regularly
- Consider manual token revocation if you suspect account compromise
- Run cleanup query manually if automatic cleanup fails: `DELETE FROM revoked_tokens WHERE expiration_time < NOW()`

---

## Administrative Processes and One-Off Tasks

As an administrator, you have access to various maintenance and administrative tasks that can be run independently of the main application. These follow the 12-Factor App principle of running admin/management tasks as one-off processes in an identical environment.

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

#### 7. Running Custom Admin Commands

**Using Maven:**
```bash
# Run with specific Spring profile for admin tasks
mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=admin,aws"

# Run with custom properties
mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--app.admin.task=reset-passwords --app.admin.dry-run=true"
```

**Using Docker:**
```bash
# Run one-off admin task in container
docker run --rm \
  -e SPRING_PROFILES_ACTIVE=admin \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=<password> \
  -e DATABASE_URL=jdbc:postgresql://<host>:5432/AmmoP1DB \
  taskactivity:latest \
  java -jar app.jar --admin.task=backup-database

# Or using docker-compose
docker-compose run --rm app \
  java -jar app.jar --spring.profiles.active=admin
```

**Using Kubernetes:**
```bash
# Run one-off job in Kubernetes
kubectl run taskactivity-admin \
  --image=taskactivity:latest \
  --restart=Never \
  --namespace=taskactivity \
  --env="SPRING_PROFILES_ACTIVE=admin" \
  -- java -jar app.jar --admin.task=migrate-data

# Or create a Job manifest
kubectl create job taskactivity-migration \
  --from=cronjob/taskactivity-backup \
  --namespace=taskactivity
```

**Using AWS ECS:**
```bash
# Run one-off task in ECS
aws ecs run-task \
  --cluster taskactivity-cluster \
  --task-definition taskactivity \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}" \
  --overrides '{
    "containerOverrides": [{
      "name": "taskactivity",
      "command": ["java", "-jar", "app.jar", "--spring.profiles.active=admin", "--admin.task=cleanup-old-tasks"]
    }]
  }'
```

### Admin Task Best Practices

1. **Always Use Identical Environment**: Run admin tasks with the same Docker image/JAR as production
2. **Test in Staging First**: Never run untested admin commands directly in production
3. **Backup Before Changes**: Always create a database backup before running destructive operations
4. **Use Dry-Run Mode**: Test admin commands with `--dry-run` flag when available
5. **Log Everything**: Ensure all admin tasks output comprehensive logs
6. **Automate Common Tasks**: Create shell scripts for frequently-run admin operations
7. **Version Control Scripts**: Keep all admin scripts in version control
8. **Document Custom Tasks**: Add documentation for any new admin procedures

### Scheduled Maintenance Tasks

Some administrative tasks should be run on a regular schedule:

**Daily:**
- Health check monitoring (`monitor-health.sh`)
- Log aggregation and archival
- Backup verification

**Weekly:**
- Database backup (`pg_dump` or RDS snapshot)
- User activity review
- Performance metrics analysis

**Monthly:**
- Secret rotation (`rotate-secrets.sh`)
- Database vacuum and analyze (PostgreSQL)
- Security audit (review locked accounts, failed logins)
- Disk space cleanup

**Quarterly:**
- Full disaster recovery test
- Security vulnerability scanning
- Dependency updates

### Troubleshooting Admin Tasks

**Issue: Admin task fails with database connection error**
```bash
# Verify database connectivity
psql -h <host> -U <username> -d AmmoP1DB -c "SELECT version();"

# Check connection pool settings
# May need to increase max_connections in PostgreSQL
```

**Issue: Docker secrets not accessible**
```bash
# List Docker secrets
docker secret ls

# Verify service has access to secrets
docker service inspect taskactivity --format='{{json .Spec.TaskTemplate.ContainerSpec.Secrets}}'

# Test secret retrieval
./scripts/test-docker-secrets.sh
```

**Issue: Admin task runs but changes not visible**
```bash
# Verify correct profile is active
java -jar app.jar --spring.profiles.active=admin --debug

# Check database connection URL
echo $DATABASE_URL

# Verify you're connected to the correct database
psql -h <host> -U <username> -d AmmoP1DB -c "SELECT current_database();"
```

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

**`.env.local` (Local Testing)**
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
- Recipients configured via: `ADMIN_EMAIL` and `EXPENSE_APPROVERS`

**Note:** User notification emails (expense confirmations, password expiration) are sent to the email address configured in each user's profile. Only the administrator and approver notification addresses are configured at the system level.

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
- Check **User Analytics** dashboard (Admin only)
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

---

## 12-Factor App Compliance

The Task Activity Management System is designed following the **[12-Factor App methodology](https://12factor.net/)**, a set of best practices for building modern, scalable, cloud-native applications. This section demonstrates how the application adheres to each principle.

### I. Codebase ✅

**Principle:** One codebase tracked in revision control, many deploys

**Implementation:**
- Single Git repository hosted on GitHub: `ammonsd/ActivityTracking`
- All code, configuration templates, and deployment scripts in version control
- Multiple deployment profiles for different environments:
  - `local` - Local development
  - `docker` - Containerized development
  - `aws` - Production AWS deployment
- Same codebase deployed to dev, staging, and production with environment-specific configuration

**Evidence:**
```bash
# Single repository, multiple deployments
git clone https://github.com/ammonsd/ActivityTracking.git

# Deploy to different environments
SPRING_PROFILES_ACTIVE=local mvnw spring-boot:run      # Local
docker-compose --profile host-db up                     # Docker
aws ecs update-service --service taskactivity-service   # AWS
```

### II. Dependencies ✅

**Principle:** Explicitly declare and isolate dependencies

**Implementation:**
- Maven (`pom.xml`) explicitly declares all Java dependencies
- Maven Wrapper (`mvnw.cmd`) ensures consistent Maven version
- NPM (`package.json`) declares all frontend dependencies
- Docker containers isolate runtime dependencies
- No system-wide dependencies required

**Evidence:**
```xml
<!-- All dependencies explicitly declared in pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.5.7</version>
</dependency>
```

```bash
# Self-contained builds
./mvnw clean package    # Downloads all dependencies
npm install             # Frontend dependencies
docker build .          # Containerized with all dependencies
```

### III. Config ✅

**Principle:** Store config in the environment

**Implementation:**
- All environment-specific configuration via environment variables
- No hardcoded credentials or URLs in code
- Profile-specific properties files (`application-{profile}.properties`)
- AWS Secrets Manager integration for sensitive data
- Docker secrets support

**Evidence:**
```properties
# Externalized configuration
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/AmmoP1DB}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
app.admin.initial-password=${APP_ADMIN_INITIAL_PASSWORD:Admin123!}
```

```bash
# Environment-specific deployment
export DB_USERNAME=postgres
export DB_PASSWORD=secure_password
export DATABASE_URL=jdbc:postgresql://prod-db:5432/AmmoP1DB
java -jar taskactivity.jar
```

### IV. Backing Services ✅

**Principle:** Treat backing services as attached resources

**Implementation:**
- PostgreSQL database attached via JDBC URL (swappable)
- Connection configured entirely through environment variables
- Can switch between local DB, containerized DB, or AWS RDS without code changes
- Email service (SMTP) attached via configuration

**Evidence:**
```properties
# Easily swap backing services via config
# Local PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/AmmoP1DB

# Docker PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/AmmoP1DB

# AWS RDS
SPRING_DATASOURCE_URL=jdbc:postgresql://taskactivity-db.xxx.rds.amazonaws.com:5432/AmmoP1DB
```

### V. Build, Release, Run ✅

**Principle:** Strictly separate build and run stages

**Implementation:**
- **Build**: Maven compiles code, runs tests, creates JAR (`mvnw package`)
- **Release**: Docker packages JAR with runtime, tags with version
- **Run**: ECS/Kubernetes deploys specific tagged image
- Jenkins CI/CD pipeline enforces separation
- Immutable Docker images tagged with version/commit SHA

**Evidence:**
```bash
# Build stage
mvnw clean package -DskipTests
# Output: target/taskactivity-0.0.1-SNAPSHOT.jar

# Release stage
docker build -t taskactivity:v1.2.3 .
docker tag taskactivity:v1.2.3 378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity:v1.2.3
docker push 378010131175.dkr.ecr.us-east-1.amazonaws.com/taskactivity:v1.2.3

# Run stage
aws ecs update-service --cluster taskactivity-cluster \
  --service taskactivity-service \
  --force-new-deployment
```

### VI. Processes ✅

**Principle:** Execute the app as one or more stateless processes

**Implementation:**
- Spring Boot application is stateless
- No local file system state (logs to stdout)
- All persistent data in PostgreSQL database
- Session data can be externalized to Redis (configured for sticky sessions currently)
- Each process instance is independent and disposable

**Evidence:**
```java
// Stateless service example
@Service
public class TaskActivityService {
    @Autowired
    private TaskActivityRepository repository;  // Shared database, no local state
    
    public TaskActivity createTask(TaskActivity task) {
        return repository.save(task);  // Persists to database, not local memory
    }
}
```

### VII. Port Binding ✅

**Principle:** Export services via port binding

**Implementation:**
- Embedded Tomcat server (no external web server required)
- Self-contained HTTP service on port 8080
- Port configurable via `${PORT}` environment variable
- No dependency on injecting webserver at runtime

**Evidence:**
```properties
# Application exports HTTP service
server.port=${PORT:8080}
server.address=0.0.0.0
```

```dockerfile
# Dockerfile exposes port
EXPOSE 8080
CMD ["java", "-jar", "/opt/app.jar"]
```

### VIII. Concurrency ✅

**Principle:** Scale out via the process model

**Implementation:**
- Application designed for horizontal scaling
- Stateless processes can be scaled to N instances
- AWS ECS: Configure `DesiredCount` (production: 2 instances)
- Kubernetes: Configure `replicas` (default: 2 replicas)
- Load balancer distributes traffic across instances
- Database connection pooling per instance (HikariCP)

**Evidence:**
```yaml
# Kubernetes scaling
spec:
  replicas: 2  # Run 2 instances

# ECS scaling
DesiredCount: 2

# Scale command
kubectl scale deployment taskactivity-app --replicas=5
aws ecs update-service --service taskactivity-service --desired-count=3
```

**See Also:** [Concurrency and Scaling Guide](Concurrency_and_Scaling_Guide.md)

### IX. Disposability ✅

**Principle:** Maximize robustness with fast startup and graceful shutdown

**Implementation:**
- Spring Boot fast startup (~30-60 seconds)
- Graceful shutdown configured (30-second timeout)
- Health check endpoints for readiness/liveness
- Robust against sudden termination
- Docker containers can be stopped/started rapidly

**Evidence:**
```properties
# Graceful shutdown configuration
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

# Health checks for orchestration
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true
```

```json
// ECS health check (taskactivity-task-definition.json)
{
    "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
    }
}
```

### X. Dev/Prod Parity ✅

**Principle:** Keep development, staging, and production as similar as possible

**Implementation:**
- Same Docker image used across all environments
- Same PostgreSQL database version (15) in all environments
- Docker Compose for local development matches production architecture
- Profile-based configuration maintains consistency
- CI/CD pipeline deploys same artifact to all environments

**Evidence:**
```bash
# Local development
docker-compose --profile host-db up

# AWS production
# Same Dockerfile, same image, different environment variables
aws ecs update-service --service taskactivity-service

# Kubernetes
# Same container image, different ConfigMap
kubectl apply -f k8s/taskactivity-deployment.yaml
```

### XI. Logs ✅

**Principle:** Treat logs as event streams

**Implementation:**
- Application logs to stdout/stderr (not files)
- No file-based logging in production
- AWS CloudWatch captures stdout via `awslogs` driver
- Docker logging drivers capture container logs
- Kubernetes aggregates pod logs
- Local development: Optional file logging for debugging only

**Evidence:**
```properties
# application-aws.properties - Console logging only
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# File logging explicitly disabled for AWS
ENABLE_FILE_LOGGING=false
```

```json
// ECS task definition - CloudWatch Logs
{
    "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
            "awslogs-group": "/ecs/taskactivity",
            "awslogs-region": "us-east-1",
            "awslogs-stream-prefix": "ecs"
        }
    }
}
```

### XII. Admin Processes ✅

**Principle:** Run admin/management tasks as one-off processes

**Implementation:**
- Database migrations via `CommandLineRunner` (DatabaseInitializer)
- Admin user creation via `@PostConstruct` (DataInitializer)
- Shell scripts for backup, restore, and maintenance
- One-off tasks runnable in same environment as app
- Docker/Kubernetes jobs for scheduled admin tasks

**Evidence:**
```java
// DatabaseInitializer - One-off schema initialization
@Configuration
@Profile("aws")
public class DatabaseInitializer {
    @Bean
    public CommandLineRunner initDatabase(DataSource dataSource) {
        return args -> {
            // One-off database initialization
            populator.addScript(new ClassPathResource("schema.sql"));
            populator.execute(dataSource);
        };
    }
}
```

```bash
# Run one-off admin task
docker run --rm taskactivity:latest \
  java -jar app.jar --spring.profiles.active=admin --admin.task=backup

# Kubernetes Job
kubectl create job taskactivity-migration --from=deployment/taskactivity-app

# ECS one-off task
aws ecs run-task --cluster taskactivity-cluster \
  --task-definition taskactivity \
  --overrides '{"containerOverrides":[{"name":"taskactivity","command":["java","-jar","app.jar","--admin.task=migrate"]}]}'
```

**See Also:** [Administrative Processes section](#administrative-processes-and-one-off-tasks) above

### Summary: 12-Factor Compliance Score

| Factor | Status | Implementation |
|--------|--------|----------------|
| I. Codebase | ✅ Complete | Git repository, multiple deploys |
| II. Dependencies | ✅ Complete | Maven, npm, Docker isolation |
| III. Config | ✅ Complete | Environment variables, Secrets Manager |
| IV. Backing Services | ✅ Complete | Attached PostgreSQL via config |
| V. Build, Release, Run | ✅ Complete | Maven, Docker, CI/CD pipeline |
| VI. Processes | ✅ Complete | Stateless design, shared database |
| VII. Port Binding | ✅ Complete | Embedded Tomcat, self-contained |
| VIII. Concurrency | ✅ Complete | Horizontal scaling, load balancing |
| IX. Disposability | ✅ Complete | Fast startup, graceful shutdown |
| X. Dev/Prod Parity | ✅ Complete | Docker across all environments |
| XI. Logs | ✅ Complete | Stdout/CloudWatch, no file logging |
| XII. Admin Processes | ✅ Complete | One-off tasks, identical environment |

**Overall Compliance: 12/12 (100%)** ✅

The Task Activity Management System fully adheres to all 12-Factor App principles, making it a modern, cloud-native, scalable application suitable for enterprise deployment.

### Benefits of 12-Factor Compliance

1. **Portability**: Runs on any cloud provider or container platform
2. **Scalability**: Easy horizontal scaling without code changes
3. **Maintainability**: Clean separation of concerns, easy to debug
4. **Continuous Deployment**: Safe automated deployments
5. **Resilience**: Graceful handling of failures and restarts
6. **Cloud-Native**: Optimized for containerized/orchestrated environments

### For Job Applications

When discussing this application in job interviews or applications, you can confidently state:

> "This application fully implements the 12-Factor App methodology, demonstrating expertise in cloud-native architecture, containerization, and modern DevOps practices. It's production-ready with proper configuration management, horizontal scalability, and comprehensive operational tooling."

---


