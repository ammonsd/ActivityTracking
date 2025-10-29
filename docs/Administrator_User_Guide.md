# Task Activity Management System - Administrator User Guide

## Welcome

This guide is for administrators of the Task Activity Management System. As an administrator, you have access to additional features for managing users, viewing all tasks, and configuring system settings.

## Administrator Features

### Managing Users

Administrators can create, edit, and delete user accounts:

1. **Access User Management**: Click **"👥 Manage Users"** from the header
2. **View All Users**: See a list of all system users with their full names, company, role, and last login time
3. **Filter Users**: Use the filter section to find specific users:
    - **Username**: Filter by username (partial match)
    - **Role**: Filter by user role (ADMIN, USER, VIEWER)
    - **Company**: Filter by company name (partial match)
    - Click **"Search"** to apply filters or **"Reset filters"** to clear
4. **Add New User**: Click **"Add User"** button
    - Enter username (required)
    - Enter first name (optional)
    - Enter last name (required)
    - Enter company (optional, maximum 100 characters)
    - Set initial password
    - Assign role (USER or ADMIN)
    - Enable/disable account
    - Optionally force password change on first login
5. **Edit Users**: Modify first name, last name, company, role, or account status
    - **Note**: Usernames are immutable and cannot be changed after account creation
    - If a username needs to be changed, deactivate the current account and create a new user with the desired username
    - All other user details (first name, last name, company, role, enabled status) can be edited at any time
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

### Changing User Passwords

1. **Access User Management**: Navigate to **"Manage Users"**
2. **Find User**: Locate the user in the list
3. **Click "Change Password"**: Opens password change form
4. **Enter New Password**: Type the new password (twice for confirmation)
5. **Show Passwords (Optional)**: Use eye icons or checkbox to view passwords
6. **Optional**: Check "Force password update on next login"
7. **Save**: Click **"Change Password"**

### Managing Dropdowns

1. **Access Dropdown Management**: Click **"🔧 Manage Dropdowns"**

2. **Select Category**:

    - **Clients**: Manage client list
    - **Projects**: Manage project names
    - **Phases**: Manage work phases

3. **Add New Values**:

    - Enter new value name
    - Click "Add" button
    - Value appears in dropdown immediately

4. **Edit Values**:

    - Click "Edit" next to the value
    - Update the name
    - Click "Update"

5. **Delete Values**:

    - Click "Delete" next to the value
    - Confirm deletion
    - Can't delete values that are in use by existing tasks
