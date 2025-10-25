-- Data Migration Script: Old Schema to New Schema
-- This script copies data from the restored database (old schema) to current database (new schema)
-- 
-- INSTRUCTIONS:
-- 1. Connect to your CURRENT database: taskactivity-db.cuhqge48qwm5.us-east-1.rds.amazonaws.com
-- 2. Database: AmmoP1DB, User: postgres
-- 3. Run this script

-- Connection string for restored database
\set RESTORED_DB 'host=restore-from-2025-10-23.cuhqge48qwm5.us-east-1.rds.amazonaws.com port=5432 dbname=AmmoP1DB user=postgres password=TaskActivity2025!SecureDB'

-- Step 1: Install dblink extension (if needed)
CREATE EXTENSION IF NOT EXISTS dblink;

-- Step 2: Test connection to restored database
SELECT dblink_connect('restored_db', :'RESTORED_DB');
SELECT 'Connection successful!' as status;
SELECT dblink_disconnect('restored_db');

-- Step 3: Migrate Users table
-- Transforms: password -> userpassword, role -> userrole, forcePasswordUpdate -> forcepasswordupdate
SELECT dblink_connect('restored_db', :'RESTORED_DB');

INSERT INTO public.users (id, username, userpassword, userrole, forcepasswordupdate)
SELECT * FROM dblink('restored_db',
    'SELECT "RECID", "username", "password", "role", "forcePasswordUpdate" FROM dbo."Users"'
) AS t(id bigint, username varchar(100), userpassword varchar(255), userrole varchar(50), forcepasswordupdate boolean)
ON CONFLICT (id) DO NOTHING;

SELECT 'Users migrated: ' || COUNT(*) as migration_status FROM public.users;

-- Step 4: Migrate DropdownValues table
-- Transforms: RECID -> id, value -> itemvalue, displayOrder -> displayorder, isActive -> isactive
INSERT INTO public.dropdownvalues (id, category, itemvalue, displayorder, isactive)
SELECT * FROM dblink('restored_db',
    'SELECT "RECID", "category", "value", "displayOrder", "isActive" FROM dbo."DropdownValues"'
) AS t(id bigint, category varchar(50), itemvalue varchar(100), displayorder int, isactive boolean)
ON CONFLICT (id) DO NOTHING;

SELECT 'DropdownValues migrated: ' || COUNT(*) as migration_status FROM public.dropdownvalues;

-- Step 5: Migrate TaskActivity table
-- Transforms: RECID -> id, taskDate -> taskdate, hours -> taskhours
INSERT INTO public.taskactivity (id, username, taskdate, category, subcategory, taskhours, notes)
SELECT * FROM dblink('restored_db',
    'SELECT "RECID", "username", "taskDate", "category", "subcategory", "hours", "notes" FROM dbo."TaskActivity"'
) AS t(id bigint, username varchar(100), taskdate date, category varchar(50), subcategory varchar(100), taskhours numeric(5,2), notes text)
ON CONFLICT (id) DO NOTHING;

SELECT 'TaskActivity records migrated: ' || COUNT(*) as migration_status FROM public.taskactivity;

-- Step 6: Disconnect
SELECT dblink_disconnect('restored_db');

-- Step 7: Verify record counts
SELECT 'Verification - Record Counts:' as status;
SELECT 'Users: ' || COUNT(*) as record_count FROM public.users;
SELECT 'DropdownValues: ' || COUNT(*) as record_count FROM public.dropdownvalues;
SELECT 'TaskActivity: ' || COUNT(*) as record_count FROM public.taskactivity;
