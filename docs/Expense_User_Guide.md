<!--
  Description: Task Activity Management System — user guide for expense management features.

  Author: Dean Ammons
  Date: February 2026
-->

# Task Activity Management System — Expense User Guide

## Welcome

Welcome to the Expense Management section of the Task Activity Management System! This guide covers everything you need to record business expenses, upload receipts, submit expenses for approval, and track reimbursement status.

> **Separate guide available:** If you also have access to task tracking features, see the [Task User Guide](Task_User_Guide.md).

**Features Covered in This Guide:**

- **Expense Management**: Record and submit business expenses with receipt attachments
- **Approval Workflows**: Submit expenses for review and track their approval status
- **Weekly Expense Sheet**: View a consolidated week-by-week view of your expenses
- **CSV Export**: Export expense lists for use in spreadsheets or accounting tools

---

## Getting Started

### User Roles and Expense Permissions

The system uses a role-based access control model. The roles relevant to expense management are:

**User (Standard Access)**

- ✅ Create, edit, clone, and delete your own draft expenses
- ✅ Upload receipts for expenses
- ✅ Submit expenses for approval
- ✅ View your expense approval history and status
- ✅ Access weekly expense sheet
- ✅ Export your expenses to CSV

**Expense Administrator (Expense Approver)**

- ✅ All User permissions for expenses
- ✅ View all submitted expenses across all users
- ✅ Approve or reject expense submissions
- ✅ Mark expenses as reimbursed
- ✅ View and record approval notes

**Email Requirement**

You **must have a valid email address** configured in your profile to access expense features:

