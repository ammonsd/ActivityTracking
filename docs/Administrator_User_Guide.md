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

The system supports four user roles with different permission levels:

**GUEST (Read-Only Access)**
- Can view task list and task details in read-only mode
- Cannot create, edit, or delete tasks
- No access to weekly timesheet, expenses, user management, or dropdown settings
- **Cannot change password** (password changes must be done by an administrator)
- **Password expiration warnings are suppressed** (GUEST users won't see expiration warnings)
- **Cannot log in if password has expired** (must contact administrator for password reset)
- Useful for stakeholders who need visibility without editing capabilities

**Important for GUEST Users:**
- When a GUEST user's password expires, they will be blocked from logging in with the message: "Password has expired. Contact system administrator."
- Administrators must reset GUEST passwords and update the expiration date when needed

**USER (Standard Access)**
- Can view, create, edit, and delete their own tasks and expenses
- Access to weekly timesheet and weekly expense sheet
- Can upload receipts for expenses
- Can submit expenses for approval
- Can change their own password
- Cannot view other users' tasks or expenses (except when submitted for approval)
- Standard role for team members doing time and expense tracking

**ADMIN (Full Access)**
- All USER permissions plus administrative capabilities
- Can view and manage all users' tasks and expenses
- Can create, edit, and delete user accounts
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
    - **Email Notifications**: When an account is locked due to excessive failed login attempts, an email notification is automatically sent to the administrator with details about the lockout (username, number of attempts, IP address, and timestamp)
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

- Users can change their own password via the "Update Password" button in My Profile
- Password changes redirect back to My Profile after completion
- Passwords must meet security requirements:
  - Minimum 10 characters
  - At least 1 uppercase letter
  - At least 1 numeric digit
  - At least 1 special character (+&%$#@!~)
  - Not contain more than 2 consecutive identical characters
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

---

## Expense Management Administration

### Managing User Expenses

Administrators with ADMIN or EXPENSE_ADMIN roles can view and manage all user expenses:

1. **Access Expense List**: Click **"💰 Expense List"** from the navigation header
2. **View All Expenses**: By default, administrators see expenses from all users
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

1. **Access Dropdown Management**: Click **"🔧 Manage Dropdowns"** from the navigation header

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

### Guest Activity Dashboard

Administrators have access to a dedicated dashboard for monitoring GUEST user login activity. This feature helps track who's accessing the system as a guest and provides security visibility.

**Accessing the Dashboard:**
1. **Navigate**: From any admin page, click **"📊 Guest Activity"** button next to "👥 Manage Users" in the header
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


