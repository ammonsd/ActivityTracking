# Task Activity Management System - User Guide

## Welcome

Welcome to the Task Activity Management System! This comprehensive application helps you track both time spent on tasks and project-related expenses. Whether you're logging daily work activities, reviewing your weekly timesheet, submitting expenses for reimbursement, or managing expense approvals, this guide will help you make the most of the system.

**Key Features:**

- **Task Activity Tracking**: Log time spent on client projects and tasks with detailed phase tracking
- **Expense Management**: Record and submit business expenses with receipt attachments
- **Approval Workflows**: Submit expenses for review and track their approval status
- **Weekly Views**: View consolidated weekly timesheets and expense sheets
- **Reporting & Analytics**: Interactive charts and dashboards for analyzing your time and expenses
- **CSV Export**: Export task lists, timesheets, and expense reports for external use

## Task Activity List

Tasks are displayed in a table with the following columns:

- **Date**: When the task was performed
- **Client**: The client the work was done for
- **Project**: The specific project
- **Phase**: The work phase (Development, Testing, etc.)
- **Hours**: Time spent on the task
  - **Visual Indicator**: Non-billable hours are displayed in **red bold text** to help you quickly identify overhead or internal time that won't be billed to clients
- **Task ID**: Optional external reference identifier (e.g., ticket number such as `TA-001` or `MP1T-6394`)
- **Task Name**: Optional short label or title for the task
- **Details**: Description of work performed
- **Actions**: Edit, Delete, and Clone buttons

### Filtering Tasks

Use the filter controls at the top of the task list to narrow down your view:

1. **Client Filter**: Show only tasks for a specific client
2. **Project Filter**: Filter by project name
3. **Phase Filter**: Filter by work phase
4. **Task ID Filter**: Filter by ticket or reference number (partial match, case-insensitive)
5. **Start Date**: Show tasks from this date onward
6. **End Date**: Show tasks up to this date

**To Apply Filters:**

- Select your desired filter options
- Click **"Filter"** button
- To reset: Click **"Clear"** button

### Using the Sidebar Menu

The Task Activity List page includes a **floating sidebar menu** for quick access to features and export options.

**To Access the Menu:**

1. Click the **☰** (hamburger menu) button in the upper-right corner of the page
2. The sidebar menu will slide in from the right side
3. Click the **✕** button or click the menu button again to close

**Menu Items:**

- **Update Profile**: Access your profile to update your information and password
- **Export CSV**: Export filtered task list to CSV format

**Note:** Guest users can see the menu but all options (including Update Profile) will be disabled. Administrator users have additional management options in the sidebar — refer to the [Administrator User Guide](Administrator_User_Guide.md).

### Exporting Task List to CSV

You can export your filtered task list to CSV format for reporting or import into spreadsheets.

**To Export Tasks:**

1. **Apply Filters** (optional): Filter the task list to show only the tasks you want to export
2. **Click Export Button**:
   - Option 1: Click the **"Export CSV"** button in the task list toolbar (above the table)
   - Option 2: Click **"☰"** to open the sidebar menu, then click **"Export CSV"**
   - The menu will automatically close after selecting Export CSV
3. **Wait for Data**: The system will fetch ALL filtered tasks (not just the current page)
4. **Choose Action**:
   - **Copy to Clipboard**: Copies CSV data for pasting into Excel, email, etc.
   - **Download CSV**: Downloads a CSV file
   - **Close**: Close the export window

**CSV Export Features:**

- **Exports ALL filtered tasks** - not limited to the 20 visible on the current page
- If there are 270 filtered tasks, all 270 will be included in the CSV
- Filename includes a date stamp
- Example filename: `TaskActivity_20251101.csv`
- Columns exported: Date, Client, Project, Phase, Hours, Task ID, Task Name, Details

**CSV Format:**

```
Date,Client,Project,Phase,Hours,Task ID,Task Name,Details
10/28/2025,Acme Corp,Website Redesign,Development,8.00,TA-001,Implement login,Fixed login bug
10/29/2025,Acme Corp,Website Redesign,Testing,6.50,TA-002,QA regression,Performed regression testing
```

## Getting Started

### User Roles and Permissions

The system uses a **database-driven role-based access control system** with the following default roles:

**Default Roles:**

**GUEST (Read-Only Access)**

- ✅ View task list
- ✅ View task details
- ✅ Create, edit, and delete tasks (full CRUD access for task activities)
- ✅ Access weekly timesheet
- ❌ Cannot access expenses (no expense-related permissions)
- ❌ Cannot change password

