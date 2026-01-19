---
name: database-migration
description: "Creates and applies database schema changes with proper ALTER statements, rollback scripts, and documentation. Handles PostgreSQL migrations safely"
---

# Database Migration Skill

This skill helps create safe database migrations for PostgreSQL schema changes.

## When to Use

- Adding new tables or columns
- Modifying existing schema
- Creating indexes for performance
- Data migrations or transformations

## Migration Principles

1. **Never destructive without confirmation** - Always confirm before DROP
2. **Always provide rollback** - Every migration has a reverse script
3. **Test on copy first** - Never run untested migrations on production
4. **Document impact** - Explain what changes and why

## Migration Template

### Forward Migration (apply changes)

```sql
-- Migration: [Brief description]
-- Author: Dean Ammons
-- Date: January 2026
-- Ticket/Issue: #123

-- Pre-migration validation
DO $$
BEGIN
    -- Check prerequisites
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        RAISE EXCEPTION 'Required table users does not exist';
    END IF;
END $$;

-- Migration starts here
BEGIN;

-- Add new column
ALTER TABLE users
ADD COLUMN phone_number VARCHAR(20);

-- Add index for performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_phone
ON users(phone_number);

-- Update existing data (if needed)
UPDATE users
SET phone_number = '+1-555-0000'
WHERE phone_number IS NULL;

-- Add constraint
ALTER TABLE users
ADD CONSTRAINT chk_phone_format
CHECK (phone_number ~ '^\+?[1-9]\d{1,14}$');

COMMIT;

-- Post-migration validation
SELECT
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'phone_number';
```

### Rollback Migration (undo changes)

```sql
-- Rollback Migration: [Brief description]
-- Author: Dean Ammons
-- Date: January 2026

BEGIN;

-- Remove constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_phone_format;

-- Drop index
DROP INDEX CONCURRENTLY IF EXISTS idx_users_phone;

-- Remove column
ALTER TABLE users DROP COLUMN IF EXISTS phone_number;

COMMIT;

-- Verify rollback
SELECT column_name FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'phone_number';
-- Should return 0 rows
```

## Common Migration Patterns

### Adding a New Table

```sql
-- Create table with proper structure
CREATE TABLE IF NOT EXISTS expense_categories (
    id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes
CREATE INDEX idx_expense_categories_active ON expense_categories(is_active);

-- Add initial data
INSERT INTO expense_categories (category_name, description) VALUES
('Travel', 'Business travel expenses'),
('Meals', 'Client meals and entertainment'),
('Office', 'Office supplies and equipment')
ON CONFLICT (category_name) DO NOTHING;

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON expense_categories TO taskactivity_app;
GRANT USAGE, SELECT ON SEQUENCE expense_categories_id_seq TO taskactivity_app;
```

### Adding a Foreign Key

```sql
-- Add column first
ALTER TABLE expenses
ADD COLUMN category_id BIGINT;

-- Populate with default values
UPDATE expenses
SET category_id = (SELECT id FROM expense_categories WHERE category_name = 'Other')
WHERE category_id IS NULL;

-- Add NOT NULL constraint
ALTER TABLE expenses
ALTER COLUMN category_id SET NOT NULL;

-- Add foreign key constraint
ALTER TABLE expenses
ADD CONSTRAINT fk_expenses_category
FOREIGN KEY (category_id)
REFERENCES expense_categories(id)
ON DELETE RESTRICT;

-- Add index on foreign key
CREATE INDEX idx_expenses_category ON expenses(category_id);
```

### Renaming a Column

```sql
-- Rename column
ALTER TABLE taskactivity
RENAME COLUMN hours TO billable_hours;

-- Update any views or functions that reference the old name
-- (Check first: SELECT * FROM pg_views WHERE definition LIKE '%hours%';)
```

### Adding an Index for Performance

```sql
-- Create index concurrently (doesn't lock table)
CREATE INDEX CONCURRENTLY idx_taskactivity_date_user
ON taskactivity(task_date, user_id);

-- Analyze table to update statistics
ANALYZE taskactivity;
```

## Migration File Organization

