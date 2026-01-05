# Task Activity & Expense Management System - User Guide

## Welcome

Welcome to the Task Activity & Expense Management System! This comprehensive application helps you track both time spent on tasks and project-related expenses. Whether you're logging daily work activities, reviewing your weekly timesheet, submitting expenses for reimbursement, or managing expense approvals, this guide will help you make the most of the system.

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
- **Details**: Description of work performed
- **Actions**: Edit, Delete, and Clone buttons

### Filtering Tasks

Use the filter controls at the top of the task list to narrow down your view:

1. **Client Filter**: Show only tasks for a specific client
2. **Project Filter**: Filter by project name
3. **Phase Filter**: Filter by work phase
4. **Start Date**: Show tasks from this date onward
5. **End Date**: Show tasks up to this date
6. **Username** (Admin only): Filter tasks by user

**To Apply Filters:**

- Select your desired filter options
- Click **"Filter"** button
- To reset: Click **"Clear"** button

### Exporting Task List to CSV

You can export your filtered task list to CSV format for reporting or import into spreadsheets.

**To Export Tasks:**

1. **Apply Filters** (optional): Filter the task list to show only the tasks you want to export
2. **Click Export Button**: Click **"📋 Export CSV"** in the header
3. **Wait for Data**: The system will fetch ALL filtered tasks (not just the current page)
4. **Choose Action**:
   - **📋 Copy to Clipboard**: Copies CSV data for pasting into Excel, email, etc.
   - **💾 Download CSV**: Downloads a CSV file with an intelligent filename
   - **Close**: Close the export window

**CSV Export Features:**

- **Exports ALL filtered tasks** - not limited to the 20 visible on the current page
- If there are 270 filtered tasks, all 270 will be included in the CSV
- Filename includes active filters and timestamp
- Example filename: `TaskActivity_Acme_Corp_Website_20251101.csv`
- For admins: Includes username column in export

**CSV Format:**

```
Date,Client,Project,Phase,Hours,Details
10/28/2025,Acme Corp,Website Redesign,Development,8.00,Fixed login bug
10/29/2025,Acme Corp,Website Redesign,Testing,6.50,QA testing
```

**Note:** The export includes all tasks matching your current filters, regardless of pagination. If you see "Showing 1-20 of 270 entries", the CSV export will contain all 270 entries.

## Getting Started

### User Roles and Permissions

The system uses a **database-driven role-based access control system**. While four default roles are provided, administrators can create custom roles and assign permissions as needed through the Role & Permission Management interface.

**Default Roles:**

**GUEST (Read-Only Access)**

- ✅ View task list
- ✅ View task details
- ✅ Create, edit, and delete tasks (full CRUD access for task activities)
- ✅ Access weekly timesheet
- ❌ Cannot access expenses (no expense-related permissions)
- ❌ Cannot change password
- ❌ No access to user, dropdown or role management

**USER (Standard Access)**

- ✅ View, create, edit, and delete your own tasks
- ✅ Access weekly timesheet
- ✅ Clone tasks
- ✅ Manage your own expenses
- ✅ Change your own password
- ✅ Export your tasks to CSV
- ❌ Cannot view other users' tasks
- ❌ No access to user, dropdown or role management

**EXPENSE_ADMIN (Expense Approver)**

- ✅ All USER permissions for tasks
- ✅ View and manage all users' expenses
- ✅ Filter expenses by any username
- ✅ Approve or reject expense submissions from expense detail page
- ✅ Process reimbursements
- ❌ No access to task management for other users
- ❌ No access to user, dropdown or role management

**Email Requirement for Expense Access**

All users (except GUEST) must have a valid email address configured in their profile to access expense features. Without an email:

- ❌ Expense tracking buttons and links are hidden
- ❌ Cannot create, view, or manage expenses
- ❌ Cannot submit expenses for approval
- ✅ Can still access all task activity features

**To enable expense access**: Contact your administrator to add an email address to your user profile.

**ADMIN (Full Access)**

- ✅ All USER permissions
- ✅ View and manage all users' tasks
- ✅ Filter tasks by any username
- ✅ Create, edit, and delete user accounts
- ✅ Manage roles and permissions - create custom roles and assign permissions via web UI
- ✅ Manage dropdown values (clients, projects, phases)
- ✅ Change other users' passwords
- ✅ All EXPENSE_ADMIN permissions for expenses

**Note:** Contact your administrator if you need different access permissions. Administrators can create custom roles tailored to your organization's needs.

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
- You will see a message: "Your account has been locked due to too many failed login attempts. Please contact your administrator."
- You cannot log in until an administrator unlocks your account
- The administrator receives an automatic email notification about the lockout