**USER (Standard Access)**

- ✅ View, create, edit, and delete your own tasks
- ✅ Access weekly timesheet
- ✅ Clone tasks
- ✅ Manage your own expenses
- ✅ Change your own password
- ✅ Export your tasks to CSV

**EXPENSE_ADMIN (Expense Approver)**

- ✅ All USER permissions for tasks
- ✅ View and manage all users' expenses
- ✅ Approve or reject expense submissions from expense detail page
- ✅ Process reimbursements

**Email Requirement for Expense Access**

All users (except GUEST) must have a valid email address configured in their profile to access expense features. Without an email:

- ❌ Expense tracking buttons and links are hidden
- ❌ Cannot create, view, or manage expenses
- ❌ Cannot submit expenses for approval
- ✅ Can still access all task activity features

**Note:** The Weekly Timesheet view shows only your own tasks, regardless of role.

**Note:** Contact your administrator if you need different access permissions.

### Accessing the Application

Open your web browser and navigate to:

```
https://taskactivitytracker.com
```

## Logging In

### First Time Login

1. **Navigate to Login Page**: Open the application URL in your browser
2. **Enter Credentials**: Type your username and password
3. **Click "Login"**: Press the login button
4. **Change Password (if required)**: Some accounts require a password change on first login

### If You've Forgotten Your Password

If you've forgotten your password, you can reset it yourself using the **Reset Password** feature:

1. **Click "Reset Password"**: On the login page, click the **"Reset Password"** link below the login button
2. **Enter Your Email**: Type the email address associated with your account
3. **Click "Reset Password"**: Submit the form
4. **Check Your Email**: You'll see a success message. Check your email inbox for a password reset link
5. **Click the Reset Link**: The email contains a secure link that expires in **15 minutes**
6. **Enter New Password**: You'll be taken to the password change page
   - Enter your new password twice (see password requirements below)
   - You do NOT need to enter your current password
7. **Click "Change Password"**: Submit the form
8. **Confirmation Email**: You'll receive a confirmation email that your password was changed
9. **Return to Login**: Click "Return to Login" and sign in with your new password

**Important Notes:**

- Reset links expire after **15 minutes** for security
- Each reset link can only be used **once**
- If the link expires, simply request a new reset link
- You'll receive a confirmation email after successfully changing your password
- For security, the system won't reveal if an email address is registered

**If You Don't Receive the Email:**

1. **Check spam/junk folders**: Reset emails might be filtered
2. **Wait a few minutes**: Email delivery can take 1-2 minutes
3. **Verify email address**: Make sure you entered the correct email
4. **GUEST users**: If you have a GUEST role, password reset is disabled for security reasons. Contact your system administrator to have your password changed.
5. **Contact administrator**: If you still can't reset, contact your system administrator

### If You're Required to Change Your Password

Some new accounts are set up to require a password change on first login:

1. After entering your credentials, you'll be redirected to a password change screen
2. Enter your new password twice (see password requirements below)
3. Click "Update Password & Continue"
4. You'll be logged in and can start using the system

### Session Timeout

For security, your session will expire after **30 minutes** of inactivity.

**What Happens:**

- After 30 minutes without activity, your session expires
- You'll see a message: "⚠️ Your session has expired. Please log in again."
- Simply log in again to continue working

### Logging Out

To end your session securely:

1. **Click Logout**: Click the **"Logout"** link in the header
2. **Token Revocation**: Your session token is immediately invalidated and added to a server-side blacklist
3. **Redirect**: You'll be redirected to the login page
4. **Security**: The revoked token cannot be reused, even if intercepted

**What Token Revocation Means:**

- Your access token is permanently blacklisted on the server
- Even if someone has a copy of your old token, they cannot use it
- This provides stronger security than client-side-only logout
- All your tokens are automatically revoked when you change your password

**Best Practices:**

- Always log out when using shared computers
- Log out before closing your browser on public computers
- If you forget to log out, your session will automatically expire after 30 minutes

### Account Lockout Policy

To protect your account from unauthorized access, the system automatically locks accounts after **5 failed login attempts**.

**What Happens When Your Account Is Locked:**

- After 5 incorrect password entries, your account will be locked
- You will see a message: "Account locked due to too many failed login attempts. Please use the 'Reset Password' link below to reset your password and unlock your account."
- The system administrator also receives an automatic email notification about the lockout

**What to Do:**

