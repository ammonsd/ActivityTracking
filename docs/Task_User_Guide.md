<!--
  Description: Task Activity Management System — user guide for task tracking features.

  Author: Dean Ammons
  Date: February 2026
-->

# Task Activity Management System — Task User Guide

## Welcome

Welcome to the Task Activity Management System! This guide covers everything you need to track time spent on tasks and projects, view your weekly timesheet, analyze your time allocation, and manage your account.

> **Separate guide available:** If you also have access to expense management features, see the [Expense User Guide](Expense_User_Guide.md).

**Features Covered in This Guide:**

- **Task Activity Tracking**: Log time spent on client projects and tasks with detailed phase tracking
- **Weekly Timesheet**: View a consolidated week-by-week view of your logged hours
- **Reporting & Analytics**: Interactive charts and dashboards for analyzing your time
- **CSV Export**: Export task lists and timesheets for use in spreadsheets or reporting tools

---

## Getting Started

### User Roles and Task Permissions

The system uses a role-based access control model. The role relevant to task tracking is:

**User (Standard Access)**

- ✅ View, create, edit, delete, and clone your own tasks
- ✅ Access weekly timesheet
- ✅ Export tasks to CSV
- ✅ Change your own password

> **Note:** Additional roles (such as Expense Administrator) are defined in the system. If your access does not match what you need, use the **Contact System Admin** option from the sidebar menu to request a change.

### Accessing the Application

You can access the application using any of the following URLs. If you bookmark your preferred entry point, you will be taken directly to that page after logging in.

| URL | Description |
|-----|-------------|
| https://taskactivitytracker.com/ | **Main Application** — Task list, timesheets, and all user-facing features |
| https://taskactivitytracker.com/app/dashboard | **User Dashboard** — Overview panels and quick-access links |

---

## Logging In

### Standard Login

1. **Navigate to the Login Page**: Open the application URL in your browser
2. **Enter Credentials**: Type your username and password
3. **Click "Login"**
4. **Change Password (if required)**: Some accounts require a password change on first login

### If You've Forgotten Your Password

1. Click the **"Reset Password"** link on the login page
2. Enter the email address associated with your account
3. Click **"Reset Password"**
4. Check your email inbox for a reset link (expires in **15 minutes**)
5. Click the link and enter your new password twice
6. Click **"Change Password"** — you will receive a confirmation email
7. Return to the login page and sign in with your new password

**If you don't receive the email:**

1. Check spam/junk folders
2. Wait 1–2 minutes for delivery
3. Verify you entered the correct email address
4. Contact your administrator if the issue persists

### Forced Password Change at First Login

If your account was configured to require a password change:

1. After logging in you will be redirected to a password change screen
2. Enter your new password twice and click **"Update Password & Continue"**
3. You will be logged in and can start using the system immediately

### Session Timeout

Your session expires after **30 minutes of inactivity**.

- You will see: _"⚠️ Your session has expired. Please log in again."_
- Log in again to continue — your data is saved

### Logging Out

Click **"Logout"** in the header. You will be redirected to the login page.

> **Best practice:** Always log out when using shared or public computers.

### Account Lockout

Your account will be locked after **5 consecutive failed login attempts**.

- Message shown: _"Account locked due to too many failed login attempts. Please use the 'Reset Password' link below to reset your password and unlock your account."_
- Use the **Reset Password** link to reset your password and automatically unlock your account
- Alternatively, use **Contact System Admin** from the sidebar menu to request manual assistance

### Password Expiration Warnings

Passwords expire every **90 days**. Starting 7 days before expiry you will see a banner:

```
⚠️ Your password will expire in X day(s). Please change it soon.
```

At expiry you will be redirected to the password change screen before you can access the system.

---

## Task Activity List

After logging in, your task list is the main view. Tasks are displayed in a table with the following columns:

| Column        | Description                                                    |
| ------------- | -------------------------------------------------------------- |
| **Date**      | When the task was performed                                    |
| **Client**    | Client the work was done for                                   |
| **Project**   | Specific project                                               |
| **Phase**     | Work phase (Development, Testing, etc.)                        |
| **Hours**     | Time spent — non-billable hours are shown in **red bold text** |
| **Task ID**   | Optional external reference (e.g., `TA-001`, `MP1T-6394`)      |
| **Task Name** | Optional short label or title                                  |
| **Details**   | Description of work performed                                  |
| **Actions**   | Edit, Delete, and Clone buttons                                |

### Filtering Tasks

Use the filter controls at the top of the task list to narrow down your view:

1. **Client Filter** — show only tasks for a specific client
2. **Project Filter** — filter by project name
3. **Phase Filter** — filter by work phase
4. **Task ID Filter** — filter by ticket or reference number
5. **Start Date / End Date** — restrict to a date range

