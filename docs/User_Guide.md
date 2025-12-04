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

The system has three user roles with different levels of access:

**GUEST (Read-Only Access)**

- ✅ View task list
- ✅ View task details
- ❌ Cannot create, edit, or delete tasks
- ❌ Cannot access weekly timesheet
- ❌ Cannot change password
- ❌ No access to user management or dropdown settings

**USER (Standard Access)**

- ✅ View, create, edit, and delete your own tasks
- ✅ Access weekly timesheet
- ✅ Clone tasks
- ✅ Change your own password
- ✅ Export your tasks to CSV
- ❌ Cannot view other users' tasks
- ❌ No access to user management or dropdown settings

**EXPENSE_ADMIN (Expense Approver)**

- ✅ All USER permissions for tasks
- ✅ View and manage all users' expenses
- ✅ Filter expenses by any username
- ✅ Access expense approval queue
- ✅ Approve or reject expense submissions
- ✅ Process reimbursements
- ❌ No access to task management for other users
- ❌ No access to user management or dropdown settings

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
- ✅ Manage dropdown values (clients, projects, phases)
- ✅ Change other users' passwords
- ✅ All EXPENSE_ADMIN permissions for expenses

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

**Tips to Avoid Lockouts:**

- Keep your password in a secure location
- Use a password manager if available
- Type carefully when entering your password
- If you're unsure of your password, contact your administrator before using all 5 attempts

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

**Tips:**

- Save your work regularly
- The system will show a clear message when your session expires
- All your data is saved automatically when you create or edit tasks

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

## Changing Your Password

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
   - Select an image file (JPG, PNG, PDF)
   - Receipt is uploaded and attached to the expense

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
4. **Wait for Review**: EXPENSE_ADMIN will review and approve or reject
5. **Email Notification**: You'll receive an email when your expense is approved, rejected, or reimbursed

**Important:** Once submitted, you cannot edit or delete the expense.

**Email Notifications**

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
- Visible in approval queue
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

### Tips for Using Reports

- **Review your Overview tab weekly** to track your time allocation and ensure you're meeting targets
- **Use Client Analysis** to understand which clients consume most of your time
- **Check Time Trends** to identify patterns in your daily work hours and adjust your schedule
- **Monitor Phase Distribution** to ensure balanced time across different work phases
- **ADMIN users:** Use User Analysis to monitor team performance and identify top contributors

### Understanding Your Data

- All charts are based on your submitted task activities
- Date ranges default to the current month but can be filtered
- Hours are displayed with two decimal places (e.g., 8.50 hours)
- Percentages are calculated based on total hours in the selected period
- "Unknown" may appear for tasks without a username (legacy data)