1. **Use the Password Reset Feature**: Click the **"Reset Password"** link on the login page
2. Enter your email address and follow the password reset process (see "If You've Forgotten Your Password" section above)
3. After successfully resetting your password, your account will be automatically unlocked
4. Alternatively, **Contact your administrator** if you prefer manual assistance
5. The administrator can manually unlock your account from the User Management page

### Password Expiration Warnings

**Automatic Expiration**: All passwords expire automatically **90 days** after they are set or changed.

**7-Day Warning**: Starting 7 days before your password expires, you'll see a warning banner at the top of every page:

```
⚠️ Your password will expire in X day(s). Please change it soon.
```

**Note**: GUEST users will not see password expiration warnings since they cannot change their own passwords.

**What to Do:**

1. Click **"Update Profile"** in the sidebar menu
2. Click the **"Update Password"** button at the bottom of the profile page
3. Follow the password change process (see "Changing Your Password" section below)
4. Your new password will be valid for another 90 days

**Expired Password:**

**For roles other than GUEST:**

1. You'll be redirected to the password change screen immediately after login
2. You must change your password before accessing the system
3. The message will say: "Your password has expired. Please change it to continue."
4. After changing, you'll gain full access again

**For GUEST role:**

1. GUEST users cannot change their own passwords
2. If a GUEST password expires, they will be blocked from logging in
3. The login page will display: "Password has expired. Contact system administrator."
4. An administrator must reset the GUEST user's password

## Managing Your Tasks

### Adding a New Task

1. **Access Task Entry Form**
   
   - After logging in, you'll see the task entry form
   - Or click **"Add New Task"** from any page

2. **Fill in Task Details**
   
   - **Task Date**: Click the calendar icon to select the date (required)
   - **Client**: Choose the client from the dropdown (required)
   - **Project**: Select the project you worked on (required)
     - **Non-Billable Project**: Use "Non-Billable" for overhead activities like meetings, training, or administrative work
     - This helps distinguish billable client work from internal overhead

   > **Note:** The Client and Project dropdowns show only the values your administrator has assigned to your account under **Task** access, plus any values flagged as visible to all users. If you do not see an expected client or project, contact your administrator to update your **Task** tab access settings.

   - **Phase**: Pick the work phase (Development, Testing, etc.) (required)
   - **Hours**: Enter time spent (use decimals like 2.5 for 2½ hours) (required)
   - **Task ID**: Enter the ticket or reference number from your project management tool (optional, e.g., `TA-001`, `MP1T-6394`)
   - **Task Name**: Enter a short label or title for the task (optional)
   - **Details**: Describe what you worked on (optional - not needed for tasks like PTO)

3. **Save Your Task**
   
   - Click **"Save Task Activity"**
   - The form does not clear, allowing easier entries for the same date

### Editing a Task

1. **Find the Task**: Locate the task in your task list
2. **Click "Edit"**: Press the edit button (pencil icon) for that task
3. **Update Information**: Change any field as needed (except username, which cannot be changed)
4. **Save Changes**: Click **"Update Task Activity"**
5. **Confirmation**: You'll see a success message

### Deleting a Task

1. **Find the Task**: Locate the task in your task list
2. **Click "Delete"**: Press the delete button (trash icon)
3. **Confirm Deletion**: A confirmation dialog will appear
4. **Complete**: Click "Yes" to permanently delete

**⚠️ Warning:** Deleted tasks cannot be recovered!

### Cloning a Task

1. **Find Similar Task**: Locate a task you want to duplicate
2. **Click "Clone"**: Press the clone button (copy icon)
3. **Review Pre-filled Data**: All fields except date are copied
4. **Update Date**: Change the task date
5. **Modify Other Fields**: Update any other details as needed
6. **Save**: Click **"Save Task Activity"**

**Important: Inactive Dropdown Values**

When cloning a task that contains inactive dropdown values (Client, Project, or Phase), these values will be automatically cleared:

- **Inactive Values Cleared**: If the original task has a Client, Project, or Phase that has been marked inactive by an administrator, that field will be empty in the clone form
- **You Must Select Active Values**: Before saving, you'll need to select from the available active values in the dropdown lists
- **Why This Happens**: This ensures you're using current, active categories and prevents propagation of outdated or discontinued values
- **View/Edit Still Shows Inactive Values**: When viewing or editing existing tasks, you can still see any inactive values that were previously assigned - they just won't be available when creating new tasks or cloning

## Weekly Timesheet

The weekly timesheet gives you a comprehensive view of your time across an entire week.