```
sql/migrations/
├── 001_add_phone_number_to_users.sql
├── 001_rollback_add_phone_number_to_users.sql
├── 002_create_expense_categories_table.sql
├── 002_rollback_create_expense_categories_table.sql
└── README.md  # Migration log
```

## Migration Checklist

### Before Running

- [ ] Migration tested on database copy
- [ ] Rollback script created and tested
- [ ] Impact analysis completed (which tables/rows affected)
- [ ] Downtime requirements identified
- [ ] Backup created (or verified backup exists)
- [ ] Team notified if production migration

### After Running

- [ ] Verify migration applied successfully
- [ ] Check application logs for errors
- [ ] Validate data integrity
- [ ] Update schema.sql with changes (for fresh installs)
- [ ] Document migration in git commit
- [ ] Update memory bank if architecture changes

## Testing Migrations

```powershell
# Create test database
psql -U postgres -c "CREATE DATABASE taskactivity_test;"

# Restore production backup to test
pg_restore -U postgres -d taskactivity_test latest_backup.dump

# Apply migration
psql -U postgres -d taskactivity_test -f sql/migrations/001_add_phone_number.sql

# Verify results
psql -U postgres -d taskactivity_test -c "\d users"

# Test rollback
psql -U postgres -d taskactivity_test -f sql/migrations/001_rollback_add_phone_number.sql

# Verify rollback
psql -U postgres -d taskactivity_test -c "\d users"

# Drop test database
psql -U postgres -c "DROP DATABASE taskactivity_test;"
```

## Production Migration Process

1. **Backup First**

    ```bash
    pg_dump -U postgres -d taskactivity -F c -f backup_pre_migration_$(date +%Y%m%d).dump
    ```

2. **Apply Migration**

    ```bash
    psql -U postgres -d taskactivity -f sql/migrations/001_migration.sql
    ```

3. **Verify Success**

    ```bash
    # Check migration results
    psql -U postgres -d taskactivity -c "SELECT * FROM schema_version;"
    ```

4. **Monitor Application**
    - Check application logs
    - Monitor error rates
    - Verify functionality

5. **Rollback if Needed**
    ```bash
    psql -U postgres -d taskactivity -f sql/migrations/001_rollback.sql
    ```

## Common Issues

### Issue: "relation already exists"

**Solution:** Use `IF NOT EXISTS` or check if already applied

### Issue: "cannot drop column because other objects depend on it"

**Solution:** Find dependencies with:

```sql
SELECT
    'VIEW' AS object_type,
    v.table_name AS dependent_object
FROM information_schema.view_column_usage v
WHERE v.column_name = 'column_name' AND v.table_name = 'table_name'
UNION
SELECT
    'CONSTRAINT' AS object_type,
    tc.constraint_name
FROM information_schema.constraint_column_usage ccu
JOIN information_schema.table_constraints tc ON ccu.constraint_name = tc.constraint_name
WHERE ccu.column_name = 'column_name' AND ccu.table_name = 'table_name';
```

### Issue: Migration times out

**Solution:**

- Break into smaller migrations
- Use `CREATE INDEX CONCURRENTLY`
- Schedule during low-traffic period

## Memory Bank References

- Check `ai/architecture-patterns.md` for database schema details
- Check `ai/project-overview.md` for entity relationships
- Check `sql/production_optimization.sql` for index examples

## Migration Documentation Template

```markdown
# Migration: Add Phone Number to Users

**Date:** January 19, 2026
**Author:** Dean Ammons
**Ticket:** #123

## Purpose

Add phone number field to users table for SMS notifications.

## Impact

- Table: `users`
- Estimated rows affected: 150
- Downtime required: No
- Rollback available: Yes

## Changes

- Add column: `phone_number VARCHAR(20)`
- Add index: `idx_users_phone`
- Add constraint: Phone number format validation

## Risks

- Low risk - nullable column, no data migration required

## Rollback

Run: `001_rollback_add_phone_number_to_users.sql`

## Post-Migration Tasks

- [ ] Update UserService to handle phone numbers
- [ ] Update user profile UI to include phone field
- [ ] Update API documentation
```
