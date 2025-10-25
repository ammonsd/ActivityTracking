# Restore AWS RDS Database from Backup

**TESTED PROCEDURE - Successfully recovered 3 users, 12 dropdown values, and 271 task activities on October 24, 2025**

This guide documents the complete, tested process for restoring your PostgreSQL RDS database from an AWS automated backup after data loss due to schema changes. This procedure was successfully used to recover from a production incident.

## Overview

When Hibernate's `ddl-auto=update` encounters major schema changes (table renames, schema changes from `dbo` to `public`), it may create new empty tables instead of migrating existing data. This procedure recovers data from AWS RDS automated backups and migrates it to the new schema.

**Time Required:** 1-2 hours  
**Cost:** ~$0.50-$1.00 (temporary restored instance for 1-2 hours)  
**Prerequisites:**

-   AWS CLI installed and configured
-   Python 3.x with psycopg2 installed (`pip install psycopg2-binary`)
-   IAM permissions (see Required IAM Permissions below)

## Required IAM Permissions

Your IAM user needs these permissions. Add them to your policy before starting:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "rds:DescribeDBSnapshots",
                "rds:DescribeDBInstances",
                "rds:CreateDBSnapshot",
                "rds:RestoreDBInstanceFromDBSnapshot",
                "rds:DescribeDBSubnetGroups",
                "rds:ListTagsForResource",
                "rds:AddTagsToResource",
                "rds:ModifyDBInstance",
                "rds:DeleteDBInstance"
            ],
            "Resource": "*"
        }
    ]
}
```

## Step 1: Identify the Correct Backup Snapshot

**CRITICAL:** Choose a snapshot from BEFORE the schema changes were deployed.

### Using the Automated Script (Recommended)

```powershell
cd C:\Users\deana\GitHub\ActivityTracking
.\scripts\restore-aws-database-simple.ps1
```

This interactive script will:

1. Verify AWS CLI is installed
2. List your RDS instances
3. Show available snapshots sorted by date (newest first)
4. Create a safety snapshot of current state
5. Guide you through the restore process

### Manual Method

List snapshots manually:

```powershell
# List all snapshots for your database
aws rds describe-db-snapshots `
  --db-instance-identifier taskactivity-db `
  --query "DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime,SnapshotType]" `
  --output table
```

**What to look for:**

-   `[AUTO]` snapshots are automated backups (7-day retention by default)
-   Find one from BEFORE you deployed the schema changes
-   Note the exact `DBSnapshotIdentifier` (e.g., `rds:taskactivity-db-2025-10-23-03-08`)

**Common Mistake:** Don't select the most recent snapshot if the schema changes have already been deployed - it will have the new schema with no data!

## Step 2: Create Safety Snapshot

Before making any changes, snapshot your current database state:

```powershell
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
aws rds create-db-snapshot `
  --db-instance-identifier taskactivity-db `
  --db-snapshot-identifier "taskactivity-db-before-restore-$timestamp"
```

This captures the current empty state in case you need to roll back.

## Step 3: Restore Snapshot to Temporary Instance

Create a NEW temporary database instance from the old snapshot:

```powershell
# Replace the snapshot identifier with your chosen snapshot
aws rds restore-db-instance-from-db-snapshot `
  --db-instance-identifier taskactivity-restored-old `
  --db-snapshot-identifier rds:taskactivity-db-2025-10-23-03-08 `
  --db-instance-class db.t3.micro `
  --vpc-security-group-ids sg-08f4bdf0f4619d2e0 `
  --db-subnet-group-name default `
  --publicly-accessible
```

**Important Parameters:**

-   `--db-instance-identifier`: Name for the temporary restored instance (must be unique)
-   `--db-snapshot-identifier`: The snapshot you identified in Step 1
-   `--publicly-accessible`: Required so you can connect from your local machine for migration

**Wait for completion (10-30 minutes):**

```powershell
# Monitor restore progress
aws rds describe-db-instances `
  --db-instance-identifier taskactivity-restored-old `
  --query "DBInstances[0].[DBInstanceStatus,Endpoint.Address]" `
  --output table
```

Status should progress: `creating` → `modifying` → `available`

## Step 4: Make Databases Publicly Accessible

Both the restored instance and your current instance need to be publicly accessible for the Python migration script to connect.

### Make Restored Instance Public (if not already)

```powershell
aws rds modify-db-instance `
  --db-instance-identifier taskactivity-restored-old `
  --publicly-accessible `
  --apply-immediately
```

