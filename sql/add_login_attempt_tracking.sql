-- Migration Script: Add Login Attempt Tracking and Account Lockout
-- Description: Adds security fields to prevent brute force attacks
-- Author: Dean Ammons
-- Date: 2025-11-12

-- Add failed login attempts counter
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;

-- Add account locked flag
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS account_locked BOOLEAN NOT NULL DEFAULT FALSE;

-- Add comments for documentation
COMMENT ON COLUMN users.failed_login_attempts IS 'Counter for failed login attempts - reset on successful login';
COMMENT ON COLUMN users.account_locked IS 'Account locked flag set when max failed attempts reached';

-- Create index for performance when checking locked accounts
CREATE INDEX IF NOT EXISTS idx_users_account_locked ON users(account_locked) WHERE account_locked = TRUE;

-- Log the migration
DO $$
BEGIN
    RAISE NOTICE 'Successfully added login attempt tracking columns to users table';
    RAISE NOTICE 'failed_login_attempts: tracks failed login count';
    RAISE NOTICE 'account_locked: flag to lock account after max attempts';
END $$;
