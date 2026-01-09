/**
 * Description: Production Database Optimization Script - creates indexes to optimize database performance after deploying to production
 *
 * Author: Dean Ammons
 * Date: October 2025
 */

-- Production Database Optimization Script
-- Run this after deploying to production

-- OPTION 1: Standard indexes (can run in transaction, will lock tables briefly)
CREATE INDEX IF NOT EXISTS idx_task_activity_user_date 
on public.taskactivity (username, taskdate);

-- Simple date index for range queries (no WHERE clause to avoid immutable function issue)
CREATE INDEX IF NOT EXISTS idx_task_activity_date 
on public.taskactivity (taskdate);

CREATE INDEX IF NOT EXISTS idx_users_username_active 
ON public.users (username) 
WHERE enabled = true;

-- Update table statistics
ANALYZE public.taskactivity;
ANALYZE public.users;
ANALYZE public.dropdownvalues;

-- Enable auto-vacuum for performance
ALTER TABLE public.taskactivity SET (autovacuum_vacuum_scale_factor = 0.1);
ALTER TABLE public.taskactivity SET (autovacuum_analyze_scale_factor = 0.05);

-- Connection pool optimization settings
-- These should be set at PostgreSQL instance level:
-- max_connections = 200
-- shared_buffers = 256MB
-- effective_cache_size = 1GB
-- work_mem = 4MB
-- maintenance_work_mem = 64MB
-- checkpoint_completion_target = 0.9
-- wal_buffers = 16MB
-- default_statistics_target = 100