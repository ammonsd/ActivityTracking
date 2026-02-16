/**
 * Description: Database initialization data - inserts dropdown values, roles, permissions, and default users for the application
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

-- Insert dropdown values (skip if already exists)
-- Structure: category=TASK, subcategory=CLIENT/PROJECT/PHASE
-- This allows for future EXPENSE category with its own subcategories
INSERT INTO public.dropdownvalues 
(category, subcategory, itemvalue, displayorder, isactive, non_billable)
VALUES 
  -- TASK -> PHASE subcategory
  ('TASK', 'PHASE', 'Development', 1, true, false),
  ('TASK', 'PHASE', 'Holiday', 2, true, true),
  ('TASK', 'PHASE', 'Meeting', 3, true, false),
  ('TASK', 'PHASE', 'Miscellaneous', 4, true, false),
  ('TASK', 'PHASE', 'Code Review', 5, true, false),
  ('TASK', 'PHASE', 'PTO', 6, true, true),
  ('TASK', 'PHASE', 'Training', 7, true, false),
  -- TASK -> CLIENT subcategory
  ('TASK', 'CLIENT', 'Corporate', 1, true, true),
  -- TASK -> PROJECT subcategory
  ('TASK', 'PROJECT', 'General Administration', 1, true, false),
  ('TASK', 'PROJECT', 'Non-Billable', 2, true, true),
  
  -- EXPENSE -> EXPENSE_TYPE subcategory (Travel Expenses)
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Airfare', 1, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Hotel', 2, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Ground Transportation', 3, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Rental Car', 4, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Parking', 5, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Mileage', 6, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Meals (Client Meeting)', 7, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Meals (Travel Days)', 8, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Other', 9, true, false),
  
  -- EXPENSE -> EXPENSE_TYPE subcategory (Home Office Expenses)
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Internet', 10, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Phone/Mobile', 11, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Office Supplies', 12, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Equipment', 13, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Furniture', 14, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Software/Subscriptions', 15, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Utilities (Portion)', 16, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Other', 17, true, false),
  
  -- EXPENSE -> EXPENSE_TYPE subcategory (Other Business Expenses)
  ('EXPENSE', 'EXPENSE_TYPE', 'Training/Education', 18, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Professional Development', 19, true, false),
  ('EXPENSE', 'EXPENSE_TYPE', 'Miscellaneous', 20, true, false),
  
  -- EXPENSE -> PAYMENT_METHOD subcategory
  ('EXPENSE', 'PAYMENT_METHOD', 'Corporate Credit Card', 1, true, false),
  ('EXPENSE', 'PAYMENT_METHOD', 'Personal Credit Card', 2, true, false),
  ('EXPENSE', 'PAYMENT_METHOD', 'Cash', 3, true, false),
  ('EXPENSE', 'PAYMENT_METHOD', 'Check', 4, true, false),
  ('EXPENSE', 'PAYMENT_METHOD', 'Direct Debit', 5, true, false),
  ('EXPENSE', 'PAYMENT_METHOD', 'Reimbursement Due', 6, true, false),
  
  -- EXPENSE -> EXPENSE_STATUS subcategory
  ('EXPENSE', 'EXPENSE_STATUS', 'Draft', 1, true, false),
  ('EXPENSE', 'EXPENSE_STATUS', 'Submitted', 2, true, false),
  ('EXPENSE', 'EXPENSE_STATUS', 'Pending Approval', 3, true, false),
  ('EXPENSE', 'EXPENSE_STATUS', 'Approved', 4, true, false),
  ('EXPENSE', 'EXPENSE_STATUS', 'Rejected', 5, true, false),
  ('EXPENSE', 'EXPENSE_STATUS', 'Resubmitted', 6, true, false),
  ('EXPENSE', 'EXPENSE_STATUS', 'Reimbursed', 7, true, false),
  
  -- EXPENSE -> VENDOR subcategory
  ('EXPENSE', 'VENDOR', 'Amazon', 1, true, false),
  ('EXPENSE', 'VENDOR', 'Delta Airlines', 2, true, false),
  ('EXPENSE', 'VENDOR', 'United Airlines', 3, true, false),
  ('EXPENSE', 'VENDOR', 'Hilton', 4, true, false),
  ('EXPENSE', 'VENDOR', 'Marriott', 5, true, false),
  ('EXPENSE', 'VENDOR', 'Uber', 6, true, false),
  ('EXPENSE', 'VENDOR', 'Lyft', 7, true, false),
  ('EXPENSE', 'VENDOR', 'Enterprise', 8, true, false),
  ('EXPENSE', 'VENDOR', 'Hertz', 9, true, false),
  ('EXPENSE', 'VENDOR', 'Staples', 10, true, false),
  ('EXPENSE', 'VENDOR', 'Office Depot', 11, true, false),
  ('EXPENSE', 'VENDOR', 'Other', 12, true, false),
  
  -- EXPENSE -> RECEIPT_STATUS subcategory
  ('EXPENSE', 'RECEIPT_STATUS', 'No Receipt', 1, true, false),
  ('EXPENSE', 'RECEIPT_STATUS', 'Receipt Uploaded', 2, true, false),
  ('EXPENSE', 'RECEIPT_STATUS', 'Receipt Pending', 3, true, false),
  ('EXPENSE', 'RECEIPT_STATUS', 'Receipt Missing', 4, true, false),
  
  -- EXPENSE -> CURRENCY subcategory
  ('EXPENSE', 'CURRENCY', 'USD', 1, true, false),
  ('EXPENSE', 'CURRENCY', 'EUR', 2, true, false),
  ('EXPENSE', 'CURRENCY', 'GBP', 3, true, false),
  ('EXPENSE', 'CURRENCY', 'CAD', 4, true, false),
  ('EXPENSE', 'CURRENCY', 'AUD', 5, true, false),
  ('EXPENSE', 'CURRENCY', 'JPY', 6, true, false),
  ('EXPENSE', 'CURRENCY', 'CNY', 7, true, false),
  ('EXPENSE', 'CURRENCY', 'INR', 8, true, false),
  ('EXPENSE', 'CURRENCY', 'MXN', 9, true, false),
  
  -- EXPENSE -> CLIENT subcategory (shares with TASK)
  ('EXPENSE', 'CLIENT', 'Corporate', 1, true, true),
  
  -- EXPENSE -> PROJECT subcategory (shares with TASK)
  ('EXPENSE', 'PROJECT', 'General Administration', 1, true, false),
  ('EXPENSE', 'PROJECT', 'Non-Billable', 2, true, true)
ON CONFLICT (category, subcategory, itemvalue) DO NOTHING;

-- Insert existing roles
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Full system administrator with all permissions'),
    ('USER', 'Standard user with basic permissions'),
    ('GUEST', 'Guest user with read-only access'),
    ('EXPENSE_ADMIN', 'Administrator for expense-related features')
ON CONFLICT (name) DO NOTHING;

-- Define permissions for task activities
INSERT INTO permissions (resource, action, description) VALUES
    ('TASK_ACTIVITY', 'CREATE', 'Create new task activities'),
    ('TASK_ACTIVITY', 'READ', 'View task activities'),
    ('TASK_ACTIVITY', 'UPDATE', 'Modify existing task activities'),
    ('TASK_ACTIVITY', 'DELETE', 'Delete task activities'),
    ('TASK_ACTIVITY', 'READ_ALL', 'View all users task activities (admin)')
ON CONFLICT (resource, action) DO NOTHING;

-- Define permissions for user management
INSERT INTO permissions (resource, action, description) VALUES
    ('USER_MANAGEMENT', 'CREATE', 'Create new users'),
    ('USER_MANAGEMENT', 'READ', 'View user information'),
    ('USER_MANAGEMENT', 'UPDATE', 'Modify user information'),
    ('USER_MANAGEMENT', 'DELETE', 'Delete users'),
    ('USER_MANAGEMENT', 'MANAGE_ROLES', 'Assign roles to users')
ON CONFLICT (resource, action) DO NOTHING;

-- Define permissions for reports
INSERT INTO permissions (resource, action, description) VALUES
    ('REPORTS', 'VIEW', 'Access reports'),
    ('REPORTS', 'EXPORT', 'Export reports to file')
ON CONFLICT (resource, action) DO NOTHING;

-- Define permissions for expenses
INSERT INTO permissions (resource, action, description) VALUES
    ('EXPENSE', 'CREATE', 'Create new expenses'),
    ('EXPENSE', 'READ', 'View own expenses'),
    ('EXPENSE', 'READ_ALL', 'View all users expenses (admin)'),
    ('EXPENSE', 'UPDATE', 'Modify own expenses'),
    ('EXPENSE', 'DELETE', 'Delete own expenses'),
    ('EXPENSE', 'SUBMIT', 'Submit expenses for approval'),
    ('EXPENSE', 'APPROVE', 'Approve expense submissions'),
    ('EXPENSE', 'REJECT', 'Reject expense submissions'),
    ('EXPENSE', 'MARK_REIMBURSED', 'Mark expenses as reimbursed'),
    ('EXPENSE', 'MANAGE_RECEIPTS', 'Upload and manage receipt files')
ON CONFLICT (resource, action) DO NOTHING;

-- Define permissions for Jenkins CI/CD notifications
INSERT INTO permissions (resource, action, description) VALUES
    ('JENKINS', 'NOTIFY', 'Send build notifications from Jenkins CI/CD pipeline')
ON CONFLICT (resource, action) DO NOTHING;

-- Assign permissions to ADMIN role (has everything)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign permissions to USER role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'TASK_ACTIVITY' 
    AND p.action IN ('CREATE', 'READ', 'UPDATE', 'DELETE')
WHERE r.name = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Give USER role basic user management permissions (for profile self-service)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'USER_MANAGEMENT'
    AND p.action IN ('READ', 'UPDATE')
WHERE r.name = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Give USER role report viewing permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'REPORTS'
    AND p.action = 'VIEW'
WHERE r.name = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign permissions to GUEST role (all TASK_ACTIVITY functions for own tasks, no expenses)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (p.resource = 'TASK_ACTIVITY' AND p.action IN ('CREATE', 'READ', 'UPDATE', 'DELETE'))
    OR (p.resource = 'REPORTS' AND p.action = 'VIEW')
WHERE r.name = 'GUEST'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign permissions to EXPENSE_ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'EXPENSE'
WHERE r.name = 'EXPENSE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Give EXPENSE_ADMIN access to view task activities
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'TASK_ACTIVITY'
    AND p.action IN ('READ', 'READ_ALL')
WHERE r.name = 'EXPENSE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Give EXPENSE_ADMIN access to reports
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'REPORTS'
WHERE r.name = 'EXPENSE_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Give regular USER role standard expense permissions (create, read, update, delete, submit own)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.resource = 'EXPENSE'
    AND p.action IN ('CREATE', 'READ', 'UPDATE', 'DELETE', 'SUBMIT', 'MANAGE_RECEIPTS')
WHERE r.name = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;