### Accessing Weekly Timesheet

Navigate to the Weekly Timesheet from the main navigation menu.

### Understanding the Timesheet

The timesheet displays:

- **Week Range**: Start and end dates of the displayed week
- **Daily Columns**: Monday through Sunday
- **Your Tasks**: All tasks for that week, grouped by day, with columns for Client, Project, Phase, Hours, Task ID, Task Name, and Task Details
- **Daily Totals**: Hours worked each day
- **Weekly Total**: Total hours for the entire week

### Navigating Weeks

**View Different Weeks:**

1. **Previous Week**: Click **"◄ Previous Week"** button
2. **Next Week**: Click **"Next Week ►"** button
3. **Specific Week**: Use the date picker to jump to any week

### Filtering by Billability

You can filter the timesheet to show only billable or non-billable tasks:

**Visual Indicator**: Non-billable task hours are displayed in **red bold text** throughout the timesheet, making it easy to distinguish billable client work from internal overhead at a glance.

**Billability Filter Options:**
- **All** (default): Shows all tasks regardless of billability status
- **Billable**: Shows only tasks where the client, project, AND phase are all marked as billable
- **Non-Billable**: Shows tasks where any component (client, project, or phase) is marked as non-billable

**How Billability is Determined:**
- Each dropdown value (client, project, phase, expense type) can be marked as "Non-Billable" by administrators
- A task is considered billable only if ALL its components (client, project, phase) are billable
- If ANY component is marked as non-billable, the entire task is classified as non-billable

**To Filter Tasks:**
1. Locate the **Billability** dropdown below the week navigation
2. Select your desired filter:
   - **All**: View all tasks (default)
   - **Billable**: View only billable tasks
   - **Non-Billable**: View only non-billable tasks
3. The timesheet automatically refreshes with filtered results
4. Daily and weekly totals recalculate based on filtered tasks
5. The filter persists when navigating to different weeks

**Filter Use Cases:**
- **Client Billing**: Filter to "Billable" to see only hours that should be invoiced to clients
- **Overhead Tracking**: Filter to "Non-Billable" to review internal time (meetings, training, admin work)
- **Reporting**: Export filtered timesheets for specific billing or accounting purposes

### Exporting Timesheet Data

You can export your weekly timesheet data to CSV format for easy sharing or importing into other applications.

**To Export Your Timesheet:**

1. **Navigate to Week**: Display the week you want to export
2. **Click Export Button**: Click **"Export CSV"** in the header
3. **Choose Action**:
   - **Copy to Clipboard**: Copies CSV data for pasting into Excel, email, etc.
   - **Download CSV**: Downloads a file named `Timesheet_Week_of_MM-DD-YYYY_to_MM-DD-YYYY.csv`
   - **Close**: Close the export window

**CSV Format:**

The exported data includes:

- Date (MM/DD/YYYY)
- Client name
- Project name
- Phase
- Hours worked
- Task ID
- Task Name
- Task details

**Example CSV Output:**

```
Date,Client,Project,Phase,Hours,Task ID,Task Name,Task Details
10/28/2025,Acme Corp,Website Redesign,Development,8.00,TASK-101,Fix login bug,Fixed login bug
10/29/2025,Acme Corp,Website Redesign,Testing,6.50,TASK-102,QA testing,QA testing
```

## Managing Your Profile

All users (except Guest users) can manage their own profile information, including personal details and passwords. 

### Accessing My Profile

There are multiple ways to access your profile:

**From the Dashboard:**

1. Click the **"My Profile"** card on the dashboard
2. Or select **"My Profile"** from the side menu

**From the Backend:**

- Navigate to the user menu and select "My Profile"

### Editing Your Profile

The profile editor allows you to update:

- **First Name**: Your first/given name
- **Last Name**: Your surname/family name
- **Company**: Your company or organization name
- **Email Address**: Your email (required for expense management features)

**Note**: Your username, role, and account status cannot be changed through the profile editor. These fields are managed by administrators.

**To Update Your Profile (Dashboard UI):**

1. Access My Profile from the dashboard or side menu
2. Modify the fields you want to update
3. Click **"Update Profile"**
4. A success message confirms your changes
5. Click **"Cancel"** to return to the dashboard without saving

**To Update Your Profile (Backend UI):**

1. Access My Profile from the user menu
2. Modify the fields you want to update
3. Click **"Update Profile"**
4. A success message appears at the top of the page
5. Use the top navigation to return to other pages

### Email Requirement for Expenses

