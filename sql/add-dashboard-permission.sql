-- =============================================================================
-- Migration: Add DASHBOARD:VIEW Permission
--
-- Description: Adds the DASHBOARD:VIEW permission to control which users see
--              the Angular and React dashboard links in the sidebar menu.
--              By default, assigns this permission only to the ADMIN role.
--
--              To grant a non-admin user dashboard access, assign them a role
--              that has this permission, or create a dedicated role
--              (e.g. DASHBOARD_USER) and assign it this permission.
--
-- Author: Dean Ammons
-- Date: March 2026
-- =============================================================================

-- Step 1: Insert the permission (idempotent - safe to run multiple times)
INSERT INTO permissions (resource, action, description)
VALUES ('DASHBOARD', 'VIEW', 'Access to Angular and React dashboard views')
ON CONFLICT (resource, action) DO NOTHING;

-- Step 2: Grant the permission to the ADMIN role
--         Uses WHERE NOT EXISTS to avoid duplicate errors
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.resource = 'DASHBOARD'
  AND p.action   = 'VIEW'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- =============================================================================
-- OPTIONAL: Grant to additional roles
-- Uncomment and modify as needed.
--
-- Example: grant to a custom DASHBOARD_USER role
--
-- INSERT INTO role_permissions (role_id, permission_id)
-- SELECT r.id, p.id
-- FROM roles r
-- CROSS JOIN permissions p
-- WHERE r.name = 'DASHBOARD_USER'
--   AND p.resource = 'DASHBOARD'
--   AND p.action   = 'VIEW'
-- ON CONFLICT (role_id, permission_id) DO NOTHING;
-- =============================================================================