**What to Do:**

1. **Contact your administrator** to unlock your account
2. The administrator can unlock your account from the User Management page
3. **Double-check your password** before attempting to log in again
4. If you've forgotten your password, ask your administrator to reset it

### Password Expiration Warnings

**Automatic Expiration**: All passwords expire automatically **90 days** after they are set or changed.

**7-Day Warning**: Starting 7 days before your password expires, you'll see a warning banner at the top of every page:

```
⚠️ Your password will expire in X day(s). Please change it soon.
```

**Note**: GUEST users will not see password expiration warnings since they cannot change their own passwords.

**What to Do:**

1. Click the **"🔒 Update Password"** button in the header
2. Follow the password change process (see "Changing Your Password" section below)
3. Your new password will be valid for another 90 days

**Expired Password:**

**For USER and ADMIN roles:**

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
   - **Phase**: Pick the work phase (Development, Testing, etc.) (required)
   - **Hours**: Enter time spent (use decimals like 2.5 for 2½ hours) (required)
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

## Weekly Timesheet

The weekly timesheet gives you a comprehensive view of your time across an entire week.

### Accessing Weekly Timesheet

1. **From Task List**: Click **"📊 Weekly Timesheet"** button
2. **From Main Menu**: Select "Weekly View" option

### Understanding the Timesheet

The timesheet displays:

- **Week Range**: Start and end dates of the displayed week
- **Daily Columns**: Monday through Sunday
- **Your Tasks**: All tasks for that week, grouped by day
- **Daily Totals**: Hours worked each day
- **Weekly Total**: Total hours for the entire week

### Navigating Weeks

**View Different Weeks:**

1. **Previous Week**: Click **"◄ Previous Week"** button
2. **Next Week**: Click **"Next Week ►"** button
3. **Specific Week**: Use the date picker to jump to any week

### Exporting Timesheet Data

You can export your weekly timesheet data to CSV format for easy sharing or importing into other applications.

**To Export Your Timesheet:**

1. **Navigate to Week**: Display the week you want to export
2. **Click Export Button**: Click **"📋 Export CSV"** in the header
3. **Choose Action**:
   - **📋 Copy to Clipboard**: Copies CSV data for pasting into Excel, email, etc.
   - **💾 Download CSV**: Downloads a file named `Timesheet_Week_of_MM-DD-YYYY_to_MM-DD-YYYY.csv`
   - **Close**: Close the export window

**CSV Format:**

The exported data includes:

- Date (MM/DD/YYYY)
- Client name
- Project name
- Phase
- Hours worked
- Task details
- Username

**Example CSV Output:**

```
Date,Client,Project,Phase,Hours,Task Details,Username
10/28/2025,Acme Corp,Website Redesign,Development,8.00,Fixed login bug,jsmith
10/29/2025,Acme Corp,Website Redesign,Testing,6.50,QA testing,jsmith
```

## Managing Your Profile

All users (except Guest users) can manage their own profile information, including personal details and passwords. 

### Accessing My Profile

There are multiple ways to access your profile:

**From the Dashboard:**

1. Click the **"My Profile"** card on the dashboard (non-Admin users only)
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

If you need to track expenses but don't have an email configured, contact your administrator to update your profile.

## Changing Your Password

All users can change their own password at any time. Passwords expire every 90 days and must meet security requirements.

### Password Requirements

Your new password must:

