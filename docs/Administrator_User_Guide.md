# Task Activity Management System - Administrator User Guide

## Welcome

This guide is for administrators of the Task Activity Management System. As an administrator, you have access to additional features for managing users, viewing all tasks, and configuring system settings.

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

## Administrator Features

### User Roles Overview

The system supports three user roles with different permission levels:

**GUEST (Read-Only Access)**
- Can view task list and task details in read-only mode
- Cannot create, edit, or delete tasks
- No access to weekly timesheet, user management, or dropdown settings
- **Cannot change password** (password changes must be done by an administrator)
- **Password expiration warnings are suppressed** (GUEST users won't see expiration warnings)
- **Cannot log in if password has expired** (must contact administrator for password reset)
- Useful for stakeholders who need visibility without editing capabilities

**Important for GUEST Users:**
- When a GUEST user's password expires, they will be blocked from logging in with the message: "Password has expired. Contact system administrator."
- Administrators must reset GUEST passwords and update the expiration date when needed

**USER (Standard Access)**
- Can view, create, edit, and delete their own tasks
- Access to weekly timesheet and task cloning
- Can change their own password
- Cannot view other users' tasks or access admin features
- Standard role for team members doing time tracking

**ADMIN (Full Access)**
- All USER permissions plus administrative capabilities
- Can view and manage all users' tasks
- Can create, edit, and delete user accounts
- Can manage dropdown values (clients, projects, phases)
- Can change passwords for any user

### Managing Users

Administrators can create, edit, and delete user accounts:

1. **Access User Management**: Click **"👥 Manage Users"** from the header
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
6. **Delete Users**: Remove user accounts (with confirmation)
    - **Note**: The Delete button is disabled (grayed out) for:
        - Your own account (cannot delete yourself)
        - Users who have task entries in the system
    - Users with task entries cannot be deleted to maintain data integrity
    - To prevent a user from accessing the system, disable their account instead of deleting

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

### Managing Dropdowns

Dropdown management has been consolidated into a single, dynamic interface that supports multiple categories from one screen.

1. **Access Dropdown Management**: Click **"🔧 Manage Dropdowns"** from the navigation header

2. **Select Category**: Use the dropdown filter to choose which category to manage:
    - **All Categories**: View all dropdown values across all categories
    - **CLIENT**: Manage client list
    - **PROJECT**: Manage project names
    - **PHASE**: Manage work phases
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
    - Inactive values don't appear in dropdown lists when creating/editing tasks
    - Use this instead of deleting values that are still referenced by existing tasks

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
- **Tasks**: Number of task activities submitted
- **Avg Hours/Day**: Average daily work hours
- **Top Client**: Client with most hours for this user
- **Top Project**: Project with most hours for this user
- **Last Activity**: Date of most recent task submission

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

