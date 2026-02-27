-- =============================================================================
-- PostgreSQL Database User Management
-- File: create-readonly-user.sql
--
-- Author: Dean Ammons
-- Date: February 2026
--
-- PURPOSE
-- -------
-- This script creates and configures database users (roles) on the RDS
-- PostgreSQL instance. It is idempotent — safe to run multiple times.
-- Running it again will not error; it will update the password if the
-- role already exists.
--
-- This is the primary reference for all database user setup and management.
--
-- DATABASE INFO
-- -------------
-- Host:     taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com
-- Port:     5432
-- Database: AmmoP1DB
-- Schema:   public
-- Master:   postgres
--
-- EXISTING USERS
-- --------------
-- postgres              - Master user (AWS RDS). Full read/write/DDL.
--                         Password managed separately; never stored in scripts.
-- taskactivity_readonly - Application read-only user. SELECT only.
--                         Password: stored encrypted; see localdocs/secrets.
--                         Used by: connect-to-rds.ps1 (default non-admin session)
--
-- HOW TO RUN THIS SCRIPT
-- ----------------------
-- 1. From your local machine, start an admin ECS session:
--       .\scripts\connect-to-rds.ps1 -Admin
--    Enter the postgres master password when prompted.
--
-- 2. Inside the ECS psql session, paste the SQL below directly, or use:
--       \i /tmp/create-readonly-user.sql
--    (copy the file to /tmp inside the container first if using \i)
--
-- 3. The SELECT at the bottom verifies grants were applied correctly.
--    You should see one row per table for each privilege granted.
--
-- PASSWORD ROTATION
-- -----------------
-- To change a user's password, simply re-run this script with the new
-- password in the DO block, or run directly:
--
--   ALTER ROLE taskactivity_readonly WITH PASSWORD 'NewPassword';
--   ALTER ROLE postgres WITH PASSWORD 'NewPassword';
--
-- Passwords should avoid ! (bash history expansion) and unquoted # (bash
-- comment character) when passed on the command line. Wrapping in single
-- quotes in bash avoids most issues.
--
-- ADDING A NEW DATABASE USER
-- --------------------------
-- Copy the DO block and GRANT statements below, substituting the new role
-- name, password, and desired privileges. Common privilege sets:
--
--   Read-only (SELECT only):
--     GRANT SELECT ON ALL TABLES IN SCHEMA public TO newrole;
--     GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO newrole;
--
--   Read/Write (no schema changes):
--     GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO newrole;
--     GRANT USAGE, UPDATE ON ALL SEQUENCES IN SCHEMA public TO newrole;
--
--   Single table access:
--     GRANT SELECT ON TABLE public.taskactivity TO newrole;
--
--   Reporting (specific tables only):
--     GRANT SELECT ON TABLE public.taskactivity, public.users, public.expenses TO newrole;
--
-- Always include the ALTER DEFAULT PRIVILEGES block for any role that
-- needs access to tables created in the future. Without it, new tables
-- require a manual re-grant after creation.
--
-- REMOVING A USER
-- ---------------
--   REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM oldrole;
--   REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM oldrole;
--   REVOKE USAGE ON SCHEMA public FROM oldrole;
--   REVOKE CONNECT ON DATABASE "AmmoP1DB" FROM oldrole;
--   DROP ROLE oldrole;
--
-- SSL REQUIREMENT
-- ---------------
-- RDS pg_hba.conf requires SSL for all non-master users (hostssl rule).
-- Always include PGSSLMODE=require when connecting as any role other than
-- postgres. The connect-to-rds.ps1 script handles this automatically.
-- Manual psql example:
--   PGPASSWORD='...' PGSSLMODE=require psql -h <host> -p 5432 -U taskactivity_readonly -d AmmoP1DB
--
-- TABLES CURRENTLY IN DATABASE (as of February 2026)
-- ---------------------------------------------------
--   dropdownvalues, expenses, password_history, permissions,
--   revoked_tokens, role_permissions, roles, taskactivity,
--   user_dropdown_access, users
-- =============================================================================


-- -----------------------------------------------------------------------------
-- ROLE: taskactivity_readonly
-- Access: SELECT only on all tables and sequences in public schema
-- Used by: connect-to-rds.ps1 default (non-admin) sessions
-- -----------------------------------------------------------------------------

-- Create the role if it does not exist; update password if it does.
-- This makes the script safe to re-run for password rotation.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'taskactivity_readonly') THEN
        CREATE ROLE taskactivity_readonly WITH LOGIN PASSWORD 'TaskActivity_RO_2026#';
    ELSE
        ALTER ROLE taskactivity_readonly WITH PASSWORD 'TaskActivity_RO_2026#';
    END IF;
END
$$;

-- Required: allows the role to open a connection to the database at all.
-- Without this, all connection attempts fail regardless of other grants.
GRANT CONNECT ON DATABASE "AmmoP1DB" TO taskactivity_readonly;

-- Required: allows the role to see and reference objects inside the schema.
-- Without USAGE, the role cannot query tables even if SELECT is granted.
GRANT USAGE ON SCHEMA public TO taskactivity_readonly;

-- Grant SELECT on all tables that exist right now in the public schema.
GRANT SELECT ON ALL TABLES IN SCHEMA public TO taskactivity_readonly;

-- Grant SELECT on sequences. Required for queries that reference serial/
-- identity columns (e.g., currval, lastval) even in read-only contexts.
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO taskactivity_readonly;

-- Automatically grant SELECT on any tables/sequences created in the future
-- by the postgres role. Without this, every new table requires a manual
-- re-grant after it is created.
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT ON TABLES TO taskactivity_readonly;

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public
    GRANT SELECT ON SEQUENCES TO taskactivity_readonly;


-- -----------------------------------------------------------------------------
-- VERIFICATION
-- Run after applying grants to confirm everything is in place.
-- Expected: one row per table (10 tables × SELECT = 10 rows minimum)
-- -----------------------------------------------------------------------------
SELECT
    grantee,
    table_schema,
    table_name,
    privilege_type
FROM information_schema.role_table_grants
WHERE grantee = 'taskactivity_readonly'
ORDER BY table_name;
