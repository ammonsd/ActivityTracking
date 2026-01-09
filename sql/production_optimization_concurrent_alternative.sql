/**
 * Description: Production Database Concurrent Index Creation - alternative to production_optimization.sql for zero-downtime index creation
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

-- ALTERNATIVE: Production Database Concurrent Index Creation
-- Use this INSTEAD of production_optimization.sql if you need zero-downtime index creation
-- These commands must be run individually (NOT in a transaction block)

-- STEP 1: Run each index command separately (one at a time):

-- Command 1:
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_task_activity_user_date 
ON public.taskactivity (username, taskdate);

-- Command 2:
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_task_activity_date_range 
ON public.taskactivity (taskdate) 
WHERE taskdate >= CURRENT_DATE - INTERVAL '30 days';

-- Command 3:
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username_active 
ON public.users (username) 
WHERE enabled = true;

-- Note: Run each CREATE INDEX CONCURRENTLY command one at a time
-- Do not run them as a batch/script if your tool wraps them in a transaction

-- STEP 2: After all indexes are created, run the optimization settings:
ANALYZE public.taskactivity;
ANALYZE public.users;
ANALYZE public.dropdownvalues;

ALTER TABLE public.taskactivity SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE public.taskactivity SET (autovacuum_analyze_scale_factor = 0.05);