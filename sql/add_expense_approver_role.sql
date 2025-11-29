-- Add EXPENSE_APPROVER role documentation
-- This role allows users to approve/reject expenses without full admin privileges

-- The EXPENSE_APPROVER role provides:
-- 1. Access to the Expense Approval Queue
-- 2. Ability to approve or reject expense submissions
-- 3. View all submitted expenses for approval

-- The EXPENSE_APPROVER role does NOT provide:
-- 1. User management capabilities
-- 2. Dropdown value management
-- 3. System configuration access
-- 4. Access to other admin functions

-- NOTE: The role value in the database should be stored WITHOUT the "ROLE_" prefix
-- Spring Security will automatically add "ROLE_" prefix when checking authorities
-- Example: Database stores "EXPENSE_APPROVER", Spring checks "ROLE_EXPENSE_APPROVER"

-- To assign EXPENSE_APPROVER role to a user:
-- UPDATE users SET role = 'EXPENSE_APPROVER' WHERE username = 'manager1';

-- To verify users with EXPENSE_APPROVER role:
-- SELECT username, firstname, lastname, email, role, enabled 
-- FROM users 
-- WHERE role = 'EXPENSE_APPROVER' 
-- ORDER BY lastname, firstname;

-- Example: Create a sample expense approver user
-- INSERT INTO users (username, password, role, enabled, firstname, lastname, email, created_date)
-- VALUES (
--     'expense.approver',
--     '$2a$10$...',  -- Use bcrypt encoded password
--     'EXPENSE_APPROVER',
--     true,
--     'Expense',
--     'Approver',
--     'approver@example.com',
--     CURRENT_TIMESTAMP
-- );

-- Roles in the system:
-- ADMIN             - Full system access (all features including user management)
-- EXPENSE_APPROVER  - Expense approval only (no admin functions)
-- USER              - Regular user (can submit expenses, track tasks)
-- GUEST             - Limited access (task tracking only, read-only for some features)
