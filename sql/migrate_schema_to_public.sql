-- Migration script to copy data from old schema to new schema
-- Run this ONLY if old tables (dbo."Users", dbo."TaskActivity", dbo."DropdownValues") still exist
-- Check first with: SELECT * FROM information_schema.tables WHERE table_schema = 'dbo';

-- ===================================================================
-- STEP 1: Check if old tables exist
-- ===================================================================
-- Run this query first to see if old tables are still there:
-- SELECT table_schema, table_name FROM information_schema.tables 
-- WHERE table_schema IN ('dbo', 'public') 
-- ORDER BY table_schema, table_name;

-- ===================================================================
-- STEP 2: Migrate Users data (if old table exists)
-- ===================================================================
-- Only run this section if dbo."Users" table exists

INSERT INTO public.users 
    (id, username, firstname, lastname, company, userpassword, userrole, enabled, forcepasswordupdate, created_date, last_login)
SELECT 
    id, 
    username, 
    firstname, 
    lastname, 
    company, 
    password as userpassword,  -- Column renamed: password → userpassword
    role as userrole,          -- Column renamed: role → userrole
    enabled, 
    "forcePasswordUpdate" as forcepasswordupdate,  -- Column renamed: forcePasswordUpdate → forcepasswordupdate
    created_date, 
    last_login
FROM dbo."Users"
ON CONFLICT (username) DO NOTHING;  -- Skip if username already exists

-- Reset the users sequence to avoid ID conflicts
SELECT setval('public.users_id_seq', (SELECT MAX(id) FROM public.users));

-- ===================================================================
-- STEP 3: Migrate DropdownValues data (if old table exists)
-- ===================================================================
-- Only run this section if dbo."DropdownValues" table exists

INSERT INTO public.dropdownvalues 
    (id, category, itemvalue, displayorder, isactive)
SELECT 
    id, 
    category, 
    value as itemvalue,             -- Column renamed: value → itemvalue
    "displayOrder" as displayorder, -- Column renamed: displayOrder → displayorder
    "isActive" as isactive          -- Column renamed: isActive → isactive
FROM dbo."DropdownValues"
ON CONFLICT (category, itemvalue) DO NOTHING;  -- Skip if category+itemvalue already exists

-- Reset the dropdownvalues sequence to avoid ID conflicts
SELECT setval('public.dropdownvalues_id_seq', (SELECT MAX(id) FROM public.dropdownvalues));

-- ===================================================================
-- STEP 4: Migrate TaskActivity data (if old table exists)
-- ===================================================================
-- Only run this section if dbo."TaskActivity" table exists

INSERT INTO public.taskactivity 
    (id, taskdate, client, project, phase, taskhours, details, username)
SELECT 
    id, 
    "taskDate" as taskdate,  -- Column renamed: taskDate → taskdate
    client, 
    project, 
    phase, 
    hours as taskhours,      -- Column renamed: hours → taskhours
    details, 
    username
FROM dbo."TaskActivity"
ON CONFLICT (taskdate, client, project, phase, details, username) DO NOTHING;  -- Skip duplicates

-- Reset the taskactivity sequence to avoid ID conflicts
SELECT setval('public.taskactivity_id_seq', (SELECT MAX(id) FROM public.taskactivity));

-- ===================================================================
-- STEP 5: Verify migration
-- ===================================================================
-- Run these queries to verify data was migrated:

-- SELECT COUNT(*) as old_users FROM dbo."Users";
-- SELECT COUNT(*) as new_users FROM public.users;

-- SELECT COUNT(*) as old_dropdowns FROM dbo."DropdownValues";
-- SELECT COUNT(*) as new_dropdowns FROM public.dropdownvalues;

-- SELECT COUNT(*) as old_tasks FROM dbo."TaskActivity";
-- SELECT COUNT(*) as new_tasks FROM public.taskactivity;

-- ===================================================================
-- STEP 6: Optional cleanup (DANGEROUS - backup first!)
-- ===================================================================
-- Only run these after verifying all data is successfully migrated
-- and you have a backup!

-- DROP TABLE IF EXISTS dbo."TaskActivity" CASCADE;
-- DROP TABLE IF EXISTS dbo."DropdownValues" CASCADE;
-- DROP TABLE IF EXISTS dbo."Users" CASCADE;
-- DROP SCHEMA IF EXISTS dbo CASCADE;
