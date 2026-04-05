-- =============================================================================
-- Migration: Add UNIQUE constraint to users.email
--
-- Description: Enforces email address uniqueness at the database level.
--              Prevents multiple accounts from sharing the same email, which
--              breaks the Forgot Password flow (findByEmail expects one result).
--
-- Author: Dean Ammons
-- Date: April 2026
-- =============================================================================

-- PREREQUISITE: Resolve any duplicate emails before running this script.
-- Use the query below to confirm no duplicates remain:
SELECT email, COUNT(*) AS cnt
FROM public.users
WHERE email IS NOT NULL AND email <> ''
GROUP BY email
HAVING COUNT(*) > 1;

-- If the above query returns rows, fix them first, then run the statement below.

-- Add the unique constraint
ALTER TABLE public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);
