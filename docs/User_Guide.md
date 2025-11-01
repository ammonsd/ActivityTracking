# Task Activity Management System - User Guide

## Welcome

Welcome to the Task Activity Management System! This application helps you track time spent on various tasks and projects. Whether you're logging daily work activities or reviewing your weekly timesheet, this guide will help you make the most of the system.

## Task Activity List

Tasks are displayed in a table with the following columns:

-   **Date**: When the task was performed
-   **Client**: The client the work was done for
-   **Project**: The specific project
-   **Phase**: The work phase (Development, Testing, etc.)
-   **Hours**: Time spent on the task
-   **Details**: Description of work performed
-   **Actions**: Edit, Delete, and Clone buttons

### Filtering Tasks

Use the filter controls at the top of the task list to narrow down your view:

1. **Client Filter**: Show only tasks for a specific client
2. **Project Filter**: Filter by project name
3. **Phase Filter**: Filter by work phase
4. **Start Date**: Show tasks from this date onward
5. **End Date**: Show tasks up to this date
6. **Username** (Admin only): Filter tasks by user

**To Apply Filters:**

-   Select your desired filter options
-   Click **"Filter"** button
-   To reset: Click **"Clear"** button

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

-   **Exports ALL filtered tasks** - not limited to the 20 visible on the current page
-   If there are 270 filtered tasks, all 270 will be included in the CSV
-   Filename includes active filters and timestamp
-   Example filename: `TaskActivity_Acme_Corp_Website_20251101.csv`
-   For admins: Includes username column in export

**CSV Format:**

```
Date,Client,Project,Phase,Hours,Details
10/28/2025,Acme Corp,Website Redesign,Development,8.00,Fixed login bug
10/29/2025,Acme Corp,Website Redesign,Testing,6.50,QA testing
```

**Note:** The export includes all tasks matching your current filters, regardless of pagination. If you see "Showing 1-20 of 270 entries", the CSV export will contain all 270 entries.

## Getting Started

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

## Managing Your Tasks

### Adding a New Task

1. **Access Task Entry Form**

    - After logging in, you'll see the task entry form
    - Or click **"Add New Task"** from any page

2. **Fill in Task Details**

    - **Task Date**: Click the calendar icon to select the date (required)
    - **Client**: Choose the client from the dropdown (required)
    - **Project**: Select the project you worked on (required)
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

-   **Week Range**: Start and end dates of the displayed week
-   **Daily Columns**: Monday through Sunday
-   **Your Tasks**: All tasks for that week, grouped by day
-   **Daily Totals**: Hours worked each day
-   **Weekly Total**: Total hours for the entire week

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

-   Date (MM/DD/YYYY)
-   Client name
-   Project name
-   Phase
-   Hours worked
-   Task details
-   Username

**Example CSV Output:**

```
Date,Client,Project,Phase,Hours,Task Details,Username
10/28/2025,Acme Corp,Website Redesign,Development,8.00,Fixed login bug,jsmith
10/29/2025,Acme Corp,Website Redesign,Testing,6.50,QA testing,jsmith
```

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
3. Follow the same steps as above (no current password needed)
4. After changing, you'll gain full access to the system
