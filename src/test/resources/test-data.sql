/**
 * Description: Test data for H2 database - provides test data for unit and integration tests
 *
 * Author: Dean Ammons
 * Date: December 2025
 */

-- Test data for H2 database using database-driven authorization
-- Based on main data.sql but simplified for testing

-- Insert Roles
INSERT INTO roles (id, name, description, created_date) VALUES
(1, 'ADMIN', 'Full system administrator with all permissions', CURRENT_TIMESTAMP),
(2, 'USER', 'Standard user with task and expense management', CURRENT_TIMESTAMP),
(3, 'GUEST', 'Limited read-only access', CURRENT_TIMESTAMP),
(4, 'EXPENSE_ADMIN', 'Expense approval and management', CURRENT_TIMESTAMP);

-- Insert Permissions
INSERT INTO permissions (id, resource, action, description, created_date) VALUES
-- Task Activity Permissions
(1, 'TASK_ACTIVITY', 'CREATE', 'Create new task activities', CURRENT_TIMESTAMP),
(2, 'TASK_ACTIVITY', 'READ', 'View own task activities', CURRENT_TIMESTAMP),
(3, 'TASK_ACTIVITY', 'READ_ALL', 'View all users task activities', CURRENT_TIMESTAMP),
(4, 'TASK_ACTIVITY', 'UPDATE', 'Update own task activities', CURRENT_TIMESTAMP),
(5, 'TASK_ACTIVITY', 'DELETE', 'Delete own task activities', CURRENT_TIMESTAMP),
-- Expense Permissions
(6, 'EXPENSE', 'CREATE', 'Create new expenses', CURRENT_TIMESTAMP),
(7, 'EXPENSE', 'READ', 'View own expenses', CURRENT_TIMESTAMP),
(8, 'EXPENSE', 'READ_ALL', 'View all expenses', CURRENT_TIMESTAMP),
(9, 'EXPENSE', 'UPDATE', 'Update own expenses', CURRENT_TIMESTAMP),
(10, 'EXPENSE', 'DELETE', 'Delete own expenses', CURRENT_TIMESTAMP),
(11, 'EXPENSE', 'SUBMIT', 'Submit expenses for approval', CURRENT_TIMESTAMP),
(12, 'EXPENSE', 'APPROVE', 'Approve expense requests', CURRENT_TIMESTAMP),
(13, 'EXPENSE', 'REJECT', 'Reject expense requests', CURRENT_TIMESTAMP),
(14, 'EXPENSE', 'MARK_REIMBURSED', 'Mark expenses as reimbursed', CURRENT_TIMESTAMP),
(15, 'EXPENSE', 'MANAGE_RECEIPTS', 'Upload and manage receipts', CURRENT_TIMESTAMP),
-- User Management Permissions
(16, 'USER_MANAGEMENT', 'CREATE', 'Create new users', CURRENT_TIMESTAMP),
(17, 'USER_MANAGEMENT', 'READ', 'View user information', CURRENT_TIMESTAMP),
(18, 'USER_MANAGEMENT', 'UPDATE', 'Update user information', CURRENT_TIMESTAMP),
(19, 'USER_MANAGEMENT', 'DELETE', 'Delete users', CURRENT_TIMESTAMP),
(20, 'USER_MANAGEMENT', 'MANAGE_ROLES', 'Manage user roles and permissions', CURRENT_TIMESTAMP),
-- Reports Permissions
(21, 'REPORTS', 'VIEW', 'View reports', CURRENT_TIMESTAMP),
(22, 'REPORTS', 'GENERATE', 'Generate new reports', CURRENT_TIMESTAMP);

-- Assign Permissions to ADMIN role (all permissions)
INSERT INTO role_permissions (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5),
(1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(1, 11), (1, 12), (1, 13), (1, 14), (1, 15),
(1, 16), (1, 17), (1, 18), (1, 19), (1, 20),
(1, 21), (1, 22);

-- Assign Permissions to USER role (task and expense management)
INSERT INTO role_permissions (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 4), (2, 5),  -- Task: CREATE, READ, UPDATE, DELETE
(2, 6), (2, 7), (2, 9), (2, 10), (2, 11), (2, 15),  -- Expense: CREATE, READ, UPDATE, DELETE, SUBMIT, MANAGE_RECEIPTS
(2, 17), (2, 18),  -- User Management: READ, UPDATE (for self-service profile management)
(2, 21);  -- Reports: VIEW

-- Assign Permissions to GUEST role (read-only)
INSERT INTO role_permissions (role_id, permission_id) VALUES
(3, 2),  -- Task: READ
(3, 21); -- Reports: VIEW

-- Assign Permissions to EXPENSE_ADMIN role
INSERT INTO role_permissions (role_id, permission_id) VALUES
(4, 2), (4, 3),  -- Task: READ, READ_ALL
(4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (4, 11), (4, 12), (4, 13), (4, 14), (4, 15),  -- All Expense permissions
(4, 21), (4, 22);  -- Reports: VIEW, GENERATE

-- Insert test users (password is 'password' for all)
INSERT INTO users (username, userpassword, firstname, lastname, email, role_id, enabled, forcepasswordupdate, account_locked, failed_login_attempts, created_date) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Admin', 'User', 'admin@example.com', 1, true, false, false, 0, CURRENT_TIMESTAMP),
('user', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Regular', 'User', 'user@example.com', 2, true, false, false, 0, CURRENT_TIMESTAMP),
('guest', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Guest', 'User', 'guest@example.com', 3, true, false, false, 0, CURRENT_TIMESTAMP),
('expenseadmin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Expense', 'Admin', 'expenseadmin@example.com', 4, true, false, false, 0, CURRENT_TIMESTAMP),
('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Test', 'User', 'testuser@example.com', 2, true, false, false, 0, CURRENT_TIMESTAMP),
('otheruser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'Other', 'User', 'otheruser@example.com', 2, true, false, false, 0, CURRENT_TIMESTAMP);

-- Insert dropdown values for testing
INSERT INTO dropdownvalues (category, subcategory, itemvalue, displayorder, isactive, non_billable) VALUES
('TASK', 'CLIENT', 'Test Client', 1, true, false),
('TASK', 'PROJECT', 'Test Project', 1, true, false),
('TASK', 'PHASE', 'Test Phase', 1, true, false),
('TASK', 'CLIENT', 'Client A', 2, true, false),
('TASK', 'PROJECT', 'Project X', 2, true, false);