Click **"Filter"** to apply. Click **"Clear"** to reset all filters.

### Sidebar Menu

The task list includes a **floating sidebar menu** for quick access to features.

**To open:** Click the **☰** button in the upper-right corner.  
**To close:** Click **✕** or click the ☰ button again.

**Menu items:**

- **Update Profile** — update your personal details and password
- **Export CSV** — export your filtered task list to CSV
- **Contact System Admin** — send a message to the administrator without leaving the application

### Contacting the System Administrator

Use **Contact System Admin** to request changes (e.g., add a client or project to your account) or ask questions — without leaving the application.

1. Click ☰ to open the sidebar, then click **Contact System Admin**
2. Enter a **Subject** and a **Message**
3. Click **Send**

The administrator receives an email with your username, subject, message, and timestamp. If you have an email on your profile, it is included so the administrator can reply to you directly. This feature is available to all users.

### Exporting Tasks to CSV

1. Apply filters (optional) to show only the tasks you want to export
2. Click **"Export CSV"** in the toolbar above the table, or open ☰ → **Export CSV**
3. Choose:
    - **Copy to Clipboard** — paste into Excel, email, etc.
    - **Download CSV** — saves a `.csv` file
    - **Close** — close without exporting

**Notes:**

- The export includes **all filtered tasks**, not just the current page
- Filename format: `TaskActivity_YYYYMMDD.csv`
- Columns exported: Date, Client, Project, Phase, Hours, Task ID, Task Name, Details

**Example CSV:**

```
Date,Client,Project,Phase,Hours,Task ID,Task Name,Details
10/28/2025,Acme Corp,Website Redesign,Development,8.00,TA-001,Implement login,Fixed login bug
10/29/2025,Acme Corp,Website Redesign,Testing,6.50,TA-002,QA regression,Performed regression testing
```

---

## Managing Your Tasks

### Adding a New Task

1. The task entry form appears at the top of the task list page, or click **"Add New Task"**
2. Fill in the required fields:
    - **Task Date** — click the calendar icon (required)
    - **Client** — choose from the dropdown (required)
    - **Project** — select the project (required)

        > The Client and Project dropdowns show only values your administrator has assigned to your account under **Task** access, plus values flagged as visible to all users. If an expected client or project is missing, use **Contact System Admin** to request it be added.

    - **Phase** — select the work phase (required)
    - **Hours** — enter time spent; use decimals for partial hours (e.g., `2.5` for 2½ hours) (required)
    - **Task ID** — optional ticket/reference number; spaces are not allowed (e.g., `TA-001`)
    - **Task Name** — optional short label or title
    - **Details** — optional description of the work performed

3. Click **"Save Task Activity"**

> The form does not clear after saving, making it easy to enter multiple tasks for the same date.

### Editing a Task

1. Find the task in your list
2. Click the **edit** (pencil) icon
3. Update any field as needed (username cannot be changed)
4. Click **"Update Task Activity"**

### Deleting a Task

1. Find the task in your list
2. Click the **delete** (trash) icon
3. Confirm the deletion in the dialog

> **⚠️ Warning:** Deleted tasks cannot be recovered.

### Cloning a Task

Cloning copies a task's fields so you can quickly create a similar entry.

1. Click the **clone** (copy) icon on the task row
2. All fields except the date are pre-filled from the original
3. Update the date and any other fields as needed
4. Click **"Save Task Activity"**

**Note — Inactive dropdown values:** If the original task references a Client, Project, or Phase that has since been marked inactive by an administrator, that field will be cleared in the clone form. Select an active value before saving.

---

## Weekly Timesheet

The weekly timesheet gives you a complete view of your hours across an entire week.

### Accessing the Timesheet

Navigate to **Weekly Timesheet** from the main navigation menu.

### Reading the Timesheet

The view displays:

- **Week Range** — start and end dates for the displayed week
- **Daily Columns** — Monday through Sunday
- **Your Tasks** — all tasks for the week, grouped by day, with columns for Client, Project, Phase, Hours, Task ID, Task Name, and Details
- **Daily Totals** — hours worked each day
- **Weekly Total** — total hours for the entire week

### Navigating Between Weeks

- Click **"◄ Previous Week"** to go back one week
- Click **"Next Week ►"** to go forward one week
- Use the date picker to jump to any specific week

### Filtering by Billability

**Visual indicator:** Non-billable task hours are shown in **red bold text** throughout the timesheet.

| Filter            | What it shows                                                |
| ----------------- | ------------------------------------------------------------ |
| **All** (default) | All tasks regardless of billability                          |
| **Billable**      | Only tasks where client, project, AND phase are all billable |
| **Non-Billable**  | Tasks where any component is marked non-billable             |

A task is billable only when all three components (client, project, and phase) are marked as billable. If any one is non-billable, the entire task is non-billable.