**Important**: You must have an email address configured in your profile to use expense management features:

- The "Add New Expense" button is hidden if no email is configured
- Expense submissions require email for approval notifications
- Receipt uploads are linked to your email identity

To enable expense access, add an email address to your profile. See the [Managing Your Profile](#managing-your-profile) section for instructions.

## Changing Your Password

All users can change their own password at any time. Passwords expire every 90 days and must meet security requirements.

### Password Requirements

Your new password must:

- Be at least 10 characters long
- Contain at least one uppercase letter
- Contain at least one number
- Contain at least one special character (+&%$#@!~*)
- Not contain more than 2 consecutive identical characters
- Not contain the username (case-insensitive)
- Not match your current password
- Cannot match any of your previous 5 passwords

### How to Change Your Password

**From Angular Profile:**

1. Access your profile (see "Accessing My Profile" above)
2. **Click the "Update Password"** button
3. A dialog opens with password change form:
   - Enter your **Current Password**
   - Enter your **New Password** (real-time validation feedback)
   - Enter your **Confirm Password** (must match new password)
   - View password requirements with live status indicators
   - Use "Show passwords" checkbox if needed
4. Click **"Update"** to save or **"Cancel"** to close without changes
5. Dialog closes on success with confirmation message
6. Specific error messages display for validation failures (e.g., "Password must contain at least 1 special character (+&%$#@!~*)")

**From the Backend Menu:**

1. Navigate to the user menu
2. Select "Change Password"
3. Dedicated page opens with password change form
4. Enter current password, new password, and confirm password
5. Click "Change Password" button
6. Redirected back to My Profile with success message

### Password Expiration

- Passwords expire after **90 days**
- You'll see a warning message when your password is nearing expiration
- When expired, you'll be prompted to change it at login
- The system tracks password history to prevent reuse

### Account Lockout Protection

For security, your account will be locked after **5 consecutive failed login attempts**:

- Locked accounts must be unlocked by an administrator
- The lockout counter resets after a successful login
- Contact your administrator if you've been locked out

---

## Expense Tracking

### Expense List

The expense list shows all your recorded expenses with the following information:

- **Date**: Date of the expense
- **Client**: Client associated with the expense
- **Project**: Project the expense is related to
- **Type**: Category of expense (Travel, Home Office, etc.)
- **Amount**: Expense amount with currency (formatted with comma separators for readability)
  - **Visual Indicator**: Non-billable amounts are displayed in **red bold text** to help you quickly identify expenses that won't be charged back to clients
- **Status**: Current workflow status (Draft, Submitted, Approved, Rejected, Reimbursed)
- **Actions**: Edit, Clone, and Delete buttons for managing your expenses

The expense management system provides:
- **Add Expense** - Create new expenses with receipt upload
- **Edit Expense** - Modify draft expenses and upload/replace receipts
- **Clone Expense** - Duplicate existing expenses for similar entries
- **Delete Expense** - Remove draft expenses
- **Receipt Upload** - Attach receipts directly in Add/Edit dialogs

### Using the Sidebar Menu

The Expense List page includes a **floating sidebar menu** for quick access to export and navigation features.

**To Access the Menu:**

1. Click the **☰** (hamburger menu) button in the upper-right corner of the page
2. The sidebar menu will slide in from the right side
3. Click the **✕** button or click the menu button again to close

**Menu Items:**

- **Export CSV**: Export filtered expense list to CSV format

**Note:** Guest users can see the menu but the Export CSV option will be disabled.

### Filtering Expenses

Use the filter controls at the top of the expense list to find specific expenses:

1. **Client Filter**: Show expenses for a specific client
2. **Project Filter**: Filter by project name
3. **Expense Type**: Filter by expense category
4. **Status**: Filter by workflow status
5. **Payment Method**: Filter by how you paid
6. **Start Date / End Date**: Filter by expense date range

**To Apply Filters:**

- Select your desired filter options
- Filters apply automatically
- Click **"Reset Filters"** to clear all filters

### Creating a New Expense

1. **Access Expense Form**: Click **"Add Expense"** from the navigation

2. **Fill in Required Fields**:
   
   - **Expense Date**: Date you incurred the expense (required)
   - **Client**: Client associated with this expense (required)
   - **Project**: Project related to the expense (optional)

   > **Note:** The Client and Project dropdowns for expenses show only the values your administrator has assigned to your account under **Expense** access, plus any values flagged as visible to all users. Task and expense access are configured independently — your task clients/projects and your expense clients/projects may differ. If you do not see an expected client or project, contact your administrator to update your **Expense** tab access settings.

   - **Expense Type**: Category from dropdown (Travel - Airfare, Hotel, Home Office Equipment, etc.) (required)
   - **Description**: What the expense was for (required)
   - **Amount**: Cost of the expense (required)
   - **Currency**: Default is USD (optional)
   - **Payment Method**: How you paid (Personal Credit Card, Cash, etc.) (required)

3. **Fill in Optional Fields**:
   
   - **Vendor**: Name of merchant or vendor
   - **Reference Number**: Receipt number, confirmation code, or invoice number
   - **Notes**: Additional information or justification

4. **Upload Receipt** (Recommended):
   
   - Click **"Choose File"** next to Receipt
   - Select an image file (JPEG, PNG, or PDF only)
   - The system displays the maximum allowed file size for your environment
   - Receipt is uploaded and attached to the expense
   - **File Size Limits**: 
     - Local development: 2MB maximum
     - Production/AWS: 10MB maximum
     - Docker: 5MB maximum
   - **File Validation**: The system validates the actual file content (magic numbers), not just the file extension
   - **Accepted File Types**: JPEG images, PNG images, PDF documents
   - **Rejected Files**: 
     - Files exceeding the size limit
     - Executables, scripts, and files with mismatched content
   - If you see "File size exceeds maximum" error, reduce the file size or resolution before uploading
   - If you see "Invalid file type" error, ensure your file is a genuine JPEG, PNG, or PDF

5. **Save as Draft**: Click **"Save Expense"**
   
   - Expense is saved with status "Draft"
   - You can edit it anytime before submitting

### Editing an Expense

1. **Find the Expense**: Locate the expense in your list
2. **Click "Edit"**: Press the edit button (pencil icon)
3. **Update Information**: Change any fields as needed
4. **Save Changes**: Click **"Update Expense"**

**Note:** You can only edit expenses with status "Draft". Once submitted, you cannot edit them.

### Cloning an Expense

To quickly create a similar expense:

1. **Find Original Expense**: Locate an expense similar to what you want to create
2. **Click "Clone"**: Press the clone button on the expense row
3. **Review Pre-filled Data**: All fields except date and receipt are copied from the original
4. **Update Details**: 
   - Change the expense date to the new date
   - Modify amount if different
   - Update description as needed
   - Upload a receipt for the new expense
5. **Save**: Click **"Save Expense"**

**Important: Inactive Dropdown Values**

When cloning an expense that contains inactive dropdown values, these values will be automatically cleared:

- **Inactive Values Cleared**: If the original expense has a Client, Project, Expense Type, Payment Method, Vendor, or Currency that has been marked inactive by an administrator, that field will be empty in the clone form
- **You Must Select Active Values**: Before saving, you'll need to select from the available active values in the dropdown lists
- **Why This Happens**: This ensures you're using current, active categories and prevents propagation of outdated or discontinued values
- **View/Edit Still Shows Inactive Values**: When viewing or editing existing expenses, you can still see any inactive values that were previously assigned - they just won't be available when creating new expenses or cloning

### Submitting an Expense for Approval

1. **Complete Draft Expense**: Ensure all information is correct and receipt is attached
2. **Submit**: Click **"Submit for Approval"** button
3. **Status Change**: Expense status changes to "Submitted"
4. **Approver Notification**: All configured approvers receive an email notification with expense details
5. **Wait for Review**: EXPENSE_ADMIN will review and approve or reject
6. **Email Notification**: You'll receive an email when your expense is approved, rejected, or reimbursed

**Important:** Once submitted, you cannot edit or delete the expense.

**Email Notifications**

When you submit an expense, configured approvers receive an immediate email notification with:

- Your full name and username
- Expense ID and description
- Amount and date
- Instructions to review in the Approval Queue

After submitting an expense, you'll receive automatic email notifications at each status change:

- **Approved**: Notifies you the expense is ready for reimbursement processing
- **Rejected**: Includes reviewer notes explaining the rejection
- **Reimbursed**: Confirms payment has been processed

Each email includes:

- Your full name
- Expense description
- Amount and currency
- New status
- Processor's full name (who approved/rejected/reimbursed)
- Date and time

**Note**: Emails are sent to the address in your user profile. Ensure your email is current.

### Understanding Expense Status

**Draft**: Initial state after creating an expense

- You can edit and delete
- Not visible to approvers
- Upload receipt before submitting

**Submitted**: Expense is awaiting approval

- Cannot edit or delete
- Visible to approvers in expense list (filtered by status)
- Approver will review details and receipt

**Approved**: Expense has been approved

- Ready for reimbursement processing
- You'll see approval date and approver name
- Check approval notes for any comments

**Rejected**: Expense was not approved

- Read rejection notes carefully
- Make corrections and create new expense
- Contact approver if you have questions

**Reimbursed**: Payment has been processed

- View reimbursement date and amount
- Check reimbursement notes for payment details
- Expense workflow is complete

### Viewing Expense Details

1. **Click on Expense**: Click anywhere on the expense row
2. **View Complete Information**: See all fields, notes, and approval details
3. **View Receipt**: Click receipt thumbnail to view full-size image
4. **Download Receipt**: Download receipt for your records

### Weekly Expense Sheet

Similar to the weekly timesheet, the expense sheet shows your expenses for the current week:

1. **Access Expense Sheet**: Click **"Weekly Expenses"** from navigation
2. **View Current Week**: See all expenses from Monday through Sunday
3. **See Totals**: View total expenses by day and for the week
4. **Filter by Client/Project**: Filter to see expenses for specific work

**Visual Indicator**: Non-billable expense amounts are displayed in **red bold text** throughout the expense sheet, making it easy to distinguish billable expenses from internal or non-reimbursable costs at a glance.

**Filtering by Billability:**

Just like the weekly timesheet, you can filter expenses by billability status:

- **All** (default): Shows all expenses
- **Billable**: Shows only expenses where client, project, AND expense type are all billable
- **Non-Billable**: Shows expenses where any component is marked as non-billable

**To Filter Expenses:**
1. Locate the **Billability** dropdown below the week navigation
2. Select your desired filter (All/Billable/Non-Billable)
3. The expense sheet refreshes automatically with filtered results
4. Expense totals recalculate based on filtered expenses
5. The filter persists when navigating between weeks

**Expense Billability Determination:**
- An expense is billable only if its client, project, AND expense type are all marked as billable
- If ANY component (client, project, or type) is non-billable, the entire expense is classified as non-billable
- Administrators configure billability flags for dropdown values (clients, projects, types)

### Exporting Expenses to CSV

Export your filtered expense list for reporting or record-keeping:

1. **Apply Filters** (optional): Filter expenses as needed
2. **Click Export Button**: 
   - Click **"☰"** to open the sidebar menu, then click **"Export CSV"**
   - The menu will automatically close after selecting Export CSV
3. **Wait for Data**: The system will fetch ALL filtered expenses (not just the current page)
4. **Choose Action**:
   - **Copy to Clipboard**: Copies CSV data for pasting into Excel, email, etc.
   - **Download CSV**: Downloads a CSV file
   - **Close**: Close the export window

**CSV Export Features:**

- **Exports ALL filtered expenses** - not limited to the visible entries on the current page
- Filename includes a date stamp
- Example filename: `Expenses_20260113.csv`

---

## Changing Your Password

Your new password must meet these requirements:

1. ✅ **At least 10 characters long**
2. ✅ **At least 1 uppercase letter** (A-Z)
3. ✅ **At least 1 number** (0-9)
4. ✅ **At least 1 special character** from: `+ & % $ # @ ! ~`
5. ✅ **Not contain more than 2 consecutive identical characters** (e.g., "aaa" is not allowed)
6. ✅ **Not contain the username** (case-insensitive)
7. ✅ **Not be the same as your current password**

### How to Change Your Password (Regular Users)

1. **Open Task List**: Navigate to your task activity list
2. **Click Password Button**: Click **"Update Password"** in the header
3. **Enter Current Password**: Type your existing password
4. **Enter New Password**: Type your new password (must meet requirements)
5. **Confirm New Password**: Retype your new password exactly
6. **Show Passwords (Optional)**:
   - Click the eye icon next to each field to view what you're typing
   - Or check "Show passwords" to reveal all password fields
7. **Submit**: Click **"Change Password"**
8. **Success**: You'll be redirected with a confirmation message

**Security Impact of Password Changes:**

When you change your password:
- **All your existing session tokens are immediately revoked**
- You'll be logged out automatically and must log in with your new password
- Any other active sessions (e.g., on different devices) are also terminated
- This prevents unauthorized access if someone had access to your old credentials or tokens

This enhanced security ensures that changing your password fully protects your account, even if someone previously obtained your authentication token.

### Forced Password Changes

If your administrator has enabled "Force Password Update" for your account:

1. You'll be redirected to the password change screen immediately after login
2. You cannot access other features until you change your password
3. The message will say: "Your administrator has required you to change your password"
4. Follow the same steps as above (no current password needed)
5. After changing, you'll gain full access to the system

**Note**: This is different from password expiration. Force Password Update is manually set by administrators (typically for new accounts), while password expiration happens automatically after 90 days.

---

## Reports & Analytics

The Reports section provides interactive charts and visualizations to help you analyze your time tracking data.

### Accessing Reports

Navigate to **Reports** from the main menu to view your analytics dashboard.

### Date Range Filtering

**All reports now support flexible date range filtering!**

At the top of the Analytics & Reports page, you'll find comprehensive date filtering options:

**Preset Date Ranges (Quick Selection):**
- **This Month** - Current calendar month
- **Last Month** - Previous calendar month
- **Last 3 Months** - Rolling 3-month period
- **This Year** - Current calendar year (Jan 1 - Dec 31)
- **All Time** - View all historical data

**Custom Date Range:**
- **Start Date** - Select any start date using the date picker
- **End Date** - Select any end date using the date picker
- **Clear Button** - Reset date filters to default (current month)

When you change the date range, all report tabs automatically update to show data for the selected period. This allows you to analyze historical trends, compare different time periods, and review past performance.

### Available Reports

**For All Users:**

#### 1. Overview Tab

Dashboard summary with key metrics for the selected date range:

- Total hours for selected period and current week
- Top clients and projects
- Average hours per day
- Quick insights into your time allocation

#### 2. Client Analysis Tab

- **Time distribution by client** (pie chart) - Visual breakdown of time spent per client for selected date range
- **Top activities breakdown** - Most time-consuming tasks within the period
- Client-focused time metrics

#### 3. Project Analysis Tab

- **Time distribution by project** (bar chart) - Hours worked per project within date range
- **Phase distribution** (donut chart) - Time spent in different phases (Development, Testing, etc.)
- Project-level time breakdown with phase details

#### 4. Time Trends Tab

- **Daily time tracking** (line chart) - Daily hours worked for the selected date range
- **Weekly summary** with trends - Week-by-week comparison within period
- **Monthly comparison** (grouped bar chart) - Compare hours across months in selected range

---

## Troubleshooting

### File Upload Issues

**Problem:** "Invalid file type" error when uploading receipts

**Causes:**
- The file extension doesn't match the actual file content
- The file is not a genuine JPEG, PNG, or PDF
- The file might be an executable or script with a fake image extension
- The file is corrupted or too small

**Solutions:**
1. **Verify File Type**: Open the file on your computer to ensure it's a valid image or PDF
2. **Re-save the File**: Open the image in an image editor and save it as JPEG or PNG
3. **Convert PDF**: If using a PDF, ensure it was created by a legitimate PDF tool
4. **Check File Size**: Ensure the file is at least 8 bytes (very small files are rejected)
5. **Avoid Screenshots from Unknown Sources**: Screenshots should be saved properly as JPEG or PNG

**Technical Note:** The system validates the actual file content (magic numbers/signatures) rather than trusting the file extension. This prevents malicious files from being uploaded, even if they're renamed to look like images.

### Password Change Issues

**Problem:** "Cannot reuse any of your previous passwords" error

**Cause:**
- You're trying to use a password that matches one of your last 5 passwords
- The system stores your password history to prevent reuse of recent passwords

**Solutions:**
1. **Choose a Different Password**: Create a new password that you haven't used in your last 5 password changes
2. **Make It Unique**: Even small changes won't work if the overall password matches a previous one
3. **Consider a Password Manager**: Use a password manager to generate and store unique passwords

**Why This Exists:** Password history validation is a security feature that prevents you from cycling through the same few passwords. This ensures better account security over time.

---

### Report Features

- **Interactive Charts**: Hover over chart elements for detailed information and exact values
- **Real-Time Data**: All reports reflect your current task data from the database
- **Your Data Only**: Reports show only your own task activities
- **Color-Coded Visualizations**: Easy-to-read charts with consistent, professional color schemes
- **Responsive Design**: Charts adapt to different screen sizes

### Understanding Your Data

- All charts are based on your submitted task activities
- Date ranges default to the current month but can be filtered
- Hours are displayed with two decimal places (e.g., 8.50 hours)
- Percentages are calculated based on total hours in the selected period
- "Unknown" may appear for tasks without a username (legacy data)