### Make Current Instance Public

```powershell
aws rds modify-db-instance `
  --db-instance-identifier taskactivity-db `
  --publicly-accessible `
  --apply-immediately
```

**Wait for modifications to complete (1-2 minutes):**

```powershell
# Check both instances are available and public
aws rds describe-db-instances `
  --db-instance-identifier taskactivity-restored-old `
  --query "DBInstances[0].[DBInstanceStatus,PubliclyAccessible]" `
  --output table

aws rds describe-db-instances `
  --db-instance-identifier taskactivity-db `
  --query "DBInstances[0].[DBInstanceStatus,PubliclyAccessible]" `
  --output table
```

Both should show: `available` and `True`

## Step 5: Flush DNS Cache

Windows may cache old DNS entries. Flush the cache to ensure Python can resolve the new endpoints:

```cmd
ipconfig /flushdns
```

## Step 6: Run the Data Migration Script

The Python script (`scripts/migrate_data.py`) handles:

-   Connecting to both databases
-   Reading from old schema (`dbo."Users"`, `dbo."TaskActivity"`, `dbo."DropdownValues"`)
-   Transforming column names (`RECID`→`id`, `password`→`userpassword`, `role`→`userrole`, etc.)
-   Providing defaults for missing columns (`lastname`, `firstname`, `company`)
-   Writing to new schema (`public.users`, `public.taskactivity`, `public.dropdownvalues`)

### Update Connection Details

Before running, verify the endpoints in `scripts/migrate_data.py`:

```python
SOURCE_DB = {
    'host': 'taskactivity-restored-old.cuhqge48qwm5.us-east-1.rds.amazonaws.com',
    'port': 5432,
    'database': 'AmmoP1DB',
    'user': 'postgres',
    'password': 'YourPasswordFromSecretsManager'
}

TARGET_DB = {
    'host': 'taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com',
    'port': 5432,
    'database': 'AmmoP1DB',
    'user': 'postgres',
    'password': 'YourPasswordFromSecretsManager'
}
```

Get the actual endpoints:

```powershell
# Get restored instance endpoint
aws rds describe-db-instances `
  --db-instance-identifier taskactivity-restored-old `
  --query "DBInstances[0].Endpoint.Address" `
  --output text

# Get current instance endpoint
aws rds describe-db-instances `
  --db-instance-identifier taskactivity-db `
  --query "DBInstances[0].Endpoint.Address" `
  --output text

# Get password from Secrets Manager
aws secretsmanager get-secret-value `
  --secret-id taskactivity-db-secret `
  --query "SecretString" `
  --output text
```

### Install Python Dependencies

```powershell
pip install psycopg2-binary
```

### Run Migration

```powershell
cd C:\Users\deana\GitHub\ActivityTracking
$env:PYTHONIOENCODING="utf-8"
python scripts\migrate_data.py
```

**Expected Output:**

```
============================================================
AWS RDS Data Migration Script
From: Old Schema (dbo.*) -> New Schema (public.*)
============================================================

Connecting to SOURCE database (restored instance)...
  ✓ Connected to restored database

Connecting to TARGET database (current instance)...
  ✓ Connected to current database

[1/3] Migrating Users table...
  Found 3 users in source database
  ✓ Migrated 3 users

[2/3] Migrating DropdownValues table...
  Found 12 dropdown values in source database
  ✓ Migrated 12 dropdown values

[3/3] Migrating TaskActivity table...
  Found 271 task activities in source database
  ✓ Migrated 271 task activities

============================================================
VERIFICATION - Record Counts:
============================================================
  Users:          3
  DropdownValues: 12
  TaskActivity:   271
============================================================

MIGRATION COMPLETE!
============================================================
  Users:          3 migrated (3 total)
  DropdownValues: 12 migrated (12 total)
  TaskActivity:   271 migrated (271 total)
============================================================

✓ All data has been successfully migrated!
```

Test your application:

1. **Start the Spring Boot application:**

```powershell
cd C:\Users\deana\GitHub\ActivityTracking
.\mvnw.cmd spring-boot:run
```

2. **Access the application:**

    - Navigate to http://localhost:8080
    - Log in with one of the migrated users
    - Verify task activities are visible
    - Check dropdown values are working

3. **Query the database directly (optional):**