- Without an email, the **"Add New Expense"** button is hidden and expense pages are inaccessible
- Email is required so you receive approval/rejection/reimbursement notifications
- To enable expense access, add your email in your profile (see [Managing Your Profile](#managing-your-profile))

> **Note:** Use the **Contact System Admin** option in the sidebar menu if you need different access permissions or are missing an expected client or project in your expense dropdowns.

### Accessing the Application

Open your web browser and navigate to:

```
https://taskactivitytracker.com
```

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
3. You will gain immediate access to the system

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

### Password Expiration Warnings

Passwords expire every **90 days**. Starting 7 days before expiry you will see a banner:

```
⚠️ Your password will expire in X day(s). Please change it soon.
```

At expiry you will be redirected to the password change screen before you can access the system.

---

## Managing Your Profile

All users can manage their own profile information. **Adding your email address is required to access expense features.**

### Accessing Your Profile

- Click **"My Profile"** on the dashboard
- Or select **"Update Profile"** from the ☰ sidebar menu

### Editing Your Profile

You can update:

- **First Name**
- **Last Name**
- **Company**
- **Email Address** — **required for all expense management features**

> Username, role, and account status are managed by administrators and cannot be changed here.

**To update:**

1. Open your profile
2. Modify the desired fields
3. Click **"Update Profile"**
4. A success message confirms your changes

### Email Requirement for Expense Access

Without an email address on your profile:

- ❌ The "Add New Expense" button is hidden
- ❌ You cannot create, view, or manage expenses
- ❌ You cannot submit expenses for approval
- Approval/rejection/reimbursement email notifications cannot be delivered

Add your email address to your profile to enable full expense access.

---

## Changing Your Password

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
- After changing, you gain immediate full access

---

## Expense Tracking

### Expense List

After navigating to the Expenses section, your expense list displays all your recorded expenses with the following columns:

| Column      | Description                                                       |
| ----------- | ----------------------------------------------------------------- |
| **Date**    | Date of the expense                                               |
| **Client**  | Client associated with the expense                                |
| **Project** | Project the expense is related to                                 |
| **Type**    | Expense category (Travel, Home Office, etc.)                      |
| **Amount**  | Amount — non-billable amounts are shown in **red bold text**      |
| **Status**  | Workflow status: Draft, Submitted, Approved, Rejected, Reimbursed |
| **Actions** | Edit, Clone, and Delete buttons                                   |

### Sidebar Menu

The Expense List page includes a **floating sidebar menu** for quick access to features.

**To open:** Click **☰** in the upper-right corner.  
**To close:** Click **✕** or click ☰ again.

**Menu items:**

- **Export CSV** — export your filtered expense list to CSV
- **Contact System Admin** — send a message to the administrator without leaving the application

### Contacting the System Administrator

Use **Contact System Admin** to request changes (e.g., add a client or project to your expense access) or ask questions.

1. Click ☰ to open the sidebar, then click **Contact System Admin**
2. Enter a **Subject** and a **Message**
3. Click **Send**

The administrator receives an email with your username, subject, message, and timestamp. If you have an email on your profile, it is included so the administrator can reply to you directly.

### Filtering Expenses

Use the filter controls at the top of the expense list:

1. **Client Filter** — show expenses for a specific client
2. **Project Filter** — filter by project name
3. **Expense Type** — filter by expense category
4. **Status** — filter by workflow status
5. **Payment Method** — filter by payment type
6. **Start Date / End Date** — filter by expense date range

Filters apply automatically. Click **"Reset Filters"** to clear all filters.

---

## Creating a New Expense

1. Click **"Add Expense"** from the navigation

2. **Fill in required fields:**
    - **Expense Date** — date you incurred the expense (required)
    - **Client** — client associated with this expense (required)
    - **Project** — project related to the expense (optional)

        > The Client and Project dropdowns for expenses show only values your administrator has assigned to your account under **Expense** access, plus values flagged as visible to all users. Task and Expense access are configured independently — your task and expense clients/projects may differ. Contact your administrator if an expected item is missing.

    - **Expense Type** — category from dropdown: Travel - Airfare, Hotel, Home Office Equipment, etc. (required)
    - **Description** — what the expense was for (required)
    - **Amount** — cost of the expense (required)
    - **Currency** — default is USD (optional)
    - **Payment Method** — Personal Credit Card, Cash, etc. (required)

3. **Fill in optional fields:**
    - **Vendor** — name of the merchant
    - **Reference Number** — receipt number, confirmation code, or invoice number
    - **Notes** — additional information or justification

4. **Upload a receipt (recommended):**
    - Click **"Choose File"** next to Receipt
    - Select an image file (JPEG, PNG, or PDF only; **maximum 10 MB**)
    - The receipt is uploaded and attached to the expense
    - If you see **"File size exceeds maximum"**, reduce the file size or resolution
    - If you see **"Invalid file type"**, ensure the file is a genuine JPEG, PNG, or PDF (not just renamed)

5. Click **"Save Expense"** — the expense is saved with status **Draft**

---

## Editing an Expense

1. Find the expense in your list
2. Click the **edit** (pencil) icon
3. Update any fields as needed
4. Click **"Update Expense"**

> **Note:** You can only edit expenses with status **Draft**. Once submitted, expenses cannot be edited.

---

## Cloning an Expense

Cloning copies an expense's fields so you can quickly create a similar entry.

1. Click the **clone** icon on the expense row
2. All fields except the date and receipt are pre-filled from the original
3. Update the date, amount, description, and upload a receipt for the new expense
4. Click **"Save Expense"**

**Note — Inactive dropdown values:** If the original expense references a Client, Project, Expense Type, Payment Method, Vendor, or Currency that has been marked inactive by an administrator, that field will be cleared in the clone form. Select an active value before saving.

---

## Deleting an Expense

1. Find the expense in your list
2. Click the **delete** icon
3. Confirm the deletion

> **Note:** Only **Draft** expenses can be deleted. Submitted expenses cannot be deleted.

---

## Submitting an Expense for Approval

1. Ensure the draft expense is complete and a receipt is attached
2. Click **"Submit for Approval"** on the expense
3. The status changes to **Submitted**
4. Your expense approver(s) receive an email notification with:
    - Your full name and username
    - Expense ID, description, amount, and date
    - Instructions to review the expense

> **Once submitted, you cannot edit or delete the expense.**

### Email Notifications You Will Receive

After submitting, you receive automatic email notifications at each status change:

| Status Change  | What the email tells you                         |
| -------------- | ------------------------------------------------ |
| **Approved**   | Expense is ready for reimbursement processing    |
| **Rejected**   | Includes reviewer notes explaining the rejection |
| **Reimbursed** | Confirms payment has been processed              |

Each notification includes: your name, expense description, amount and currency, new status, processor's name, and date/time.

> Emails go to the address on your profile. Keep your email address current.

---

## Understanding Expense Status

| Status         | Meaning                                 | Can Edit/Delete? |
| -------------- | --------------------------------------- | ---------------- |
| **Draft**      | Initial state after creation            | ✅ Yes           |
| **Submitted**  | Awaiting approval                       | ❌ No            |
| **Approved**   | Approved; ready for reimbursement       | ❌ No            |
| **Rejected**   | Not approved — read the rejection notes | ❌ No            |
| **Reimbursed** | Payment processed; workflow complete    | ❌ No            |

**If rejected:** Read the rejection notes carefully, create a corrected expense, and resubmit.

---

## Viewing Expense Details

1. Click anywhere on the expense row
2. View all fields, notes, and approval history
3. Click the receipt thumbnail to view the full-size image
4. Download the receipt for your records

---

## Weekly Expense Sheet

The weekly expense sheet shows all your expenses grouped by week.

### Accessing the Expense Sheet

Navigate to **Weekly Expenses** from the main navigation menu.

### Reading the Expense Sheet

- **Week Range** — start and end dates of the displayed week
- **Daily columns** — your expenses grouped by day
- **Daily Totals** — total expense amount per day
- **Weekly Total** — total for the entire week

**Visual indicator:** Non-billable expense amounts are shown in **red bold text** throughout the sheet.

### Navigating Between Weeks

- Click **"◄ Previous Week"** to go back
- Click **"Next Week ►"** to go forward
- Use the date picker to jump to any specific week

### Filtering by Billability

| Filter            | What it shows                                                          |
| ----------------- | ---------------------------------------------------------------------- |
| **All** (default) | All expenses regardless of billability                                 |
| **Billable**      | Only expenses where client, project, AND expense type are all billable |
| **Non-Billable**  | Expenses where any component is marked non-billable                    |

An expense is billable only when all three components (client, project, and expense type) are marked as billable.

To filter: locate the **Billability** dropdown below the week navigation and select an option. Expense totals recalculate automatically. The filter persists when navigating between weeks.

---

## Exporting Expenses to CSV

1. Apply filters (optional) to scope the expenses you want to export
2. Click ☰ to open the sidebar menu, then click **Export CSV**
3. Choose:
    - **Copy to Clipboard** — paste into Excel, email, etc.
    - **Download CSV** — saves a `.csv` file
    - **Close** — close without exporting

**Notes:**

- The export includes **all filtered expenses**, not just the current page
- Filename format: `Expenses_YYYYMMDD.csv`

---

## Billable vs. Non-Billable Expenses

The system uses a flag-based approach to determine billability:

- Each dropdown value (client, project, expense type) has a **Non-Billable** flag
- An expense is **billable** only if its client, project, AND expense type are all marked as billable
- If **any one** component is non-billable, the entire expense is non-billable
- **Non-billable amounts** are shown in red bold text as a visual reminder

**Why it matters:**

- Billable expenses may be charged back to clients
- Non-billable expenses (overhead, internal, non-reimbursable) are tracked separately
- Use the billability filter in the weekly expense sheet to generate billing-ready views

---

## Troubleshooting

### "The Add New Expense button is not visible"

Your user profile does not have an email address configured. Add your email in your profile (see [Managing Your Profile](#managing-your-profile)). Expense features require a valid email address.

### "I can't see a client or project I expect in the expense dropdown"

Expense dropdowns show only clients and projects assigned to your account under **Expense** access. This is configured independently from task access — contact your administrator to request the appropriate items be added to your expense access.

### "Invalid file type" error when uploading a receipt

- Verify the file is a genuine JPEG, PNG, or PDF (not just renamed)
- Open the file on your computer to confirm it is a valid image or PDF
- Re-save the image using an image editor and export it as JPEG or PNG
- If it is a PDF, ensure it was created by a legitimate PDF application
- The system validates the actual file content, not just the file extension

### "File size exceeds maximum" error when uploading a receipt

- Maximum file size is **10 MB**
- Reduce the image resolution or compress the file before uploading
- For PDFs, try re-saving at a lower quality setting

### "I submitted an expense but I can't edit it"

Submitted expenses are locked to preserve the approval record. To make corrections:

1. Contact your expense approver and ask them to reject it with a note explaining the change needed
2. After rejection, create a new corrected expense and resubmit

### "I didn't receive an approval/rejection/reimbursement email"

1. Check your spam/junk folder
2. Verify your email address is correctly set in your profile (see [Managing Your Profile](#managing-your-profile))
3. Contact your administrator if the address is correct but emails are still not arriving

### "Cannot reuse any of your previous passwords" error

The system prevents reuse of your last 5 passwords. Choose a password you have not used recently. Consider using a password manager to generate unique passwords.