To filter: locate the **Billability** dropdown below the week navigation, select your option, and the timesheet updates automatically. Daily and weekly totals recalculate. The filter persists when navigating between weeks.

### Exporting the Timesheet to CSV

1. Display the week you want to export
2. Click **"Export CSV"** in the header
3. Choose:
    - **Copy to Clipboard**
    - **Download CSV** — filename: `Timesheet_Week_of_MM-DD-YYYY_to_MM-DD-YYYY.csv`
    - **Close**

**CSV columns:** Date, Client, Project, Phase, Hours, Task ID, Task Name, Task Details

---

## Managing Your Profile

All users can manage their own profile information.

### Accessing Your Profile

- Click **"My Profile"** on the dashboard
- Or select **"Update Profile"** from the ☰ sidebar menu

### Editing Your Profile

You can update:

- **First Name**
- **Last Name**
- **Company**
- **Email Address** — required if you have or will gain access to expense features

> Username, role, and account status are managed by administrators and cannot be changed here.

**To update:**

1. Open your profile
2. Modify the desired fields
3. Click **"Update Profile"**
4. A success message confirms your changes

---

## Changing Your Password

All users can change their password at any time.

### Password Requirements

Your new password must:

- Be at least **10 characters** long
- Contain at least one **uppercase letter** (A–Z)
- Contain at least one **number** (0–9)
- Contain at least one **special character**: `+ & % $ # @ ! ~ *`
- Not contain more than **2 consecutive identical characters** (e.g., `aaa` is not allowed)
- Not contain your **username** (regardless of capitalization)
- Not match your **current password**
- Not match any of your **previous 5 passwords**

### How to Change Your Password

**From your profile:**

1. Open your profile (see [Managing Your Profile](#managing-your-profile))
2. Click **"Update Password"**
3. In the dialog:
    - Enter your **Current Password**
    - Enter your **New Password** (requirements are checked as you type)
    - **Confirm** your new password
    - Use "Show passwords" to reveal what you are typing
4. Click **"Update"** to save

**When you change your password**, you are automatically logged out of all active sessions (including other devices) and must log in again with your new password.

### Forced Password Change

If your administrator has required a password change for your account:

- You will be redirected to the password change screen immediately after login
- You cannot access other features until the change is complete
- Message shown: _"Your administrator has required you to change your password"_
- After changing, you gain immediate full access

---

## Reports & Analytics

The Reports section provides interactive charts and visualizations to help you analyze your time tracking data.

### Accessing Reports

Navigate to **Reports** from the main menu.

### Date Range Filtering

All reports support flexible date range filtering. Options at the top of the page:

**Preset ranges:**

| Preset        | Period                                      |
| ------------- | ------------------------------------------- |
| This Month    | Current calendar month                      |
| Last Month    | Previous calendar month                     |
| Last 3 Months | Rolling 3-month period                      |
| This Year     | January 1 – December 31 of the current year |
| All Time      | All historical data                         |

**Custom Date Range:**

- Set a **Start Date** and/or **End Date** using the date pickers
- Click **Clear** to reset to the default (current month)

When you change the date range, all report tabs update automatically.

### Available Report Tabs

#### 1. Overview

Dashboard summary with key metrics for the selected period:

- Total hours for the period and the current week
- Top clients and projects
- Average hours per day
- Quick insights into your time allocation

#### 2. Client Analysis

- **Time distribution by client** (pie chart) — visual breakdown of time per client
- **Top activities** — most time-consuming tasks within the period

#### 3. Project Analysis

- **Time by project** (bar chart) — hours per project
- **Phase distribution** (donut chart) — time in each work phase (Development, Testing, etc.)

#### 4. Time Trends

- **Daily tracking** (line chart) — hours per day for the selected range
- **Weekly summary** — week-by-week comparison
- **Monthly comparison** (grouped bar chart) — month-over-month hours

### Report Tips

- **Interactive charts:** Hover over elements for exact values
- **Your data only:** Reports show only your own task activities
- **Real-time:** Charts always reflect your most current data
- Hours are displayed with two decimal places (e.g., `8.50`)

---

## Troubleshooting

### "I can't log in" — Account Locked

Use the **Reset Password** link on the login page. This both resets your password and unlocks your account automatically. If you have no email address on your profile, use **Contact System Admin** from the sidebar menu.

### "I forgot my password"

Use the **Reset Password** link on the login page. See [If You've Forgotten Your Password](#if-youve-forgotten-your-password) for step-by-step instructions.

### "I can't see a client or project I expect in the dropdown"

The task dropdowns show only clients and projects assigned to your account under **Task** access. Use **Contact System Admin** to request the appropriate items be added.

### "Cannot reuse any of your previous passwords" error

The system prevents reuse of your last 5 passwords. Choose a password you have not used recently. Consider a password manager to generate unique passwords.