```python
# Quick verification script
import psycopg2

conn = psycopg2.connect(
    host='taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com',
    port=5432,
    database='AmmoP1DB',
    user='postgres',
    password='YourPassword'
)

cur = conn.cursor()

# Count records
cur.execute("SELECT COUNT(*) FROM public.users")
print(f"Users: {cur.fetchone()[0]}")

cur.execute("SELECT COUNT(*) FROM public.dropdownvalues")
print(f"Dropdown Values: {cur.fetchone()[0]}")

cur.execute("SELECT COUNT(*) FROM public.taskactivity")
print(f"Task Activities: {cur.fetchone()[0]}")

# Check recent tasks
cur.execute("SELECT username, taskdate, client, project, taskhours FROM public.taskactivity ORDER BY taskdate DESC LIMIT 5")
print("\nRecent tasks:")
for row in cur.fetchall():
    print(f"  {row}")

conn.close()
```

## Step 8: Clean Up Restored Instance

**IMPORTANT:** Delete the temporary restored instance to avoid ongoing AWS costs (~$13/month for db.t3.micro).

```powershell
# Delete the restored instance (no snapshot needed - we have the original)
aws rds delete-db-instance `
  --db-instance-identifier taskactivity-restored-old `
  --skip-final-snapshot
```

Verify deletion:

```powershell
aws rds describe-db-instances `
  --db-instance-identifier taskactivity-restored-old `
  --query "DBInstances[0].DBInstanceStatus"
```

Should show `deleting` then eventually the instance won't be found.

## Step 9: Restore Production Security Settings

Make your production database private again:

```powershell
aws rds modify-db-instance `
  --db-instance-identifier taskactivity-db `
  --no-publicly-accessible `
  --apply-immediately
```

Verify:

```powershell
aws rds describe-db-instances `
  --db-instance-identifier taskactivity-db `
  --query "DBInstances[0].PubliclyAccessible"
```

Should return `False`.

## Step 10: Update User Profiles (Optional)

The migration script sets `lastname` to the username as a temporary value. Update user profiles with real names:

1. Log into the application as admin
2. Go to user management
3. Update first name and last name for each user
4. Save changes

Alternatively, update directly in the database if you have the information.

## Troubleshooting

### Issue: "Could not translate host name to address"

**Solution:**

1. Flush DNS cache: `ipconfig /flushdns`
2. Wait 2-3 minutes for DNS propagation after making instance publicly accessible
3. Verify instance is publicly accessible: Check AWS Console or use `aws rds describe-db-instances`

### Issue: "Access Denied" when running AWS CLI commands

**Solution:** Add missing IAM permissions (see Required IAM Permissions section at top)

### Issue: "null value in column violates not-null constraint"

**Solution:** The restored database has an older schema missing required columns. The migration script should handle this automatically by providing defaults. If you see this, verify you're using the updated `scripts/migrate_data.py` that includes default value logic.

### Issue: "Foreign key constraint violated"

**Solution:** This happens if users fail to migrate first. The migration script runs in order (Users → DropdownValues → TaskActivity). Check the Users migration completed successfully before TaskActivity migration runs.

### Issue: "current transaction is aborted"

**Solution:** This cascading error occurs when the first insert fails. Check the very first error message in the output - that's the root cause. Usually it's a schema mismatch or missing column issue.

### Issue: Migration shows 0 records migrated

**Solution:**

1. Check you selected a snapshot from BEFORE the schema changes
2. Verify the restored instance has data: Run `scripts/check_columns.py` to inspect the schema
3. Ensure the migration script has correct source/target hostnames

### Issue: Snapshot doesn't exist or is from after schema changes

**Solution:**

1. Check backup retention period: `aws rds describe-db-instances --db-instance-identifier taskactivity-db --query "DBInstances[0].BackupRetentionPeriod"`
2. If backups are disabled or too old, you may need to restore from a manual snapshot if one exists
3. In worst case, data may be unrecoverable if no backups exist from before the incident

## Prevention for Future

### 1. Use Database Migration Tools

Instead of relying on Hibernate's `ddl-auto`, use a proper migration tool:

**Flyway** (Recommended):

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Create versioned migration files: `src/main/resources/db/migration/V1__initial_schema.sql`, `V2__add_lastname.sql`, etc.

**Liquibase** (Alternative):

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

### 2. Set Hibernate to Validate Mode in Production

**CRITICAL:** Never use `ddl-auto=update` in production!

```properties
# application-prod.properties
spring.jpa.hibernate.ddl-auto=validate
spring.sql.init.mode=never
```

This prevents Hibernate from making schema changes. It will only verify the schema matches your entities.

### 3. Manual Snapshots Before Deployments

Create manual snapshots before any deployment that touches the database:

```powershell
# Before deployment
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
aws rds create-db-snapshot `
  --db-instance-identifier taskactivity-db `
  --db-snapshot-identifier "pre-deployment-$timestamp"