- Be at least 10 characters long
- Contain at least one uppercase letter
- Contain at least one number
- Contain at least one special character (+&%$#@!~)
- Not contain more than 2 consecutive identical characters
- Not contain the username (case-insensitive)
- Not match your current password

### How to Change Your Password

**From My Profile:**

1. Access your profile (see "Accessing My Profile" above)
2. Click the **"Update Password"** button
3. Enter your **Current Password**
4. Enter your **New Password**
5. Enter your **Confirm Password** (must match new password)
6. Click **"Change Password"**
7. You'll be redirected back to My Profile with a success message

**From the Backend Menu:**

1. Navigate to the user menu
2. Select "Change Password"
3. Follow the same steps as above

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
- **Amount**: Expense amount with currency
- **Status**: Current workflow status (Draft, Submitted, Approved, Rejected, Reimbursed)
- **Receipt**: Indicator if receipt is attached
- **Actions**: View, Edit, Submit, Delete buttons

### Filtering Expenses

Use the filter controls at the top of the expense list to find specific expenses:

1. **Client Filter**: Show expenses for a specific client
2. **Project Filter**: Filter by project name
3. **Expense Type**: Filter by expense category
4. **Status**: Filter by workflow status
5. **Payment Method**: Filter by how you paid
6. **Start Date / End Date**: Filter by expense date range
7. **Username** (Admin only): Filter expenses by user

**To Apply Filters:**

- Select your desired filter options
- Filters apply automatically
- Click **"Reset Filters"** to clear all filters

### Creating a New Expense

1. **Access Expense Form**: Click **"💰 Add Expense"** from the navigation

2. **Fill in Required Fields**:
   
   - **Expense Date**: Date you incurred the expense (required)
   - **Client**: Client associated with this expense (required)
   - **Project**: Project related to the expense (optional)
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
   - Receipt is uploaded and attached to the expense
   - **File Validation**: The system validates the actual file content (magic numbers), not just the file extension
   - **Accepted File Types**: JPEG images, PNG images, PDF documents
   - **Rejected Files**: Executables, scripts, and files with mismatched content are automatically rejected
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

1. **Access Expense Sheet**: Click **"📊 Weekly Expense Sheet"** from navigation
2. **View Current Week**: See all expenses from Monday through Sunday
3. **See Totals**: View total expenses by day and for the week
4. **Filter by Client/Project**: Filter to see expenses for specific work

### Exporting Expenses to CSV

Export your filtered expense list for reporting or record-keeping:

1. **Apply Filters** (optional): Filter expenses as needed
2. **Click Export**: Click **"📋 Export CSV"** button
3. **Choose Action**:
   - Copy to Clipboard for pasting into Excel
   - Download CSV file with intelligent filename
4. **CSV Includes**: All filtered expenses with complete details

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
2. **Click Password Button**: Click **"🔒 Update Password"** in the header
3. **Enter Current Password**: Type your existing password
4. **Enter New Password**: Type your new password (must meet requirements)
5. **Confirm New Password**: Retype your new password exactly
6. **Show Passwords (Optional)**:
   - Click the eye icon (👁️) next to each field to view what you're typing
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

### Available Reports

**For All Users:**

#### 1. Overview Tab

Dashboard summary with key metrics:

- Total hours this month and week
- Top clients and projects
- Average hours per day
- Quick insights into your time allocation

#### 2. Client Analysis Tab

- **Time distribution by client** (pie chart) - Visual breakdown of time spent per client
- **Top activities breakdown** - Most time-consuming tasks
- Client-focused time metrics

#### 3. Project Analysis Tab

- **Time distribution by project** (bar chart) - Hours worked per project
- **Phase distribution** (donut chart) - Time spent in different phases (Development, Testing, etc.)
- Project-level time breakdown with phase details

#### 4. Time Trends Tab

- **Daily time tracking** (line chart) - Daily hours worked over time
- **Weekly summary** with trends - Week-by-week comparison
- **Monthly comparison** (grouped bar chart) - Compare hours across months

**For ADMIN Users Only:**

#### 5. User Analysis Tab

ADMIN users see an additional tab with team performance analytics:

- **User Performance Summary** table with rankings
  
  - Trophy icons for top 3 performers:
    - 🏆 Gold trophy for #1 performer
    - 🥈 Silver medal for #2 performer
    - 🥉 Bronze medal for #3 performer

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

---
  - Metrics displayed:
    - Total hours worked
    - **Billable hours** (green) - Hours on client projects
    - **Non-Billable hours** (orange) - Overhead activities logged to "Non-Billable" project
    - Task count
    - **Average billable hours per day** - Calculated from days with billable work
    - Top client (excludes Non-Billable project)
    - Top project (excludes Non-Billable project)
    - Last activity date
  - **Billable Tracking**: The system distinguishes billable vs. non-billable hours using project name - tasks logged to "Non-Billable" project are tracked separately from client work

- **Hours by User** comparison (bar chart)
  
  - Visual comparison of hours across all team members
  - Shows percentage of total team hours per user
  - Color-coded bars for easy visualization
  - Interactive tooltips with detailed information

### Report Features

- **Interactive Charts**: Hover over chart elements for detailed information and exact values
- **Real-Time Data**: All reports reflect your current task data from the database
- **Role-Based Filtering**: 
  - Regular users (USER, GUEST) see only their own data
  - ADMIN users see data for all users in the system
- **Color-Coded Visualizations**: Easy-to-read charts with consistent, professional color schemes
- **Responsive Design**: Charts adapt to different screen sizes

### Understanding Your Data

- All charts are based on your submitted task activities
- Date ranges default to the current month but can be filtered
- Hours are displayed with two decimal places (e.g., 8.50 hours)
- Percentages are calculated based on total hours in the selected period
- "Unknown" may appear for tasks without a username (legacy data)