```

Add this to your deployment checklist.

### 4. Enable Automated Backups

Ensure automated backups are enabled with adequate retention:

```powershell
# Set 7-day retention (AWS default)
aws rds modify-db-instance `
  --db-instance-identifier taskactivity-db `
  --backup-retention-period 7 `
  --apply-immediately
```

For critical production systems, consider 14-30 day retention.

### 5. Test Schema Changes in Staging First

1. Create a staging database with production-like data
2. Test schema migrations in staging
3. Verify application works with new schema
4. Only then deploy to production

### 6. Database Change Management Process

Implement a formal change process:

1. **Plan:** Write migration scripts in advance
2. **Review:** Peer review all schema changes
3. **Backup:** Create manual snapshot before deployment
4. **Test:** Run in staging environment first
5. **Execute:** Apply changes during maintenance window
6. **Verify:** Confirm data integrity post-deployment
7. **Monitor:** Watch for errors in the hours after deployment

## Schema Transformation Reference

This migration handles the following transformations:

### Table Names

-   `dbo."Users"` → `public.users`
-   `dbo."DropdownValues"` → `public.dropdownvalues`
-   `dbo."TaskActivity"` → `public.taskactivity`

### Column Name Transformations

**Users Table:**

-   `RECID` → `id` (already changed in this backup)
-   `password` → `userpassword`
-   `role` → `userrole`
-   `forcePasswordUpdate` → `forcepasswordupdate`
-   Added: `firstname` (defaults to empty string)
-   Added: `lastname` (defaults to username temporarily)
-   Added: `company` (defaults to empty string)

**DropdownValues Table:**

-   `RECID` → `id`
-   `value` → `itemvalue`
-   `displayOrder` → `displayorder`
-   `isActive` → `isactive`

**TaskActivity Table:**

-   `RECID` → `id`
-   `taskDate` → `taskdate`
-   `hours` → `taskhours`
-   Column structure: `client`, `project`, `phase`, `details` (unchanged)

## Cost Estimate

**Temporary Restored Instance:**

-   db.t3.micro: ~$0.017/hour
-   For 2 hours: ~$0.034

**Data Transfer:**

-   Minimal (within same region/AZ)
-   Estimated: $0.00-$0.01

**Total Estimated Cost: $0.05-$0.50**

## Summary Checklist

-   [ ] Add required IAM permissions to your user
-   [ ] Identify snapshot from BEFORE schema changes
-   [ ] Create safety snapshot of current state
-   [ ] Restore old snapshot to temporary instance (10-30 min)
-   [ ] Make both databases publicly accessible
-   [ ] Flush DNS cache
-   [ ] Update `scripts/migrate_data.py` with correct endpoints and password
-   [ ] Install Python dependencies (`pip install psycopg2-binary`)
-   [ ] Run migration script (`python scripts/migrate_data.py`)
-   [ ] Verify migration completed successfully (check counts)
-   [ ] Test application functionality
-   [ ] Delete temporary restored instance
-   [ ] Make production database private again
-   [ ] Update user profiles with real names (optional)
-   [ ] Implement prevention measures (Flyway, ddl-auto=validate, etc.)

## Support Files

These files support the restore process:

-   `scripts/restore-aws-database-simple.ps1` - Interactive restore wizard
-   `scripts/migrate_data.py` - Data migration with schema transformation
-   `scripts/check_schema.py` - Inspect database schema
-   `scripts/check_columns.py` - Verify column names and counts
-   `scripts/check_all_tables.py` - List all tables and structure

All scripts are located in the `scripts/` directory of this repository.

---

**Document Version:** 2.0  
**Last Updated:** October 24, 2025  
**Last Tested:** October 24, 2025 - Successfully recovered 3 users, 12 dropdown values, 271 task activities  
**Author:** Automated recovery procedure based on production incident response
